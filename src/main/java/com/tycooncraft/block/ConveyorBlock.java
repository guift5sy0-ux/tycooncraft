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
 * 웅크리면 멈춰 설 수 있다. v0.15: 다리 달린 반 블록 높이(벨트 면 = 8px) — 기계 투입구와 높이가 맞는다.
 * SPEED(1~3)는 이어진 변속기가 정한다.
 */
public class ConveyorBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ConveyorBlock> CODEC = simpleCodec(ConveyorBlock::new);
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);
    /** 벨트 윗면 높이(블록 단위) */
    public static final double BELT_TOP = 0.5;
    public static final net.minecraft.world.level.block.state.properties.IntegerProperty SPEED_LEVEL =
            net.minecraft.world.level.block.state.properties.IntegerProperty.create("speed", 1, 5);
    /** 벨트 속도(블록/틱): 1단 = 기본, 5단 = 5배 */
    private static final double[] SPEEDS = {0, 0.15, 0.30, 0.45, 0.60, 0.75};
    /** 땅 마찰(0.6×0.98)로 깎이는 만큼 보정 → 물건이 실제로 벨트 속도대로 움직인다 */
    private static final double FRICTION_COMP = 1 / 0.588;
    private static final double CENTERING = 0.2;

    public ConveyorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(SPEED_LEVEL, 1));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, SPEED_LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // 이웃 컨베이어(또는 변속기)의 속도를 이어받는다
        int sp = 1;
        for (Direction d : Direction.values()) {
            BlockState n = context.getLevel().getBlockState(context.getClickedPos().relative(d));
            if (n.getBlock() instanceof ConveyorBlock) sp = Math.max(sp, n.getValue(SPEED_LEVEL));
            else if (n.getBlock() instanceof TransmissionBlock) sp = Math.max(sp, n.getValue(TransmissionBlock.SPEED));
        }
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection()).setValue(SPEED_LEVEL, sp);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** 운송기 위에서도 같은 속도로 밀기 위해 */
    public static double pushSpeed(BlockState state) {
        return SPEEDS[state.getValue(SPEED_LEVEL)] * FRICTION_COMP;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.getY() < pos.getY() + BELT_TOP - 0.1 || entity.isShiftKeyDown() || entity.isSpectator()) return;
        Direction dir = state.getValue(FACING);
        double SPEED = SPEEDS[state.getValue(SPEED_LEVEL)] * FRICTION_COMP;
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
