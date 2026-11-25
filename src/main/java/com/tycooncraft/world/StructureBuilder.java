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
import net.minecraft.world.item.alchemy.Potions;
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

    /** v0.25.0 경비병: 큰 마을(64)은 입구 4곳+광장 2곳, 옛 마을(32)은 가운데 3명. 모자란 만큼만 채움 */
    public static int spawnGuards(ServerLevel level, BlockPos origin, int size) {
        int[][] spots = size >= 60 ? new int[][]{{31, 4}, {31, 59}, {4, 31}, {59, 31}, {26, 30}, {37, 33}} : new int[][]{{16, 8}, {16, 24}, {8, 16}};
        BlockPos center = origin.offset(size / 2, 0, size / 2);
        var box = new net.minecraft.world.phys.AABB(origin).expandTowards(size, 12, size).inflate(4);
        int have = level.getEntitiesOfClass(com.tycooncraft.village.VillageGuard.class, box).size(), made = 0;
        for (int i = have; i < spots.length; i++) {
            BlockPos p = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, origin.offset(spots[i][0], 0, spots[i][1]));
            var g = com.tycooncraft.registry.ModEntities.VILLAGE_GUARD.get().create(level);
            if (g == null) continue;
            g.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
            g.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
            g.setHome(new BlockPos(center.getX(), p.getY(), center.getZ()));
            level.addFreshEntity(g);
            made++;
        }
        return made;
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

    public static final int KIT_W = 15, KIT_D = 11;

    /** 공장 키트: 기본 공장 (분쇄 → 제련 → 압착 → 판매 라인 + 작업 공간). 네 면 모두 출입구 */
    public static void starterFactory(ServerLevel level, BlockPos origin) {
        StructureBuilder b = new StructureBuilder(level, origin);
        int X = KIT_W - 1, Z = KIT_D - 1;
        b.fill(0, 0, 0, X, 7, Z, Blocks.AIR);
        b.foundation(0, -5, 0, X, -2, Z, Blocks.STONE_BRICKS);
        b.hollow(0, -1, 0, X, 5, Z, Blocks.STONE_BRICKS);
        b.fill(1, -1, 1, X - 1, -1, Z - 1, Blocks.POLISHED_ANDESITE);
        for (int x = 1; x < X; x++) b.set(x, -1, 4, Blocks.YELLOW_CONCRETE);
        for (int x = 1; x < X; x++) b.set(x, -1, 6, Blocks.YELLOW_CONCRETE);
        // 기둥 + 창
        for (int x : new int[]{0, 7, X}) for (int z : new int[]{0, Z}) b.fill(x, 0, z, x, 4, z, Blocks.DEEPSLATE_BRICKS);
        for (int z : new int[]{0, 5, Z}) for (int x : new int[]{0, X}) b.fill(x, 0, z, x, 4, z, Blocks.DEEPSLATE_BRICKS);
        for (int[] seg : new int[][]{{1, 2}, {12, 13}}) for (int z : new int[]{0, Z}) b.fill(seg[0], 2, z, seg[1], 3, z, Blocks.GLASS);
        for (int z : new int[]{0, Z}) b.fill(8, 2, z, 11, 3, z, Blocks.GLASS);
        for (int x : new int[]{0, X}) { b.fill(x, 2, 1, x, 3, 3, Blocks.GLASS); b.fill(x, 2, 7, x, 3, 9, Blocks.GLASS); }
        // 출입구 (앞뒤 넓게, 양옆 작게)
        for (int z : new int[]{0, Z}) b.fill(3, 0, z, 6, 2, z, Blocks.AIR);
        for (int x : new int[]{0, X}) b.fill(x, 0, 4, x, 2, 6, Blocks.AIR);
        b.fill(0, 4, 4, 0, 4, 6, Blocks.STONE_BRICKS);
        b.fill(X, 4, 4, X, 4, 6, Blocks.STONE_BRICKS);
        // 지붕 + 조명
        b.fill(0, 6, 0, X, 6, Z, Blocks.DEEPSLATE_TILE_SLAB);
        for (int[] p : new int[][]{{3, 2}, {11, 2}, {3, 8}, {11, 8}, {7, 5}}) b.set(p[0], 5, p[1], Blocks.SEA_LANTERN);
        // 생산 라인: 원석을 첫 벨트에 던지면 → 분쇄 → 제련 → 압착 → 판매
        Block c = ModBlocks.CONVEYOR.get();
        b.line(2, 0, 5, c, m(MachineType.CRUSHER), c, m(MachineType.SMELTER), c, m(MachineType.PRESS), c, ModBlocks.SELL_STATION.get());
        b.set(1, 0, 5, Blocks.AIR);
        // v0.27.0 전기: 보일러(아래 물 원천) → 증기기관 → 기계 위를 지나는 전선 (기계는 1.5배 빨라짐)
        b.set(11, -1, 5, Blocks.WATER);
        b.set(11, 0, 5, ModBlocks.BOILER.get().defaultBlockState().setValue(com.tycooncraft.power.BoilerBlock.FACING, Direction.SOUTH));
        b.set(10, 0, 5, ModBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(com.tycooncraft.power.SteamEngineBlock.FACING, Direction.SOUTH));
        for (int x = 2; x <= 10; x++) b.set(x, 1, 5, ModBlocks.POWER_CABLE.get().defaultBlockState());
        if (b.inside(b.at(11, 0, 5)) && b.level.getBlockEntity(b.at(11, 0, 5)) instanceof com.tycooncraft.power.BoilerBlockEntity boiler)
            boiler.fuel().insertItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COAL, 16), false);
        // 작업 공간
        b.set(2, 0, 1, Blocks.CRAFTING_TABLE);
        b.set(9, 0, 1, Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.SOUTH));
        b.set(10, 0, 1, Blocks.FURNACE.defaultBlockState().setValue(net.minecraft.world.level.block.FurnaceBlock.FACING, Direction.SOUTH));
        b.set(12, 0, 1, ModBlocks.EXCHANGE.get());
        for (int x = 9; x <= 12; x++) b.chest(x, 0, Z - 1, Direction.NORTH, null);
        b.set(2, 0, Z - 1, Blocks.SMITHING_TABLE);
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
        set(cx - 1, 0, cz + 1, Blocks.BARREL);
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
                    offer(go, 4, ModItems.BOOTS_1.get(), 1), offer(go, 18, ModItems.POLICE_SHIELD.get(), 1));
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
            // ── v0.24.0 산업 마을 새 상인 ──
            case "trader.tycooncraft.gun" -> List.of(
                    offer(go, 12, ModItems.PISTOL.get(), 1), offer(go, 18, ModItems.DUAL_PISTOLS.get(), 1), offer(go, 30, ModItems.SMG.get(), 1),
                    offer(go, 28, ModItems.SHOTGUN.get(), 1), offer(em, 5, ModItems.RIFLE.get(), 1), offer(em, 6, ModItems.VSS.get(), 1),
                    offer(em, 8, ModItems.SNIPER.get(), 1), offer(em, 8, ModItems.RPG.get(), 1),
                    offer(ir, 6, ModItems.LIGHT_AMMO.get(), 32), offer(ir, 8, ModItems.HEAVY_AMMO.get(), 16), offer(ir, 6, ModItems.SHOTGUN_SHELL.get(), 16),
                    offer(go, 3, ModItems.ROCKET.get(), 2), offer(go, 4, ModItems.HISPANO_AMMO.get(), 4),
                    offer(go, 3, ModItems.SILENCER.get(), 1), offer(go, 8, ModItems.SILENCER_ADVANCED.get(), 1),
                    offer(ir, 8, ModItems.FRAG_GRENADE.get(), 2), offer(ir, 6, ModItems.FLASH_GRENADE.get(), 2), offer(ir, 5, ModItems.SMOKE_GRENADE.get(), 2),
                    offer(go, 1, ModItems.GUN_BARREL.get(), 2), offer(go, 2, ModItems.GUN_RECEIVER.get(), 1), offer(go, 1, ModItems.GUN_STOCK.get(), 1),
                    offer(go, 2, ModItems.GUN_WORKBENCH.get(), 1), offer(go, 8, ModItems.VEST_1.get(), 1), offer(go, 16, ModItems.VEST_2.get(), 1),
                    offer(go, 3, ModItems.PARACHUTE.get(), 1));
            case "trader.tycooncraft.engineer" -> List.of(
                    offer(go, 2, ModItems.BOILER.get(), 1), offer(go, 2, ModItems.STEAM_ENGINE.get(), 1),
                    offer(ir, 6, ModItems.POWER_CABLE.get(), 16), offer(ir, 10, ModItems.POWER_CABLE.get(), 32),
                    offer(go, 3, ModItems.SOLAR_GENERATOR.get(), 1), offer(go, 6, ModItems.ACCUMULATOR.get(), 1),
                    offer(go, 5, ModItems.ROBOT_ARM.get(), 1), offer(go, 2, ModItems.SORTER.get(), 2), offer(go, 1, ModItems.WAREHOUSE.get(), 1),
                    offer(em, 3, ModItems.machineItem(MachineType.INDUCTION_FURNACE), 1), offer(em, 5, ModItems.machineItem(MachineType.ARC_FURNACE), 1),
                    offer(ir, 8, ModItems.CIRCUIT.get(), 4), offer(go, 2, ModItems.ADVANCED_CIRCUIT.get(), 2), offer(go, 3, ModItems.ELECTRIC_MOTOR.get(), 1),
                    offer(ir, 6, ModItems.COPPER_COIL.get(), 4), offer(go, 1, ModItems.BATTERY_CELL.get(), 2), offer(ir, 8, ModItems.BALL_BEARING.get(), 8),
                    offer(ir, 4, ModItems.BOLT.get(), 32), offer(go, 2, ModItems.TRANSFORMER.get(), 1), offer(ir, 6, ModItems.STEEL_GEAR.get(), 4),
                    offer(go, 2, ModItems.CCTV.get(), 1), offer(go, 3, ModItems.CCTV_SCREEN.get(), 1));
            case "trader.tycooncraft.scrap_buyer" -> List.of(   // 공산품 매입: 시세(판매소 포화)와 상관없이 일정한 값
                    offer(ModItems.IRON_PLATE.get(), 16, cu, 40), offer(ModItems.COPPER_PLATE.get(), 16, cu, 40), offer(ModItems.STEEL_INGOT.get(), 8, cu, 20),
                    offer(ModItems.STEEL_PLATE.get(), 8, cu, 30), offer(ModItems.IRON_GEAR.get(), 8, cu, 30), offer(ModItems.STEEL_GEAR.get(), 4, cu, 30),
                    offer(ModItems.CIRCUIT.get(), 4, cu, 25), offer(ModItems.ADVANCED_CIRCUIT.get(), 2, ir, 6), offer(ModItems.MECHANISM.get(), 2, ir, 6),
                    offer(ModItems.COPPER_WIRE.get(), 32, cu, 30), offer(ModItems.STEEL_ROD.get(), 16, cu, 30), offer(ModItems.ENGINE.get(), 1, ir, 7),
                    offer(ModItems.ELECTRIC_MOTOR.get(), 1, ir, 9), offer(ModItems.BALL_BEARING.get(), 8, cu, 30), offer(ModItems.BOLT.get(), 32, cu, 20),
                    offer(ModItems.H_BEAM.get(), 4, cu, 40), offer(ModItems.REBAR.get(), 16, cu, 40), offer(ModItems.CEMENT.get(), 16, cu, 30),
                    offer(ModItems.PLASTIC_PELLET.get(), 16, cu, 30), offer(ModItems.ALUMINUM_INGOT.get(), 8, cu, 40), offer(ModItems.MICROCHIP.get(), 4, ir, 5),
                    offer(ModItems.GOLD_WIRE.get(), 16, ir, 5), offer(ModItems.STEEL_PIPE.get(), 8, cu, 40), offer(ModItems.PCB.get(), 4, ir, 4));
            case "trader.tycooncraft.miner" -> List.of(
                    offer(ir, 2, Items.COAL, 16), offer(ir, 3, Items.RAW_IRON, 16), offer(ir, 3, Items.RAW_COPPER, 16), offer(ir, 6, Items.RAW_GOLD, 8),
                    offer(ir, 3, Items.REDSTONE, 16), offer(ir, 3, Items.LAPIS_LAZULI, 16), offer(ir, 4, ModItems.SULFUR.get(), 8),
                    offer(ir, 4, ModItems.RAW_BAUXITE.get(), 8), offer(ir, 3, ModItems.ROCK_SALT.get(), 8), offer(ir, 4, Items.QUARTZ, 16),
                    offer(ir, 3, Items.AMETHYST_SHARD, 8), offer(ir, 2, Items.CLAY_BALL, 16), offer(ir, 6, Items.OBSIDIAN, 4), offer(go, 2, Items.DIAMOND, 1),
                    offer(ir, 2, Items.FLINT, 16), offer(ir, 4, Items.IRON_INGOT, 8), offer(ir, 4, Items.COPPER_INGOT, 16), offer(ir, 6, Items.GOLD_INGOT, 4),
                    offer(ir, 2, Items.COAL_BLOCK, 2), offer(ir, 4, Items.GLOWSTONE_DUST, 8), offer(ir, 2, Items.SAND, 32), offer(ir, 2, Items.GRAVEL, 32));
            case "trader.tycooncraft.chemist" -> List.of(
                    offer(ir, 4, ModItems.SULFURIC_ACID.get(), 2), offer(ir, 4, ModItems.CAUSTIC_SODA.get(), 2), offer(ir, 5, ModItems.GAS_CYLINDER.get(), 1),
                    offer(ir, 5, ModItems.CHLORINE_CYLINDER.get(), 1), offer(ir, 3, ModItems.PLASTIC_PELLET.get(), 8), offer(ir, 4, ModItems.EPOXY_RESIN.get(), 2),
                    offer(ir, 2, ModItems.LATEX.get(), 4), offer(ir, 3, ModItems.RUBBER.get(), 8), offer(ir, 2, ModItems.QUICKLIME.get(), 8),
                    offer(ir, 3, ModItems.COKE.get(), 8), offer(ir, 4, ModItems.GRAPHITE.get(), 4), offer(ir, 2, ModItems.SILICA_DUST.get(), 8),
                    offer(ir, 4, ModItems.ALUMINA.get(), 4), offer(ir, 2, ModItems.SALT.get(), 8), offer(ir, 2, Items.GLASS_BOTTLE, 8),
                    offer(ir, 4, Items.GUNPOWDER, 8), offer(ir, 6, Items.BLAZE_POWDER, 4), offer(ir, 3, Items.SLIME_BALL, 4),
                    offer(ir, 4, ModItems.FIBERGLASS.get(), 4), offer(ir, 3, ModItems.GASKET.get(), 8));
            case "trader.tycooncraft.vehicle" -> List.of(
                    offer(ir, 6, ModItems.FUEL_CANISTER.get(), 2), offer(ir, 4, ModItems.WHEEL.get(), 4), offer(go, 9, ModItems.ENGINE.get(), 1),
                    offer(go, 2, ModItems.TRANSMISSION.get(), 1), offer(ir, 6, ModItems.STEEL_PLATE.get(), 8), offer(em, 1, ModItems.CAR.get(), 1),
                    offer(em, 2, ModItems.LOCOMOTIVE.get(), 1), offer(em, 3, ModItems.HELICOPTER.get(), 1), offer(go, 4, ModItems.DRILL_HEAD.get(), 1),
                    offer(ir, 4, Items.RAIL, 32), offer(go, 1, Items.POWERED_RAIL, 16), offer(ir, 8, Items.MINECART, 1),
                    offer(go, 3, ModItems.SHIP_MOTOR.get(), 1), offer(go, 2, ModItems.SHIP_HELM.get(), 1), offer(ir, 6, ModItems.SHIP_FLOAT.get(), 4),
                    offer(go, 2, ModItems.SHIP_BOOSTER.get(), 1), offer(ir, 4, Items.SADDLE, 1), offer(ir, 6, Items.OAK_BOAT, 1),
                    offer(go, 3, ModItems.PARACHUTE.get(), 1), offer(ir, 4, Items.LEAD, 2));
            case "trader.tycooncraft.pharmacist" -> List.of(
                    potion(ir, 6, Potions.HEALING), potion(ir, 10, Potions.STRONG_HEALING), potion(ir, 8, Potions.REGENERATION),
                    potion(ir, 8, Potions.FIRE_RESISTANCE), potion(ir, 6, Potions.SWIFTNESS), potion(ir, 6, Potions.NIGHT_VISION),
                    potion(ir, 6, Potions.WATER_BREATHING), potion(ir, 10, Potions.STRENGTH), potion(ir, 8, Potions.LEAPING),
                    potion(ir, 6, Potions.SLOW_FALLING), potion(ir, 12, Potions.LONG_REGENERATION), potion(ir, 8, Potions.INVISIBILITY),
                    offer(em, 1, Items.GOLDEN_APPLE, 1), offer(ir, 4, Items.MILK_BUCKET, 1), offer(ir, 4, Items.HONEY_BOTTLE, 2),
                    offer(go, 1, Items.GOLDEN_CARROT, 8), offer(ir, 3, Items.GLISTERING_MELON_SLICE, 2), offer(em, 4, Items.TOTEM_OF_UNDYING, 1),
                    offer(ir, 6, Items.EXPERIENCE_BOTTLE, 4), offer(ir, 3, Items.BEETROOT_SOUP, 1));
            default -> null;
        };
    }

    private static MerchantOffer potion(Item cost, int n, net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> p) {
        return new MerchantOffer(new ItemCost(cost, n), Optional.empty(),
                net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, p), Integer.MAX_VALUE, 0, 0f);
    }

    /** v0.27.0 물가 조정: 상점은 기준가의 2배(이전 3배), 시간 기준 장비(총·기계)는 0.7배 */
    public static final double TRADER_MARKUP = 2.0, TRADER_PAY = 0.8, HOURS_BUY = 0.7;
    /** v0.26.2: 바로 쓰는 물건(총·탄약·수류탄·방어구·탈것·드론)은 상점에서 3배 비싸게 — 돈이 빠져나가는 곳 */
    public static final double USABLE_MARKUP = 3.0;

    public static boolean usable(Item it) {
        return it instanceof com.tycooncraft.gun.GunItem || it instanceof com.tycooncraft.gun.GrenadeItem || it instanceof com.tycooncraft.gear.GearItem
                || it instanceof com.tycooncraft.gear.PoliceShieldItem || it instanceof com.tycooncraft.vehicle.VehicleItem || it instanceof com.tycooncraft.vehicle.DroneItem
                || com.tycooncraft.gun.GunCrafting.isGunOrAmmo(it) || it == ModItems.PARACHUTE.get() || it == ModItems.FUEL_CANISTER.get()
                || it instanceof com.tycooncraft.mob.carrier.DroneSwarmItem || it instanceof com.tycooncraft.mech.ProtoDestroyerItem
                || it == ModItems.SILENCER.get() || it == ModItems.SILENCER_ADVANCED.get();
    }

    public static double usableMult(Item it) {
        return usable(it) ? USABLE_MARKUP : 1.0;
    }

    /** 핵심 장비는 "손노동 몇 시간어치"로 값을 매김 (권총 = 1시간) */
    public static double hours(Item it) {
        if (it == ModItems.PISTOL.get()) return 1;
        if (it == ModItems.DUAL_PISTOLS.get()) return 1.5;
        if (it == ModItems.SMG.get() || it == ModItems.SHOTGUN.get()) return 2;
        if (it == ModItems.RIFLE.get()) return 3;
        if (it == ModItems.VSS.get()) return 4;
        if (it == ModItems.SNIPER.get() || it == ModItems.RPG.get()) return 5;
        if (it == ModItems.POLICE_SHIELD.get()) return 1;
        if (it == ModItems.VEST_1.get()) return 0.6;
        if (it == ModItems.HELMET_1.get() || it == ModItems.PANTS_1.get()) return 0.4;
        if (it == ModItems.BOOTS_1.get()) return 0.3;
        if (it == ModItems.SELL_STATION.get()) return 0.2;
        if (it == ModItems.machineItem(MachineType.CRUSHER) || it == ModItems.machineItem(MachineType.SMELTER)) return 0.6;
        if (it == ModItems.machineItem(MachineType.PRESS)) return 1;
        if (it == ModItems.machineItem(MachineType.WIRE_DRAWER) || it == ModItems.machineItem(MachineType.LATHE) || it == ModItems.machineItem(MachineType.ALLOY_FURNACE)) return 1.5;
        if (it == ModItems.machineItem(MachineType.ASSEMBLER)) return 4;
        if (it == ModItems.machineItem(MachineType.CHEMICAL_REACTOR) || it == ModItems.machineItem(MachineType.ELECTROLYZER)) return 5;
        if (it == ModItems.machineItem(MachineType.INDUCTION_FURNACE)) return 4;
        if (it == ModItems.machineItem(MachineType.ARC_FURNACE)) return 6;
        return 0;
    }

    /** 자동 시험용: 흔들림 없이 이 상품을 파는 값 (원) */
    public static long stockOf(String trader, MerchantOffer o) {
        Item it = o.getResult().getItem();
        double h = hours(it);
        if (h > 0) return Math.round(h * com.tycooncraft.economy.PriceTable.MANUAL_HOUR * HOURS_BUY * o.getResult().getCount());
        return Math.round(com.tycooncraft.economy.PriceTable.basePrice(it) * o.getResult().getCount() * TRADER_MARKUP);
    }

    /** 금액을 한 칸(64개 이하)의 동전으로 */
    private static ItemStack oneStack(long won) {
        var ds = new java.util.ArrayList<>(com.tycooncraft.economy.Coins.denominations());
        java.util.Collections.reverse(ds);   // 작은 단위부터: 64개 안에 들어가는 가장 작은 단위 = 가장 정확
        for (Item c : ds) {
            long v = ((com.tycooncraft.economy.CoinItem) c).getValue();
            if (won >= v && won / v <= 64) return new ItemStack(c, (int) Math.max(1, Math.round((double) won / v)));
        }
        return new ItemStack(ModItems.COIN_COPPER.get(), (int) Math.min(64, won));
    }

    /** 오늘의 진열: 후보 중 무작위 몇 개, 값은 ±(0.85~1.25배) 흔들림. 후보 순서는 유지. */
    public static List<MerchantOffer> traderStock(String key, net.minecraft.util.RandomSource rnd) {
        List<MerchantOffer> pool = traderOffers(key);
        if (pool == null) return null;
        // v0.24.0: 진열 늘림 (장비·기계·총기·기계공학 14개, 나머지 12개)
        int n = Math.min(pool.size(), key.endsWith("gear") || key.endsWith("machines") || key.endsWith("gun") || key.endsWith("engineer") ? 14 : 12);
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
            double price = buyer ? com.tycooncraft.economy.PriceTable.basePrice(o.getItemCostA().item().value())
                    : com.tycooncraft.economy.PriceTable.basePrice(o.getResult().getItem());
            double hrs = buyer ? 0 : hours(o.getResult().getItem());
            if (hrs > 0) {   // 시간 기준 장비 (±흔들림 포함)
                long won = Math.round(hrs * com.tycooncraft.economy.PriceTable.MANUAL_HOUR * HOURS_BUY * usableMult(o.getResult().getItem()) * o.getResult().getCount() * f);
                ItemStack[] c = com.tycooncraft.economy.VillagerCoins.coinsForTest((int) won);
                out.add(new MerchantOffer(new ItemCost(c[0].getItem(), c[0].getCount()),
                        c.length > 1 ? Optional.of(new ItemCost(c[1].getItem(), c[1].getCount())) : Optional.empty(),
                        o.getResult().copy(), Integer.MAX_VALUE, 0, 0f));
                continue;
            }
            if (price > 0) {   // 가격표 기준 (상인은 기준가의 3배에 팔고, 0.8배에 사 줌)
                if (buyer) {
                    long won = Math.max(1, Math.round(price * o.getItemCostA().count() * TRADER_PAY * (2.1f - f)));
                    out.add(new MerchantOffer(o.getItemCostA(), Optional.empty(), oneStack(won), Integer.MAX_VALUE, 0, 0f));
                } else {
                    long won = Math.max(1, Math.round(price * o.getResult().getCount() * TRADER_MARKUP * usableMult(o.getResult().getItem()) * f));
                    ItemStack[] c = com.tycooncraft.economy.VillagerCoins.coinsForTest((int) Math.min(Integer.MAX_VALUE, won));
                    out.add(new MerchantOffer(new ItemCost(c[0].getItem(), c[0].getCount()),
                            c.length > 1 ? Optional.of(new ItemCost(c[1].getItem(), c[1].getCount())) : Optional.empty(),
                            o.getResult().copy(), Integer.MAX_VALUE, 0, 0f));
                }
                continue;
            }
            // 가격표에 없는 것: 적어 둔 동전 금액 × 경제 배율 (v0.26.0)
            if (buyer) {   // 사 주는 값(결과 동전)이 흔들림
                long won = Math.max(1, Math.round(com.tycooncraft.economy.Coins.valueOf(o.getResult()) * com.tycooncraft.economy.PriceTable.ECON * (2.1f - f)));
                out.add(new MerchantOffer(o.getItemCostA(), o.getItemCostB(), oneStack(won), Integer.MAX_VALUE, 0, 0f));
            } else {
                ItemCost a = o.getItemCostA();
                long won = Math.max(1, Math.round(com.tycooncraft.economy.Coins.valueOf(new ItemStack(a.item().value(), a.count())) * com.tycooncraft.economy.PriceTable.ECON
                        * usableMult(o.getResult().getItem()) * f));
                ItemStack[] c = com.tycooncraft.economy.VillagerCoins.coinsForTest((int) Math.min(Integer.MAX_VALUE, won));
                out.add(new MerchantOffer(new ItemCost(c[0].getItem(), c[0].getCount()),
                        c.length > 1 ? Optional.of(new ItemCost(c[1].getItem(), c[1].getCount())) : Optional.empty(),
                        o.getResult().copy(), Integer.MAX_VALUE, 0, 0f));
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

    // ── v0.24.0 산업 마을 (64×64, 기존의 2배) ──

    /** 상인 12명: {x, z, 번역 키, 직업} */
    private static final Object[][] TRADERS = {
            {22, 22, "trader.tycooncraft.food", VillagerProfession.FARMER, Blocks.YELLOW_WOOL},
            {26, 22, "trader.tycooncraft.builder", VillagerProfession.MASON, Blocks.LIGHT_GRAY_WOOL},
            {37, 22, "trader.tycooncraft.gear", VillagerProfession.ARMORER, Blocks.RED_WOOL},
            {41, 22, "trader.tycooncraft.gun", VillagerProfession.WEAPONSMITH, Blocks.BLACK_WOOL},
            {22, 41, "trader.tycooncraft.machines", VillagerProfession.TOOLSMITH, Blocks.BLUE_WOOL},
            {26, 41, "trader.tycooncraft.engineer", VillagerProfession.LIBRARIAN, Blocks.ORANGE_WOOL},
            {37, 41, "trader.tycooncraft.buyer", VillagerProfession.CLERIC, Blocks.GREEN_WOOL},
            {41, 41, "trader.tycooncraft.scrap_buyer", VillagerProfession.LEATHERWORKER, Blocks.BROWN_WOOL},
            {22, 26, "trader.tycooncraft.miner", VillagerProfession.CARTOGRAPHER, Blocks.CYAN_WOOL},
            {22, 37, "trader.tycooncraft.chemist", VillagerProfession.SHEPHERD, Blocks.LIME_WOOL},
            {41, 26, "trader.tycooncraft.vehicle", VillagerProfession.FLETCHER, Blocks.WHITE_WOOL},
            {41, 37, "trader.tycooncraft.pharmacist", VillagerProfession.BUTCHER, Blocks.PINK_WOOL},
    };
    public static final int VILLAGE_SIZE = 64;

    /** 상인 12명 + 주민 + 경비 골렘 (월드 생성 때는 청크가 실제로 로딩될 때 따로 배치) */
    private void traders() {
        for (Object[] t : TRADERS) {
            String key = (String) t[2];
            trader((int) t[0], 0, (int) t[1], key, (VillagerProfession) t[3], traderStock(key, random));
        }
        // 오가는 주민 (일거리 블록을 차지하지 않는 주민) + 아이들
        int[][] spots = {{31, 12}, {31, 52}, {12, 31}, {52, 31}, {26, 32}, {40, 30}, {31, 24}, {31, 39}, {8, 20}, {20, 50}, {50, 8}, {54, 50}};
        for (int i = 0; i < spots.length; i++) {
            int[] sp = spots[i];
            final boolean baby = i >= 8;
            spawnMob(EntityType.VILLAGER, sp[0], 0, sp[1], v -> {
                v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.NITWIT));
                if (baby) v.setAge(-24000);
                v.getPersistentData().putBoolean("tycooncraft_resident", true);
            });
        }
        spawnMob(EntityType.IRON_GOLEM, 31, 0, 18, g -> g.setPlayerCreated(false));
        spawnMob(EntityType.IRON_GOLEM, 31, 0, 46, g -> g.setPlayerCreated(false));
    }

    private void buildVillage() {
        StructureBuilder b = this;
        final int N = VILLAGE_SIZE - 1;
        b.fill(0, 0, 0, N, 16, N, Blocks.AIR);
        b.fill(0, -1, 0, N, -1, N, Blocks.GRASS_BLOCK);
        b.foundation(0, -10, 0, N, -2, N, Blocks.DIRT);

        // 큰길 (+자, 4칸) — 가장자리는 석재 벽돌
        b.fill(30, -1, 0, 33, -1, N, Blocks.POLISHED_ANDESITE);
        b.fill(0, -1, 30, N, -1, 33, Blocks.POLISHED_ANDESITE);
        for (int i = 0; i <= N; i++) {
            if (i < 19 || i > 44) {
                b.set(29, -1, i, Blocks.STONE_BRICKS); b.set(34, -1, i, Blocks.STONE_BRICKS);
                b.set(i, -1, 29, Blocks.STONE_BRICKS); b.set(i, -1, 34, Blocks.STONE_BRICKS);
            }
        }
        // v0.26.0 블랙마켓 맨홀 (서쪽 큰길 바닥)
        b.set(14, -1, 31, ModBlocks.MANHOLE.get());
        // 골목길
        b.fill(9, -1, 19, 9, -1, 44, Blocks.DIRT_PATH);
        b.fill(19, -1, 9, 44, -1, 9, Blocks.DIRT_PATH);
        b.fill(19, -1, 54, 44, -1, 54, Blocks.DIRT_PATH);
        b.fill(54, -1, 19, 54, -1, 44, Blocks.DIRT_PATH);

        // 중앙 광장
        b.fill(19, -1, 19, 44, -1, 44, Blocks.STONE_BRICKS);
        b.fill(20, -1, 20, 43, -1, 43, Blocks.SMOOTH_STONE);
        for (int x = 21; x <= 42; x += 3) for (int z = 21; z <= 42; z += 3) b.set(x, -1, z, Blocks.POLISHED_ANDESITE);
        // 분수 (8×8) + 시계탑 기둥
        b.fill(28, -1, 28, 35, -1, 35, Blocks.CHISELED_STONE_BRICKS);
        b.fill(28, 0, 28, 35, 0, 35, Blocks.STONE_BRICK_WALL);
        b.fill(29, 0, 29, 34, 0, 34, Blocks.AIR);
        b.fill(29, -1, 29, 34, -1, 34, Blocks.WATER);
        b.fill(31, -1, 31, 32, 6, 32, Blocks.STONE_BRICKS);
        b.fill(31, 7, 31, 32, 7, 32, Blocks.CHISELED_STONE_BRICKS);
        b.fill(30, 8, 30, 33, 8, 33, Blocks.STONE_BRICK_SLAB);
        for (int[] c : new int[][]{{30, 30}, {33, 30}, {30, 33}, {33, 33}}) b.set(c[0], 9, c[1], Blocks.LANTERN);
        b.set(31, 9, 31, Blocks.BELL);
        // 환전기 4대, 판매소 2대
        for (int[] c : new int[][]{{27, 27}, {36, 27}, {27, 36}, {36, 36}}) b.set(c[0], 0, c[1], ModBlocks.EXCHANGE.get());
        b.set(24, 0, 31, ModBlocks.SELL_STATION.get());
        b.set(39, 0, 32, ModBlocks.SELL_STATION.get());
        // 노점 12개
        for (Object[] t : TRADERS) b.stall((int) t[0], (int) t[1], (Block) t[4]);
        // 가로등
        for (int i = 4; i <= N - 4; i += 8) {
            if (i > 18 && i < 45) continue;
            b.lamp(28, i); b.lamp(35, i); b.lamp(i, 28); b.lamp(i, 35);
        }
        for (int[] c : new int[][]{{19, 19}, {44, 19}, {19, 44}, {44, 44}}) b.lamp(c[0], c[1]);

        if (clip == null) b.traders();

        // 주택가 (북서·남서) 8채
        b.house(2, 2, true); b.house(11, 2, true); b.house(2, 11, true); b.house(11, 11, true);
        b.house(2, 47, false); b.house(11, 47, false); b.house(2, 56, false); b.house(11, 56, false);
        // 북동: 공장 지구 (공장·굴뚝·물탱크)
        b.industryHall(46, 2);
        b.waterTower(51, 21);
        // 남동: 창고 + 밭
        b.storageYard(46, 47);
        b.fill(56, -1, 47, 62, -1, 61, Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        b.fill(59, -1, 47, 59, -1, 61, Blocks.WATER);
        BlockState wheat = Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7);
        BlockState carrot = Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7);
        b.fill(56, 0, 47, 58, 0, 61, wheat);
        b.fill(60, 0, 47, 62, 0, 61, carrot);
        // 서쪽 끝: 사격장 (총기상 손님용)
        b.range(2, 22);
        // 울타리 (큰길 입구는 열어 둠) + 입구 아치
        for (int i = 0; i <= N; i++) {
            if (i >= 29 && i <= 34) continue;
            b.set(i, 0, 0, Blocks.SPRUCE_FENCE); b.set(i, 0, N, Blocks.SPRUCE_FENCE);
            b.set(0, 0, i, Blocks.SPRUCE_FENCE); b.set(N, 0, i, Blocks.SPRUCE_FENCE);
        }
        for (int[] g : new int[][]{{29, 0, 34, 0}, {29, N, 34, N}, {0, 29, 0, 34}, {N, 29, N, 34}}) {
            b.fill(g[0], 0, g[1], g[0], 4, g[1], Blocks.STONE_BRICKS);
            b.fill(g[2], 0, g[3], g[2], 4, g[3], Blocks.STONE_BRICKS);
            b.fill(g[0], 5, g[1], g[2], 5, g[3], Blocks.STONE_BRICK_SLAB);
            b.set((g[0] + g[2]) / 2, 4, (g[1] + g[3]) / 2, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
    }

    private void lamp(int x, int z) {
        fill(x, 0, z, x, 2, z, Blocks.DARK_OAK_FENCE);
        set(x, 3, z, Blocks.LANTERN);
    }

    /** 공장 건물 16×14: 벽돌 벽·유리창·굴뚝, 안에 보일러·증기기관·축·기계 */
    private void industryHall(int x, int z) {
        hollow(x, -1, z, x + 15, 6, z + 13, Blocks.BRICKS);
        fill(x + 1, -1, z + 1, x + 14, -1, z + 12, Blocks.POLISHED_ANDESITE);
        for (int i = 0; i <= 15; i += 5) {
            fill(x + i, 0, z, x + i, 6, z, Blocks.DEEPSLATE_BRICKS);
            fill(x + i, 0, z + 13, x + i, 6, z + 13, Blocks.DEEPSLATE_BRICKS);
        }
        for (int i : new int[]{2, 7, 12}) {
            fill(x + i, 2, z, x + i + 1, 4, z, Blocks.GLASS);
            fill(x + i, 2, z + 13, x + i + 1, 4, z + 13, Blocks.GLASS);
        }
        fill(x, 0, z + 5, x, 2, z + 8, Blocks.AIR);           // 서쪽 큰 문
        fill(x, 7, z, x + 15, 7, z + 13, Blocks.DEEPSLATE_TILE_SLAB);
        fill(x + 1, -1, z + 6, x + 14, -1, z + 7, Blocks.YELLOW_CONCRETE);
        // 굴뚝
        fill(x + 13, 0, z + 2, x + 14, 14, z + 3, Blocks.BRICKS);
        fill(x + 13, 15, z + 2, x + 14, 15, z + 3, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE, true));
        // 증기 설비 + 축 + 기계 줄 (석탄은 손님 몫)
        set(x + 12, 0, z + 3, ModBlocks.BOILER.get().defaultBlockState().setValue(com.tycooncraft.power.BoilerBlock.FACING, Direction.SOUTH));
        set(x + 11, 0, z + 3, ModBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(com.tycooncraft.power.SteamEngineBlock.FACING, Direction.SOUTH));
        for (int i = 2; i <= 11; i++) set(x + i, 1, z + 3, ModBlocks.POWER_CABLE.get());
        Block[] ms = {m(MachineType.CRUSHER), m(MachineType.SMELTER), m(MachineType.PRESS), m(MachineType.LATHE), m(MachineType.WIRE_DRAWER)};
        for (int i = 0; i < ms.length; i++) set(x + 2 + i * 2, 0, z + 3, ms[i].defaultBlockState().setValue(MachineBlock.FACING, Direction.SOUTH));
        // 전기 구역: 발전기 - 전선 - 유도로·로봇 팔
        set(x + 12, 0, z + 10, ModBlocks.SOLAR_GENERATOR.get());
        for (int i = 3; i <= 11; i++) set(x + i, 0, z + 11, ModBlocks.POWER_CABLE.get());
        set(x + 3, 0, z + 10, ModBlocks.WAREHOUSE.get());
        set(x + 4, 0, z + 10, ModBlocks.ROBOT_ARM.get().defaultBlockState().setValue(com.tycooncraft.automation.RobotArmBlock.FACING, Direction.EAST));
        set(x + 5, 0, z + 10, m(MachineType.INDUCTION_FURNACE).defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        set(x + 6, 0, z + 10, ModBlocks.ROBOT_ARM.get().defaultBlockState().setValue(com.tycooncraft.automation.RobotArmBlock.FACING, Direction.EAST));
        set(x + 7, 0, z + 10, ModBlocks.WAREHOUSE.get());
        set(x + 9, 0, z + 10, ModBlocks.ACCUMULATOR.get());
        for (int i = 2; i <= 13; i += 11) set(x + i, 5, z + 6, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        set(x + 1, 0, z + 1, Blocks.CRAFTING_TABLE);
        chest(x + 1, 0, z + 12, Direction.EAST, VILLAGE_CHEST);
    }

    /** 물탱크 (4×4 다리 + 통) */
    private void waterTower(int x, int z) {
        for (int[] c : new int[][]{{0, 0}, {3, 0}, {0, 3}, {3, 3}}) fill(x + c[0], 0, z + c[1], x + c[0], 7, z + c[1], Blocks.DARK_OAK_LOG);
        fill(x - 1, 8, z - 1, x + 4, 8, z + 4, Blocks.SPRUCE_PLANKS);
        hollow(x - 1, 8, z - 1, x + 4, 12, z + 4, Blocks.SPRUCE_PLANKS);
        fill(x, 9, z, x + 3, 11, z + 3, Blocks.WATER);
        fill(x - 1, 13, z - 1, x + 4, 13, z + 4, Blocks.SPRUCE_SLAB);
        for (int y = 0; y <= 7; y++) set(x + 4, y, z + 1, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.EAST));
    }

    /** 창고 마당: 지붕 있는 창고 + 나무통·상자 더미 */
    private void storageYard(int x, int z) {
        fill(x, -1, z, x + 8, -1, z + 14, Blocks.GRAVEL);
        hollow(x, 0, z, x + 8, 4, z + 7, Blocks.SPRUCE_PLANKS);
        for (int[] c : new int[][]{{0, 0}, {8, 0}, {0, 7}, {8, 7}}) fill(x + c[0], 0, z + c[1], x + c[0], 4, z + c[1], Blocks.STRIPPED_SPRUCE_LOG);
        fill(x + 3, 0, z, x + 5, 2, z, Blocks.AIR);
        fill(x - 1, 5, z - 1, x + 9, 5, z + 8, Blocks.SPRUCE_SLAB);
        fill(x + 1, 0, z + 5, x + 7, 1, z + 6, Blocks.BARREL);
        set(x + 1, 0, z + 1, ModBlocks.WAREHOUSE.get());
        set(x + 7, 0, z + 1, ModBlocks.WAREHOUSE.get());
        chest(x + 4, 0, z + 6, Direction.NORTH, VILLAGE_CHEST);
        for (int[] c : new int[][]{{1, 10}, {2, 10}, {1, 11}, {5, 12}, {6, 12}, {6, 11}}) set(x + c[0], 0, z + c[1], Blocks.BARREL);
        set(x + 1, 1, z + 10, Blocks.BARREL);
        for (int[] c : new int[][]{{3, 9}, {4, 9}, {3, 13}}) set(x + c[0], 0, z + c[1], Blocks.HAY_BLOCK);
    }

    /** 사격장: 모래주머니 벽 + 과녁 */
    private void range(int x, int z) {
        fill(x, -1, z, x + 5, -1, z + 5, Blocks.COARSE_DIRT);
        fill(x, 0, z, x, 1, z + 5, Blocks.HAY_BLOCK);
        for (int i = 1; i <= 4; i += 3) set(x + 1, 0, z + i, Blocks.TARGET);
        fill(x + 5, 0, z, x + 5, 0, z + 5, Blocks.SANDSTONE_WALL);
    }
}
