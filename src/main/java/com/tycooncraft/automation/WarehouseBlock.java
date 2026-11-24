package com.tycooncraft.automation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WarehouseBlock extends BaseEntityBlock {
    public static final MapCodec<WarehouseBlock> CODEC = simpleCodec(WarehouseBlock::new);

    public WarehouseBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WarehouseBlockEntity w) player.openMenu(w);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState ns, boolean moved) {
        if (!state.is(ns.getBlock()) && level.getBlockEntity(pos) instanceof WarehouseBlockEntity w) Containers.dropContents(level, pos, w.inv);
        super.onRemove(state, level, pos, ns, moved);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState s) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState s, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof WarehouseBlockEntity w ? net.minecraft.world.inventory.AbstractContainerMenu.getRedstoneSignalFromContainer(w.inv) : 0;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WarehouseBlockEntity(pos, state);
    }
}
