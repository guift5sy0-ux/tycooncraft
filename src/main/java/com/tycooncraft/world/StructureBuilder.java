package com.tycooncraft.world;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.block.ConveyorBlock;
import com.tycooncraft.block.MachineBlock;
import com.tycooncraft.machine.MachineType;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

import java.util.List;
import java.util.Optional;

/**
 * 구조물 생성: 작동하는 공장 / 상인 마을 / 폐공장.
 * 좌표는 origin 기준 (x 동쪽, z 남쪽, y=0 이 지면 위 첫 칸, y=-1 이 바닥)
 */
public final class StructureBuilder {
    public static final ResourceKey<LootTable> VILLAGE_CHEST = loot("chests/tycoon_village");
    public static final ResourceKey<LootTable> RUIN_CHEST = loot("chests/abandoned_factory");

    private final ServerLevel level;
    private final BlockPos origin;
    private final RandomSource random;

    private StructureBuilder(ServerLevel level, BlockPos origin) {
        this.level = level;
        this.origin = origin;
        this.random = level.getRandom();
    }

    private static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, TycoonCraft.id(path));
    }

    // ── 기본 도구 ──

    private BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    private void set(int x, int y, int z, BlockState state) {
        level.setBlock(at(x, y, z), state, Block.UPDATE_CLIENTS);
    }

    private void set(int x, int y, int z, Block block) {
        set(x, y, z, block.defaultBlockState());
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    set(x, y, z, state);
    }

    private void fill(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        fill(x1, y1, z1, x2, y2, z2, block.defaultBlockState());
    }

    /** 속이 빈 상자 (벽·바닥·천장) */
    private void hollow(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) {
                    boolean edge = x == x1 || x == x2 || y == y1 || y == y2 || z == z1 || z == z2;
                    set(x, y, z, edge ? block.defaultBlockState() : Blocks.AIR.defaultBlockState());
                }
    }

    /** 빈 곳(공기·물)만 채우기 = 기초 */
    private void foundation(int x1, int y1, int z1, int x2, int y2, int z2, Block block) {
        for (int x = x1; x <= x2; x++)
            for (int y = y1; y <= y2; y++)
                for (int z = z1; z <= z2; z++) {
                    BlockState s = level.getBlockState(at(x, y, z));
                    if (s.isAir() || s.liquid() || s.canBeReplaced()) set(x, y, z, block);
                }
    }

    private void chest(int x, int y, int z, Direction facing, ResourceKey<LootTable> table) {
        set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        if (table != null && level.getBlockEntity(at(x, y, z)) instanceof ChestBlockEntity chest) {
            chest.setLootTable(table, random.nextLong());
        }
    }

    /** 기계·컨베이어를 동쪽으로 흐르게 한 줄 설치 */
    private void line(int x, int y, int z, Block... blocks) {
        for (int i = 0; i < blocks.length; i++) {
            Block b = blocks[i];
            BlockState s = b.defaultBlockState();
            if (b instanceof MachineBlock) s = s.setValue(MachineBlock.FACING, Direction.EAST);
            else if (b instanceof ConveyorBlock) s = s.setValue(ConveyorBlock.FACING, Direction.EAST);
            set(x + i, y, z, s);
        }
    }

    private <T extends Mob> void spawnMob(EntityType<T> type, int x, int y, int z) {
        T mob = type.create(level);
        if (mob == null) return;
        BlockPos p = at(x, y, z);
        mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, random.nextFloat() * 360, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        level.addFreshEntity(mob);
    }

    private static MerchantOffer offer(Item cost, int costCount, Item result, int count) {
        return new MerchantOffer(new ItemCost(cost, costCount), Optional.empty(), new ItemStack(result, count), Integer.MAX_VALUE, 0, 0f);
    }

    private static MerchantOffer offer(Item cost, int costCount, Item costB, int costBCount, Item result, int count) {
        return new MerchantOffer(new ItemCost(cost, costCount), Optional.of(new ItemCost(costB, costBCount)),
                new ItemStack(result, count), Integer.MAX_VALUE, 0, 0f);
    }

    /** 무적·고정·사라지지 않는 상인 */
    private void trader(int x, int y, int z, String nameKey, VillagerProfession profession, List<MerchantOffer> offers) {
        Villager v = EntityType.VILLAGER.create(level);
        if (v == null) return;
        BlockPos p = at(x, y, z);
        v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        v.setVillagerData(v.getVillagerData().setProfession(profession).setLevel(5));
        v.setVillagerXp(250);
        MerchantOffers list = new MerchantOffers();
        list.addAll(offers);
        v.overrideOffers(list);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.setCustomName(Component.translatable(nameKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        v.setCustomNameVisible(true);
        level.addFreshEntity(v);
    }

    // ── 공장 ──

    private void factoryShell() {
        fill(0, 0, 0, 22, 9, 14, Blocks.AIR);
        foundation(0, -5, 0, 22, -2, 14, Blocks.STONE_BRICKS);
        hollow(0, -1, 0, 22, 7, 14, Blocks.STONE_BRICKS);
        fill(1, -1, 1, 21, -1, 13, Blocks.POLISHED_ANDESITE);
        for (int x : new int[]{0, 6, 11, 16, 22}) {
            fill(x, 0, 0, x, 6, 0, Blocks.DEEPSLATE_BRICKS);
            fill(x, 0, 14, x, 6, 14, Blocks.DEEPSLATE_BRICKS);
        }
        for (int[] seg : new int[][]{{1, 5}, {7, 10}, {12, 15}, {17, 21}}) {
            fill(seg[0], 2, 0, seg[1], 4, 0, Blocks.GLASS);
            fill(seg[0], 2, 14, seg[1], 4, 14, Blocks.GLASS);
        }
        fill(0, 0, 6, 0, 2, 8, Blocks.AIR);
        fill(0, 8, 0, 22, 8, 14, Blocks.DEEPSLATE_TILE_SLAB);
        for (int z : new int[]{5, 9}) fill(1, -1, z, 12, -1, z, Blocks.YELLOW_CONCRETE);
        fill(14, -1, 2, 21, -1, 12, Blocks.SMOOTH_STONE);
    }

    /** 작동하는 공장: 라인 3개 + 조립 작업장 */
    public static void factory(ServerLevel level, BlockPos origin, boolean working) {
        StructureBuilder b = new StructureBuilder(level, origin);
        b.factoryShell();
        for (int[] p : new int[][]{{4, 3}, {4, 11}, {11, 3}, {11, 11}, {18, 3}, {18, 11}, {11, 7}}) b.set(p[0], 7, p[1], Blocks.SEA_LANTERN);
        Block c = ModBlocks.CONVEYOR.get(), s = ModBlocks.SELL_STATION.get();
        b.line(2, 0, 3, c, m(MachineType.CRUSHER), c, m(MachineType.SMELTER), c, m(MachineType.PRESS), c, m(MachineType.LATHE), c, s);
        b.line(2, 0, 7, c, m(MachineType.CRUSHER), c, m(MachineType.SMELTER), c, m(MachineType.WIRE_DRAWER), c, c, c, s);
        b.line(2, 0, 11, c, m(MachineType.CRUSHER), c, m(MachineType.SMELTER), c, m(MachineType.ALLOY_FURNACE), c, m(MachineType.PRESS), c, s);
        b.line(16, 0, 7, c, m(MachineType.ASSEMBLER), c, s);
        b.set(15, 0, 2, Blocks.CRAFTING_TABLE);
        b.set(15, 0, 12, ModBlocks.EXCHANGE.get());
        for (int x = 16; x <= 20; x++) {
            b.chest(x, 0, 2, Direction.SOUTH, null);
            b.chest(x, 0, 12, Direction.NORTH, null);
        }
    }

    private static Block m(MachineType type) {
        return ModBlocks.machine(type);
    }

    /** 폐공장: 무너지고 고장난 공장 + 재료 상자 + 몹·스포너 */
    public static void ruin(ServerLevel level, BlockPos origin) {
        StructureBuilder b = new StructureBuilder(level, origin);
        RandomSource r = b.random;
        b.factoryShell();

        // 낡은 벽
        for (int i = 0; i < 90; i++) {
            int side = r.nextInt(4);
            int x = side < 2 ? r.nextInt(23) : (side == 2 ? 0 : 22);
            int z = side < 2 ? (side == 0 ? 0 : 14) : r.nextInt(15);
            b.set(x, r.nextInt(8), z, r.nextBoolean() ? Blocks.MOSSY_STONE_BRICKS : Blocks.CRACKED_STONE_BRICKS);
        }
        // 깨진 창문·벽 구멍
        for (int x = 0; x <= 22; x++) for (int y = 2; y <= 4; y++) for (int z : new int[]{0, 14})
            if (level.getBlockState(b.at(x, y, z)).is(Blocks.GLASS)) b.set(x, y, z, r.nextFloat() < 0.3f ? Blocks.GLASS : Blocks.AIR);
        // 무너진 지붕 + 잔해
        for (int i = 0; i < 6; i++) {
            int x = 2 + r.nextInt(17), z = 2 + r.nextInt(9), w = 1 + r.nextInt(3), d = 1 + r.nextInt(3);
            b.fill(x, 7, z, x + w, 8, z + d, Blocks.AIR);
            b.fill(x, 0, z, x + w, 0, z + d, r.nextBoolean() ? Blocks.GRAVEL : Blocks.COBBLESTONE);
        }
        // 바닥 파손
        Block[] cracks = {Blocks.COBBLESTONE, Blocks.GRAVEL, Blocks.MOSSY_COBBLESTONE, Blocks.COARSE_DIRT};
        for (int i = 0; i < 40; i++) b.set(1 + r.nextInt(21), -1, 1 + r.nextInt(13), cracks[r.nextInt(cracks.length)]);
        // 거미줄
        for (int i = 0; i < 28; i++) b.set(1 + r.nextInt(21), 3 + r.nextInt(4), 1 + r.nextInt(13), Blocks.COBWEB);

        // 고장난 라인
        for (int z : new int[]{3, 7, 11}) {
            for (int x = 2; x <= 11; x++) {
                float f = r.nextFloat();
                if (x % 2 == 1) b.set(x, 0, z, f < 0.75f ? ModBlocks.BROKEN_MACHINE.get() : Blocks.AIR);
                else b.set(x, 0, z, f < 0.6f ? Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(TrapDoorBlock.FACING, Direction.EAST)
                        : Blocks.AIR.defaultBlockState());
            }
        }
        b.set(17, 0, 7, ModBlocks.BROKEN_MACHINE.get());
        b.set(18, 0, 7, ModBlocks.BROKEN_MACHINE.get());
        b.set(15, 0, 2, Blocks.CHIPPED_ANVIL);
        b.set(15, 0, 12, Blocks.DAMAGED_ANVIL);
        for (int[] p : new int[][]{{4, 3}, {11, 7}, {18, 11}}) b.set(p[0], 7, p[1], Blocks.REDSTONE_LAMP);

        // 재료 상자
        b.chest(17, 0, 2, Direction.SOUTH, RUIN_CHEST);
        b.chest(19, 0, 2, Direction.SOUTH, RUIN_CHEST);
        b.chest(20, 0, 12, Direction.NORTH, RUIN_CHEST);
        b.chest(13, 0, 7, Direction.WEST, RUIN_CHEST);

        // 스포너 + 몹
        b.spawner(7, 0, 5, EntityType.ZOMBIE);
        b.spawner(18, 0, 9, EntityType.SKELETON);
        b.spawnMob(EntityType.ZOMBIE, 4, 0, 4);
        b.spawnMob(EntityType.ZOMBIE, 9, 0, 9);
        b.spawnMob(EntityType.ZOMBIE, 14, 0, 6);
        b.spawnMob(EntityType.SKELETON, 18, 0, 4);
        b.spawnMob(EntityType.SKELETON, 6, 0, 12);
        b.spawnMob(EntityType.SPIDER, 12, 0, 10);
        b.spawnMob(EntityType.HUSK, 20, 0, 9);
    }

    private void spawner(int x, int y, int z, EntityType<?> type) {
        set(x, y, z, Blocks.SPAWNER);
        if (level.getBlockEntity(at(x, y, z)) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, random);
        }
    }

    // ── 마을 ──

    private void stall(int cx, int cz, Block roof) {
        for (int[] d : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) fill(cx + d[0], 0, cz + d[1], cx + d[0], 1, cz + d[1], Blocks.OAK_FENCE);
        fill(cx - 1, 2, cz - 1, cx + 1, 2, cz + 1, roof);
        set(cx, 2, cz, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
    }

    private void house(int x, int z, boolean doorSouth) {
        fill(x, -1, z, x + 6, -1, z + 6, Blocks.COBBLESTONE);
        hollow(x, 0, z, x + 6, 3, z + 6, Blocks.OAK_PLANKS);
        fill(x + 1, 0, z + 1, x + 5, 0, z + 5, Blocks.AIR);
        for (int[] c : new int[][]{{0, 0}, {6, 0}, {0, 6}, {6, 6}}) fill(x + c[0], 0, z + c[1], x + c[0], 3, z + c[1], Blocks.OAK_LOG);
        set(x + 3, 1, z, Blocks.GLASS_PANE);
        set(x + 3, 1, z + 6, Blocks.GLASS_PANE);
        set(x, 1, z + 3, Blocks.GLASS_PANE);
        set(x + 6, 1, z + 3, Blocks.GLASS_PANE);
        int dz = doorSouth ? z + 6 : z;
        fill(x + 3, 0, dz, x + 3, 1, dz, Blocks.AIR);
        fill(x - 1, 4, z - 1, x + 7, 4, z + 7, Blocks.SPRUCE_SLAB);
        fill(x, 4, z, x + 6, 4, z + 6, Blocks.SPRUCE_PLANKS);
        set(x + 3, 0, z + 3, Blocks.LANTERN);
        set(x + 1, 0, z + 1, Blocks.CRAFTING_TABLE);
        chest(x + 5, 0, z + 1, doorSouth ? Direction.SOUTH : Direction.NORTH, VILLAGE_CHEST);
    }

    /** 상인 마을: 광장·우물·노점 5개·집 4채·밭·환전기 */
    public static void village(ServerLevel level, BlockPos origin) {
        StructureBuilder b = new StructureBuilder(level, origin);
        b.fill(0, 0, 0, 32, 12, 32, Blocks.AIR);
        b.fill(0, -1, 0, 32, -1, 32, Blocks.GRASS_BLOCK);
        b.foundation(0, -4, 0, 32, -2, 32, Blocks.DIRT);

        b.fill(15, -1, 0, 17, -1, 32, Blocks.DIRT_PATH);
        b.fill(0, -1, 15, 32, -1, 17, Blocks.DIRT_PATH);
        b.fill(9, -1, 9, 23, -1, 23, Blocks.STONE_BRICKS);
        b.fill(10, -1, 10, 22, -1, 22, Blocks.SMOOTH_STONE);

        b.fill(14, -1, 14, 18, -1, 18, Blocks.COBBLESTONE);
        b.fill(15, -1, 15, 17, -1, 17, Blocks.WATER);
        for (int[] c : new int[][]{{14, 14}, {18, 14}, {14, 18}, {18, 18}}) b.fill(c[0], 0, c[1], c[0], 2, c[1], Blocks.COBBLESTONE_WALL);
        b.fill(14, 3, 14, 18, 3, 18, Blocks.SPRUCE_SLAB);

        for (int[] p : new int[][]{{14, 4}, {18, 28}, {4, 18}, {28, 14}, {10, 10}, {22, 10}, {10, 22}, {22, 22}}) {
            b.fill(p[0], 0, p[1], p[0], 2, p[1], Blocks.OAK_FENCE);
            b.set(p[0], 3, p[1], Blocks.LANTERN);
        }
        b.set(13, 0, 16, ModBlocks.EXCHANGE.get());
        b.set(19, 0, 16, ModBlocks.EXCHANGE.get());

        b.stall(12, 12, Blocks.YELLOW_WOOL);
        b.stall(20, 12, Blocks.LIGHT_GRAY_WOOL);
        b.stall(12, 20, Blocks.RED_WOOL);
        b.stall(20, 20, Blocks.BLUE_WOOL);
        b.stall(16, 11, Blocks.GREEN_WOOL);

        Item cu = ModItems.COIN_COPPER.get(), ir = ModItems.COIN_IRON.get(), go = ModItems.COIN_GOLD.get(), em = ModItems.COIN_EMERALD.get();
        b.trader(12, 0, 12, "trader.tycooncraft.food", VillagerProfession.FARMER, List.of(
                offer(ir, 1, Items.BREAD, 2), offer(ir, 1, Items.APPLE, 3), offer(ir, 2, Items.COOKED_BEEF, 4),
                offer(ir, 5, Items.GOLDEN_CARROT, 4), offer(ir, 1, Items.WHEAT_SEEDS, 16), offer(ir, 1, Items.CARROT, 8),
                offer(ir, 2, Items.COCOA_BEANS, 8), offer(ir, 2, Items.EGG, 8)));
        b.trader(20, 0, 12, "trader.tycooncraft.builder", VillagerProfession.MASON, List.of(
                offer(ir, 1, Items.STONE_BRICKS, 16), offer(ir, 1, Items.BRICKS, 16), offer(ir, 1, Items.SMOOTH_STONE, 16),
                offer(ir, 2, cu, 4, Items.GLASS, 16), offer(ir, 1, Items.OAK_PLANKS, 32), offer(ir, 1, Items.TORCH, 16),
                offer(ir, 3, Items.LANTERN, 4), offer(ir, 2, Items.WHITE_WOOL, 8)));
        b.trader(12, 0, 20, "trader.tycooncraft.gear", VillagerProfession.ARMORER, List.of(
                offer(ir, 5, Items.IRON_PICKAXE, 1), offer(ir, 5, Items.IRON_SWORD, 1), offer(ir, 4, Items.IRON_HELMET, 1),
                offer(ir, 8, Items.IRON_CHESTPLATE, 1), offer(ir, 7, Items.IRON_LEGGINGS, 1), offer(ir, 4, Items.IRON_BOOTS, 1),
                offer(ir, 3, Items.SHIELD, 1), offer(go, 3, Items.DIAMOND_PICKAXE, 1),
                offer(ir, 3, ModItems.LIGHT_AMMO.get(), 16), offer(ir, 4, ModItems.HEAVY_AMMO.get(), 8),
                offer(ir, 3, ModItems.SHOTGUN_SHELL.get(), 8), offer(go, 6, ModItems.PISTOL.get(), 1)));
        b.trader(20, 0, 20, "trader.tycooncraft.machines", VillagerProfession.TOOLSMITH, List.of(
                offer(ir, 2, ModItems.CONVEYOR.get(), 8), offer(ir, 3, ModItems.LIFT_UP.get(), 2), offer(ir, 3, ModItems.LIFT_DOWN.get(), 2),
                offer(ir, 6, ModItems.SELL_STATION.get(), 1), offer(ir, 6, ModItems.machineItem(MachineType.CRUSHER), 1),
                offer(ir, 6, ModItems.machineItem(MachineType.SMELTER), 1), offer(ir, 8, ModItems.machineItem(MachineType.PRESS), 1),
                offer(go, 2, ModItems.machineItem(MachineType.ALLOY_FURNACE), 1), offer(go, 2, ModItems.machineItem(MachineType.WIRE_DRAWER), 1),
                offer(go, 2, ModItems.machineItem(MachineType.LATHE), 1), offer(em, 2, ModItems.machineItem(MachineType.ASSEMBLER), 1)));
        b.trader(16, 0, 11, "trader.tycooncraft.buyer", VillagerProfession.CLERIC, List.of(
                offer(Items.ROTTEN_FLESH, 16, cu, 4), offer(Items.BONE, 8, cu, 5), offer(Items.STRING, 8, cu, 6),
                offer(Items.SPIDER_EYE, 4, cu, 4), offer(Items.FEATHER, 16, cu, 5), offer(Items.LEATHER, 4, ir, 1),
                offer(Items.GUNPOWDER, 4, ir, 1), offer(Items.SLIME_BALL, 4, ir, 1), offer(Items.ENDER_PEARL, 2, ir, 3)));

        b.house(2, 2, true);
        b.house(24, 2, true);
        b.house(2, 24, false);
        b.house(24, 24, false);

        b.fill(24, -1, 11, 30, -1, 13, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        b.fill(24, -1, 12, 30, -1, 12, Blocks.WATER);
        BlockState wheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        b.fill(24, 0, 11, 30, 0, 11, wheat);
        b.fill(24, 0, 13, 30, 0, 13, wheat);
    }
}
