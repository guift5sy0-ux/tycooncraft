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

    /** v0.31: 인챈트는 되지만 반짝이지는 않음 */
    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }

    /** 가시는 안 붙음 (방탄복에 가시는 어울리지 않음) */
    @Override
    public boolean supportsEnchantment(ItemStack stack, Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        if (enchantment.is(net.minecraft.world.item.enchantment.Enchantments.THORNS)) return false;
        return super.supportsEnchantment(stack, enchantment);
    }

    @Override
    public boolean isPrimaryItemFor(ItemStack stack, Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        if (enchantment.is(net.minecraft.world.item.enchantment.Enchantments.THORNS)) return false;
        return super.isPrimaryItemFor(stack, enchantment);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(tip, args).withStyle(ChatFormatting.AQUA));
    }
}
