package com.tycooncraft.world.gen;

import com.tycooncraft.world.StructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** 구조물 조각 하나 = 마을/폐공장 전체. 청크마다 같은 seed 로 다시 지으며 그 청크 영역만 실제로 놓는다 */
public class TycoonPiece extends StructurePiece {
    private final String kind;
    private final BlockPos origin;
    private final long seed;

    public TycoonPiece(String kind, BlockPos origin, long seed) {
        super(ModStructures.TYCOON_PIECE.get(), 0, box(kind, origin));
        this.kind = kind;
        this.origin = origin;
        this.seed = seed;
    }

    public TycoonPiece(CompoundTag tag) {
        super(ModStructures.TYCOON_PIECE.get(), tag);
        this.kind = tag.getString("Kind");
        this.origin = new BlockPos(tag.getInt("OX"), tag.getInt("OY"), tag.getInt("OZ"));
        this.seed = tag.getLong("Seed");
    }

    public String kind() {
        return kind;
    }

    public BlockPos origin() {
        return origin;
    }

    /** 바닥(y-1)부터 지붕 위까지 — 지형 다듬기가 이 상자 아래를 메우고 안쪽을 비운다 */
    static BoundingBox box(String kind, BlockPos o) {
        return new BoundingBox(o.getX() - 1, o.getY() - 1, o.getZ() - 1,
                o.getX() + TycoonStructure.sizeX(kind) + 1, o.getY() + 16, o.getZ() + TycoonStructure.sizeZ(kind) + 1);
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext ctx, CompoundTag tag) {
        tag.putString("Kind", kind);
        tag.putInt("OX", origin.getX());
        tag.putInt("OY", origin.getY());
        tag.putInt("OZ", origin.getZ());
        tag.putLong("Seed", seed);
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                            BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        StructureBuilder.generate(kind, level, origin, seed, chunkBox);
    }
}
