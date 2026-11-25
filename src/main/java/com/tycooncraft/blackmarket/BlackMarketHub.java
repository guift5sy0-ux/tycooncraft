package com.tycooncraft.blackmarket;

import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChainBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

/** 암시장 골목 (x -16~16, z -8~8, 바닥 y=63) — 처음 한 번 짓고, 상인은 죽으면 다음 날 다시 나옴 */
public final class BlackMarketHub {
    public static final int VERSION = 2;
    public static final BlockPos[] DEALER_POS = {new BlockPos(-8, 64, -6), new BlockPos(0, 64, -6), new BlockPos(8, 64, -6)};
    public static final BlockPos EXIT = new BlockPos(-15, 63, 0), BOUNTY = new BlockPos(-4, 64, 7), VAULT = new BlockPos(4, 64, 7);

    public static class Data extends SavedData {
        int built;
        final long[] deathDay = {-1, -1, -1};

        static Data get(ServerLevel l) {
            return l.getDataStorage().computeIfAbsent(new Factory<>(Data::new, Data::load), "tycooncraft_black_market");
        }

        static Data load(CompoundTag t, HolderLookup.Provider r) {
            Data d = new Data();
            d.built = t.getInt("built");
            long[] dd = t.getLongArray("deathDay");
            for (int i = 0; i < Math.min(3, dd.length); i++) d.deathDay[i] = dd[i];
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
            t.putInt("built", built);
            t.putLongArray("deathDay", deathDay);
            return t;
        }
    }

    static long day(ServerLevel l) {
        return l.getServer().overworld().getDayTime() / 24000L;
    }

    public static void ensure(ServerLevel l) {
        Data d = Data.get(l);
        if (d.built >= VERSION) return;
        build(l);
        d.built = VERSION;
        d.setDirty();
        tickDealers(l);
    }

    public static void dealerDied(ServerLevel l, int kind) {
        Data d = Data.get(l);
        d.deathDay[kind] = day(l);
        d.setDirty();
    }

