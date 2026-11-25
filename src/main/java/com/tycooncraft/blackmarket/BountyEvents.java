package com.tycooncraft.blackmarket;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = TycoonCraft.MODID)
public final class BountyEvents {
    @SubscribeEvent
    public static void onDeath(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer victim) || !(e.getSource().getEntity() instanceof ServerPlayer killer) || killer == victim) return;
        if (com.tycooncraft.alliance.Alliances.friendly(victim.server, victim.getUUID(), killer.getUUID())) return;   // 같은 동맹끼리는 못 받음
        BountyData.claim(victim, killer);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        long t = BountyData.get(p.server).totalOn(p.getUUID());
        if (t > 0) p.sendSystemMessage(Component.translatable("message.tycooncraft.bounty.on_you", Coins.format(t)).withStyle(ChatFormatting.DARK_RED));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 1200 == 0) BountyData.expire(e.getServer());
    }

    private BountyEvents() {}
}
