package com.tycooncraft.blackmarket;

import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 맨홀 뚜껑 (길바닥과 같은 높이). 바깥: 출입증 등록자만 내려감 · 암시장 안: 들어왔던 맨홀로 나감.
 * 처음 한 번은 출입증을 들고 우클릭해야 등록됨 (출입증 소모).
 */
public class ManholeBlock extends Block {
    public ManholeBlock(Properties p) {
        super(p);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack st, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!st.is(ModItems.BLACK_MARKET_PASS.get()) || BlackMarket.isBM(level)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide && player instanceof ServerPlayer sp) {
            if (!BlackMarket.member(sp)) {
                BlackMarket.persisted(sp).putBoolean(BlackMarket.REG, true);
                if (!sp.isCreative()) st.shrink(1);
                sp.sendSystemMessage(Component.translatable("message.tycooncraft.bm.registered").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            BlackMarket.enter(sp, pos);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;
        if (BlackMarket.isBM(level)) BlackMarket.leave(sp);
        else if (BlackMarket.member(sp)) BlackMarket.enter(sp, pos);
        else sp.displayClientMessage(Component.translatable("message.tycooncraft.bm.locked").withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.SUCCESS;
    }
}
