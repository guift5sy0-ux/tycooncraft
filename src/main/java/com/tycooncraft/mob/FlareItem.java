package com.tycooncraft.mob;

import com.tycooncraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** 카르텔 신호탄: 땅에 쓰면 보스 "카르텔 두목"이 나타난다 */
public class FlareItem extends Item {

    public FlareItem(Properties properties) {
        super(properties.stacksTo(4));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel() instanceof ServerLevel sl) {
            BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            CartelBoss boss = ModEntities.CARTEL_BOSS.get().create(sl);
            if (boss == null) return InteractionResult.FAIL;
            boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ctx.getRotation() + 180, 0);
            boss.finalizeSpawn(sl, sl.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            sl.addFreshEntity(boss);
            sl.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 0.4, 3, 0.4, 0.05);
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 40, 1, 1, 1, 0.05);
            sl.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0f, 1.3f);
            ctx.getItemInHand().consume(1, ctx.getPlayer());
        }
        return InteractionResult.sidedSuccess(ctx.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.tycooncraft.cartel_flare").withStyle(ChatFormatting.RED));
    }
}
