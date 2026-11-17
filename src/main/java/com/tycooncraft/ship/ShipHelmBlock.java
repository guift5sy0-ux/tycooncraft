package com.tycooncraft.ship;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** 조종기(타륜): 배의 앞 = 설치할 때 바라본 방향. 우클릭하면 이어진 블록 전체가 배가 되어 출항, 조종석에 앉는다 */
public class ShipHelmBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<ShipHelmBlock> CODEC = simpleCodec(ShipHelmBlock::new);
    /** 의자(북쪽) + 타륜 기둥(남쪽) — 남쪽을 바라보는 기본 방향 기준 */
    private static final VoxelShape SHAPE_S = net.minecraft.world.phys.shapes.Shapes.or(Block.box(2.5, 0, 0, 13.5, 16, 9), Block.box(5, 0, 10, 11, 16, 15));
    private static final java.util.Map<net.minecraft.core.Direction, VoxelShape> SHAPES = new java.util.EnumMap<>(net.minecraft.core.Direction.class);
    static {
        for (net.minecraft.core.Direction d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            VoxelShape s = SHAPE_S;
            int turns = Math.floorMod((int) (d.toYRot() / 90), 4);   // south=0, west=1, north=2, east=3
            for (int i = 0; i < turns; i++) {
                VoxelShape[] acc = {net.minecraft.world.phys.shapes.Shapes.empty()};
                s.forAllBoxes((x0, y0, z0, x1, y1, z1) -> acc[0] = net.minecraft.world.phys.shapes.Shapes.or(acc[0],
                        net.minecraft.world.phys.shapes.Shapes.box(1 - z1, y0, x0, 1 - z0, y1, x1)));
                s = acc[0];
            }
            SHAPES.put(d, s);
        }
    }

    public ShipHelmBlock(Properties p) {
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES.getOrDefault(state.getValue(FACING), SHAPE_S);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl) ShipAssembler.assemble(sl, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
