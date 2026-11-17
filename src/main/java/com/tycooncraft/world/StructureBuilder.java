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
import net.minecraft.util.Mth;
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
    public static final ResourceKey<LootTable> GANG_VAULT = loot("chests/gang_vault");
    public static final ResourceKey<LootTable> GANG_SUPPLY = loot("chests/gang_supply");

    private final net.minecraft.world.level.WorldGenLevel level;
    private final BlockPos origin;
    private final RandomSource random;
    /** 월드 생성 중이면 지금 만드는 청크 영역 (그 밖은 건드리지 않음), 즉석 건설이면 null */
    @org.jetbrains.annotations.Nullable
    private final net.minecraft.world.level.levelgen.structure.BoundingBox clip;
    /** 이번 건설에서 놓은 블록 (청크 밖 블록도 기억 → 모든 청크에서 같은 결정을 내림) */
    private final java.util.Map<BlockPos, BlockState> placed = new java.util.HashMap<>();

    private StructureBuilder(ServerLevel level, BlockPos origin) {
        this(level, origin, level.getRandom(), null);
    }

    private StructureBuilder(net.minecraft.world.level.WorldGenLevel level, BlockPos origin, RandomSource random,
                             @org.jetbrains.annotations.Nullable net.minecraft.world.level.levelgen.structure.BoundingBox clip) {
        this.level = level;
        this.origin = origin;
        this.random = random;
        this.clip = clip;
    }

    /** 월드 생성용: 같은 seed 로 청크마다 다시 돌려도 결과가 똑같다 */
    public static void generate(String kind, net.minecraft.world.level.WorldGenLevel level, BlockPos origin, long seed,
                                net.minecraft.world.level.levelgen.structure.BoundingBox clip) {
        StructureBuilder b = new StructureBuilder(level, origin, RandomSource.create(seed), clip);
        if (kind.equals("village")) b.buildVillage();
        else if (kind.equals("hideout")) b.buildHideout();
        else b.buildRuin();
    }

    /** 월드 생성된 마을: 시작 청크가 로딩되면 상인만 배치 */
    public static void spawnVillageTraders(ServerLevel level, BlockPos origin) {
        new StructureBuilder(level, origin).traders();
    }

    private boolean inside(BlockPos p) {
        return clip == null || clip.isInside(p);
    }

    private BlockState get(int x, int y, int z) {
        BlockPos p = at(x, y, z);
        BlockState s = placed.get(p);
        if (s != null) return s;
        return inside(p) ? level.getBlockState(p) : Blocks.AIR.defaultBlockState();
    }

    private static ResourceKey<LootTable> loot(String path) {
        return ResourceKey.create(Registries.LOOT_TABLE, TycoonCraft.id(path));
    }

    // ── 기본 도구 ──

    private BlockPos at(int x, int y, int z) {
        return origin.offset(x, y, z);
    }

    private void set(int x, int y, int z, BlockState state) {
        BlockPos p = at(x, y, z);
        placed.put(p, state);
        if (inside(p)) level.setBlock(p, state, Block.UPDATE_CLIENTS);
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
                    BlockState s = get(x, y, z);
                    if (s.isAir() || s.liquid() || s.canBeReplaced()) set(x, y, z, block);
                }
    }

    private void chest(int x, int y, int z, Direction facing, ResourceKey<LootTable> table) {
        set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        long lootSeed = random.nextLong();
        if (table != null && inside(at(x, y, z)) && level.getBlockEntity(at(x, y, z)) instanceof ChestBlockEntity chest) {
            chest.setLootTable(table, lootSeed);
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

    private <T extends Mob> T spawnMob(EntityType<T> type, int x, int y, int z) {
        return spawnMob(type, x, y, z, null);
    }

    private <T extends Mob> T spawnMob(EntityType<T> type, int x, int y, int z, java.util.function.Consumer<T> before) {
        BlockPos p = at(x, y, z);
        float yaw = random.nextFloat() * 360;
        if (!inside(p)) return null;
        T mob = type.create(level.getLevel());
        if (mob == null) return null;
        mob.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, yaw, 0);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
        mob.setPersistenceRequired();
        if (before != null) before.accept(mob);
        level.addFreshEntityWithPassengers(mob);      // 트럭처럼 승무원을 태운 몹도 함께
        return mob;
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
        BlockPos p = at(x, y, z);
        if (!inside(p)) return;
        Villager v = EntityType.VILLAGER.create(level.getLevel());
        if (v == null) return;
        v.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
        v.setVillagerData(v.getVillagerData().setProfession(profession).setLevel(5));
        v.setVillagerXp(250);
        com.tycooncraft.economy.VillagerCoins.setOffers(v, offers);   // overrideOffers 는 클라이언트용(서버에선 아무것도 안 함)
        v.getPersistentData().putLong("tycooncraft_stock_day", level.getLevel().getDayTime() / 24000L);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.setSilent(true);
        v.setPersistenceRequired();
        v.setCustomName(Component.translatable(nameKey).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        v.getPersistentData().putString("tycooncraft_trader", nameKey);
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
        new StructureBuilder(level, origin).buildRuin();
    }

    private void buildRuin() {
        StructureBuilder b = this;
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
            if (b.get(x, y, z).is(Blocks.GLASS)) b.set(x, y, z, r.nextFloat() < 0.3f ? Blocks.GLASS : Blocks.AIR);
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

        // 지하실 (40%): 사다리로 내려가면 파괴수 골렘이 기다림
        if (forceBasement || r.nextFloat() < BASEMENT_CHANCE) b.basement(r);
    }

    public static final float BASEMENT_CHANCE = 0.4f;
    public static boolean forceBasement;

    private void basement(RandomSource r) {
        if (origin.getY() - 14 <= level.getMinBuildHeight()) return;   // 땅이 너무 얕음 (평지 월드 등)
        hollow(2, -13, 1, 20, -5, 13, Blocks.DEEPSLATE_BRICKS);
        fill(3, -12, 2, 19, -6, 12, Blocks.AIR);
        fill(3, -13, 2, 19, -13, 12, Blocks.POLISHED_DEEPSLATE);
        for (int i = 0; i < 30; i++) set(3 + r.nextInt(17), -13, 2 + r.nextInt(11), r.nextBoolean() ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.COBBLED_DEEPSLATE);
        for (int i = 0; i < 25; i++) {
            int side = r.nextInt(4);
            int x = side < 2 ? 2 + r.nextInt(19) : (side == 2 ? 2 : 20);
            int z = side < 2 ? (side == 0 ? 1 : 13) : 1 + r.nextInt(13);
            set(x, -12 + r.nextInt(7), z, Blocks.CRACKED_DEEPSLATE_BRICKS);
        }
        // 기둥 + 조명
        for (int[] c : new int[][]{{6, 5}, {16, 5}, {6, 9}, {16, 9}}) {
            fill(c[0], -12, c[1], c[0], -6, c[1], Blocks.DEEPSLATE_TILES);
            set(c[0], -12, c[1] + (c[1] < 7 ? -1 : 1), Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, false));
        }
        for (int i = 0; i < 12; i++) set(3 + r.nextInt(17), -7 + r.nextInt(2), 2 + r.nextInt(11), Blocks.COBWEB);
        // 사다리 통로: 1층 바닥 (19, -1, 12) → 지하
        fill(19, -12, 12, 19, -1, 12, Blocks.AIR);
        for (int y = -12; y <= -1; y++)
            set(19, y, 12, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.NORTH));
        fill(19, -12, 13, 19, -1, 13, Blocks.DEEPSLATE_BRICKS);
        // 잔해와 상자 1개
        set(4, -12, 3, ModBlocks.BROKEN_MACHINE.get());
        set(5, -12, 3, ModBlocks.BROKEN_MACHINE.get());
        chest(4, -12, 11, Direction.EAST, RUIN_CHEST);
        // 그 골렘
        spawnMob(com.tycooncraft.registry.ModEntities.DESTROYER_GOLEM.get(), 11, -12, 7,
                g -> g.getPersistentData().putBoolean(com.tycooncraft.mob.DestroyerGolem.BASEMENT_TAG, true));
    }

    private void spawner(int x, int y, int z, EntityType<?> type) {
        set(x, y, z, Blocks.SPAWNER);
        if (inside(at(x, y, z)) && level.getBlockEntity(at(x, y, z)) instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, RandomSource.create(at(x, y, z).asLong()));
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
        new StructureBuilder(level, origin).buildVillage();
    }

    /** 상인 5명 (월드 생성 때는 청크가 실제로 로딩될 때 따로 배치) */
    private void traders() {
        trader(12, 0, 12, "trader.tycooncraft.food", VillagerProfession.FARMER, traderStock("trader.tycooncraft.food", random));
        trader(20, 0, 12, "trader.tycooncraft.builder", VillagerProfession.MASON, traderStock("trader.tycooncraft.builder", random));
        trader(12, 0, 20, "trader.tycooncraft.gear", VillagerProfession.ARMORER, traderStock("trader.tycooncraft.gear", random));
        trader(20, 0, 20, "trader.tycooncraft.machines", VillagerProfession.TOOLSMITH, traderStock("trader.tycooncraft.machines", random));
        trader(16, 0, 11, "trader.tycooncraft.buyer", VillagerProfession.CLERIC, traderStock("trader.tycooncraft.buyer", random));
    }

    /**
     * 상인별 상품 후보 전체 (v0.18: 매일 이 중에서 무작위로 골라 진열). 사는 값은 v0.17.1 기준 2배.
     * 1동화=1원, 1철화=10원, 1금화=100원, 1에메랄드화=1000원.
     */
    public static List<MerchantOffer> traderOffers(String key) {
        Item cu = ModItems.COIN_COPPER.get(), ir = ModItems.COIN_IRON.get(), go = ModItems.COIN_GOLD.get(), em = ModItems.COIN_EMERALD.get();
        return switch (key) {
            case "trader.tycooncraft.food" -> List.of(
                    offer(ir, 2, Items.BREAD, 2), offer(ir, 2, Items.APPLE, 3), offer(ir, 4, Items.COOKED_BEEF, 4),
                    offer(go, 1, Items.GOLDEN_CARROT, 4), offer(ir, 2, Items.WHEAT_SEEDS, 16), offer(ir, 2, Items.CARROT, 8),
                    offer(ir, 4, Items.COCOA_BEANS, 8), offer(ir, 4, Items.EGG, 8), offer(ir, 4, Items.COOKED_PORKCHOP, 4),
                    offer(ir, 3, Items.COOKED_CHICKEN, 4), offer(ir, 2, Items.BAKED_POTATO, 6), offer(ir, 3, Items.PUMPKIN_PIE, 2),
                    offer(ir, 8, Items.CAKE, 1), offer(ir, 4, Items.HONEY_BOTTLE, 2), offer(ir, 2, Items.MELON_SLICE, 12),
                    offer(ir, 2, Items.SWEET_BERRIES, 12), offer(ir, 4, Items.COOKED_SALMON, 4), offer(em, 1, Items.GOLDEN_APPLE, 1),
                    offer(ir, 3, Items.SUGAR_CANE, 8), offer(ir, 2, Items.POTATO, 8), offer(ir, 2, Items.BEETROOT_SEEDS, 16),
                    offer(ir, 3, Items.MELON_SEEDS, 8), offer(ir, 3, Items.PUMPKIN_SEEDS, 8), offer(ir, 3, Items.MUSHROOM_STEW, 1));
            case "trader.tycooncraft.builder" -> List.of(
                    offer(ir, 2, Items.STONE_BRICKS, 16), offer(ir, 2, Items.BRICKS, 16), offer(ir, 2, Items.SMOOTH_STONE, 16),
                    offer(ir, 4, cu, 8, Items.GLASS, 16), offer(ir, 2, Items.OAK_PLANKS, 32), offer(ir, 2, Items.TORCH, 16),
                    offer(ir, 6, Items.LANTERN, 4), offer(ir, 4, Items.WHITE_WOOL, 8), offer(ir, 2, Items.SPRUCE_PLANKS, 32),
                    offer(ir, 10, Items.QUARTZ_BLOCK, 8), offer(ir, 3, Items.TERRACOTTA, 16), offer(ir, 4, Items.WHITE_CONCRETE, 16),
                    offer(ir, 4, Items.GRAY_CONCRETE, 16), offer(ir, 4, Items.BLACK_CONCRETE, 16), offer(ir, 6, Items.IRON_BARS, 16),
                    offer(ir, 4, Items.DEEPSLATE_TILES, 16), offer(ir, 2, Items.POLISHED_ANDESITE, 16), offer(ir, 3, Items.OAK_LOG, 16),
                    offer(ir, 8, Items.GLOWSTONE, 8), offer(go, 1, Items.SEA_LANTERN, 4), offer(ir, 2, Items.LADDER, 16),
                    offer(ir, 3, Items.SCAFFOLDING, 16), offer(ir, 4, Items.CHAIN, 8), offer(ir, 2, Items.SANDSTONE, 16),
                    offer(ir, 2, Items.OAK_DOOR, 2));
            case "trader.tycooncraft.gear" -> List.of(
                    offer(ir, 10, Items.IRON_PICKAXE, 1), offer(ir, 10, Items.IRON_SWORD, 1), offer(ir, 8, Items.IRON_HELMET, 1),
                    offer(ir, 16, Items.IRON_CHESTPLATE, 1), offer(ir, 14, Items.IRON_LEGGINGS, 1), offer(ir, 8, Items.IRON_BOOTS, 1),
                    offer(ir, 6, Items.SHIELD, 1), offer(go, 6, Items.DIAMOND_PICKAXE, 1), offer(go, 6, Items.DIAMOND_SWORD, 1),
                    offer(ir, 6, ModItems.LIGHT_AMMO.get(), 16), offer(ir, 8, ModItems.HEAVY_AMMO.get(), 8),
                    offer(ir, 6, ModItems.SHOTGUN_SHELL.get(), 8), offer(go, 12, ModItems.PISTOL.get(), 1),
                    offer(go, 30, ModItems.SMG.get(), 1), offer(go, 28, ModItems.SHOTGUN.get(), 1), offer(em, 5, ModItems.RIFLE.get(), 1),
                    offer(ir, 8, Items.CROSSBOW, 1), offer(ir, 6, Items.BOW, 1), offer(ir, 4, Items.ARROW, 16), offer(ir, 10, Items.IRON_AXE, 1),
                    offer(ir, 6, Items.IRON_SHOVEL, 1), offer(ir, 4, Items.SHEARS, 1), offer(ir, 8, Items.SPYGLASS, 1),
                    offer(go, 8, ModItems.VEST_1.get(), 1), offer(go, 5, ModItems.HELMET_1.get(), 1), offer(go, 5, ModItems.PANTS_1.get(), 1),
                    offer(go, 4, ModItems.BOOTS_1.get(), 1));
            case "trader.tycooncraft.machines" -> List.of(
                    offer(ir, 4, ModItems.CONVEYOR.get(), 8), offer(ir, 6, ModItems.LIFT_UP.get(), 2), offer(ir, 6, ModItems.LIFT_DOWN.get(), 2),
                    offer(ir, 12, ModItems.SELL_STATION.get(), 1), offer(ir, 12, ModItems.machineItem(MachineType.CRUSHER), 1),
                    offer(ir, 12, ModItems.machineItem(MachineType.SMELTER), 1), offer(ir, 16, ModItems.machineItem(MachineType.PRESS), 1),
                    offer(go, 4, ModItems.machineItem(MachineType.ALLOY_FURNACE), 1), offer(go, 4, ModItems.machineItem(MachineType.WIRE_DRAWER), 1),
                    offer(go, 4, ModItems.machineItem(MachineType.LATHE), 1), offer(em, 4, ModItems.machineItem(MachineType.ASSEMBLER), 1),
                    offer(go, 10, ModItems.machineItem(MachineType.CHEMICAL_REACTOR), 1), offer(go, 10, ModItems.machineItem(MachineType.ELECTROLYZER), 1),
                    offer(ir, 8, Items.HOPPER, 1), offer(ir, 2, Items.CHEST, 2), offer(ir, 2, Items.BARREL, 2), offer(ir, 4, Items.REDSTONE, 16),
                    offer(ir, 6, Items.PISTON, 2), offer(ir, 6, Items.OBSERVER, 2), offer(ir, 4, Items.REPEATER, 2), offer(ir, 6, Items.COMPARATOR, 1),
                    offer(ir, 4, Items.RAIL, 16), offer(go, 1, Items.POWERED_RAIL, 8), offer(ir, 8, Items.MINECART, 1),
                    offer(ir, 6, ModItems.FUEL_CANISTER.get(), 1), offer(ir, 6, ModItems.STEEL_PLATE.get(), 4), offer(ir, 4, ModItems.IRON_GEAR.get(), 4),
                    offer(ir, 3, ModItems.COPPER_WIRE.get(), 8));
            case "trader.tycooncraft.buyer" -> List.of(   // 상인이 사 주는 값은 그대로
                    offer(Items.ROTTEN_FLESH, 16, cu, 4), offer(Items.BONE, 8, cu, 5), offer(Items.STRING, 8, cu, 6),
                    offer(Items.SPIDER_EYE, 4, cu, 4), offer(Items.FEATHER, 16, cu, 5), offer(Items.LEATHER, 4, ir, 1),
                    offer(Items.GUNPOWDER, 4, ir, 1), offer(Items.SLIME_BALL, 4, ir, 1), offer(Items.ENDER_PEARL, 2, ir, 3),
                    offer(Items.PHANTOM_MEMBRANE, 2, ir, 1), offer(Items.BLAZE_ROD, 2, ir, 4), offer(Items.GHAST_TEAR, 1, ir, 5),
                    offer(Items.PRISMARINE_SHARD, 8, ir, 1), offer(Items.INK_SAC, 8, cu, 6), offer(Items.RABBIT_HIDE, 4, cu, 6),
                    offer(Items.GLOW_INK_SAC, 4, ir, 1), offer(Items.MAGMA_CREAM, 2, ir, 2), offer(Items.NAUTILUS_SHELL, 1, ir, 6),
                    offer(Items.HONEYCOMB, 4, ir, 1));
            default -> null;
        };
    }

    /** 오늘의 진열: 후보 중 무작위 몇 개, 값은 ±(0.85~1.25배) 흔들림. 후보 순서는 유지. */
    public static List<MerchantOffer> traderStock(String key, net.minecraft.util.RandomSource rnd) {
        List<MerchantOffer> pool = traderOffers(key);
        if (pool == null) return null;
        int n = Math.min(pool.size(), key.endsWith("gear") || key.endsWith("machines") ? 10 : 8);
        List<Integer> idx = new java.util.ArrayList<>();
        for (int i = 0; i < pool.size(); i++) idx.add(i);
        net.minecraft.Util.shuffle(idx, rnd);
        idx = new java.util.ArrayList<>(idx.subList(0, n));
        java.util.Collections.sort(idx);
        boolean buyer = key.endsWith("buyer");
        List<MerchantOffer> out = new java.util.ArrayList<>();
        for (int i : idx) {
            MerchantOffer o = pool.get(i);
            float f = 0.85f + rnd.nextFloat() * 0.4f;
            if (buyer) {   // 사 주는 값(결과 동전 수)이 흔들림
                ItemStack r = o.getResult().copy();
                r.setCount(Mth.clamp(Math.round(r.getCount() * (2.1f - f)), 1, 64));
                out.add(new MerchantOffer(o.getItemCostA(), o.getItemCostB(), r, Integer.MAX_VALUE, 0, 0f));
            } else {
                ItemCost a = o.getItemCostA();
                ItemCost na = new ItemCost(a.item().value(), Mth.clamp(Math.round(a.count() * f), 1, 64));
                out.add(new MerchantOffer(na, o.getItemCostB(), o.getResult().copy(), Integer.MAX_VALUE, 0, 0f));
            }
        }
        return out;
    }

    /** 테스트·명령어용: 갱단 아지트를 지금 바로 짓는다 */
    public static void hideout(ServerLevel level, BlockPos origin) {
        new StructureBuilder(level, origin).buildHideout();
    }

    /**
     * 카르텔 아지트 (31×31): 돌벽 + 철창, 모서리 감시탑 4개(용병 저격조), 창고 안 철문 금고실(상자 3) + 카르텔 두목,
     * 마당엔 마대 바리케이드·모닥불·깃발·주차된 무장 트럭과 경비 갱단.
     */
    private void buildHideout() {
        StructureBuilder b = this;
        RandomSource r = b.random;
        int N = 30;
        b.fill(0, 0, 0, N, 12, N, Blocks.AIR);
        b.foundation(0, -5, 0, N, -2, N, Blocks.DIRT);
        for (int x = 0; x <= N; x++)
            for (int z = 0; z <= N; z++) {
                float f = r.nextFloat();
                b.set(x, -1, z, f < 0.55f ? Blocks.COARSE_DIRT : f < 0.85f ? Blocks.GRAVEL : Blocks.PACKED_MUD);
            }
        // 외벽 (높이 4) + 철창
        for (int i = 0; i <= N; i++) {
            for (int[] p : new int[][]{{i, 0}, {i, N}, {0, i}, {N, i}}) {
                boolean gate = p[1] == N && p[0] >= 13 && p[0] <= 17;
                for (int y = 0; y < 4; y++) {
                    if (gate) continue;
                    float f = r.nextFloat();
                    b.set(p[0], y, p[1], f < 0.6f ? Blocks.STONE_BRICKS : f < 0.82f ? Blocks.CRACKED_STONE_BRICKS : Blocks.MOSSY_STONE_BRICKS);
                }
                if (!gate) b.set(p[0], 4, p[1], Blocks.IRON_BARS);
            }
        }
        // 정문 기둥 + 등불
        for (int x : new int[]{12, 18}) {
            b.fill(x, 0, N, x, 5, N, Blocks.POLISHED_DEEPSLATE);
            b.set(x, 6, N, Blocks.LANTERN);
        }
        b.fill(12, 5, N, 18, 5, N, Blocks.DEEPSLATE_BRICK_SLAB);
        // 감시탑 4개
        for (int[] c : new int[][]{{0, 0}, {N - 4, 0}, {0, N - 4}, {N - 4, N - 4}}) {
            int x0 = c[0], z0 = c[1];
            for (int[] k : new int[][]{{0, 0}, {4, 0}, {0, 4}, {4, 4}}) b.fill(x0 + k[0], 0, z0 + k[1], x0 + k[0], 9, z0 + k[1], Blocks.SPRUCE_LOG);
            b.fill(x0, 7, z0, x0 + 4, 7, z0 + 4, Blocks.SPRUCE_PLANKS);
            for (int i = 0; i <= 4; i++) {
                b.set(x0 + i, 8, z0, Blocks.SPRUCE_FENCE);
                b.set(x0 + i, 8, z0 + 4, Blocks.SPRUCE_FENCE);
                b.set(x0, 8, z0 + i, Blocks.SPRUCE_FENCE);
                b.set(x0 + 4, 8, z0 + i, Blocks.SPRUCE_FENCE);
            }
            b.fill(x0 - 1, 10, z0 - 1, x0 + 5, 10, z0 + 5, Blocks.SPRUCE_SLAB);
            b.fill(x0 + 2, 0, z0 + 2, x0 + 2, 6, z0 + 2, Blocks.AIR);
            for (int y = 0; y < 7; y++) b.set(x0 + 2, y, z0 + 3, Blocks.SPRUCE_PLANKS);
            for (int y = 0; y < 8; y++) b.set(x0 + 2, y, z0 + 2, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.NORTH));
            b.set(x0 + 1, 8, z0 + 1, Blocks.LANTERN);
            b.spawnMob(com.tycooncraft.registry.ModEntities.MERCENARY.get(), x0 + 3, 8, z0 + 1);
        }
        // 창고 (15×11, 높이 6)
        int wx0 = 7, wx1 = 21, wz0 = 5, wz1 = 15;
        b.fill(wx0, -1, wz0, wx1, -1, wz1, Blocks.POLISHED_ANDESITE);
        for (int y = 0; y <= 5; y++)
            for (int x = wx0; x <= wx1; x++)
                for (int z = wz0; z <= wz1; z++) {
                    boolean edge = x == wx0 || x == wx1 || z == wz0 || z == wz1;
                    if (!edge) continue;
                    boolean corner = (x == wx0 || x == wx1) && (z == wz0 || z == wz1);
                    b.set(x, y, z, corner ? Blocks.STRIPPED_DARK_OAK_LOG : y == 2 && (x - wx0) % 4 == 2 ? Blocks.IRON_BARS : Blocks.DARK_OAK_PLANKS);
                }
        b.fill(wx0, 6, wz0, wx1, 6, wz1, Blocks.DEEPSLATE_TILES);
        b.fill(wx0 + 1, 6, wz0 + 1, wx1 - 1, 6, wz1 - 1, Blocks.DEEPSLATE_TILE_SLAB);
        b.fill(13, 0, wz1, 15, 2, wz1, Blocks.AIR);                    // 창고 입구
        for (int x = wx0 + 3; x <= wx1 - 3; x += 4) b.set(x, 5, (wz0 + wz1) / 2, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        // 금고실: 철창 칸막이 + 철문 (부수고 들어가야 함)
        int vx0 = wx0 + 1, vx1 = wx0 + 5, vz0 = wz0 + 1, vz1 = wz0 + 4;
        for (int y = 0; y <= 4; y++) {
            for (int x = vx0; x <= vx1 + 1; x++) b.set(x, y, vz1 + 1, Blocks.IRON_BARS);
            for (int z = vz0; z <= vz1 + 1; z++) b.set(vx1 + 1, y, z, Blocks.IRON_BARS);
        }
        b.set(vx0 + 2, 0, vz1 + 1, Blocks.IRON_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH));
        b.set(vx0 + 2, 1, vz1 + 1, Blocks.IRON_DOOR.defaultBlockState().setValue(net.minecraft.world.level.block.DoorBlock.FACING, Direction.SOUTH)
                .setValue(net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER));
        b.chest(vx0, 0, vz0, Direction.SOUTH, GANG_VAULT);
        b.chest(vx0 + 2, 0, vz0, Direction.SOUTH, GANG_VAULT);
        b.chest(vx0 + 4, 0, vz0, Direction.SOUTH, GANG_VAULT);
        b.set(vx0 + 1, 0, vz0, Blocks.GOLD_BLOCK);
        b.set(vx0 + 3, 0, vz0, Blocks.GOLD_BLOCK);
        // 창고 안 물자: 술통·상자·작업대
        for (int x = wx1 - 5; x <= wx1 - 1; x++) {
            b.set(x, 0, wz0 + 1, Blocks.BARREL);
            if (x % 2 == 0) b.set(x, 1, wz0 + 1, Blocks.BARREL);
        }
        b.chest(wx1 - 1, 0, wz1 - 2, Direction.WEST, GANG_SUPPLY);
        b.set(wx1 - 1, 0, wz1 - 3, Blocks.SMITHING_TABLE);
        b.set(wx0 + 8, 0, wz0 + 6, Blocks.DARK_OAK_PLANKS);
        b.set(wx0 + 8, 1, wz0 + 6, Blocks.DARK_OAK_PRESSURE_PLATE);
        b.spawnMob(com.tycooncraft.registry.ModEntities.CARTEL_BOSS.get(), wx0 + 9, 0, wz0 + 5);
        b.spawnMob(com.tycooncraft.registry.ModEntities.MERCENARY.get(), wx0 + 11, 0, wz0 + 7);
        b.spawnMob(com.tycooncraft.registry.ModEntities.OUTLAW.get(), wx0 + 6, 0, wz0 + 8);
        // 마당: 마대 바리케이드, 모닥불, 깃발
        for (int x = 9; x <= 21; x++) if (x < 13 || x > 17) b.set(x, 0, 25, Blocks.MUD_BRICK_WALL);
        for (int x = 10; x <= 20; x++) if (x < 13 || x > 17) b.set(x, 1, 25, Blocks.MUD_BRICK_SLAB);
        b.set(24, 0, 20, Blocks.CAMPFIRE);
        for (int[] p : new int[][]{{23, 19}, {25, 19}, {23, 21}, {25, 21}}) b.set(p[0], 0, p[1], Blocks.SPRUCE_LOG);
        for (int y = 0; y < 6; y++) b.set(4, y, 20, Blocks.SPRUCE_FENCE);
        b.fill(5, 4, 20, 7, 5, 20, Blocks.RED_WOOL);
        b.set(6, 4, 20, Blocks.BLACK_WOOL);
        for (int[] p : new int[][]{{25, 7}, {26, 8}, {25, 9}, {24, 12}}) b.set(p[0], 0, p[1], r.nextBoolean() ? Blocks.BARREL : Blocks.HAY_BLOCK);
        // 경비 갱단 + 주차된 무장 트럭
        b.spawnMob(com.tycooncraft.registry.ModEntities.GANGSTER.get(), 14, 0, 24);
        b.spawnMob(com.tycooncraft.registry.ModEntities.GANGSTER.get(), 16, 0, 24);
        b.spawnMob(com.tycooncraft.registry.ModEntities.GANGSTER.get(), 23, 0, 23);
        b.spawnMob(com.tycooncraft.registry.ModEntities.OUTLAW.get(), 6, 0, 24);
        b.spawnMob(com.tycooncraft.registry.ModEntities.GANG_TRUCK.get(), 3, 0, 12);
    }

    private void buildVillage() {
        StructureBuilder b = this;
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

        if (clip == null) b.traders();

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
