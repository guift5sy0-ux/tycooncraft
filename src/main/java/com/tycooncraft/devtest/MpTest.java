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
    static int ticks, joinTick = -1;
    static net.minecraft.core.BlockPos village, ruin;
    static ServerPlayer tester;

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
        tester = sp;
        joinTick = ticks;
        // 자연 생성 구조물 확인: 가장 가까운 상인 마을·폐공장을 찾아 실제로 생성시키고 내용물 확인
        var level = sp.serverLevel();
        village = level.findNearestMapStructure(com.tycooncraft.world.WorldGen.structureTag("village"), sp.blockPosition(), 100, false);
        ruin = level.findNearestMapStructure(com.tycooncraft.world.WorldGen.structureTag("ruin"), sp.blockPosition(), 100, false);
        System.out.println("[MPTEST] structures near spawn village=" + village + " ruin=" + ruin);
        inspect(level, "village", village, 32, 32);
        inspect(level, "ruin", ruin, 22, 14);
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent e) {
        System.out.println("[MPTEST] server logout " + e.getEntity().getGameProfile().getName() + " detonations=" + com.tycooncraft.vehicle.DroneEntity.DETONATIONS);
    }

    /** 구조물 주변 청크를 생성시키고 상인·스포너·상자 수를 센다 */
    static void inspect(net.minecraft.server.level.ServerLevel level, String kind, net.minecraft.core.BlockPos at, int sx, int sz) {
        if (at == null) return;
        for (int cx = (at.getX() >> 4) - 3; cx <= (at.getX() >> 4) + 3; cx++)
            for (int cz = (at.getZ() >> 4) - 3; cz <= (at.getZ() >> 4) + 3; cz++) level.getChunk(cx, cz);
        var box = new net.minecraft.world.phys.AABB(at).inflate(40, 40, 40);
        int traders = level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, box, v -> v.hasCustomName()).size();
        int mobs = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, box).size();
        int spawners = 0, chests = 0, machines = 0;
        int y0 = at.getY() - 20, y1 = at.getY() + 40;
        for (var p : net.minecraft.core.BlockPos.betweenClosed(at.getX() - 40, y0, at.getZ() - 40, at.getX() + 40, y1, at.getZ() + 40)) {
            var s = level.getBlockState(p);
            if (s.is(net.minecraft.world.level.block.Blocks.SPAWNER)) spawners++;
            else if (s.is(net.minecraft.world.level.block.Blocks.CHEST)) chests++;
            else if (s.getBlock() instanceof com.tycooncraft.block.MachineBlock || s.is(com.tycooncraft.registry.ModBlocks.BROKEN_MACHINE.get())) machines++;
        }
        int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        System.out.println("[MPTEST] " + kind + " at " + at.getX() + "," + at.getZ() + " groundY=" + ground + " traders=" + traders
                + " monsters=" + mobs + " spawners=" + spawners + " chests=" + chests + " machines=" + machines);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        // 클라이언트 촬영용: 드론 시험이 끝난 뒤 마을·폐공장 위로 보낸다
        if (tester != null && joinTick >= 0) {
            int dt = ticks - joinTick;
            if (dt == 1300 && village != null) view(village, 16);
            if (dt == 1500 && ruin != null) view(ruin, 11);
        }
        if (++ticks % 200 == 0)
            System.out.println("[MPTEST] server tick=" + ticks + " players=" + e.getServer().getPlayerCount() + " detonations=" + com.tycooncraft.vehicle.DroneEntity.DETONATIONS);
    }

    static void view(net.minecraft.core.BlockPos at, int half) {
        var level = tester.serverLevel();
        int gy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        tester.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        double x = at.getX() + 22, z = at.getZ() + 26, y = gy + 18;
        float yaw = (float) Math.toDegrees(Math.atan2(-(at.getX() - x), at.getZ() - z));
        tester.teleportTo(level, x, y, z, yaw, 35f);
    }

    private MpTest() {}
}
