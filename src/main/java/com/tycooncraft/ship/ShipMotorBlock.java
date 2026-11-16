package com.tycooncraft.ship;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** 배 전용 모터: 배 블록 아무 곳에나 붙이면 추진력. 많을수록 빠르고, 배가 클수록 느리다. 프로펠러 = 설치할 때 바라본 쪽 */
public class ShipMotorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ShipMotorBlock> CODEC = simpleCodec(ShipMotorBlock::new);

    public ShipMotorBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }
}
