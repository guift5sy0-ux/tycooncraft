package com.tycooncraft.block;

import com.tycooncraft.economy.Coins;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 환전기. 우클릭하면 인벤토리의 동전을 모두 모아 가장 큰 단위로 바꿔 준다.
 * (작은 단위가 필요하면 제작대에서 큰 동전 1개 → 작은 동전 10개)
 */
public class ExchangeBlock extends Block {

    public ExchangeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        Inventory inv = player.getInventory();
        long total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (Coins.isCoin(stack)) {
                total += Coins.valueOf(stack);
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (total <= 0) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.exchange.none"), true);
            return InteractionResult.CONSUME;
        }
        Coins.give(player, total);
        player.displayClientMessage(Component.translatable("message.tycooncraft.exchange.done", Coins.format(total)), true);
        level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 0.8f, 1.4f);
        return InteractionResult.CONSUME;
    }
}
