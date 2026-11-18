package com.tycooncraft.gear;

import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import java.util.List;

/**
 * 경찰 방패(진압 방패): 일반 방패보다 크고 튼튼함(내구도 1200).
 * 들고 막는 동안 앞에서 오는 총알·근접·폭발을 막고, 밀려나지 않으며, 막은 총알은 뒤로 관통하지 않는다.
 */
public class PoliceShieldItem extends ShieldItem {
    public PoliceShieldItem(Properties props) {
        super(props);
    }

    public static boolean blockingWith(LivingEntity e) {
        return e.isBlocking() && e.getUseItem().getItem() instanceof PoliceShieldItem;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModItems.STEEL_PLATE.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.police_shield").withStyle(ChatFormatting.GRAY));
    }

    /** 막는 중에는 밀쳐내기 무시 */
    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (blockingWith(event.getEntity())) event.setCanceled(true);
    }

    public static boolean isPlayer(LivingEntity e) {
        return e instanceof Player;
    }
}
