package com.tycooncraft.blackmarket;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.registry.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * v0.26.0 블랙마켓: 맨홀로 들어가는 전용 차원.
 * - 출입증으로 첫 입장 시 영구 등록, 나올 땐 들어온 맨홀로 돌아감
 * - 안에서는 건축·파괴 불가(크리에이티브 제외), 싸우면 암상인들이 무장 대응
 * - 암상인을 죽이면 '추적 V' 1시간 (지울 수 없음, 죽어도 다시 걸림)
 */
@EventBusSubscriber(modid = TycoonCraft.MODID)
public final class BlackMarket {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, TycoonCraft.id("black_market"));
    public static final BlockPos SPAWN = new BlockPos(-13, 64, 0);
    public static final String REG = "tycooncraft_bm_member", RET = "tycooncraft_bm_return", TRACKED_UNTIL = "tycooncraft_tracked_until";
    public static final int TRACKED_TICKS = 72_000, TRACKED_AMP = 4, HOSTILE_TICKS = 1200;
    /** 암시장에서 싸운 사람 → 상인들이 노리는 기한(게임 시각) */
    private static final Map<UUID, Long> HOSTILE = new HashMap<>();
    public static int entries, fights, dealerKills;

    public static boolean isBM(Level level) {
        return level.dimension() == DIM;
    }

    public static boolean member(Player p) {
        return persisted(p).getBoolean(REG);
    }

    static CompoundTag persisted(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains(Player.PERSISTED_NBT_TAG)) root.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return root.getCompound(Player.PERSISTED_NBT_TAG);
    }

    /** 맨홀 → 암시장 */
    public static boolean enter(ServerPlayer p, BlockPos manhole) {
        ServerLevel bm = p.server.getLevel(DIM);
        if (bm == null) return false;
        CompoundTag t = new CompoundTag();
        t.putString("dim", p.level().dimension().location().toString());
        t.putInt("x", manhole.getX());
        t.putInt("y", manhole.getY() + 1);
        t.putInt("z", manhole.getZ());
        persisted(p).put(RET, t);
        BlackMarketHub.ensure(bm);
        p.stopRiding();
        p.teleportTo(bm, SPAWN.getX() + 0.5, SPAWN.getY(), SPAWN.getZ() + 0.5, -90f, 0f);
        bm.playSound(null, SPAWN, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.8f);
        p.displayClientMessage(Component.translatable("message.tycooncraft.bm.welcome").withStyle(ChatFormatting.DARK_PURPLE), true);
        p.sendSystemMessage(Component.translatable("message.tycooncraft.rep.status", Reputation.describe(p)).withStyle(ChatFormatting.DARK_PURPLE));
        p.sendSystemMessage(Component.translatable("message.tycooncraft.undercity.enter").withStyle(ChatFormatting.DARK_RED));
        entries++;
        return true;
    }

    /** 암시장 → 들어왔던 맨홀 */
    public static void leave(ServerPlayer p) {
        CompoundTag t = persisted(p).getCompound(RET);
        ServerLevel dst = null;
        BlockPos pos;
        if (t.contains("dim")) {
            dst = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(t.getString("dim"))));
            pos = new BlockPos(t.getInt("x"), t.getInt("y"), t.getInt("z"));
        } else pos = null;
        if (dst == null || pos == null) {
            dst = p.server.overworld();
            pos = dst.getSharedSpawnPos();
        }
        p.teleportTo(dst, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, p.getYRot(), 0f);
        dst.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
    }

    public static boolean hostile(Player p) {
        Long until = HOSTILE.get(p.getUUID());
        return until != null && until > p.level().getGameTime() && !p.isCreative() && !p.isSpectator();
    }

    public static void clearHostile(Player p) {
        HOSTILE.remove(p.getUUID());
    }

    // ── 보호: 건축·파괴·폭발 금지 ──
    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getLevel() instanceof Level l && isBM(l) && !e.getPlayer().isCreative()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent e) {
        if (e.getLevel() instanceof Level l && isBM(l) && !(e.getEntity() instanceof Player p && p.isCreative())) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplode(ExplosionEvent.Detonate e) {
        if (isBM(e.getLevel())) e.getAffectedBlocks().clear();
    }

    /** 암시장 안에서 누군가(플레이어·상인)를 공격하면 상인들이 그 사람을 처리 */
    @SubscribeEvent
    public static void onHurt(LivingIncomingDamageEvent e) {
        LivingEntity v = e.getEntity();
        if (v.level().isClientSide || !isBM(v.level())) return;
        if (!(e.getSource().getEntity() instanceof ServerPlayer p) || p == v || p.isCreative()) return;
        if (!(v instanceof Player) && !(v instanceof BlackMarketDealer)) return;
        if (!Undercity.inHub(v.getX(), v.getZ())) return;   // v0.29: 골목 밖 언더시티는 무법지대 (상인이 끼어들지 않음)
        boolean was = hostile(p);
        HOSTILE.put(p.getUUID(), p.level().getGameTime() + HOSTILE_TICKS);
        if (!was) {
            fights++;
            Reputation.add(p, Reputation.FIGHT);
            p.displayClientMessage(Component.translatable("message.tycooncraft.bm.fight").withStyle(ChatFormatting.RED), true);
            for (BlackMarketDealer d : p.level().getEntitiesOfClass(BlackMarketDealer.class, p.getBoundingBox().inflate(64))) d.setTarget(p);
        }
    }

    /** 상인 처치 → 추적 V (1시간) */
    public static void onDealerKilled(ServerPlayer killer) {
        dealerKills++;
        Reputation.add(killer, Reputation.DEALER_KILL);
        long until = killer.level().getGameTime() + TRACKED_TICKS;
        persisted(killer).putLong(TRACKED_UNTIL, until);
        applyTracked(killer);
        killer.sendSystemMessage(Component.translatable("message.tycooncraft.bm.tracked").withStyle(ChatFormatting.DARK_RED));
    }

    public static void applyTracked(ServerPlayer p) {
        long left = persisted(p).getLong(TRACKED_UNTIL) - p.level().getGameTime();
        if (left <= 0) return;
        p.addEffect(new MobEffectInstance(ModEffects.TRACKED, (int) Math.min(Integer.MAX_VALUE, left), TRACKED_AMP, false, true, true));
    }

    public static boolean tracked(Player p) {
        return p.hasEffect(ModEffects.TRACKED);
    }

    /** 재장전 20% 느려짐 */
    public static int reloadTicks(Player p, int ticks) {
        return tracked(p) ? Math.round(ticks * 1.2f) : ticks;
    }

    /** 우유·토템·명령어 등으로 지워지지 않음 (기한 남았으면) */
    @SubscribeEvent
    public static void onRemoveEffect(MobEffectEvent.Remove e) {
        if (e.getEffect() == ModEffects.TRACKED && e.getEntity() instanceof Player p && !p.level().isClientSide
                && persisted(p).getLong(TRACKED_UNTIL) > p.level().getGameTime()) e.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            applyTracked(p);
            clearHostile(p);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) applyTracked(p);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        MinecraftServer s = e.getServer();
        if (s.getTickCount() % 100 == 0) {
            ServerLevel bm = s.getLevel(DIM);
            if (bm != null && !bm.players().isEmpty()) {
                BlackMarketHub.tickDealers(bm);
                Undercity.recall(bm);
                Undercity.tick(bm);
            }
        }
        GangHunters.tick(s);
        Missions.tick(s);
    }

    private BlackMarket() {}
}
