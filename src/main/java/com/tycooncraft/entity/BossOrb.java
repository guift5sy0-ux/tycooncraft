package com.tycooncraft.entity;

import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** 폭풍 와이번의 번개 구체: 직선 비행, 맞으면 감전(피해 + 둔화) */
public class BossOrb extends ThrowableItemProjectile {
    public BossOrb(EntityType<? extends BossOrb> type, Level level) {
        super(type, level);
    }

    public BossOrb(EntityType<? extends BossOrb> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.STORM_ORB.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), random.nextGaussian() * 0.05, random.nextGaussian() * 0.05, random.nextGaussian() * 0.05);
            level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        }
        if (tickCount > 80) discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        if (!(hit.getEntity() instanceof LivingEntity t) || hit.getEntity() == getOwner()) return;
        t.hurt(damageSources().mobProjectile(this, getOwner() instanceof LivingEntity o ? o : null), 7f);
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2), getOwner());
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide) {
            ((ServerLevel) level()).sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY(), getZ(), 20, 0.3, 0.3, 0.3, 0.3);
            level().playSound(null, getX(), getY(), getZ(), ModSounds.STORM_ORB.get(), SoundSource.HOSTILE, 1.5f, 1.6f);
            discard();
        }
    }
}
