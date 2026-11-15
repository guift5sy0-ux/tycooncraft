package com.tycooncraft.entity;

import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** 여왕거미 투사체: 거미줄(맞은 곳에 거미줄 + 둔화) / 맹독 침(독·위더) */
public class SpiderShot extends ThrowableItemProjectile {
    /** 임시 거미줄: 12초 뒤 사라짐 */
    public record TempWeb(ServerLevel level, BlockPos pos, long until) {}
    public static final List<TempWeb> WEBS = new ArrayList<>();

    public SpiderShot(EntityType<? extends SpiderShot> type, Level level) {
        super(type, level);
    }

    public SpiderShot(EntityType<? extends SpiderShot> type, LivingEntity owner, Level level, boolean venom) {
        super(type, owner, level);
        setItem(new ItemStack(venom ? ModItems.VENOM_SPIT.get() : Items.COBWEB));
    }

    public boolean venom() {
        return getItem().is(ModItems.VENOM_SPIT.get());
    }

    @Override
    protected Item getDefaultItem() {
        return Items.COBWEB;
    }

    @Override
    protected double getDefaultGravity() {
        return venom() ? 0.01 : 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (venom()) level().addParticle(new DustParticleOptions(new Vector3f(0.3f, 0.85f, 0.2f), 1f), getX(), getY(), getZ(), 0, 0, 0);
            else level().addParticle(ParticleTypes.WHITE_ASH, getX(), getY(), getZ(), 0, 0, 0);
        }
        if (tickCount > 100) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!(hit.getEntity() instanceof LivingEntity t) || hit.getEntity() == getOwner()) return;
        if (venom()) {
            t.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), 5f);
            t.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 1), getOwner());
            t.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), getOwner());
            t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0), getOwner());
        } else {
            t.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), 2f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3), getOwner());
            web(t.blockPosition());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (!venom()) web(hit.getBlockPos().relative(hit.getDirection()));
    }

    private void web(BlockPos p) {
        if (level() instanceof ServerLevel sl && sl.getBlockState(p).canBeReplaced() && sl.getFluidState(p).isEmpty()) {
            sl.setBlockAndUpdate(p, Blocks.COBWEB.defaultBlockState());
            WEBS.add(new TempWeb(sl, p, sl.getGameTime() + 240));
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) {
            ((ServerLevel) level()).sendParticles(new ItemParticleOption(ParticleTypes.ITEM, getItem()), getX(), getY(), getZ(), 10, 0.2, 0.2, 0.2, 0.08);
            discard();
        }
    }

    /** 서버 틱: 만료된 임시 거미줄 제거 */
    public static void cleanup() {
        WEBS.removeIf(w -> {
            if (w.level().getGameTime() < w.until()) return false;
            if (w.level().getBlockState(w.pos()).is(Blocks.COBWEB)) w.level().removeBlock(w.pos(), false);
            return true;
        });
    }
}
