package com.tycooncraft.village;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** v0.25.0 마을 경비 수배 관리: 마을 안 총격 경고·수배, 주민 공격 즉시 수배 */
@EventBusSubscriber(modid = com.tycooncraft.TycoonCraft.MODID)
public final class GuardAlert {
    public static final int WARN_WINDOW = 300, WANTED_TICKS = 1200, NEAR = 48;
    private static final Map<UUID, Long> WARNED = new HashMap<>(), WANTED = new HashMap<>();
    public static int warnings, wantedCount, shotsFired;

    private static long now(Player p) {
        return p.level().getGameTime();
    }

    public static boolean wanted(Player p) {
        Long until = WANTED.get(p.getUUID());
        return until != null && until > now(p) && !p.isCreative() && !p.isSpectator();
    }

    private static List<VillageGuard> guardsNear(LivingEntity e) {
        return e.level().getEntitiesOfClass(VillageGuard.class, e.getBoundingBox().inflate(NEAR), g -> g.isAlive() && g.inVillage(e));
    }

    /** 플레이어가 총을 쐈음 (GunItem.fire 에서 부름) */
    public static void onGunfire(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator()) return;
        var guards = guardsNear(p);
        if (guards.isEmpty()) return;
        long t = now(p);
        if (wanted(p)) {
            WANTED.put(p.getUUID(), t + WANTED_TICKS);   // 계속 쏘면 수배 연장
            return;
        }
        Long w = WARNED.get(p.getUUID());
        if (w != null && t - w <= WARN_WINDOW) {
            makeWanted(p, guards);
        } else {
            WARNED.put(p.getUUID(), t);
            warnings++;
            p.displayClientMessage(Component.translatable("message.tycooncraft.guard.warn").withStyle(ChatFormatting.GOLD), true);
            p.playNotifySound(net.minecraft.sounds.SoundEvents.BELL_BLOCK, net.minecraft.sounds.SoundSource.NEUTRAL, 1f, 1.2f);
            for (VillageGuard g : guards) g.getLookControl().setLookAt(p, 60f, 60f);
        }
    }

    private static void makeWanted(Player p, List<VillageGuard> guards) {
        boolean already = wanted(p);
        WANTED.put(p.getUUID(), now(p) + WANTED_TICKS);
        WARNED.remove(p.getUUID());
        if (already) return;
        wantedCount++;
        p.displayClientMessage(Component.translatable("message.tycooncraft.guard.wanted").withStyle(ChatFormatting.RED), true);
        p.playNotifySound(net.minecraft.sounds.SoundEvents.RAID_HORN.value(), net.minecraft.sounds.SoundSource.NEUTRAL, 0.6f, 1.4f);
        for (VillageGuard g : guards) g.setTarget(p);
    }

    /** 주민·상인·경비병·골렘을 때리면 바로 수배 */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide) return;
        if (!(v instanceof Villager || v instanceof VillageGuard || v instanceof IronGolem)) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || p.isCreative()) return;
        var guards = guardsNear(v);
        if (!guards.isEmpty()) makeWanted(p, guards);
    }

    /** 죽으면 수배 해제 */
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof Player p) {
            WANTED.remove(p.getUUID());
            WARNED.remove(p.getUUID());
        }
    }

    public static void clear(Player p) {
        WANTED.remove(p.getUUID());
        WARNED.remove(p.getUUID());
    }

    private GuardAlert() {}
}
