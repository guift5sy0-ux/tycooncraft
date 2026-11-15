package com.tycooncraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 컨베이어. 동력 필요 없음. 설치할 때 바라본 방향(화살표)으로 위의 아이템·몹·플레이어를 운반.
 * 웅크리면 멈춰 설 수 있다.
 */
public class ConveyorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 3, 16);
    private static final double SPEED = 0.15;
    private static final double CENTERING = 0.2;

    public ConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.getY() < pos.getY() + 0.1 || entity.isShiftKeyDown() || entity.isSpectator()) return;
        Direction dir = state.getValue(FACING);
        Vec3 motion = entity.getDeltaMovement();
        double vx, vz;
        if (dir.getAxis() == Direction.Axis.X) {
            vx = dir.getStepX() * SPEED;
            vz = (pos.getZ() + 0.5 - entity.getZ()) * CENTERING;
        } else {
            vz = dir.getStepZ() * SPEED;
            vx = (pos.getX() + 0.5 - entity.getX()) * CENTERING;
        }
        entity.setDeltaMovement(vx, motion.y, vz);
    }
}
