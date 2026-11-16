package com.tycooncraft.devtest;

import com.tycooncraft.block.ConveyorBlock;
import com.tycooncraft.block.TransmissionBlock;
import com.tycooncraft.entity.GrenadeEntity;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.ship.ShipAssembler;
import com.tycooncraft.ship.ShipEntity;
import com.tycooncraft.vehicle.CarEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** v0.15 자동 테스트: 높인 컨베이어 + 변속기 + 분쇄기, EMP, 블록 배 */
public final class V15Test {
    static int gy;
    static ItemEntity ore;
    static double oreX0;
    static CarEntity empCar;
    public static ShipEntity ship;
    public static Villager sailor;
    static double sailorDist0;

    static void tick(ServerLevel level, int t, int groundY) {
        gy = groundY;
        if (t == 20)
            for (int cx = 6; cx <= 7; cx++) for (int cz = 6; cz <= 10; cz++) level.setChunkForced(cx, cz, true);
        if (t == 45) factory(level);
        if (t == 50) {
            BlockPos tp = new BlockPos(101, gy, 139);
            level.setBlock(tp, ModBlocks.TRANSMISSION.get().defaultBlockState().setValue(TransmissionBlock.SPEED, 3), 3);
            int n = TransmissionBlock.apply(level, tp, 3);
            System.out.println("[AUTOTEST] v15 transmission chain=" + n + " speeds=" + speeds(level));
        }
        if (t == 52) {
            ore = new ItemEntity(level, 98.5, gy + 0.6, 138.5, new ItemStack(Items.RAW_IRON, 3));
            ore.setDeltaMovement(0, 0, 0);
            level.addFreshEntity(ore);
        }
        if (t == 56) oreX0 = ore.getX();
        if (t == 66) System.out.printf("[AUTOTEST] v15 belt speed3 dx/10t=%.2f oreY=%.3f (belt top %.3f)%n", ore.getX() - oreX0, ore.getY(), gy + ConveyorBlock.BELT_TOP);
        if (t == 260) {
            int dust = 0;
            double y = 0;
            for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(105.5, gy - 1, 136, 112, gy + 2, 141))) {
                if (e.getItem().is(ModItems.IRON_DUST.get())) {
                    dust += e.getItem().getCount();
                    y = e.getY() - gy;
                }
            }
            System.out.printf("[AUTOTEST] v15 crusher out iron_dust=%d itemY=%.3f oreAlive=%s%n", dust, y, ore.isAlive());
        }
        if (t == 270) {   // 판매소: 빈손 우클릭 → 창 열기로 넘어가는지, 판매 후 인출되는지
            BlockPos sp = new BlockPos(110, gy, 138);
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            fp.moveTo(110.5, gy, 140.5, 180, 0);
            var hit = new net.minecraft.world.phys.BlockHitResult(sp.getCenter(), Direction.SOUTH, sp, false);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            var empty = level.getBlockState(sp).useItemOn(ItemStack.EMPTY, level, fp, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            fp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.IRON_DUST.get(), 20));
            var sell = level.getBlockState(sp).useItemOn(fp.getMainHandItem(), level, fp, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            long bal = 0;
            int coins = 0;
            if (level.getBlockEntity(sp) instanceof com.tycooncraft.blockentity.SellStationBlockEntity st) {
                bal = st.balance();
                fp.getInventory().clearContent();
                st.withdraw(fp);
                for (int i = 0; i < fp.getInventory().getContainerSize(); i++) if (!fp.getInventory().getItem(i).isEmpty()) coins += fp.getInventory().getItem(i).getCount();
            }
            System.out.println("[AUTOTEST] v15 sellstation emptyHand=" + empty + " sell=" + sell + " balance=" + bal + " withdrawnCoinItems=" + coins);
        }
        // ── EMP ──
        tickClientShip();
        if (t == 60) {
            for (int x = 96; x <= 112; x++) for (int z = 156; z <= 164; z++) {
                level.setBlockAndUpdate(new BlockPos(x, gy - 1, z), Blocks.STONE.defaultBlockState());
                for (int y = 0; y < 4; y++) level.setBlockAndUpdate(new BlockPos(x, gy + y, z), Blocks.AIR.defaultBlockState());
            }
            empCar = ModEntities.CAR.get().create(level);
            empCar.moveTo(100.5, gy, 160.5, 0, 0);
            empCar.setFuel(1000);
            level.addFreshEntity(empCar);
            GrenadeEntity g = new GrenadeEntity(ModEntities.GRENADE.get(), level);
            g.setItem(new ItemStack(ModItems.EMP_GRENADE.get()));
            g.moveTo(104.5, gy + 0.2, 160.5);
            level.addFreshEntity(g);
        }
        if (t == 130) System.out.println("[AUTOTEST] v15 emp carEmp=" + empCar.empTicks() + " hits=" + GrenadeEntity.empHits);
        // ── 배: 수조 ──
        if (t == 50) {
            for (int x = 98; x <= 126; x++) for (int z = 98; z <= 126; z++) {
                boolean in = x >= 100 && x <= 124 && z >= 100 && z <= 124;
                level.setBlock(new BlockPos(x, gy - 4, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = -3; y <= -1; y++)
                    level.setBlock(new BlockPos(x, gy + y, z), in ? Blocks.WATER.defaultBlockState() : Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 6; y++) level.setBlock(new BlockPos(x, gy + y, z), Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (t == 60) buildShip(level);
        if (t == 70) {
            ship = ShipAssembler.assemble(level, new BlockPos(110, gy, 108), null);
            System.out.println("[AUTOTEST] v15 ship assembled=" + (ship != null)
                    + (ship == null ? "" : " cells=" + ship.cells().size() + " motors=" + ship.motorCount() + " boxes=" + ship.boxes().length
                    + String.format(" pos=%.2f,%.2f,%.2f", ship.getX(), ship.getY(), ship.getZ()))
                    + " helmGone=" + level.getBlockState(new BlockPos(110, gy, 108)).isAir()
                    + " waterBack=" + level.getFluidState(new BlockPos(110, gy - 1, 107)).isSource());
            if (ship != null) {
                ship.autopilot = true;
                sailorDist0 = sailor.position().distanceTo(ship.position());
            }
        }
        if (ship != null && ship.isAlive() && t > 70 && t <= 330) {
            if (t < 110) ship.testInput(1, 0);
            else if (t < 200) ship.testInput(0.6f, 1);
            else ship.testInput(0, 0);
            if (t % 40 == 0 || t == 330)
                System.out.printf("[AUTOTEST] v15 ship t=%d pos=%.2f,%.2f yaw=%.1f speed=%.3f sailorDist=%.3f (start %.3f) sailorY-gy=%.2f moved=%d carried=%d%n",
                        t, ship.getX(), ship.getZ(), ship.yaw, ship.shipSpeed(), sailor.position().distanceTo(ship.position()), sailorDist0,
                        sailor.getY() - gy, ShipEntity.MOVED_TICKS, ShipEntity.CARRIED);
        }
        if (t == 340 && ship != null) {
            int n = ship.disassemble(null);
            int chests = 0, diamonds = 0, helms = 0;
            for (BlockPos p : BlockPos.betweenClosed(98, gy - 2, 98, 126, gy + 3, 126)) {
                BlockState s = level.getBlockState(p);
                if (s.getBlock() instanceof ChestBlock && level.getBlockEntity(p) instanceof ChestBlockEntity c) {
                    chests++;
                    for (int i = 0; i < c.getContainerSize(); i++) if (c.getItem(i).is(Items.DIAMOND)) diamonds += c.getItem(i).getCount();
                }
                if (s.is(ModBlocks.SHIP_HELM.get())) helms++;
            }
            System.out.println("[AUTOTEST] v15 ship docked placed=" + n + " chests=" + chests + " diamonds=" + diamonds + " helms=" + helms);
            ship = null;
        }
    }

    static String speeds(ServerLevel level) {
        StringBuilder sb = new StringBuilder();
        for (int x = 98; x <= 109; x++) {
            BlockState s = level.getBlockState(new BlockPos(x, gy, 138));
            sb.append(s.getBlock() instanceof ConveyorBlock ? String.valueOf(s.getValue(ConveyorBlock.SPEED_LEVEL)) : "M");
        }
        return sb.toString();
    }

    static void factory(ServerLevel level) {
        for (int x = 96; x <= 112; x++) for (int z = 134; z <= 142; z++) {
            level.setBlockAndUpdate(new BlockPos(x, gy - 1, z), Blocks.SMOOTH_STONE.defaultBlockState());
            for (int y = 0; y < 4; y++) level.setBlockAndUpdate(new BlockPos(x, gy + y, z), Blocks.AIR.defaultBlockState());
        }
        BlockState c = ModBlocks.CONVEYOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
        for (int x = 98; x <= 104; x++) level.setBlockAndUpdate(new BlockPos(x, gy, 138), c);
        level.setBlockAndUpdate(new BlockPos(105, gy, 138), ModBlocks.machine(MachineType.CRUSHER).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        for (int x = 106; x <= 109; x++) level.setBlockAndUpdate(new BlockPos(x, gy, 138), c);
        level.setBlockAndUpdate(new BlockPos(110, gy, 138), ModBlocks.SELL_STATION.get().defaultBlockState());
        // 옆 줄: 제련기 → 리프트 구성 (외형 확인용)
        for (int x = 98; x <= 102; x++) level.setBlockAndUpdate(new BlockPos(x, gy, 136), c);
        level.setBlockAndUpdate(new BlockPos(103, gy, 136), ModBlocks.machine(MachineType.SMELTER).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        level.setBlockAndUpdate(new BlockPos(104, gy, 136), c);
        level.setBlockAndUpdate(new BlockPos(105, gy, 136), ModBlocks.machine(MachineType.PRESS).defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST));
        System.out.println("[AUTOTEST] v15 factory speeds=" + speeds(level));
    }

    static void buildShip(ServerLevel level) {
        buildShipAt(level, 0);
        sailor = EntityType.VILLAGER.create(level);
        sailor.moveTo(111.5, gy, 108.5, 0, 0);
        sailor.setNoAi(true);
        sailor.addTag("tc_dummy");
        level.addFreshEntity(sailor);
    }

    public static ShipEntity clientShip;

    public static void assembleClientShip(ServerLevel level) {
        clientShip = ShipAssembler.assemble(level, new BlockPos(110, gy, 108), null);
        if (clientShip != null) clientShip.autopilot = true;
        System.out.println("[AUTOTEST] v15 clientship assembled=" + (clientShip != null));
    }

    static float cf, ct;

    public static void clientShipInput(float f, float t) {
        cf = f;
        ct = t;
    }

    static void tickClientShip() {
        if (clientShip != null && clientShip.isAlive()) clientShip.testInput(cf, ct);
    }

    public static void dockClientShip(net.minecraft.server.level.ServerPlayer sp) {
        if (clientShip != null) System.out.println("[AUTOTEST] v15 clientship docked placed=" + clientShip.disassemble(sp));
    }

    /** 클라이언트 쪽: 내 캐릭터가 갑판 위에서 배와 함께 움직이는지 */
    public static void logClient(net.minecraft.client.Minecraft mc, String tag) {
        var pl = mc.player;
        for (ShipEntity s : com.tycooncraft.ship.ShipCollisions.ships(mc.level)) {
            System.out.printf("[AUTOTEST] v15 client %s player=%.2f,%.2f,%.2f onGround=%s onDeck=%s rel=%.2f ship=%.2f,%.2f yaw=%.1f%n", tag,
                    pl.getX(), pl.getY() - gy, pl.getZ(), pl.onGround(), s.standingOn(pl, 0.2), pl.position().subtract(s.position()).horizontalDistance(),
                    s.getX(), s.getZ(), s.yaw);
        }
    }

    public static void buildShipAt(ServerLevel level, int unused) {
        BlockState plank = Blocks.SPRUCE_PLANKS.defaultBlockState();
        for (int x = 107; x <= 113; x++) for (int z = 105; z <= 111; z++) {
            boolean edge = x == 107 || x == 113 || z == 105 || z == 111;
            level.setBlock(new BlockPos(x, gy - 1, z), plank, 3);
            if (edge && !(z == 111 && x == 110)) level.setBlock(new BlockPos(x, gy, z), Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(110, gy - 1, 105), ModBlocks.SHIP_MOTOR.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 3);
        level.setBlock(new BlockPos(110, gy, 108), ModBlocks.SHIP_HELM.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH), 3);
        // 선실 + 상자
        for (int y = 0; y < 2; y++) {
            level.setBlock(new BlockPos(108, gy + y, 106), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
            level.setBlock(new BlockPos(112, gy + y, 106), Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
        }
        level.setBlock(new BlockPos(108, gy + 2, 106), Blocks.SPRUCE_SLAB.defaultBlockState(), 3);
        level.setBlock(new BlockPos(112, gy + 2, 106), Blocks.SPRUCE_SLAB.defaultBlockState(), 3);
        level.setBlock(new BlockPos(108, gy, 110), Blocks.CHEST.defaultBlockState(), 3);
        if (level.getBlockEntity(new BlockPos(108, gy, 110)) instanceof ChestBlockEntity c) c.setItem(0, new ItemStack(Items.DIAMOND, 7));
        level.setBlock(new BlockPos(112, gy, 110), Blocks.LANTERN.defaultBlockState(), 3);
    }

    private V15Test() {}
}
