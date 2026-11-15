package com.tycooncraft.miner;

import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** 자동 굴착기 아이템: 블록 위에 놓으면 바라보는 방향으로 설치 */
public class AutoMinerItem extends Item {
    public AutoMinerItem(Properties p) {
        super(p.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var level = ctx.getLevel();
        Vec3 at = Vec3.atBottomCenterOf(ctx.getClickedPos().relative(ctx.getClickedFace()));
        AutoMinerEntity m = ModEntities.AUTO_MINER.get().create(level);
        if (m == null) return InteractionResult.FAIL;
        float yaw = ctx.getPlayer() != null ? ctx.getPlayer().getDirection().toYRot() : 0;
        m.moveTo(at.x, at.y, at.z, yaw, 0);
        if (!level.noCollision(m, m.getBoundingBox())) return InteractionResult.FAIL;
        m.setFuel(ctx.getItemInHand().getOrDefault(ModComponents.AMMO.get(), 0));
        if (ctx.getPlayer() != null) m.setOwner(ctx.getPlayer().getUUID());
        level.addFreshEntity(m);
        if (ctx.getPlayer() == null || !ctx.getPlayer().getAbilities().instabuild) ctx.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.auto_miner").withStyle(ChatFormatting.GRAY));
        int f = stack.getOrDefault(ModComponents.AMMO.get(), 0);
        if (f > 0) tooltip.add(Component.translatable("tooltip.tycooncraft.auto_miner.fuel", f * 100 / AutoMinerEntity.MAX_FUEL).withStyle(ChatFormatting.YELLOW));
    }
}
