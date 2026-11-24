package com.tycooncraft.power.elec;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** 축전지 — 옆면 눈금(0~4)으로 충전량 표시. 부숴도 저장량은 사라짐 */
public class AccumulatorBlock extends ElecBlock {
    public static final MapCodec<AccumulatorBlock> CODEC = simpleCodec(AccumulatorBlock::new);
    public static final IntegerProperty LEVEL = IntegerProperty.create("charge", 0, 4);

    public AccumulatorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LEVEL);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AccumulatorBlockEntity(pos, state);
    }
}
