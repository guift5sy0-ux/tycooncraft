package com.tycooncraft.gear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 방탄조끼·방탄모·낙하산: 설명 한 줄이 붙은 방어구 */
public class GearItem extends ArmorItem {
    private final String tip;
    private final Object[] args;

    public GearItem(Holder<ArmorMaterial> material, Type type, Properties properties, String tip, Object... args) {
        super(material, type, properties);
        this.tip = tip;
        this.args = args;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tip, args).withStyle(ChatFormatting.AQUA));
    }
}