    /** 없는 상인을 다시 세움 (죽었으면 다음 날부터) */
    public static int tickDealers(ServerLevel l) {
        Data d = Data.get(l);
        if (d.built < VERSION) return 0;
        long today = day(l);
        int made = 0;
        var box = new AABB(-20, 60, -12, 20, 80, 12);
        for (int k = 0; k < 3; k++) {
            final int kind = k;
            boolean alive = !l.getEntitiesOfClass(BlackMarketDealer.class, box, e -> e.kind() == kind && e.isAlive()).isEmpty();
            if (alive || d.deathDay[k] >= today) continue;
            BlackMarketDealer e = ModEntities.BM_DEALER.get().create(l);
            if (e == null) continue;
            BlockPos p = DEALER_POS[k];
            e.setKind(k);
            e.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0f, 0f);
            e.setYHeadRot(0f);
            e.finalizeSpawn(l, l.getCurrentDifficultyAt(p), MobSpawnType.STRUCTURE, null);
            l.addFreshEntity(e);
            made++;
        }
        return made;
    }

    private static void set(ServerLevel l, int x, int y, int z, BlockState s) {
        l.setBlock(new BlockPos(x, y, z), s, 2);
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

    public static void build(ServerLevel l) {
        var rnd = l.random;
        fill(l, -22, 64, -14, 22, 90, 14, Blocks.AIR);
        for (var e : l.getEntitiesOfClass(BlackMarketDealer.class, new AABB(-22, 60, -14, 22, 90, 14))) e.discard();   // 다시 지을 때 상인도 새로
        // 바닥: 젖은 아스팔트 느낌 (검정 콘크리트 + 블랙스톤 + 물웅덩이 유리)
        for (int x = -17; x <= 17; x++)
            for (int z = -8; z <= 8; z++) {
                int r = rnd.nextInt(10);
                set(l, x, 63, z, r < 5 ? Blocks.BLACK_CONCRETE : r < 8 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS);
            }
        for (int[] p : new int[][]{{-10, 2}, {-9, 2}, {-9, 3}, {3, -1}, {4, -1}, {4, 0}, {12, 3}, {13, 3}, {-3, 4}})
            set(l, p[0], 63, p[1], Blocks.LIGHT_BLUE_STAINED_GLASS);
        fill(l, -1, 63, -2, 1, 63, 2, Blocks.POLISHED_BLACKSTONE);   // 가운데 하수구 뚜껑 자리
        // 양옆 건물 벽 (높이 13) + 창문
        for (int x = -17; x <= 17; x++) {
            for (int y = 64; y <= 76; y++) {
                Block wn = (x + y) % 7 == 0 ? Blocks.BLACKSTONE : Blocks.DEEPSLATE_BRICKS;
                set(l, x, y, -8, wn);
                set(l, x, y, 8, (x * 3 + y) % 5 == 0 ? Blocks.CRACKED_DEEPSLATE_BRICKS : Blocks.DEEPSLATE_TILES);
            }
            if (x % 4 == 0)
                for (int y = 69; y <= 75; y += 3) {
                    boolean lit = rnd.nextInt(3) == 0;
                    set(l, x, y, -8, lit ? Blocks.ORANGE_STAINED_GLASS : Blocks.TINTED_GLASS);
                    set(l, x, y, 8, rnd.nextInt(3) == 0 ? Blocks.YELLOW_STAINED_GLASS : Blocks.TINTED_GLASS);
                }
        }
        for (int z = -8; z <= 8; z++)
            for (int y = 64; y <= 76; y++) {
                set(l, -17, y, z, Blocks.DEEPSLATE_BRICKS);
                set(l, 17, y, z, Blocks.DEEPSLATE_BRICKS);
            }
        // 동쪽 끝 낙서 벽
        Block[] paint = {Blocks.MAGENTA_CONCRETE, Blocks.CYAN_CONCRETE, Blocks.LIME_CONCRETE, Blocks.PINK_CONCRETE, Blocks.PURPLE_CONCRETE};
        for (int z = -6; z <= 6; z++) for (int y = 64; y <= 68; y++) if (rnd.nextInt(3) == 0) set(l, 16, y, z, paint[rnd.nextInt(paint.length)]);
        // 보이지 않는 천장
        fill(l, -17, 77, -8, 17, 78, 8, Blocks.BARRIER);
        // 상인 노점 3곳: 뒤 벽 네온 간판, 앞 계산대
        Block[] neon = {Blocks.RED_STAINED_GLASS, Blocks.PURPLE_STAINED_GLASS, Blocks.LIME_STAINED_GLASS};
        Block[] awning = {Blocks.RED_WOOL, Blocks.PURPLE_WOOL, Blocks.LIME_WOOL};
        for (int i = 0; i < 3; i++) {
            int cx = DEALER_POS[i].getX();
            fill(l, cx - 2, 64, -7, cx + 2, 64, -7, Blocks.BARREL);
            set(l, cx - 2, 65, -7, Blocks.BARREL);
            set(l, cx + 2, 65, -7, Blocks.CHEST);
            // 낮은 계산대 (반 블록) — 상인이 계산대 너머로 쏠 수 있게
            fill(l, cx - 2, 64, -4, cx + 2, 64, -4, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM));
            fill(l, cx - 2, 69, -7, cx + 2, 69, -4, awning[i]);
            fill(l, cx - 2, 68, -8, cx + 2, 68, -8, neon[i]);
            set(l, cx - 1, 68, -9, Blocks.SEA_LANTERN);
            set(l, cx + 1, 68, -9, Blocks.SEA_LANTERN);
            set(l, cx, 68, -9, Blocks.SEA_LANTERN);
            set(l, cx, 68, -7, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
            // 노점 칸막이
            fill(l, cx - 3, 64, -7, cx - 3, 67, -4, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            fill(l, cx + 3, 64, -7, cx + 3, 67, -4, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
        }
        // 남쪽: 현상금 게시판·개인 금고
        set(l, BOUNTY.getX(), BOUNTY.getY(), BOUNTY.getZ(), ModBlocks.BOUNTY_BOARD.get());
        set(l, BOUNTY.getX(), BOUNTY.getY() + 1, BOUNTY.getZ(), ModBlocks.BOUNTY_BOARD.get());
        fill(l, BOUNTY.getX() - 1, 67, 7, BOUNTY.getX() + 1, 67, 7, Blocks.RED_STAINED_GLASS);
        set(l, BOUNTY.getX(), 67, 8, Blocks.SEA_LANTERN);
        set(l, VAULT.getX(), VAULT.getY(), VAULT.getZ(), ModBlocks.BM_VAULT.get());
        set(l, VAULT.getX() + 1, VAULT.getY(), VAULT.getZ(), ModBlocks.BM_VAULT.get());
        fill(l, VAULT.getX(), 67, 7, VAULT.getX() + 1, 67, 7, Blocks.CYAN_STAINED_GLASS);
        set(l, VAULT.getX(), 67, 8, Blocks.SEA_LANTERN);
        set(l, VAULT.getX() + 1, 67, 8, Blocks.SEA_LANTERN);
        // 출구: 서쪽 끝 맨홀 + 사다리
        set(l, EXIT.getX(), EXIT.getY(), EXIT.getZ(), ModBlocks.MANHOLE.get());
        for (int y = 64; y <= 70; y++) set(l, -16, y, 0, Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, Direction.EAST));
        set(l, -16, 71, 0, Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(net.minecraft.world.level.block.TrapDoorBlock.FACING, Direction.EAST)
                .setValue(net.minecraft.world.level.block.TrapDoorBlock.HALF, Half.TOP));
        // 보이지 않는 조명 (어둡지만 상인 얼굴은 보이게)
        for (int x = -14; x <= 14; x += 4)
            for (int z = -6; z <= 6; z += 4)
                set(l, x, 69, z, Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 11));
        for (BlockPos d : DEALER_POS) set(l, d.getX(), 66, d.getZ(), Blocks.LIGHT.defaultBlockState().setValue(net.minecraft.world.level.block.LightBlock.LEVEL, 14));
        // 골목 조명: 쇠사슬 + 랜턴, 네온 막대
        for (int x = -12; x <= 12; x += 6) {
            fill(l, x, 72, 0, x, 76, 0, Blocks.CHAIN.defaultBlockState().setValue(ChainBlock.AXIS, Direction.Axis.Y));
            set(l, x, 71, 0, Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true));
        }
        for (int x = -14; x <= 14; x += 4) {
            set(l, x, 70, -7, Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING, Direction.SOUTH));
            set(l, x, 70, 7, Blocks.END_ROD.defaultBlockState().setValue(net.minecraft.world.level.block.EndRodBlock.FACING, Direction.NORTH));
        }
        // 소품: 상자 더미, 쓰레기통(퇴비통), 환풍구
        for (int[] p : new int[][]{{12, -7}, {13, -7}, {12, -6}, {-12, 7}, {14, 6}}) set(l, p[0], 64, p[1], Blocks.BARREL);
        set(l, 12, 65, -7, Blocks.BARREL);
        set(l, 10, 64, 7, Blocks.COMPOSTER);
        set(l, -10, 64, 7, Blocks.COMPOSTER);
        set(l, 14, 66, 7, Blocks.IRON_BARS);
        set(l, 15, 66, 7, Blocks.IRON_BARS);
        set(l, -8, 64, 7, Blocks.CAMPFIRE.defaultBlockState().setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true));
        set(l, -8, 64, 6, Blocks.POLISHED_BLACKSTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.SOUTH));
        // 환전기·판매소 (암시장 안에서도 돈 정리 가능)
        set(l, 12, 64, 7, ModBlocks.EXCHANGE.get());
    }

    private BlackMarketHub() {}
}
