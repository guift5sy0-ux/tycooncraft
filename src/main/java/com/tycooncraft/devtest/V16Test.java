package com.tycooncraft.devtest;

import com.tycooncraft.mob.GangTruck;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/** v0.16 자동 테스트: 갱단 무장 트럭 (승무원·추격·기관총·하차·파괴) */
public final class V16Test {
    static GangTruck truck;
    static LivingEntity target;
    public static GangTruck clientTruck;
    public static LivingEntity clientTarget;

    static void tick(ServerLevel level, int t, int gy) {
        if (t == 290) for (int cx = 8; cx <= 9; cx++) for (int cz = 6; cz <= 10; cz++) level.setChunkForced(cx, cz, true);
        if (t == 300) {
            truck = ModEntities.GANG_TRUCK.get().spawn(level, new BlockPos(140, gy, 104), MobSpawnType.MOB_SUMMONED);
            target = AutoTest.dummyAt(level, 140.5, 150.5);
            System.out.println("[AUTOTEST] v16 truck spawned=" + (truck != null) + " crew=" + (truck == null ? 0 : truck.getPassengers().size())
                    + (truck == null ? "" : " types=" + truck.getPassengers().stream().map(e -> e.getType().toShortString()).toList()));
        }
        if (t == 800) com.tycooncraft.world.StructureBuilder.hideout(level, new BlockPos(128, gy, 140));
        if (t == 840) inspectHideout(level, new BlockPos(128, gy, 140));
        if (truck == null) return;
        if (t == 302 || t == 330 || t == 380) diag("server t=" + t, level, truck);
        if (t > 300 && t < 720 && truck.isAlive() && target.isAlive()) truck.setTarget(target);
        if (t == 400 || t == 520 || t == 640)
            System.out.printf("[AUTOTEST] v16 truck t=%d pos=%.1f,%.1f dist=%.1f crew=%d shots=%d deploys=%d rams=%d targetHp=%.0f mgYaw=%.0f%n",
                    t, truck.getX(), truck.getZ(), truck.distanceTo(target), truck.getPassengers().size(), GangTruck.SHOTS, GangTruck.DEPLOYS,
                    GangTruck.RAMS, target.getHealth(), truck.mgYaw());
        if (t == 720 && truck.isAlive()) {
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            truck.hurt(level.damageSources().playerAttack(fp), 1000f);
        }
        if (t == 760) {
            int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(truck.blockPosition()).inflate(8)).size();
            System.out.println("[AUTOTEST] v16 truck destroyed=" + !truck.isAlive() + " drops=" + drops);
            truck = null;
        }
    }

    static java.util.List<net.minecraft.world.entity.Entity> crew0 = new java.util.ArrayList<>();
    public static void diag(String tag, ServerLevel level, GangTruck tr) {
        if (crew0.isEmpty()) crew0.addAll(tr.getPassengers());
        StringBuilder sb = new StringBuilder("[AUTOTEST] v16 diag " + tag + " removed=" + tr.getRemovalReason() + " ticks=" + tr.tickCount
                + " entTicking=" + level.isPositionEntityTicking(tr.blockPosition()) + " y=" + String.format("%.2f", tr.getY())
                + " inWall=" + tr.isInWall() + " noAi=" + tr.isNoAi() + " diff=" + level.getDifficulty() + " target=" + tr.getTarget() + " crew:");
        for (var e : crew0) sb.append(' ').append(e.getType().toShortString()).append(e.isPassenger() ? "@ride" : "@off").append('/').append(e.getRemovalReason())
                .append(e instanceof LivingEntity le ? "/hp" + (int) le.getHealth() : "");
        System.out.println(sb);
    }

    public static void inspectHideout(ServerLevel level, BlockPos o) {
        AABB box = new AABB(o).expandTowards(31, 12, 31).inflate(2);
        long gang = level.getEntitiesOfClass(com.tycooncraft.mob.GunnerEntity.class, box).size();
        long trucks = level.getEntitiesOfClass(GangTruck.class, box).size();
        long boss = level.getEntitiesOfClass(com.tycooncraft.mob.CartelBoss.class, box).size();
        int chests = 0, doors = 0, bars = 0;
        for (BlockPos p : BlockPos.betweenClosed(o.getX(), o.getY() - 1, o.getZ(), o.getX() + 31, o.getY() + 11, o.getZ() + 31)) {
            var s = level.getBlockState(p);
            if (s.is(net.minecraft.world.level.block.Blocks.CHEST)) chests++;
            else if (s.is(net.minecraft.world.level.block.Blocks.IRON_DOOR)) doors++;
            else if (s.is(net.minecraft.world.level.block.Blocks.IRON_BARS)) bars++;
        }
        System.out.println("[AUTOTEST] v16 hideout gunners=" + gang + " trucks=" + trucks + " boss=" + boss + " chests=" + chests + " ironDoor=" + doors + " bars=" + bars);
    }

    private V16Test() {}
}
