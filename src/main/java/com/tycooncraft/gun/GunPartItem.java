package com.tycooncraft.gun;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 총기 파츠 아이템: 제작대에 총과 함께 넣으면 장착 */
public class GunPartItem extends Item {
    public final GunPart part;

    public GunPartItem(GunPart part, Properties p) {
        super(p);
        this.part = part;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.tycooncraft.part." + part.id).withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.tycooncraft.part.slot", Component.translatable("gunpart.slot." + part.slot.name().toLowerCase()))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.tycooncraft.part.fits").append(Component.literal(" ")).append(fits()).withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.tycooncraft.part.how").withStyle(ChatFormatting.DARK_GRAY));
    }

    private Component fits() {
        net.minecraft.network.chat.MutableComponent c = Component.empty();
        boolean first = true;
        for (String g : new String[]{"pistol", "smg", "rifle", "shotgun", "sniper", "vss", "judge", "dual_pistols"}) {
            if (!GunPart.SUPPORT.get(g).contains(part)) continue;
            if (!first) c.append(", ");
            c.append(Component.translatable("item.tycooncraft." + g));
            first = false;
        }
        return c;
    }
}
