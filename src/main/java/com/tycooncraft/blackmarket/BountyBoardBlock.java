package com.tycooncraft.blackmarket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 현상금 게시판 (암시장 안에서만 동작) */
public class BountyBoardBlock extends Block {
    public BountyBoardBlock(Properties p) {
        super(p);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer sp && BlackMarket.isBM(level)) BountyPayloads.send(sp, null);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
