package com.tycooncraft.power.elec;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 태양광 발전기 — 반 블록 높이 판 */
public class SolarBlock extends ElecBlock {
    public static final MapCodec<SolarBlock> CODEC = simpleCodec(SolarBlock::new);
    private static final VoxelShape SHAPE = box(0, 0, 0, 16, 6, 16);

    public SolarBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolarBlockEntity(pos, state);
    }
}
