package com.tycooncraft.mob;

import com.tycooncraft.entity.BossOrb;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 보스 "폭풍 와이번": 하늘을 선회하며 번개 구체 연사 · 급강하 돌격 · 낙뢰 예고 후 강타.
 */
public class StormWyvern extends FlyingMob implements Enemy {
    public static final int CIRCLE = 0, VOLLEY = 1, DIVE = 2, THUNDER = 3;
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(StormWyvern.class, EntityDataSerializers.INT);
    private final BossBar bar = new BossBar(Component.translatable("entity.tycooncraft.storm_wyvern"), BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_10, 80);
    private int attackT, circleT;
    private float orbitAngle;
    private Vec3 home, diveTarget, wanderTarget;
    private final List<Vec3> strikes = new ArrayList<>();
    private boolean diveHit;
    public int animT, lastAttack;
    public static int volleys, dives, thunders;

    public StormWyvern(EntityType<? extends FlyingMob> type, Level level) {
        super(type, level);
        this.xpReward = 180;
        this.noPhysics = false;
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 450).add(Attributes.ARMOR, 10).add(Attributes.ATTACK_DAMAGE, 16)
                .add(Attributes.FLYING_SPEED, 0.6).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FOLLOW_RANGE, 64)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ATTACK, CIRCLE);
    }

    public int attack() {
        return entityData.get(ATTACK);
    }

    private void setAttack(int a) {
        entityData.set(ATTACK, a);
        attackT = 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            int a = attack();
            if (a != lastAttack) {
                lastAttack = a;
                animT = 0;
            } else animT++;
            if (random.nextInt(3) == 0)
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getRandomX(1.5), getRandomY(), getRandomZ(1.5), 0, 0, 0);
            if (tickCount % 14 == 0 && a != DIVE)
                level().playLocalSound(getX(), getY(), getZ(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.HOSTILE, 1.6f, 1.25f + random.nextFloat() * 0.1f, false);
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity le && le != this && getTarget() == null) setTarget(le);
        return super.hurt(source, amount);
    }

    private void steer(Vec3 to, double speed, double accel) {
        Vec3 want = to.subtract(position());
        if (want.lengthSqr() > 0.01) want = want.normalize().scale(speed);
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.add(want.subtract(v).scale(accel)));
        faceVelocity();
    }

    private void faceVelocity() {
        Vec3 v = getDeltaMovement();
        if (v.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) (Mth.atan2(v.z, v.x) * Mth.RAD_TO_DEG) - 90f;
            setYRot(Mth.approachDegrees(getYRot(), yaw, 12f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
        setXRot((float) Mth.clamp(-v.y * 50, -40, 40));
    }

    private void faceTarget(LivingEntity t) {
        Vec3 d = t.position().subtract(position());
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90f;
        setYRot(Mth.approachDegrees(getYRot(), yaw, 15f));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    private double groundY(double x, double z) {
        return level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.tick(this);
        if (home == null) home = position();
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || distanceTo(t) > 80 || (t instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            setTarget(null);
            t = null;
        }
        int a = attack();
        attackT++;
        if (t == null) {
            if (a != CIRCLE) setAttack(CIRCLE);
            if (wanderTarget == null || position().distanceTo(wanderTarget) < 4 || attackT % 200 == 0) {
                double x = home.x + random.nextGaussian() * 24, z = home.z + random.nextGaussian() * 24;
                wanderTarget = new Vec3(x, groundY(x, z) + 18 + random.nextInt(12), z);
            }
            steer(wanderTarget, 0.35, 0.05);
            return;
        }
        switch (a) {
            case CIRCLE -> {
                orbitAngle += 0.035f;
                double r = 14;
                Vec3 p = new Vec3(t.getX() + Math.cos(orbitAngle) * r, Math.max(t.getY() + 10, groundY(t.getX(), t.getZ()) + 8), t.getZ() + Math.sin(orbitAngle) * r);
                steer(p, 0.55, 0.08);
                if (attackT > 70 + random.nextInt(30)) {
                    int pick = random.nextInt(10);
                    if (pick < 4) setAttack(VOLLEY);
                    else if (pick < 7) {
                        setAttack(DIVE);
                        diveHit = false;
                        playSound(ModSounds.WYVERN_SCREECH.get(), 4f, 1.1f);
                    } else {
                        setAttack(THUNDER);
                        strikes.clear();
                        strikes.add(t.position());
                        for (int i = 0; i < 2; i++) strikes.add(t.position().add(random.nextGaussian() * 4, 0, random.nextGaussian() * 4));
                        playSound(ModSounds.WYVERN_SCREECH.get(), 4f, 0.8f);
                    }
                }
            }
            case VOLLEY -> {
                setDeltaMovement(getDeltaMovement().scale(0.85));
                faceTarget(t);
                if (attackT >= 10 && attackT <= 34 && (attackT - 10) % 6 == 0) {
                    Vec3 from = position().add(Vec3.directionFromRotation(0, getYRot()).scale(2.4)).add(0, 0.9, 0);
                    Vec3 aim = t.getBoundingBox().getCenter().add(t.getDeltaMovement().scale(6)).subtract(from).normalize();
                    BossOrb orb = new BossOrb(ModEntities.BOSS_ORB.get(), this, level());
                    orb.setPos(from);
                    orb.shoot(aim.x, aim.y, aim.z, 1.4f, 1.0f);
                    level().addFreshEntity(orb);
                    playSound(ModSounds.STORM_ORB.get(), 2.5f, 1f);
                    volleys++;
                }
                if (attackT >= 45) setAttack(CIRCLE);
            }
            case DIVE -> {
                if (attackT < 14) {                            // 솟구치며 겨냥
                    setDeltaMovement(getDeltaMovement().scale(0.8).add(0, 0.06, 0));
                    faceTarget(t);
                    diveTarget = t.position().add(0, 0.5, 0);
                } else {
                    if (attackT == 14) dives++;
                    steer(diveTarget, 1.45, 0.35);
                    if (!diveHit && getBoundingBox().inflate(0.8).intersects(t.getBoundingBox())) {
                        diveHit = true;
                        t.hurt(damageSources().mobAttack(this), 16f);
                        Vec3 push = getDeltaMovement().normalize();
                        t.push(push.x * 1.5, 0.8, push.z * 1.5);
                        t.hurtMarked = true;
                        playSound(SoundEvents.PLAYER_ATTACK_KNOCKBACK, 3f, 0.5f);
                    }
                    if (diveHit || position().distanceTo(diveTarget) < 1.5 || verticalCollision || horizontalCollision || attackT > 60) {
                        setDeltaMovement(getDeltaMovement().multiply(0.5, 0, 0.5).add(0, 0.7, 0));
                        setAttack(CIRCLE);
                    }
                }
            }
            case THUNDER -> {
                setDeltaMovement(getDeltaMovement().scale(0.85));
                faceTarget(t);
                ServerLevel sl = (ServerLevel) level();
                if (attackT < 32 && attackT % 2 == 0) {
                    for (Vec3 s : strikes) {
                        double gy = groundY(s.x, s.z) + 0.1;
                        for (int i = 0; i < 12; i++) {
                            double ang = (i + attackT * 0.3) * Math.PI / 6;
                            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, s.x + Math.cos(ang) * 2.5, gy, s.z + Math.sin(ang) * 2.5, 1, 0, 0, 0, 0);
                        }
                    }
                }
                if (attackT == 32) {
                    thunders++;
                    for (Vec3 s : strikes) {
                        double gy = groundY(s.x, s.z);
                        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(sl);
                        if (bolt != null) {
                            bolt.moveTo(s.x, gy, s.z);
                            bolt.setVisualOnly(true);
                            sl.addFreshEntity(bolt);
                        }
                        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(s.x - 2.5, gy - 1, s.z - 2.5, s.x + 2.5, gy + 4, s.z + 2.5))) {
                            if (e == this) continue;
                            e.hurt(damageSources().lightningBolt(), 10f);
                            e.igniteForSeconds(2);
                        }
                    }
                    sl.playSound(null, getX(), getY(), getZ(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 6f, 1f);
                }
                if (attackT >= 44) setAttack(CIRCLE);
            }
            default -> setAttack(CIRCLE);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.STORM_SCALE.get()));
        spawnAtLocation(new ItemStack(Items.PHANTOM_MEMBRANE, 3 + random.nextInt(4)));
        spawnAtLocation(new ItemStack(Items.LIGHTNING_ROD, 2));
        spawnAtLocation(new ItemStack(ModItems.COIN_DIAMOND.get(), 1));
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.clear();
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void checkDespawn() {
        if (level().getDifficulty() == Difficulty.PEACEFUL) discard();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.WYVERN_SCREECH.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 3f;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) {
            tag.putDouble("HomeX", home.x);
            tag.putDouble("HomeY", home.y);
            tag.putDouble("HomeZ", home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeX")) home = new Vec3(tag.getDouble("HomeX"), tag.getDouble("HomeY"), tag.getDouble("HomeZ"));
    }

    public static boolean checkSpawn(EntityType<StormWyvern> type, net.minecraft.world.level.ServerLevelAccessor level, net.minecraft.world.entity.MobSpawnType reason,
                                     BlockPos pos, net.minecraft.util.RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && (reason != net.minecraft.world.entity.MobSpawnType.NATURAL || random.nextInt(6) == 0)
                && Monster.isDarkEnoughToSpawn(level, pos, random);
    }
}
