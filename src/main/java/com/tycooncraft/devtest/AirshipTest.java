package com.tycooncraft.devtest;

import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.ship.ShipAssembler;
import com.tycooncraft.ship.ShipEntity;
import com.tycooncraft.ship.ShipMotorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;

/** v0.16: 부유체 운송기 — 땅 위 조립, 호퍼 연료, 상승·전진, 연료 소진 시 하강, 정박 시 연료 반환 */
public final class AirshipTest {
    static ShipEntity ship;
    static double y0;

    /** 5×5 판자 바닥 + 조종기·엔진 2·부유체·호퍼(석탄). 반환: 조종기 위치 */
    public static BlockPos build(ServerLevel level, int x0, int y, int z0, int floats) {
        for (int x = x0 - 1; x <= x0 + 5; x++)
            for (int z = z0 - 1; z <= z0 + 5; z++) {
                level.setBlockAndUpdate(new BlockPos(x, y - 1, z), Blocks.GRASS_BLOCK.defaultBlockState());
                for (int dy = 0; dy < 8; dy++) level.setBlockAndUpdate(new BlockPos(x, y + dy, z), Blocks.AIR.defaultBlockState());
            }
        for (int x = x0; x <= x0 + 4; x++)
            for (int z = z0; z <= z0 + 4; z++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
        BlockPos helm = new BlockPos(x0 + 2, y + 1, z0 + 3);
        level.setBlockAndUpdate(helm, ModBlocks.SHIP_HELM.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        level.setBlockAndUpdate(new BlockPos(x0 + 1, y + 1, z0), ModBlocks.SHIP_MOTOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        level.setBlockAndUpdate(new BlockPos(x0 + 3, y + 1, z0), ModBlocks.SHIP_MOTOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH));
        for (int i = 0; i < floats; i++) {
            level.setBlockAndUpdate(new BlockPos(x0 + i * 4, y + 1, z0 + 2), ModBlocks.SHIP_FLOAT.get().defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x0 + i * 4, y + 2, z0 + 2), ModBlocks.SHIP_FLOAT.get().defaultBlockState());
        }
        BlockPos hop = new BlockPos(x0 + 4, y + 1, z0 + 4);
        level.setBlockAndUpdate(hop, Blocks.HOPPER.defaultBlockState());
        if (level.getBlockEntity(hop) instanceof HopperBlockEntity h) h.setItem(0, new ItemStack(Items.COAL, 5));
        level.setBlockAndUpdate(new BlockPos(x0, y + 1, z0 + 4), Blocks.SPRUCE_FENCE.defaultBlockState());
        return helm;
    }

    static void tick(ServerLevel level, int t, int gy) {
        int x0 = 130, z0 = 110;
        if (t == 900) {
            build(level, x0, gy, z0, 1);
            // 호퍼 → 엔진 (아이템 핸들러) 로 석탄 3개 주입
            var cap = level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, new BlockPos(x0 + 1, gy + 1, z0), Direction.UP);
            ItemStack rest = cap == null ? null : cap.insertItem(0, new ItemStack(Items.COAL, 3), false);
            ItemStack dirt = cap == null ? null : cap.insertItem(0, new ItemStack(Items.DIRT, 3), false);
            int f = level.getBlockEntity(new BlockPos(x0 + 1, gy + 1, z0)) instanceof ShipMotorBlockEntity m ? m.fuel() : -1;
            int[] st = ShipAssembler.structureFuel(level, new BlockPos(x0 + 3, gy + 1, z0));
            System.out.println("[AUTOTEST] v16 air motorCap=" + (cap != null) + " coalRest=" + (rest == null ? "-" : rest.getCount())
                    + " dirtRejected=" + (dirt == null ? "-" : dirt.getCount()) + " motorFuel=" + f
                    + " structure engines=" + st[0] + " fuel=" + st[1] + " floats=" + st[3] + " blocks=" + st[4]);
        }
        if (t == 905) {
            ship = ShipAssembler.assemble(level, new BlockPos(x0 + 2, gy + 1, z0 + 3), null);
            if (ship != null) {
                ship.autopilot = true;
                y0 = ship.getY();
            }
            System.out.println("[AUTOTEST] v16 air assembled=" + (ship != null) + (ship == null ? "" : " cells=" + ship.cells().size()
                    + " floats=" + ship.floatCount() + " canFly=" + ship.canFly() + " fuel=" + ship.fuel()));
        }
        if (ship == null || !ship.isAlive()) return;
        if (t > 905 && t < 1000) ship.testInput(0, 0, 1);
        else if (t >= 1000 && t < 1060) ship.testInput(1, 0, 0);
        else if (t >= 1060 && t < 1200) ship.testInput(0, 0, 0);
        if (t == 950 || t == 999 || t == 1059 || t == 1120 || t == 1199)
            System.out.printf("[AUTOTEST] v16 air t=%d dy=%.2f pos=%.1f,%.1f vs=%.3f speed=%.3f fuel=%d used=%d hopperRefuels=%d%n",
                    t, ship.getY() - y0, ship.getX(), ship.getZ(), ship.verticalSpeed(), ship.shipSpeed(), ship.fuel(), ShipEntity.FUEL_USED, ShipEntity.REFUELS);
        if (t == 1060) ship.setFuel(0);   // 연료 바닥 → 천천히 내려와야 함
        if (t == 1200) {
            ship.setFuel(1001);
            BlockPos sc0 = ship.blockPosition();
            int placed = ship.disassemble(null);
            int sum = 0, engines = 0;
            BlockPos sc = ship.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(sc.getX() - 8, sc.getY() - 3, sc.getZ() - 8, sc.getX() + 8, sc.getY() + 6, sc.getZ() + 8))
                if (level.getBlockEntity(p) instanceof ShipMotorBlockEntity m) {
                    engines++;
                    sum += m.fuel();
                }
            System.out.println("[AUTOTEST] v16 air docked placed=" + placed + " engines=" + engines + " fuelBack=" + sum);
            // 부유체 부족(블록 많은데 부유체 1개) → 조립 거부
            for (int x = x0; x <= x0 + 4; x++) for (int z = z0 + 5; z <= z0 + 14; z++) level.setBlockAndUpdate(new BlockPos(x, gy, z), Blocks.SPRUCE_PLANKS.defaultBlockState());
            System.out.println("[AUTOTEST] v16 air overweight assembled=" + (ShipAssembler.assemble(level, new BlockPos(x0 + 2, gy + 1, z0 + 3), null) != null));
            ship = null;
        }
    }

    // ── 2차: 실린 포탑·컨베이어, 피격(폭발), 추락 ──
    static ShipEntity war;
    static net.minecraft.world.entity.LivingEntity foe;

    static void tick2(ServerLevel level, int t, int gy) {
        int x0 = 148, z0 = 112;
        if (t == 1240) {
            build(level, x0, gy, z0, 2);
            var east = ModBlocks.CONVEYOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
            for (int x = x0 + 1; x <= x0 + 3; x++) level.setBlockAndUpdate(new BlockPos(x, gy + 1, z0 + 1), east);
            level.setBlockAndUpdate(new BlockPos(x0 + 4, gy + 1, z0 + 1), ModBlocks.TURRET.get().defaultBlockState());
            BlockPos helm = new BlockPos(x0 + 2, gy + 1, z0 + 3);
            war = ShipAssembler.assemble(level, helm, null);
            if (war != null) {
                war.autopilot = true;
                war.setFuel(50_000);
            }
            System.out.println("[AUTOTEST] v16 war assembled=" + (war != null) + (war == null ? "" : " cells=" + war.cells().size() + " canFly=" + war.canFly()));
        }
        if (war == null) return;
        if (t > 1240 && t < 1262) war.testInput(0, 0, 1);       // 1.5칸쯤 띄움
        else if (t >= 1262 && t < 1350) war.testInput(0, 0, 0);
        else if (t >= 1350 && t < 1385) war.testInput(0, 0, 1);
        if (t == 1385) {   // 6칸쯤 띄운 뒤 부유체 전부 파괴 → 추락·충돌
            System.out.printf("[AUTOTEST] v16 war before cut y+%.2f%n", war.getY() - gy);
            war.breakWhere(c -> c.state().getBlock() instanceof com.tycooncraft.ship.ShipFloatBlock);
            System.out.println("[AUTOTEST] v16 war floats cut canFly=" + war.canFly());
        }
        if (t == 1265) {   // 컨베이어 첫 칸에 중탄약 → 포탑으로 굴러가 장전
            var c0 = war.cellCenter(war.cells().stream().filter(c -> c.state().getBlock() instanceof com.tycooncraft.block.ConveyorBlock)
                    .min(java.util.Comparator.comparingInt(ShipEntity.Cell::x)).orElseThrow());
            var it = new net.minecraft.world.entity.item.ItemEntity(level, c0.x, c0.y + 0.2, c0.z, new ItemStack(com.tycooncraft.registry.ModItems.HEAVY_AMMO.get(), 20));
            it.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(it);
            foe = AutoTest.dummyAt(level, x0 + 14.5, z0 + 2.5);
        }
        if (t == 1330) {
            // 바깥에서 쏜 총알은 선체에 막혀야 함
            var from = war.position().add(-12, 1.2, 0);
            Vec3 stop = com.tycooncraft.gun.Ballistics.shoot(level, null, from, new Vec3(1, 0, 0), 40, 1f, 0, 1f, false, false, level.damageSources().generic(), e -> false);
            System.out.printf("[AUTOTEST] v16 war t=1330 y+%.2f conveyorPushes=%d turretShots=%d foeHp=%.0f bulletStop=%.1f (hull ~%.1f)%n",
                    war.getY() - gy, ShipEntity.CONVEYOR_PUSHES, ShipEntity.TURRET_SHOTS, foe == null ? -1 : foe.getHealth(), stop.x - from.x, war.worldBounds().minX - from.x);
        }
        if (t == 1335) {
            int before = war.cells().size();
            var m = war.cells().stream().filter(c -> c.state().getBlock() instanceof com.tycooncraft.ship.ShipMotorBlock).findFirst().orElseThrow();
            Vec3 p = war.cellCenter(m).add(0, 0, -1.2);
            level.explode(null, p.x, p.y, p.z, 3f, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
            System.out.println("[AUTOTEST] v16 war blast1 cells " + before + "->" + war.cells().size() + " broken=" + ShipEntity.BLAST_CELLS
                    + " motors=" + war.motorCount() + " fuel=" + war.fuel() + " canFly=" + war.canFly());
        }
        if (t == 1345) {   // 부유체 쪽을 크게 → 양력 상실 → 추락
            for (var c : war.cells()) if (c.state().getBlock() instanceof com.tycooncraft.ship.ShipFloatBlock) {
                Vec3 p = war.cellCenter(c);
                level.explode(null, p.x, p.y + 0.5, p.z, 4f, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
                break;
            }
            System.out.println("[AUTOTEST] v16 war blast2 cells=" + war.cells().size() + " floats=" + war.floatCount() + " canFly=" + war.canFly() + " removed=" + war.isRemoved());
        }
        if (t == 1440) {
            int planks = 0;
            for (BlockPos p : BlockPos.betweenClosed(x0 - 4, gy - 1, z0 - 4, x0 + 10, gy + 10, z0 + 10))
                if (level.getBlockState(p).is(Blocks.SPRUCE_PLANKS)) planks++;
            System.out.println("[AUTOTEST] v16 war end removed=" + war.isRemoved() + " crashes=" + ShipEntity.CRASHES + " planksOnGround=" + planks
                    + (war.isRemoved() ? "" : String.format(" y+%.2f", war.getY() - gy)));
            war = null;
        }
    }

    // ── 클라이언트 스크린샷용 ──
    public static ShipEntity clientShip;

    public static void clientBuild(ServerLevel level, int gy) {
        BlockPos helm = build(level, 20, gy, -70, 1);
        clientShip = ShipAssembler.assemble(level, helm, null);
        if (clientShip != null) {
            clientShip.autopilot = true;
            clientShip.setFuel(30_000);
        }
    }

    public static void clientInput(float f, float v) {
        if (clientShip != null && clientShip.isAlive()) clientShip.testInput(f, 0, v);
    }

    private AirshipTest() {}
}
