package com.tycooncraft.gun;

import com.tycooncraft.entity.GrenadeEntity;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** 수류탄 던지기 (우클릭). 2.5초 신관 */
public class GrenadeItem extends Item {
    private final String tip;

    public GrenadeItem(Properties p, String tip) {
        super(p.stacksTo(16));
        this.tip = tip;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GRENADE_PIN.get(), SoundSource.PLAYERS, 1f, 1f);
        if (!level.isClientSide) {
            GrenadeEntity g = new GrenadeEntity(ModEntities.GRENADE.get(), player, level);
            g.setItem(stack.copyWithCount(1));
            g.shootFromRotation(player, player.getXRot(), player.getYRot(), -6f, player.isShiftKeyDown() ? 0.55f : 1.15f, 0.5f);
            level.addFreshEntity(g);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, 20);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tip).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tycooncraft.grenade.throw").withStyle(ChatFormatting.DARK_GRAY));
    }
}
