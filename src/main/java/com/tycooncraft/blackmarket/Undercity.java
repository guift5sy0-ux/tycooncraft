package com.tycooncraft.blackmarket;

import com.tycooncraft.mob.GunnerEntity;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.29 언더시티: 맨홀 아래 지하 도시 (256×256, 천장 높이 약 36). 블랙마켓 골목은 그 한가운데 일부.
 * - 격자 거리(32칸마다, 폭 6) + 가로등 · 무너진 건물들 · 하수 운하와 다리 · 천장을 받치는 거대 기둥
 * - 갱단 은신처 8곳(그중 3곳은 변종 은신처): 붉은 표식, 잠긴 갱단 금고 상자(열쇠 필요)
 * - 갱단이 거주: 일반(갱단원·무법자·용병) + 변종(집행자·돌격대·저격수). 수가 줄면 사람 눈에 안 띄는 곳에서 다시 채워짐
 * - 블랙마켓 골목(+광장)은 중립 지대: 갱단이 들어오면 밖으로 쫓겨남
 */
public final class Undercity {
    public static final int HALF = 128, FLOOR = 63, CEIL = 100, CELL = 32, STREET = 6;
    /** 골목·카지노 + 둘레 광장 (건물 없음, 갱단 출입 금지) */
    public static final int PX0 = -22, PX1 = 41, PZ0 = -13, PZ1 = 13;
    public static int kills, spawned, built;
    public static final List<BlockPos> HIDEOUTS = new ArrayList<>(), CHESTS = new ArrayList<>();

    static boolean plaza(int x, int z) {
        return x >= PX0 && x <= PX1 && z >= PZ0 && z <= PZ1;
    }

    public static boolean inHub(double x, double z) {
        return x >= -17 && x <= 37 && z >= -8.5 && z <= 8.5;
    }

