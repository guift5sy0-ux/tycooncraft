package com.tycooncraft.economy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 동전. 판매소에 넣으면 액면가 그대로 입금된다. */
public class CoinItem extends Item {
    private final long value;

    public CoinItem(Properties properties, long value) {
        super(properties);
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.coin_value", Coins.format(value)).withStyle(ChatFormatting.GREEN));
        if (stack.getCount() > 1) {
            tooltip.add(Component.translatable("tooltip.tycooncraft.coin_total", Coins.format(value * stack.getCount()))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
