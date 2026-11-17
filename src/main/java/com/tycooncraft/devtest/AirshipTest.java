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
        for (int x = x0; x <= x0 + 4; x += 4) level.setBlockAndUpdate(new BlockPos(x, y + 1, z0 + 4), Blocks.SPRUCE_FENCE.defaultBlockState());
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
            int placed = ship.disassemble(null);
            int sum = 0, engines = 0;
            for (BlockPos p : BlockPos.betweenClosed(x0 - 6, gy - 2, z0 - 6, x0 + 12, gy + 12, z0 + 12))
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

    // ── 클라이언트 스크린샷용 ──
    public static ShipEntity clientShip;

    public static void clientBuild(ServerLevel level, int gy) {
        BlockPos helm = build(level, 20, gy, -70, 1);
        clientShip = ShipAssembler.assemble(level, helm, null);
        if (clientShip != null) clientShip.autopilot = true;
    }

    public static void clientInput(float f, float v) {
        if (clientShip != null && clientShip.isAlive()) clientShip.testInput(f, 0, v);
    }

    private AirshipTest() {}
}
