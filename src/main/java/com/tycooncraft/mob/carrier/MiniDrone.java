package com.tycooncraft.mob.carrier;

import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.27.0 미니 드론: 공중 디스트로이어가 뽑아내는 소형 비행체. 표적 주위를 8~12칸 거리로 돌며 총알(피해 4)로 견제.
 * 모체가 사라지면 추락한다.
 */
public class MiniDrone extends Monster {
    public static final float DAMAGE = 4f;
    public static final int FIRE_EVERY = 30;
    private Entity carrier;
    private int fireCd = 20;
    private float orbit;

    public MiniDrone(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 1;
        orbit = random.nextFloat() * 6.28f;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 6).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 40);
    }

    public void setCarrier(Entity c) {
        carrier = c;
    }

    @Override
    protected void registerGoals() {}

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return isNoGravity() ? false : super.causeFallDamage(d, m, s);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (carrier == null || !carrier.isAlive() || (carrier instanceof AerialDestroyer a && a.crashing())) {
            // 모체를 잃음 → 추락
            setNoGravity(false);
            if (onGround() || tickCount > 2400) hurt(damageSources().generic(), 100);
            return;
        }
        LivingEntity t = carrier instanceof Mob m ? m.getTarget() : null;
        Vec3 goal;
        orbit += 0.035f;
        if (t != null && t.isAlive()) {
            double r = 8 + 4 * Math.sin(orbit * 0.7 + getId());
            goal = t.position().add(Math.cos(orbit) * r, 4.5 + Math.sin(orbit * 1.3) * 1.5, Math.sin(orbit) * r);
            setTarget(t);
            getLookControl().setLookAt(t);
            if (--fireCd <= 0 && hasLineOfSight(t)) {
                fireCd = FIRE_EVERY + random.nextInt(10);
                shoot(t);
            }
        } else {
            goal = carrier.position().add(Math.cos(orbit) * 5, -1.5, Math.sin(orbit) * 5);
        }
        Vec3 v = goal.subtract(position());
        double d = v.length();
        Vec3 want = d > 0.01 ? v.scale(Math.min(0.45, d * 0.12) / d) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.8).add(want.scale(0.35)));
        if (t != null) setYRot((float) (Math.atan2(t.getZ() - getZ(), t.getX() - getX()) * 180 / Math.PI) - 90f);
        yBodyRot = getYRot();
    }

    void shoot(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
        Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize()
                .add(random.nextGaussian() * 0.04, random.nextGaussian() * 0.04, random.nextGaussian() * 0.04).normalize();
        Ballistics.shoot(sl, this, from.add(dir.scale(0.5)), dir, 32, DAMAGE, 0, 1.2f, false, true, damageSources().mobAttack(this),
                e -> !(e instanceof MiniDrone) && !(e instanceof AerialDestroyer) && !(e instanceof ScoutDestroyer));
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.PISTOL_FIRE.get(), SoundSource.HOSTILE, 1.2f, 1.6f);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return carrier == null;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEE_LOOP_AGGRESSIVE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ITEM_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return 2f;
    }

    @Override
    protected float getSoundVolume() {
        return 0.25f;
    }
}
