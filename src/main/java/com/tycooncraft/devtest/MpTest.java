package com.tycooncraft.devtest;

import com.tycooncraft.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** CI 전용 멀티플레이 시험 (전용 서버 쪽): 접속한 사람에게 드론을 쥐여 주고, 접속 끊김 사유를 기록 */
public final class MpTest {
    public static final boolean ON = Boolean.getBoolean("tycooncraft.mptest");
    static int ticks;

    public static void init() {
        if (ON) NeoForge.EVENT_BUS.register(MpTest.class);
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        sp.setGameMode(GameType.SURVIVAL);
        sp.getInventory().clearContent();
        for (int i = 0; i < 4; i++) sp.getInventory().setItem(i, new ItemStack(ModItems.DRONE.get()));
        sp.getInventory().setItem(4, new ItemStack(ModItems.DRONE_MK2.get()));
        sp.getInventory().selected = 0;
        sp.teleportTo(sp.serverLevel(), 0.5, sp.serverLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0), 0.5, 0, 10);
        System.out.println("[MPTEST] server join " + sp.getGameProfile().getName());
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent e) {
        System.out.println("[MPTEST] server logout " + e.getEntity().getGameProfile().getName() + " detonations=" + com.tycooncraft.vehicle.DroneEntity.DETONATIONS);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (++ticks % 200 == 0)
            System.out.println("[MPTEST] server tick=" + ticks + " players=" + e.getServer().getPlayerCount() + " detonations=" + com.tycooncraft.vehicle.DroneEntity.DETONATIONS);
    }

    private MpTest() {}
}
