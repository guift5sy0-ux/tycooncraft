package com.tycooncraft.blackmarket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** v0.28 의뢰 화물: 밀수 화물(배달 — 접선 지점에서 우클릭) / 도난 화물(회수 — 게시판에 가져가기) */
public class MissionItem extends Item {
    private final boolean delivery;

    public MissionItem(Properties p, boolean delivery) {
        super(p);
        this.delivery = delivery;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (delivery && p instanceof ServerPlayer sp) Missions.deliver(sp, st);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        var cd = st.get(DataComponents.CUSTOM_DATA);
        if (cd != null && delivery) {
            var t = cd.copyTag();
            tip.add(Component.translatable("tooltip.tycooncraft.smuggle_package.to", t.getInt("tx"), t.getInt("tz")).withStyle(ChatFormatting.YELLOW));
        }
        tip.add(Component.translatable(delivery ? "tooltip.tycooncraft.smuggle_package" : "tooltip.tycooncraft.stolen_cargo").withStyle(ChatFormatting.GRAY));
    }
}
