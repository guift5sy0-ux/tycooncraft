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
    /** v0.27.1 드론 편대: 플레이어 편 드론 */
    private java.util.UUID owner;
    private int life;
    public static int allyShots;

    public void setOwner(net.minecraft.world.entity.player.Player p, int lifeTicks) {
        owner = p.getUUID();
        life = lifeTicks;
        setCustomName(net.minecraft.network.chat.Component.translatable("entity.tycooncraft.mini_drone.ally", p.getGameProfile().getName()));
        setCustomNameVisible(false);
    }

    public boolean ally() {
        return owner != null;
    }

    public java.util.UUID ownerId() {
        return owner;
    }

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
        if (owner != null) {
            allyTick();
            return;
        }
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

    /** 편대 드론: 주인 곁을 돌다가 주인이 때린 적·주인을 때린 적·주변 적대 몹을 쏨. 수명이 다하면 사라짐 */
    private void allyTick() {
        net.minecraft.world.entity.player.Player o = owner == null ? null : level().getPlayerByUUID(owner);
        if (o == null || !o.isAlive() || --life <= 0) {
            if (level() instanceof ServerLevel sl) sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY(), getZ(), 6, 0.2, 0.2, 0.2, 0.02);
            discard();
            return;
        }
        LivingEntity t = getTarget();
        if (tickCount % 10 == 0 || t == null || !t.isAlive() || t.distanceToSqr(o) > 28 * 28) {
            t = null;
            LivingEntity a = o.getLastHurtMob(), b = o.getLastHurtByMob();
            if (a != null && a.isAlive() && a.distanceToSqr(o) < 24 * 24 && o.tickCount - o.getLastHurtMobTimestamp() < 200) t = a;
            else if (b != null && b.isAlive() && b.distanceToSqr(o) < 24 * 24) t = b;
            else t = level().getNearestEntity(level().getEntitiesOfClass(LivingEntity.class, o.getBoundingBox().inflate(16),
                    e -> e instanceof net.minecraft.world.entity.monster.Enemy && e.isAlive() && !(e instanceof MiniDrone md && md.ally())),
                    net.minecraft.world.entity.ai.targeting.TargetingConditions.forCombat().ignoreLineOfSight(), this, o.getX(), o.getY(), o.getZ());
            if (t instanceof net.minecraft.world.entity.player.Player tp && (tp == o || tp.getServer() != null
                    && com.tycooncraft.alliance.Alliances.friendly(tp.getServer(), owner, tp.getUUID()))) t = null;
            setTarget(t);
        }
        orbit += 0.05f;
        Vec3 goal;
        if (t != null) {
            double r = 6 + 2 * Math.sin(orbit * 0.7 + getId());
            goal = t.position().add(Math.cos(orbit) * r, 3.5 + Math.sin(orbit * 1.3), Math.sin(orbit) * r);
            getLookControl().setLookAt(t);
            if (--fireCd <= 0 && hasLineOfSight(t)) {
                fireCd = FIRE_EVERY + random.nextInt(10);
                shootAlly(t, o);
            }
            setYRot((float) (Math.atan2(t.getZ() - getZ(), t.getX() - getX()) * 180 / Math.PI) - 90f);
        } else {
            double r = 2.5;
            goal = o.position().add(Math.cos(orbit + getId()) * r, 2.6 + Math.sin(orbit * 2 + getId()) * 0.3, Math.sin(orbit + getId()) * r);
            setYRot(o.getYRot());
        }
        Vec3 v = goal.subtract(position());
        double d = v.length();
        Vec3 want = d > 0.01 ? v.scale(Math.min(0.6, d * 0.15) / d) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.75).add(want.scale(0.4)));
        yBodyRot = getYRot();
    }

    private void shootAlly(LivingEntity t, net.minecraft.world.entity.player.Player o) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
        Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize()
                .add(random.nextGaussian() * 0.03, random.nextGaussian() * 0.03, random.nextGaussian() * 0.03).normalize();
        Ballistics.shoot(sl, o, from.add(dir.scale(0.5)), dir, 32, DAMAGE, 0, 1.2f, false, true, damageSources().mobAttack(this),
                e -> e != o && !(e instanceof MiniDrone md && md.ally()) && !(e instanceof net.minecraft.world.entity.player.Player p
                        && (p.getServer() != null && com.tycooncraft.alliance.Alliances.friendly(p.getServer(), owner, p.getUUID()))));
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.PISTOL_FIRE.get(), SoundSource.PLAYERS, 1.0f, 1.7f);
        allyShots++;
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (owner != null) {
            t.putUUID("Owner", owner);
            t.putInt("Life", life);
        }
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.hasUUID("Owner")) {
            owner = t.getUUID("Owner");
            life = t.getInt("Life");
        }
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
        return carrier == null && owner == null;
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return owner == null && super.canBeSeenAsEnemy();   // 편대 드론은 골렘·경비병이 노리지 않음
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return owner == null;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (owner != null && src.getEntity() != null && owner.equals(src.getEntity().getUUID())) return false;   // 주인 총에 안 맞음
        return super.hurt(src, amount);
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
