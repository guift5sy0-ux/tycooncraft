package com.tycooncraft.power;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 회전축: 놓는 면 방향으로 놓이고, 축 방향으로 동력을 잇고, 옆(모든 면)에 붙은 기계를 돌림 */
public class ShaftBlock extends BaseEntityBlock {
    public static final MapCodec<ShaftBlock> CODEC = simpleCodec(ShaftBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.AXIS;
    private static final VoxelShape X = box(0, 5.5, 5.5, 16, 10.5, 10.5), Y = box(5.5, 0, 5.5, 10.5, 16, 10.5), Z = box(5.5, 5.5, 0, 10.5, 10.5, 16);

    public ShaftBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // 이미 있는 축 옆에 붙이면 그 축과 같은 방향으로 이어서 놓기 쉽게
        Direction face = ctx.getClickedFace();
        BlockState against = ctx.getLevel().getBlockState(ctx.getClickedPos().relative(face.getOpposite()));
        if (against.getBlock() instanceof ShaftBlock && against.getValue(AXIS) == face.getAxis()) return defaultBlockState().setValue(AXIS, face.getAxis());
        return defaultBlockState().setValue(AXIS, face.getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return switch (s.getValue(AXIS)) {
            case X -> X;
            case Y -> Y;
            case Z -> Z;
        };
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.ENTITYBLOCK_ANIMATED;   // 돌아가는 모습은 렌더러가 그림
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShaftBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.SHAFT.get(), (l, p, s, be) -> be.clientTick()) : null;
    }
}
