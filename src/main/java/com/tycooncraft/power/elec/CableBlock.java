package com.tycooncraft.power.elec;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.block.MachineBlock;
import com.tycooncraft.power.Layer;
import com.tycooncraft.power.PowerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 전선: 옆 전기 블록·기계와 자동으로 이어지는 가는 케이블 */
public class CableBlock extends ElecBlock {
    public static final MapCodec<CableBlock> CODEC = simpleCodec(CableBlock::new);
    private static final VoxelShape CORE = box(6, 6, 6, 10, 10, 10);
    private static final VoxelShape[] ARM = new VoxelShape[6];

    static {
        for (Direction d : Direction.values()) {
            double x0 = 6, y0 = 6, z0 = 6, x1 = 10, y1 = 10, z1 = 10;
            switch (d) {
                case DOWN -> y0 = 0;
                case UP -> y1 = 16;
                case NORTH -> z0 = 0;
                case SOUTH -> z1 = 16;
                case WEST -> x0 = 0;
                case EAST -> x1 = 16;
            }
            ARM[d.ordinal()] = box(x0, y0, z0, x1, y1, z1);
        }
    }

    public CableBlock(Properties p) {
        super(p);
        BlockState s = stateDefinition.any();
        for (Direction d : Direction.values()) s = s.setValue(prop(d), false);
        registerDefaultState(s);
    }

    public static BooleanProperty prop(Direction d) {
        return PipeBlock.PROPERTY_BY_DIRECTION.get(d);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        for (Direction d : Direction.values()) b.add(prop(d));
    }

    static boolean connects(BlockGetter level, BlockPos pos, Direction d) {
        BlockPos np = pos.relative(d);
        BlockState ns = level.getBlockState(np);
        if (ns.getBlock() instanceof CableBlock || ns.getBlock() instanceof MachineBlock) return true;
        BlockEntity be = level.getBlockEntity(np);
        return be instanceof PowerNode n && n.in(Layer.ELEC) && (n.consumer(Layer.ELEC) || n.conducts(Layer.ELEC, d.getOpposite()));
    }

    private BlockState withConnections(BlockState s, BlockGetter level, BlockPos pos) {
        for (Direction d : Direction.values()) s = s.setValue(prop(d), connects(level, pos, d));
        return s;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return withConnections(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    /** 명령·구조물 생성처럼 놓는 방식과 상관없이 연결 모양을 맞춤 */
    @Override
    protected void onPlace(BlockState s, net.minecraft.world.level.Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(s, level, pos, old, moved);
        if (level.isClientSide) return;
        BlockState c = withConnections(s, level, pos);
        if (c != s) level.setBlock(pos, c, 2);
    }

    @Override
    protected BlockState updateShape(BlockState s, Direction d, BlockState ns, LevelAccessor level, BlockPos pos, BlockPos np) {
        return s.setValue(prop(d), connects(level, pos, d));
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        VoxelShape v = CORE;
        for (Direction d : Direction.values()) if (s.getValue(prop(d))) v = Shapes.or(v, ARM[d.ordinal()]);
        return v;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }
}
