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
    /** v0.28: 4 → 3.6 (플라잉 디스트로이어 피해 -10%) · 편대 드론은 4 그대로 */
    public static final float DAMAGE = 3.6f, ALLY_DAMAGE = 4f;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> ALLY =
            net.minecraft.network.syncher.SynchedEntityData.defineId(MiniDrone.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ALLY, false);
    }

    /** 클라이언트에서 아군 도색 판정 */
    public boolean allyClient() {
        return entityData.get(ALLY);
    }
    public static final int FIRE_EVERY = 30;
    private Entity carrier;
    private int fireCd = 20;
    private float orbit;
    /** v0.27.1 드론 편대: 플레이어 편 드론 */
    private java.util.UUID owner;
    private int life;
    public static int allyShots;
    public static double maxLeash;

    public void setOwner(net.minecraft.world.entity.player.Player p, int lifeTicks) {
        owner = p.getUUID();
        life = lifeTicks;
        entityData.set(ALLY, true);
        setCustomName(net.minecraft.network.chat.Component.translatable("entity.tycooncraft.mini_drone.ally", p.getGameProfile().getName()));
        setCustomNameVisible(false);
    }

    public boolean ally() {
        return owner != null || shipId != null;
    }

    // ── v0.34: 운송기 격납고 드론 (선체 주위를 돌며 지킴) ──
    private java.util.UUID shipId;
    private long bayKey;
    private java.util.UUID bayOwner;
    private int slot;
    public static int shipShots;

    public void setShip(Entity ship, long key, @org.jetbrains.annotations.Nullable java.util.UUID owner, int slot) {
        shipId = ship.getUUID();
        bayKey = key;
        bayOwner = owner;
        this.slot = slot;
        entityData.set(ALLY, true);
    }

    public boolean shipDrone() {
        return shipId != null;
    }

    @org.jetbrains.annotations.Nullable
    private com.tycooncraft.ship.ShipEntity ship() {
        return shipId != null && level() instanceof ServerLevel sl && sl.getEntity(shipId) instanceof com.tycooncraft.ship.ShipEntity s && s.isAlive() ? s : null;
    }

    /** 배를 지키는 적: 적대 몹 + (주인이 있으면) 동맹 아닌 플레이어 + 탑승자를 때린 것 */
    private boolean shipHostile(ServerLevel sl, Entity e, com.tycooncraft.ship.ShipEntity ship) {
        if (!e.isAlive() || e instanceof MiniDrone md && md.ally() || ship.getPassengers().contains(e)) return false;
        if (e instanceof com.tycooncraft.ship.ShipEntity) return false;
        return com.tycooncraft.turret.TurretAI.targetable(sl, e, bayOwner, true);
    }

    private void shipTick() {
        com.tycooncraft.ship.ShipEntity ship = ship();
        if (ship == null || !(level() instanceof ServerLevel sl)) {
            // 배가 해체됐거나 사라짐 → 격납고로 돌아간 셈 (보유 수는 그대로)
            if (level() instanceof ServerLevel sl2) sl2.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF, getX(), getY(), getZ(), 4, 0.2, 0.2, 0.2, 0.02);
            discard();
            return;
        }
        Vec3 hull = ship.hullCenter();
        double guard = ship.hullRadius() + 22;
        LivingEntity t = getTarget();
        if (tickCount % 10 == 0 || t == null || !t.isAlive() || t.distanceToSqr(hull) > (guard + 6) * (guard + 6)) {
            t = null;
            // 탑승자를 때린 것 먼저
            for (Entity p : ship.getPassengers())
                if (p instanceof LivingEntity lp && lp.getLastHurtByMob() != null && lp.getLastHurtByMob().isAlive()
                        && shipHostile(sl, lp.getLastHurtByMob(), ship) && lp.getLastHurtByMob().distanceToSqr(hull) < guard * guard) t = lp.getLastHurtByMob();
            if (t == null) {
                double best = guard * guard;
                for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(hull, hull).inflate(guard), e -> shipHostile(sl, e, ship))) {
                    double d = e.distanceToSqr(hull);
                    if (d < best) {
                        best = d;
                        t = e;
                    }
                }
            }
            setTarget(t);
        }
        orbit += 0.04f;
        Vec3 goal;
        if (t != null) {
            double r = 6 + 2 * Math.sin(orbit * 0.7 + getId());
            goal = t.position().add(Math.cos(orbit + slot) * r, 3.5 + Math.sin(orbit * 1.3), Math.sin(orbit + slot) * r);
            getLookControl().setLookAt(t);
            if (--fireCd <= 0 && hasLineOfSight(t)) {
                fireCd = FIRE_EVERY + random.nextInt(10);
                shootShip(sl, t, ship);
            }
            setYRot((float) (Math.atan2(t.getZ() - getZ(), t.getX() - getX()) * 180 / Math.PI) - 90f);
        } else {
            // 선체 둘레를 원으로 돎 (드론마다 높이·위상 다르게)
            double r = ship.hullRadius() + 2.5 + (slot % 2) * 1.2;
            double a = orbit * 0.6 + slot * (Math.PI * 2 / 8);
            goal = new Vec3(hull.x + Math.cos(a) * r, ship.getY() + ship.hullHeight() + 1.5 + (slot % 3) * 0.8 + Math.sin(orbit * 2 + slot) * 0.3, hull.z + Math.sin(a) * r);
            setYRot((float) (a * 180 / Math.PI));
        }
        // 선체에서 너무 멀어지면 끌려 돌아옴
        if (distanceToSqr(hull) > (guard + 10) * (guard + 10)) goal = hull.add(0, ship.hullHeight() + 2, 0);
        Vec3 v = goal.subtract(position()).add(ship.getDeltaMovement().scale(4));
        double d = v.length();
        Vec3 want = d > 0.01 ? v.scale(Math.min(0.7, d * 0.15) / d) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.75).add(want.scale(0.4)));
        yBodyRot = getYRot();
    }

    private void shootShip(ServerLevel sl, LivingEntity t, com.tycooncraft.ship.ShipEntity ship) {
        Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
        Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize()
                .add(random.nextGaussian() * 0.03, random.nextGaussian() * 0.03, random.nextGaussian() * 0.03).normalize();
        Entity shooter = bayOwner != null ? sl.getPlayerByUUID(bayOwner) : null;
        com.tycooncraft.ship.ShipCollisions.IGNORE.set(ship);
        try {
            Ballistics.shoot(sl, shooter != null ? shooter : this, from.add(dir.scale(0.5)), dir, 32, ALLY_DAMAGE, 0, 1.2f, false, true, damageSources().mobAttack(this),
                    e -> e != shooter && !(e instanceof MiniDrone md && md.ally()) && !ship.getPassengers().contains(e)
                            && com.tycooncraft.turret.TurretAI.canHit(sl, e, bayOwner));
        } finally {
            com.tycooncraft.ship.ShipCollisions.IGNORE.remove();
        }
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.PISTOL_FIRE.get(), SoundSource.PLAYERS, 1.0f, 1.7f);
        shipShots++;
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
        if (shipId != null) {
            shipTick();
            return;
        }
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
        // v0.28: 모체에서 50블록 넘게는 못 감 — 목표가 그 밖이면 모체 곁으로 복귀
        if (t != null && t.distanceToSqr(carrier) > AerialDestroyer.DRONE_LEASH * AerialDestroyer.DRONE_LEASH) t = null;
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
        Vec3 off = goal.subtract(carrier.position());   // 목표점도 모체 반경 안으로
        if (off.length() > AerialDestroyer.DRONE_LEASH - 2) goal = carrier.position().add(off.normalize().scale(AerialDestroyer.DRONE_LEASH - 2));
        if (distanceToSqr(carrier) > AerialDestroyer.DRONE_LEASH * AerialDestroyer.DRONE_LEASH) {
            Vec3 back = carrier.position().subtract(position()).normalize();   // 넘어가 있으면 강하게 끌려 돌아옴
            setDeltaMovement(back.scale(0.6));
        }
        maxLeash = Math.max(maxLeash, Math.sqrt(distanceToSqr(carrier)));
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
        Ballistics.shoot(sl, o, from.add(dir.scale(0.5)), dir, 32, ALLY_DAMAGE, 0, 1.2f, false, true, damageSources().mobAttack(this),
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
            entityData.set(ALLY, true);
        }
    }

    void shoot(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.5, 0);
        Vec3 dir = t.getBoundingBox().getCenter().subtract(from).normalize()
                .add(random.nextGaussian() * 0.04, random.nextGaussian() * 0.04, random.nextGaussian() * 0.04).normalize();
        Ballistics.shoot(sl, this, from.add(dir.scale(0.5)), dir, 32, DAMAGE, 0, 1.2f, false, true, damageSources().mobAttack(this),
                e -> !Destroyers.isDestroyer(e));
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.PISTOL_FIRE.get(), SoundSource.HOSTILE, 1.2f, 1.6f);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return carrier == null && owner == null && shipId == null;
    }

    /** 격납고 드론은 저장하지 않음 (배가 다시 불러올 때 격납고가 새로 띄움) */
    @Override
    public boolean shouldBeSaved() {
        return shipId == null && super.shouldBeSaved();
    }

    @Override
    public void die(DamageSource src) {
        if (shipId != null && level() instanceof ServerLevel sl) com.tycooncraft.ship.DroneBays.lost(sl.getEntity(shipId), bayKey);
        super.die(src);
    }

    @Override
    public boolean canBeSeenAsEnemy() {
        return owner == null && shipId == null && super.canBeSeenAsEnemy();   // 편대 드론은 골렘·경비병이 노리지 않음
    }

    @Override
    public boolean shouldDespawnInPeaceful() {
        return owner == null;
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (owner != null && src.getEntity() != null && owner.equals(src.getEntity().getUUID())) return false;   // 주인 총에 안 맞음
        if (shipId != null && src.getEntity() != null) {   // 격납고 드론: 주인·동맹·탑승자 총에 안 맞음
            Entity a = src.getEntity();
            if (a instanceof MiniDrone md && md.ally()) return false;
            if (a instanceof net.minecraft.world.entity.player.Player pl && bayOwner != null && a.getServer() != null
                    && (pl.getUUID().equals(bayOwner) || com.tycooncraft.alliance.Alliances.friendly(a.getServer(), bayOwner, pl.getUUID()))) return false;
            if (a.getVehicle() instanceof com.tycooncraft.ship.ShipEntity s2 && s2.getUUID().equals(shipId)) return false;
        }
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
