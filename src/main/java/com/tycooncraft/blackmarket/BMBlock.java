package com.tycooncraft.blackmarket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
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

/** v0.28 블랙마켓 시설 블록: 의뢰 게시판 · 슬롯머신 · 룰렛 테이블 · 불법 개조대 (암시장 안에서만 동작) */
public class BMBlock extends HorizontalDirectionalBlock {
    public static final com.mojang.serialization.MapCodec<BMBlock> CODEC = simpleCodec(p -> new BMBlock(p, 0));
    private final int screen;
    private static final VoxelShape TABLE = Block.box(0, 0, 0, 16, 14, 16);

    public BMBlock(Properties p, int screen) {
        super(p);
        this.screen = screen;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return screen == BMPayloads.ROULETTE ? TABLE : super.getShape(s, l, p, c);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (!BlackMarket.isBM(level)) return InteractionResult.SUCCESS;
        open(sp, screen);
        return InteractionResult.SUCCESS;
    }

    public static void open(ServerPlayer sp, int screen) {
        switch (screen) {
            case BMPayloads.MISSIONS -> BMPayloads.missions(sp, null);
            case BMPayloads.SLOTS -> BMPayloads.casino(sp, Casino.SLOTS, null, null);
            case BMPayloads.ROULETTE -> BMPayloads.casino(sp, Casino.ROULETTE, null, null);
            default -> BMPayloads.mods(sp, null);
        }
    }
}