    static boolean street(int v) {
        int m = Math.floorMod(v + STREET / 2, CELL);
        return m < STREET;
    }

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        l.setBlock(new BlockPos(x, y, z), s, 2 | 16);
    }

    private static void set(ServerLevel l, int x, int y, int z, Block b) {
        set(l, x, y, z, b.defaultBlockState());
    }

    private static void fill(ServerLevel l, int x0, int y0, int z0, int x1, int y1, int z1, BlockState s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(l, x, y, z, s);
    }

    private static void fill(ServerLevel l, int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        fill(l, x0, y0, z0, x1, y1, z1, b.defaultBlockState());
    }

    /** 허브(골목) 뒤에 이어서 지음. 골목·광장 안은 건드리지 않음 */
    public static void build(ServerLevel l) {
        RandomSource r = RandomSource.create(2029);
        HIDEOUTS.clear();
        CHESTS.clear();
        for (int cx = -HALF >> 4; cx < HALF >> 4; cx++) for (int cz = -HALF >> 4; cz < HALF >> 4; cz++) l.getChunk(cx, cz);
        // ── 바닥 · 천장 · 외벽 ──
        Block[] ground = {Blocks.BLACKSTONE, Blocks.DEEPSLATE, Blocks.TUFF, Blocks.COBBLED_DEEPSLATE, Blocks.POLISHED_BLACKSTONE_BRICKS};
        for (int x = -HALF; x < HALF; x++)
            for (int z = -HALF; z < HALF; z++) {
                boolean edge = x == -HALF || z == -HALF || x == HALF - 1 || z == HALF - 1;
                set(l, x, CEIL, z, (x * 7 + z * 13) % 11 == 0 ? Blocks.BLACKSTONE : Blocks.DEEPSLATE);
                set(l, x, CEIL + 1, z, Blocks.DEEPSLATE);
                set(l, x, CEIL + 2, z, Blocks.BARRIER);
                if (edge) {
                    fill(l, x, FLOOR, z, x, CEIL, z, Blocks.DEEPSLATE_BRICKS);
                    continue;
                }
                if (x >= -17 && x <= 36 && z >= -8 && z <= 8) continue;   // 골목·카지노 바닥은 허브가 깜
                set(l, x, FLOOR - 1, z, Blocks.DEEPSLATE);
                boolean st = street(x) || street(z) || plaza(x, z);
                Block b = st ? (r.nextInt(9) == 0 ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : Blocks.POLISHED_BLACKSTONE_BRICKS) : ground[r.nextInt(ground.length)];
                if (st && r.nextInt(40) == 0) b = Blocks.LIGHT_BLUE_STAINED_GLASS;   // 물웅덩이
                set(l, x, FLOOR, z, b);
            }
        // 천장 종유석
        for (int i = 0; i < 260; i++) {
            int x = -HALF + 2 + r.nextInt(HALF * 2 - 4), z = -HALF + 2 + r.nextInt(HALF * 2 - 4), len = 2 + r.nextInt(7);
            fill(l, x, CEIL - len, z, x, CEIL - 1, z, r.nextBoolean() ? Blocks.POINTED_DRIPSTONE : Blocks.DEEPSLATE);
            if (r.nextInt(5) == 0) set(l, x, CEIL - len - 1, z, Blocks.SHROOMLIGHT);
        }
        // ── 하수 운하 (z = 96 ~ 100, 동서로) + 다리 ──
        for (int x = -HALF + 1; x < HALF - 1; x++) {
            for (int z = 96; z <= 100; z++) {
                set(l, x, FLOOR - 3, z, Blocks.MUD_BRICKS);
                if (z == 96 || z == 100) {
                    fill(l, x, FLOOR - 2, z, x, FLOOR, z, Blocks.MUD_BRICKS);
                } else {
                    fill(l, x, FLOOR - 2, z, x, FLOOR - 1, z, Blocks.WATER);
                    set(l, x, FLOOR, z, street(x) ? Blocks.DARK_OAK_PLANKS : Blocks.AIR);
                }
            }
            if (Math.floorMod(x, 8) == 0) set(l, x, FLOOR + 1, 96, Blocks.IRON_BARS);
        }
        // ── 천장 받치는 거대 기둥 (거리 교차로 모서리) ──
        for (int gx = -HALF + CELL; gx < HALF; gx += CELL * 2)
            for (int gz = -HALF + CELL; gz < HALF; gz += CELL * 2) {
                int x = gx + STREET / 2 + 1, z = gz + STREET / 2 + 1;
                if (plaza(x, z) || plaza(x + 3, z + 3)) continue;
                fill(l, x, FLOOR + 1, z, x + 3, CEIL - 1, z + 3, Blocks.DEEPSLATE_TILES);
                fill(l, x - 1, FLOOR + 1, z - 1, x + 4, FLOOR + 2, z + 4, Blocks.POLISHED_DEEPSLATE);
                for (int y = FLOOR + 8; y < CEIL; y += 9) {
                    set(l, x - 1, y, z + 1, Blocks.SEA_LANTERN);
                    set(l, x + 4, y, z + 2, Blocks.SEA_LANTERN);
                }
            }
        // ── 가로등 ──
        for (int x = -HALF + 4; x < HALF - 4; x += 9)
            for (int z = -HALF + 4; z < HALF - 4; z += 9) {
                if (!(street(x) ^ street(z)) || plaza(x, z) || (z >= 95 && z <= 101)) continue;
                int sx = x, sz = z;
                lamp(l, sx, sz);
            }
        // ── 블록마다 건물 / 은신처 ──
        int cells = (HALF * 2) / CELL;
        List<int[]> cellList = new ArrayList<>();
        for (int i = 0; i < cells; i++)
            for (int j = 0; j < cells; j++) {
                int x0 = -HALF + i * CELL + STREET / 2 + 1, z0 = -HALF + j * CELL + STREET / 2 + 1;
                int x1 = x0 + CELL - STREET - 2, z1 = z0 + CELL - STREET - 2;
                if (x1 >= HALF - 1) x1 = HALF - 3;
                if (z1 >= HALF - 1) z1 = HALF - 3;
                if (x0 <= -HALF) x0 = -HALF + 2;
                if (z0 <= -HALF) z0 = -HALF + 2;
                if (overlapsPlaza(x0, z0, x1, z1) || (z0 <= 101 && z1 >= 95)) continue;
                cellList.add(new int[]{x0, z0, x1, z1});
            }
        // 은신처 8곳: 골목에서 적당히 떨어진 칸
        java.util.Collections.shuffle(cellList, new java.util.Random(7));
        int hide = 0;
        for (int[] c : cellList) {
            int mx = (c[0] + c[2]) / 2, mz = (c[1] + c[3]) / 2;
            boolean farEnough = Math.abs(mx) + Math.abs(mz) > 48;
            if (hide < 8 && farEnough) {
                hideout(l, r, c, hide < 3 ? 1 : 0);
                hide++;
            } else block(l, r, c);
        }
        // ── 허브 서쪽 벽에 언더시티로 나가는 문 ──
        fill(l, -17, 64, -1, -17, 66, 1, Blocks.AIR);
        fill(l, -17, 67, -2, -17, 67, 2, Blocks.CHISELED_POLISHED_BLACKSTONE);
        set(l, -17, 64, -2, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        set(l, -17, 64, 2, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        set(l, -18, 67, 0, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
        set(l, -19, 66, 0, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 14));
        // 광장 조명
        for (int x = PX0 + 1; x < PX1; x += 7) {
            set(l, x, 68, PZ0 + 1, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
            set(l, x, 68, PZ1 - 1, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
        }
        for (int z = PZ0 + 1; z < PZ1; z += 6) set(l, PX0 + 1, 68, z, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 12));
        built++;
    }

    static boolean overlapsPlaza(int x0, int z0, int x1, int z1) {
        return x1 >= PX0 - 2 && x0 <= PX1 + 2 && z1 >= PZ0 - 2 && z0 <= PZ1 + 2;
    }

    static void lamp(ServerLevel l, int x, int z) {
        fill(l, x, FLOOR + 1, z, x, FLOOR + 4, z, Blocks.IRON_BARS);
        set(l, x, FLOOR + 5, z, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
        set(l, x, FLOOR + 6, z, Blocks.POLISHED_BLACKSTONE_SLAB);
        set(l, x, FLOOR + 4, z, Blocks.LANTERN);
        set(l, x, FLOOR + 7, z, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 13));
    }

    /** 보통 칸: 무너진 건물 1~2채 + 잔해 */
    static void block(ServerLevel l, RandomSource r, int[] c) {
        int w = c[2] - c[0], d = c[3] - c[1];
        if (w < 10 || d < 10) return;
        int n = 1 + r.nextInt(2);
        for (int i = 0; i < n; i++) {
            int bw = 8 + r.nextInt(Math.max(1, Math.min(8, w - 8))), bd = 8 + r.nextInt(Math.max(1, Math.min(8, d - 8)));
            int x0 = c[0] + (i == 0 ? 0 : w - bw), z0 = c[1] + (i == 0 ? 0 : d - bd);
            building(l, r, x0, z0, x0 + bw, z0 + bd, 8 + r.nextInt(5) * 4, false);
        }
        // 잔해·드럼통
        for (int k = 0; k < 4; k++) {
            int x = c[0] + r.nextInt(w + 1), z = c[1] + r.nextInt(d + 1);
            if (l.getBlockState(new BlockPos(x, FLOOR + 1, z)).isAir())
                set(l, x, FLOOR + 1, z, r.nextBoolean() ? Blocks.BARREL : Blocks.COBBLED_DEEPSLATE_SLAB);
        }
    }

    static final Block[] WALLS = {Blocks.DEEPSLATE_BRICKS, Blocks.STONE_BRICKS, Blocks.MUD_BRICKS, Blocks.BRICKS, Blocks.POLISHED_BLACKSTONE_BRICKS, Blocks.TUFF_BRICKS};

    /** 속 빈 다층 건물: 층마다 바닥, 창문(일부 불 켜짐), 거리 쪽 출입구, 무너진 모서리 */
    static void building(ServerLevel l, RandomSource r, int x0, int z0, int x1, int z1, int h, boolean gang) {
        Block wall = gang ? Blocks.POLISHED_BLACKSTONE_BRICKS : WALLS[r.nextInt(WALLS.length)];
        Block cracked = wall == Blocks.STONE_BRICKS ? Blocks.CRACKED_STONE_BRICKS : wall == Blocks.DEEPSLATE_BRICKS ? Blocks.CRACKED_DEEPSLATE_BRICKS
                : wall == Blocks.POLISHED_BLACKSTONE_BRICKS ? Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS : wall;
        int top = Math.min(CEIL - 4, FLOOR + h);
        boolean ruined = !gang && r.nextInt(3) == 0;
        for (int y = FLOOR + 1; y <= top; y++) {
            int fl = (y - FLOOR - 1) % 4;
            for (int x = x0; x <= x1; x++)
                for (int z = z0; z <= z1; z++) {
                    boolean side = x == x0 || x == x1 || z == z0 || z == z1;
                    // 무너진 모서리: 위층일수록 사라짐
                    if (ruined && x > (x0 + x1) / 2 && z > (z0 + z1) / 2 && y > FLOOR + h / 2 + (x - x0 + z - z0) % 4) continue;
                    if (side) {
                        boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                        boolean window = !corner && fl == 2 && ((x + z) % 3 == 0);
                        if (window) {
                            int k = r.nextInt(5);
                            set(l, x, y, z, k == 0 ? Blocks.ORANGE_STAINED_GLASS : k == 1 ? (gang ? Blocks.RED_STAINED_GLASS : Blocks.YELLOW_STAINED_GLASS) : k == 2 ? Blocks.IRON_BARS : Blocks.TINTED_GLASS);
                        } else set(l, x, y, z, corner ? Blocks.POLISHED_DEEPSLATE : r.nextInt(8) == 0 ? cracked : wall);
                    } else if (fl == 0 && y > FLOOR + 1) set(l, x, y, z, Blocks.SPRUCE_PLANKS);
                }
        }
        // 지붕 + 난간
        if (!ruined) {
            fill(l, x0, top + 1, z0, x1, top + 1, z1, Blocks.POLISHED_DEEPSLATE_SLAB);
            for (int x = x0; x <= x1; x += 2) {
                set(l, x, top + 2, z0, Blocks.DEEPSLATE_BRICK_WALL);
                set(l, x, top + 2, z1, Blocks.DEEPSLATE_BRICK_WALL);
            }
        }
        // 출입구 (네 면 중 가운데)
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        fill(l, mx, FLOOR + 1, z0, mx + 1, FLOOR + 2, z0, Blocks.AIR);
        fill(l, mx, FLOOR + 1, z1, mx + 1, FLOOR + 2, z1, Blocks.AIR);
        fill(l, x0, FLOOR + 1, mz, x0, FLOOR + 2, mz + 1, Blocks.AIR);
        // 층마다 계단 구멍 + 사다리 · 실내 조명
        for (int y = FLOOR + 5; y <= top; y += 4) {
            set(l, x0 + 1, y, z0 + 1, Blocks.AIR);
            for (int yy = y - 3; yy <= y; yy++) set(l, x0 + 1, yy, z0 + 1, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.SOUTH));
            set(l, mx, y + 2, mz, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 10));
        }
        set(l, mx, FLOOR + 3, mz, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 10));
    }

    /** 갱단 은신처: 검은 벽 + 붉은 띠 + 그래피티, 안에 잠긴 금고 상자(보통 2개, 변종 은신처 4개) */
    static void hideout(ServerLevel l, RandomSource r, int[] c, int tier) {
        int x0 = c[0] + 2, z0 = c[1] + 2, x1 = c[2] - 2, z1 = c[3] - 2;
        building(l, r, x0, z0, x1, z1, tier == 1 ? 16 : 12, true);
        // 붉은 띠 + 그래피티
        for (int x = x0; x <= x1; x++) {
            set(l, x, FLOOR + 4, z0, Blocks.RED_CONCRETE);
            set(l, x, FLOOR + 4, z1, Blocks.RED_CONCRETE);
        }
        for (int z = z0; z <= z1; z++) {
            set(l, x0, FLOOR + 4, z, Blocks.RED_CONCRETE);
            set(l, x1, FLOOR + 4, z, Blocks.RED_CONCRETE);
        }
        Block[] paint = {Blocks.MAGENTA_CONCRETE, Blocks.LIME_CONCRETE, Blocks.ORANGE_CONCRETE, Blocks.WHITE_CONCRETE};
        for (int k = 0; k < 14; k++) set(l, x0 + 1 + r.nextInt(Math.max(1, x1 - x0 - 1)), FLOOR + 1 + r.nextInt(3), z0, paint[r.nextInt(paint.length)]);
        int mx = (x0 + x1) / 2, mz = (z0 + z1) / 2;
        // 입구 바리케이드 + 붉은 등
        set(l, mx - 1, FLOOR + 1, z0 - 1, Blocks.BARREL);
        set(l, mx + 2, FLOOR + 1, z0 - 1, Blocks.BARREL);
        set(l, mx, FLOOR + 3, z0 - 1, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT, true));
        // 실내: 붉은 카펫, 테이블, 금고 상자
        fill(l, x0 + 2, FLOOR + 1, z0 + 2, x1 - 2, FLOOR + 1, z1 - 2, Blocks.RED_CARPET);
        set(l, mx, FLOOR + 1, mz, Blocks.CRAFTING_TABLE);
        set(l, mx + 1, FLOOR + 1, mz, Blocks.SMOKER);
        int n = tier == 1 ? 4 : 2;
        int[][] spots = {{x0 + 1, z1 - 1, 0}, {x1 - 1, z1 - 1, 0}, {x1 - 1, z0 + 2, 1}, {x0 + 2, z1 - 1, 0}};
        for (int i = 0; i < n; i++) {
            BlockPos p = new BlockPos(spots[i][0], FLOOR + 1, spots[i][1]);
            if (i == 3) p = new BlockPos(mx, FLOOR + 5, mz);   // 2층
            l.setBlock(p, ModBlocks.LOCKED_CHEST.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH), 2);
            if (l.getBlockEntity(p) instanceof LockedChestBlockEntity be) be.restock(r, tier);
            CHESTS.add(p);
        }
        HIDEOUTS.add(new BlockPos(mx, FLOOR + 1, z0 - 3).immutable());
        if (tier == 1) HIDEOUTS.add(new BlockPos(mx, FLOOR + 1, z0 - 2).immutable());   // 변종 은신처는 표시를 두 번 (더 자주 뽑힘)
    }

    // ── 갱단 거주 ──
    static final EntityType<?>[] NORMAL = new EntityType<?>[3], VARIANT = new EntityType<?>[3];

    @SuppressWarnings("unchecked")
    static EntityType<GunnerEntity> pick(RandomSource r, boolean nearVariantHideout) {
        float v = nearVariantHideout ? 0.55f : 0.25f;
        if (r.nextFloat() < v) {
            int k = r.nextInt(10);
            return k < 4 ? ModEntities.ENFORCER.get() : k < 7 ? ModEntities.BRUTE.get() : ModEntities.MARKSMAN.get();
        }
        int k = r.nextInt(10);
        return k < 5 ? ModEntities.GANGSTER.get() : k < 8 ? ModEntities.OUTLAW.get() : ModEntities.MERCENARY.get();
    }

    public static boolean resident(GunnerEntity g) {
        return g.getPersistentData().getBoolean("tycooncraft_undercity");
    }

    /** 플레이어 한 명 주변(72칸) 최대 거주 갱단 수 */
    public static final int LOCAL_CAP = 14;

    /** 5초마다: 골목 안에 들어온 갱단은 밖으로, 플레이어 주변 갱단이 모자라면 24~64칸 떨어진 안 보이는 곳에 보충 */
    public static void tick(ServerLevel l) {
        if (l.players().isEmpty() || built == 0 && HIDEOUTS.isEmpty()) return;
        RandomSource r = l.random;
        for (ServerPlayer p : l.players()) {
            List<GunnerEntity> near = l.getEntitiesOfClass(GunnerEntity.class, p.getBoundingBox().inflate(72, 40, 72), Undercity::resident);
            for (GunnerEntity g : near)
                if (inHub(g.getX(), g.getZ()) || plaza(g.getBlockX(), g.getBlockZ())) {
                    g.setTarget(null);
                    g.teleportTo(-30.5, FLOOR + 1, g.getZ() < 0 ? -20.5 : 20.5);   // 광장 밖 거리로
                }
            int need = Math.min(3, LOCAL_CAP - near.size());
            for (int i = 0; i < need; i++) {
                BlockPos at = spot(l, r, p);
                if (at == null) continue;
                boolean nearVar = false;
                for (int k = 0; k < Math.min(3, HIDEOUTS.size()); k++) if (HIDEOUTS.get(k).distSqr(at) < 40 * 40) nearVar = true;
                spawnAt(l, pick(r, nearVar), at);
            }
        }
    }

    public static GunnerEntity spawnAt(ServerLevel l, EntityType<GunnerEntity> type, BlockPos at) {
        GunnerEntity g = type.create(l);
        if (g == null) return null;
        g.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, l.random.nextFloat() * 360, 0);
        g.finalizeSpawn(l, l.getCurrentDifficultyAt(at), MobSpawnType.EVENT, null);
        g.getPersistentData().putBoolean("tycooncraft_undercity", true);
        g.setPersistenceRequired();
        if (l.addFreshEntity(g)) spawned++;
        return g;
    }

    /** 플레이어에게서 24~64칸: 근처 은신처 앞 또는 거리 위 · 광장 밖 · 엔티티가 돌아가는 청크 · 다른 플레이어와도 24칸 이상 */
    static BlockPos spot(ServerLevel l, RandomSource r, ServerPlayer pl) {
        List<BlockPos> hs = new ArrayList<>();
        for (BlockPos h : HIDEOUTS) if (h.distSqr(pl.blockPosition()) < 64 * 64 && h.distSqr(pl.blockPosition()) > 24 * 24) hs.add(h);
        for (int tries = 0; tries < 24; tries++) {
            BlockPos p;
            if (!hs.isEmpty() && r.nextFloat() < 0.5f) {
                BlockPos h = hs.get(r.nextInt(hs.size()));
                p = h.offset(r.nextInt(7) - 3, 0, r.nextInt(5) - 4);
            } else {
                double a = r.nextDouble() * Math.PI * 2, d = 24 + r.nextDouble() * 40;
                int x = (int) Math.floor(pl.getX() + Math.cos(a) * d), z = (int) Math.floor(pl.getZ() + Math.sin(a) * d);
                if (Math.abs(x) >= HALF - 2 || Math.abs(z) >= HALF - 2) continue;
                p = new BlockPos(x, FLOOR + 1, z);
            }
            if (plaza(p.getX(), p.getZ()) || !l.isPositionEntityTicking(p)) continue;
            if (!l.getBlockState(p).isAir() || !l.getBlockState(p.above()).isAir() || l.getBlockState(p.below()).isAir()) continue;
            final BlockPos fp = p;
            if (l.players().stream().anyMatch(o -> o.blockPosition().distSqr(fp) < 24 * 24)) continue;
            return p;
        }
        return null;
    }

    /** 다시 들어왔을 때 은신처 목록을 되살림 (저장된 금고 상자 위치에서) */
    public static void recall(ServerLevel l) {
        if (!HIDEOUTS.isEmpty()) return;
        RandomSource r = RandomSource.create(2029);
        // build() 와 같은 순서로 칸을 골라 은신처 위치만 다시 계산
        int cells = (HALF * 2) / CELL;
        List<int[]> cellList = new ArrayList<>();
        for (int i = 0; i < cells; i++)
            for (int j = 0; j < cells; j++) {
                int x0 = -HALF + i * CELL + STREET / 2 + 1, z0 = -HALF + j * CELL + STREET / 2 + 1;
                int x1 = x0 + CELL - STREET - 2, z1 = z0 + CELL - STREET - 2;
                if (x1 >= HALF - 1) x1 = HALF - 3;
                if (z1 >= HALF - 1) z1 = HALF - 3;
                if (x0 <= -HALF) x0 = -HALF + 2;
                if (z0 <= -HALF) z0 = -HALF + 2;
                if (overlapsPlaza(x0, z0, x1, z1) || (z0 <= 101 && z1 >= 95)) continue;
                cellList.add(new int[]{x0, z0, x1, z1});
            }
        java.util.Collections.shuffle(cellList, new java.util.Random(7));
        int hide = 0;
        for (int[] c : cellList) {
            int mx = (c[0] + c[2]) / 2, mz = (c[1] + c[3]) / 2;
            if (hide < 8 && Math.abs(mx) + Math.abs(mz) > 48) {
                int x0 = c[0] + 2, z0 = c[1] + 2, x1 = c[2] - 2;
                int hx = (x0 + x1) / 2;
                HIDEOUTS.add(new BlockPos(hx, FLOOR + 1, z0 - 3));
                if (hide < 3) HIDEOUTS.add(new BlockPos(hx, FLOOR + 1, z0 - 2));
                hide++;
            }
        }
        built = Math.max(built, 1);
    }

    private Undercity() {}
}
