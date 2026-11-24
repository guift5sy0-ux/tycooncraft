package com.tycooncraft.mob;

import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 중간 보스 "파괴수 골렘 디스트로이어": 오른팔 개틀링, 어깨 로켓 포드, 내려찍기.
 * 패턴: 개틀링(회전 후 2.5초 연사) · 로켓 3연발 · 가까우면 양팔 내려찍기(충격파)
 */
public class DestroyerGolem extends Monster {
    public static final String BASEMENT_TAG = "tycooncraft_basement";
    public static int BASEMENT_DROPS;
    public static final int IDLE = 0, MINIGUN = 1, ROCKETS = 2, SLAM = 3;
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(DestroyerGolem.class, EntityDataSerializers.INT);
    private final BossBar bar = new BossBar(Component.translatable("entity.tycooncraft.destroyer_golem"), BossEvent.BossBarColor.YELLOW,
            BossEvent.BossBarOverlay.NOTCHED_6, 40);
    private int attackT, gunCd = 40, rocketCd = 100, slamCd = 20;
    public int animT, lastAttack;
    public static int shots, rockets, slams;

    public DestroyerGolem(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 60;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 260).add(Attributes.ARMOR, 14).add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.ATTACK_DAMAGE, 16).add(Attributes.MOVEMENT_SPEED, 0.23).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 40).add(Attributes.STEP_HEIGHT, 1.5);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(1, com.tycooncraft.turret.TurretHuntGoal.melee(this, 20f, 20));   // 포탑 먼저 (주먹으로 부숨)
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
        // 디스트로이어는 모든 몹에게 적대적 (다른 디스트로이어·갑옷 거치대 제외)
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, net.minecraft.world.entity.Mob.class, 10, true, false,
                e -> !(e instanceof DestroyerGolem) && e.isAlive()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ATTACK, IDLE);
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
        }
    }

    /** 몸 기준 국소 좌표(오른쪽+, 위, 앞)를 월드로 */
    private Vec3 local(double right, double up, double fwd) {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -fz, rz = fx;
        return position().add(fx * fwd + rx * right, up, fz * fwd + rz * right);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.tick(this);
        if (gunCd > 0) gunCd--;
        if (rocketCd > 0) rocketCd--;
        if (slamCd > 0) slamCd--;
        LivingEntity t = getTarget();
        int a = attack();
        if (a == IDLE) {
            if (t == null || !t.isAlive()) return;
            double d = distanceTo(t);
            boolean los = hasLineOfSight(t);
            if (d < 4.5 && slamCd == 0) setAttack(SLAM);
            else if (los && d < 30 && gunCd == 0) {
                setAttack(MINIGUN);
                playSound(ModSounds.MINIGUN_SPIN.get(), 2.5f, 1f);
            } else if (los && d > 6 && d < 40 && rocketCd == 0) setAttack(ROCKETS);
            return;
        }
        attackT++;
        if (t != null) getLookControl().setLookAt(t, 40, 40);
        switch (a) {
            case MINIGUN -> {
                getNavigation().stop();
                if (t != null && attackT > 16 && attackT % 2 == 0 && t.isAlive()) {
                    if (t != null) yBodyRot = yHeadRot;
                    Vec3 muzzle = local(1.15, 2.2, 1.9);
                    Vec3 aim = t.getBoundingBox().getCenter().subtract(muzzle).normalize();
                    aim = aim.add(random.nextGaussian() * 0.045, random.nextGaussian() * 0.03, random.nextGaussian() * 0.045).normalize();
                    Ballistics.shoot((ServerLevel) level(), this, muzzle, aim, 40, 2.6f, 0, 1.3f, false, true,
                            damageSources().mobAttack(this), e -> e != this && !(e instanceof DestroyerGolem));
                    // v0.20.0: 총구 섬광·연기는 BulletFx 가 그림
                    playSound(ModSounds.SMG_FIRE.get(), 2.2f, 0.72f + random.nextFloat() * 0.06f);
                    shots++;
                }
                if (attackT >= 70) {
                    setAttack(IDLE);
                    gunCd = 50 + random.nextInt(30);
                }
            }
            case ROCKETS -> {
                if (t != null && (attackT == 10 || attackT == 18 || attackT == 26)) {
                    Vec3 from = local(-1.2, 3.3, 0.4);
                    Vec3 aim = t.getBoundingBox().getCenter().subtract(from);
                    Vec3 dir = aim.normalize().add(0, aim.horizontalDistance() * 0.004, 0).normalize();
                    level().addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level(), this, from.add(dir.scale(0.8)), dir.scale(1.15), RocketEntity.ROCKET, 2.2f));
                    playSound(ModSounds.RPG_FIRE.get(), 2.5f, 1.1f);
                    rockets++;
                }
                if (attackT >= 36) {
                    setAttack(IDLE);
                    rocketCd = 140 + random.nextInt(60);
                }
            }
            case SLAM -> {
                getNavigation().stop();
                if (attackT == 14) slamImpact();
                if (attackT >= 26) {
                    setAttack(IDLE);
                    slamCd = 50;
                }
            }
            default -> setAttack(IDLE);
        }
    }

    private void slamImpact() {
        slams++;
        ServerLevel sl = (ServerLevel) level();
        Vec3 c = local(0, 0, 1.6);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.55f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.5f);
        var floor = sl.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
        if (!floor.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), c.x, c.y + 0.2, c.z, 80, 2.0, 0.2, 2.0, 0.3);
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 1.0, 0.1, 1.0, 0);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(4.5, 1.5, 4.5))) {
            if (e == this || e instanceof DestroyerGolem) continue;
            double d = e.position().distanceTo(c);
            if (d > 5) continue;
            e.hurt(damageSources().mobAttack(this), (float) (15 - d * 1.5));
            Vec3 push = e.position().subtract(c).normalize().scale(1.2);
            e.push(push.x, 0.75, push.z);
            e.hurtMarked = true;
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (attack() == IDLE && slamCd == 0) {
            setAttack(SLAM);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) amount *= 0.5f;
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (getPersistentData().getBoolean(BASEMENT_TAG)) {   // 폐공장 지하실 골렘: 코인 + 총 1자루
            spawnAtLocation(new ItemStack(ModItems.COIN_EMERALD.get(), 1 + random.nextInt(2)));
            spawnAtLocation(new ItemStack(ModItems.COIN_GOLD.get(), 5 + random.nextInt(11)));
            net.minecraft.world.item.Item[] guns = {ModItems.PISTOL.get(), ModItems.SMG.get(), ModItems.SHOTGUN.get(), ModItems.RIFLE.get(), ModItems.SNIPER.get()};
            spawnAtLocation(new ItemStack(guns[random.nextInt(guns.length)]));
            BASEMENT_DROPS++;
            return;
        }
        spawnAtLocation(new ItemStack(ModItems.DESTROYER_CORE.get()));
        spawnAtLocation(new ItemStack(ModItems.STEEL_INGOT.get(), 4 + random.nextInt(5)));
        spawnAtLocation(new ItemStack(ModItems.HEAVY_AMMO.get(), 16 + random.nextInt(17)));
        spawnAtLocation(new ItemStack(ModItems.ROCKET.get(), 2 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(ModItems.COIN_GOLD.get(), 3 + random.nextInt(4)));
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
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.4f, 0.55f);
        if (random.nextInt(3) == 0) playSound(ModSounds.SERVO.get(), 0.6f, 0.9f + random.nextFloat() * 0.2f);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }
}
