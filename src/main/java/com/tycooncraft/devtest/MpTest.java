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
    static net.minecraft.core.BlockPos village, ruin, hideout;
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
        hideout = level.findNearestMapStructure(com.tycooncraft.world.WorldGen.structureTag("hideout"), sp.blockPosition(), 100, false);
        System.out.println("[MPTEST] structures near spawn village=" + village + " ruin=" + ruin + " hideout=" + hideout);
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent e) {
        System.out.println("[MPTEST] server logout " + e.getEntity().getGameProfile().getName() + " detonations=" + com.tycooncraft.vehicle.DroneEntity.DETONATIONS);
    }

    /** 구조물 주변 청크를 생성시키고 상인·스포너·상자 수를 센다 */
    static void inspect(net.minecraft.server.level.ServerLevel level, String kind, net.minecraft.core.BlockPos at, int sx, int sz) {
        if (at == null) return;
        int gy0 = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        at = new net.minecraft.core.BlockPos(at.getX(), gy0, at.getZ());
        var box = new net.minecraft.world.phys.AABB(at).inflate(40, 30, 40);
        int traders = level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, box, v -> v.hasCustomName()).size();
        int mobs = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, box).size();
        int villagers = level.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, box).size();
        int spawners = 0, chests = 0, machines = 0, exchanges = 0, lanterns = 0;
        int y0 = at.getY() - 12, y1 = at.getY() + 24;
        for (var p : net.minecraft.core.BlockPos.betweenClosed(at.getX() - 40, y0, at.getZ() - 40, at.getX() + 40, y1, at.getZ() + 40)) {
            var s = level.getBlockState(p);
            if (s.is(net.minecraft.world.level.block.Blocks.SPAWNER)) spawners++;
            else if (s.is(net.minecraft.world.level.block.Blocks.CHEST)) chests++;
            else if (s.getBlock() instanceof com.tycooncraft.block.MachineBlock || s.is(com.tycooncraft.registry.ModBlocks.BROKEN_MACHINE.get())) machines++;
            else if (s.is(com.tycooncraft.registry.ModBlocks.EXCHANGE.get())) exchanges++;
            else if (s.is(net.minecraft.world.level.block.Blocks.LANTERN)) lanterns++;
        }
        int ground = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        System.out.println("[MPTEST] tradersSpawnedOnLoad=" + com.tycooncraft.world.gen.VillageTraders.SPAWNED + " existing=" + com.tycooncraft.world.gen.VillageTraders.EXISTING
                + " entitiesLoaded=" + level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(at).toLong()));
        System.out.println("[MPTEST] " + kind + " at " + at.getX() + "," + at.getZ() + " groundY=" + ground + " traders=" + traders
                + " villagers=" + villagers + " monsters=" + mobs + " spawners=" + spawners + " chests=" + chests + " machines=" + machines
                + " exchanges=" + exchanges + " lanterns=" + lanterns);
        for (var en : level.getEntities((net.minecraft.world.entity.Entity) null, box, x -> !(x instanceof net.minecraft.world.entity.player.Player)))
            if (en instanceof net.minecraft.world.entity.npc.Villager || en.hasCustomName())
                System.out.println("[MPTEST]   entity " + en.getType().toShortString() + " name=" + en.getName().getString() + String.format(" at %.1f,%.1f,%.1f", en.getX(), en.getY(), en.getZ()));
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        // 클라이언트 촬영용: 드론 시험이 끝난 뒤 마을·폐공장 위로 보낸다
        if (tester != null && joinTick >= 0) {
            int dt = ticks - joinTick;
            if (dt == 1200 && village != null) view(village, 16);
            if (dt == 1320 || dt == 1380 || dt == 1440) inspect(tester.serverLevel(), "village", village, 64, 64);
            if (dt == 1500 && ruin != null) view(ruin, 11);
            if (dt == 1640) inspect(tester.serverLevel(), "ruin", ruin, 22, 14);
            if (dt == 1660) {
                var l = tester.serverLevel();
                int baux = 0, salt = 0;
                var c = tester.chunkPosition();
                for (int cx = c.x - 2; cx <= c.x + 2; cx++)
                    for (int cz = c.z - 2; cz <= c.z + 2; cz++) {
                        var ch = l.getChunk(cx, cz);
                        for (net.minecraft.core.BlockPos p : net.minecraft.core.BlockPos.betweenClosed(cx * 16, -60, cz * 16, cx * 16 + 15, 100, cz * 16 + 15)) {
                            var s = ch.getBlockState(p);
                            if (s.is(com.tycooncraft.registry.ModBlocks.BAUXITE_ORE.get()) || s.is(com.tycooncraft.registry.ModBlocks.DEEPSLATE_BAUXITE_ORE.get())) baux++;
                            else if (s.is(com.tycooncraft.registry.ModBlocks.ROCK_SALT_ORE.get())) salt++;
                        }
                    }
                System.out.println("[MPTEST] ores 5x5 chunks bauxite=" + baux + " rockSalt=" + salt);
            }
            if (dt == 1840 && hideout != null) {
                var l = tester.serverLevel();
                int gy = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hideout.getX(), hideout.getZ());
                var box = new net.minecraft.world.phys.AABB(hideout).inflate(40, 0, 40).setMinY(gy - 20).setMaxY(gy + 30);
                System.out.println("[MPTEST] hideout at " + hideout.getX() + "," + hideout.getZ() + " entitiesLoaded=" + l.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(hideout).toLong())
                        + " gunners=" + l.getEntitiesOfClass(com.tycooncraft.mob.GunnerEntity.class, box).size()
                        + " trucks=" + l.getEntitiesOfClass(com.tycooncraft.mob.GangTruck.class, box).size()
                        + " boss=" + l.getEntitiesOfClass(com.tycooncraft.mob.CartelBoss.class, box).size());
            }
        }
        if (++ticks % 200 == 0)
            System.out.println("[MPTEST] server tick=" + ticks + " players=" + e.getServer().getPlayerCount() + " detonations=" + com.tycooncraft.vehicle.DroneEntity.DETONATIONS);
    }

    static void view(net.minecraft.core.BlockPos at, int half) {
        var level = tester.serverLevel();
        level.getChunk(at.getX() >> 4, at.getZ() >> 4);      // 아직 없는 땅이면 이 청크만 먼저 생성해서 높이를 안다
        level.getChunk((at.getX() + 22) >> 4, (at.getZ() + 26) >> 4);
        int gy = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ());
        tester.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        double x = at.getX() + 22, z = at.getZ() + 26, y = gy + 18;
        float yaw = (float) Math.toDegrees(Math.atan2(-(at.getX() - x), at.getZ() - z));
        tester.teleportTo(level, x, y, z, yaw, 35f);
    }

    private MpTest() {}
}
