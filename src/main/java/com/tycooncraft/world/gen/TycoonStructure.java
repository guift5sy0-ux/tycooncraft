package com.tycooncraft.world.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * 월드 생성 구조물: 상인 마을(village) / 폐공장(ruin). 바닐라 마을처럼 새로 생기는 땅에 일정 간격으로 자연 생성된다.
 * 물 위·너무 가파른 곳은 건너뛰고, 약간의 경사는 지형 다듬기(beard)로 메운다.
 */
public class TycoonStructure extends Structure {
    public static final MapCodec<TycoonStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            settingsCodec(i),
            Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)
    ).apply(i, TycoonStructure::new));

    final String kind;

    public TycoonStructure(StructureSettings settings, String kind) {
        super(settings);
        this.kind = kind;
    }

    static int sizeX(String kind) {
        return kind.equals("village") ? 32 : 22;
    }

    static int sizeZ(String kind) {
        return kind.equals("village") ? 32 : 14;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext ctx) {
        ChunkPos cp = ctx.chunkPos();
        int sx = sizeX(kind), sz = sizeZ(kind);
        int ox = cp.getMiddleBlockX() - sx / 2, oz = cp.getMiddleBlockZ() - sz / 2;
        int[][] pts = {{0, 0}, {sx, 0}, {0, sz}, {sx, sz}, {sx / 2, sz / 2}, {sx / 2, 0}, {sx / 2, sz}, {0, sz / 2}, {sx, sz / 2}};
        int[] hs = new int[pts.length];
        for (int k = 0; k < pts.length; k++) {
            int x = ox + pts[k][0], z = oz + pts[k][1];
            int h = ctx.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, ctx.heightAccessor(), ctx.randomState());
            NoiseColumn col = ctx.chunkGenerator().getBaseColumn(x, z, ctx.heightAccessor(), ctx.randomState());
            BlockState top = col.getBlock(h - 1);
            if (!top.getFluidState().isEmpty()) return Optional.empty();     // 물·용암 위엔 짓지 않음
            hs[k] = h;
        }
        Arrays.sort(hs);
        if (hs[hs.length - 1] - hs[0] > 10) return Optional.empty();       // 너무 가파름
        int y = hs[hs.length / 2];
        if (y < ctx.chunkGenerator().getSeaLevel()) return Optional.empty();
        BlockPos origin = new BlockPos(ox, y, oz);
        long seed = ctx.seed() ^ (cp.toLong() * 0x9E3779B97F4A7C15L) ^ kind.hashCode();
        return Optional.of(new GenerationStub(origin, b -> b.addPiece(new TycoonPiece(kind, origin, seed))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TYCOON.get();
    }
}
