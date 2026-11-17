package com.tycooncraft.mob;

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
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;

/**
 * 보스 "화산 거수 베헤모스": 용암 균열이 빛나는 거대한 네발 짐승.
 * 패턴: 돌진(벽에 박으면 기절) · 앞발 들어 내려찍기(충격파+불) · 화염 브레스 · (체력 절반 이하) 운석 낙하
 */
public class Behemoth extends Monster {
    public static final int IDLE = 0, CHARGE = 1, STOMP = 2, BREATH = 3, STUN = 4, METEOR = 5;
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(Behemoth.class, EntityDataSerializers.INT);
    private final BossBar bar = new BossBar(Component.translatable("entity.tycooncraft.behemoth"), BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10, 64);
    private int attackT, cd = 40, chargeCd = 60, meteorCd = 120;
    private Vec3 chargeDir = Vec3.ZERO;
    private final Set<Integer> chargeHit = new HashSet<>();
    private boolean enraged;
    public int animT, lastAttack;
    public static int charges, stomps, breaths, meteors;

    public Behemoth(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 200;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ARMOR, 16).add(Attributes.ARMOR_TOUGHNESS, 8)
                .add(Attributes.ATTACK_DAMAGE, 20).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 48).add(Attributes.STEP_HEIGHT, 1.6);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 20f));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, true));
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
            if (random.nextInt(4) == 0)
                level().addParticle(ParticleTypes.LAVA, getRandomX(0.6), getY() + getBbHeight() * (0.4 + random.nextDouble() * 0.6), getRandomZ(0.6), 0, 0, 0);
            if (a == BREATH && animT > 10 && animT < 46) {
                Vec3 mouth = mouth();
                Vec3 dir = Vec3.directionFromRotation(getXRot() + 8, yHeadRot);
                for (int i = 0; i < 6; i++) {
                    Vec3 v = dir.scale(0.6 + random.nextDouble() * 0.5).add(random.nextGaussian() * 0.08, random.nextGaussian() * 0.08, random.nextGaussian() * 0.08);
                    level().addParticle(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z, v.x, v.y, v.z);
                }
            }
            if (a == STUN) level().addParticle(ParticleTypes.CRIT, getX() + Mth.cos(animT * 0.5f) * 1.2, getY() + getBbHeight() + 0.2, getZ() + Mth.sin(animT * 0.5f) * 1.2, 0, 0, 0);
        }
    }

    private Vec3 forward() {
        return Vec3.directionFromRotation(0, yBodyRot);
    }

    private Vec3 mouth() {
        return position().add(Vec3.directionFromRotation(0, yHeadRot).scale(getBbWidth() * 0.9)).add(0, 2.0, 0);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        bar.tick(this);
        if (cd > 0) cd--;
        if (chargeCd > 0) chargeCd--;
        if (meteorCd > 0) meteorCd--;
        if (!enraged && getHealth() < getMaxHealth() * 0.5f) {
            enraged = true;
            bar.setDarken(true);
            playSound(ModSounds.BEHEMOTH_ROAR.get(), 5f, 0.7f);
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
            ((ServerLevel) level()).sendParticles(ParticleTypes.LAVA, getX(), getY() + 2, getZ(), 60, 2, 1, 2, 0.2);
        }
        LivingEntity t = getTarget();
        int a = attack();
        if (a == IDLE) {
            if (t == null || !t.isAlive() || cd > 0) return;
            double d = distanceTo(t);
            boolean los = hasLineOfSight(t);
            if (d < 5.5) setAttack(STOMP);
            else if (enraged && meteorCd == 0 && d < 30) setAttack(METEOR);
            else if (los && d > 9 && d < 30 && chargeCd == 0) {
                setAttack(CHARGE);
                playSound(ModSounds.BEHEMOTH_ROAR.get(), 4f, 1f);
            } else if (los && d < 10) setAttack(BREATH);
            return;
        }
        attackT++;
        switch (a) {
            case CHARGE -> tickCharge(t);
            case STOMP -> {
                getNavigation().stop();
                if (attackT == 18) stompImpact();
                if (attackT >= 32) endAttack(30);
            }
            case BREATH -> {
                getNavigation().stop();
                if (t != null) getLookControl().setLookAt(t, 20, 20);
                if (attackT == 10) {
                    playSound(SoundEvents.BLAZE_SHOOT, 3f, 0.5f);
                    breaths++;
                }
                if (attackT > 10 && attackT < 46 && attackT % 5 == 0) breathDamage();
                if (attackT % 10 == 0 && attackT > 8 && attackT < 46) playSound(SoundEvents.FIRECHARGE_USE, 2.5f, 0.6f);
                if (attackT >= 50) endAttack(30);
            }
            case STUN -> {
                getNavigation().stop();
                if (attackT >= 50) endAttack(10);
            }
            case METEOR -> {
                getNavigation().stop();
                if (attackT == 1) playSound(ModSounds.BEHEMOTH_ROAR.get(), 5f, 0.6f);
                if (t != null && attackT >= 12 && attackT <= 32 && attackT % 4 == 0) {
                    for (int i = 0; i < 2; i++) {
                        Vec3 at = t.position().add(random.nextGaussian() * 4, 18 + random.nextDouble() * 6, random.nextGaussian() * 4);
                        SmallFireball fb = new SmallFireball(level(), this, new Vec3(random.nextGaussian() * 0.02, -1, random.nextGaussian() * 0.02));
                        fb.setPos(at);
                        level().addFreshEntity(fb);
                    }
                    meteors++;
                }
                if (attackT >= 40) {
                    meteorCd = 220;
                    endAttack(30);
                }
            }
            default -> setAttack(IDLE);
        }
    }

    private void endAttack(int cooldown) {
        setAttack(IDLE);
        cd = enraged ? cooldown / 2 : cooldown;
    }

    private void tickCharge(LivingEntity t) {
        getNavigation().stop();
        if (attackT < 22) {                        // 앞발 긁기 + 포효
            if (t != null) {
                getLookControl().setLookAt(t, 30, 30);
                Vec3 to = t.position().subtract(position());
                chargeDir = new Vec3(to.x, 0, to.z).normalize();
                setYRot((float) (Mth.atan2(chargeDir.z, chargeDir.x) * Mth.RAD_TO_DEG) - 90f);
                yBodyRot = getYRot();
            }
            if (attackT % 4 == 0) {
                var floor = level().getBlockState(blockPosition().below());
                if (!floor.isAir()) ((ServerLevel) level()).sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor),
                        getX(), getY() + 0.1, getZ(), 12, 1.2, 0.1, 1.2, 0.1);
            }
            if (attackT == 21) {
                chargeHit.clear();
                charges++;
            }
            return;
        }
        double speed = enraged ? 0.95 : 0.8;
        setDeltaMovement(chargeDir.x * speed, getDeltaMovement().y, chargeDir.z * speed);
        setYRot((float) (Mth.atan2(chargeDir.z, chargeDir.x) * Mth.RAD_TO_DEG) - 90f);
        yBodyRot = getYRot();
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.6))) {
            if (e == this || !chargeHit.add(e.getId())) continue;
            e.hurt(damageSources().mobAttack(this), enraged ? 22f : 18f);
            e.push(chargeDir.x * 2.2, 0.7, chargeDir.z * 2.2);
            e.hurtMarked = true;
            playSound(SoundEvents.RAVAGER_ATTACK, 2f, 0.6f);
        }
        if (horizontalCollision && attackT > 26) {     // 벽에 박음 → 기절
            playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 3f, 0.4f);
            ((ServerLevel) level()).sendParticles(ParticleTypes.EXPLOSION, getX() + chargeDir.x * 2, getY() + 1.5, getZ() + chargeDir.z * 2, 3, 0.5, 0.5, 0.5, 0);
            chargeCd = 120;
            setAttack(STUN);
            return;
        }
        if (attackT >= 52) {
            chargeCd = 120;
            endAttack(20);
        }
    }

    private void stompImpact() {
        stomps++;
        ServerLevel sl = (ServerLevel) level();
        Vec3 c = position().add(forward().scale(2.0));
        sl.playSound(null, c.x, c.y, c.z, ModSounds.BEHEMOTH_STOMP.get(), SoundSource.HOSTILE, 4f, 1f);
        var floor = sl.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
        for (int ring = 1; ring <= 7; ring++) {
            int n = ring * 10;
            for (int i = 0; i < n; i++) {
                double ang = i * Math.PI * 2 / n;
                double x = c.x + Math.cos(ang) * ring, z = c.z + Math.sin(ang) * ring;
                if (!floor.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), x, c.y + 0.1, z, 2, 0.1, 0.1, 0.1, 0.15);
                if (ring % 2 == 0 && i % 3 == 0) sl.sendParticles(ParticleTypes.FLAME, x, c.y + 0.2, z, 1, 0.05, 0.1, 0.05, 0.02);
            }
        }
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(7, 2, 7))) {
            if (e == this) continue;
            double d = e.position().distanceTo(c);
            if (d > 7.5 || !e.onGround()) continue;
            e.hurt(damageSources().mobAttack(this), (float) (16 - d * 1.4));
            e.igniteForSeconds(3);
            Vec3 push = e.position().subtract(c).normalize().scale(1.0);
            e.push(push.x, 0.9, push.z);
            e.hurtMarked = true;
        }
        if (sl.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING)) {
            for (int i = 0; i < 6; i++) {
                BlockPos p = BlockPos.containing(c.x + random.nextGaussian() * 3, c.y, c.z + random.nextGaussian() * 3);
                if (sl.getBlockState(p).isAir() && sl.getBlockState(p.below()).isFaceSturdy(sl, p.below(), net.minecraft.core.Direction.UP))
                    sl.setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
            }
        }
    }

    private void breathDamage() {
        Vec3 mouth = mouth();
        Vec3 dir = Vec3.directionFromRotation(getXRot() + 8, yHeadRot);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(10))) {
            if (e == this) continue;
            Vec3 to = e.getBoundingBox().getCenter().subtract(mouth);
            double d = to.length();
            if (d > 10 || to.normalize().dot(dir) < 0.85) continue;
            e.hurt(damageSources().mobAttack(this), 4f);
            e.igniteForSeconds(5);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        if (attack() == IDLE) {
            setAttack(STOMP);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (attack() == STUN) amount *= 1.5f;       // 기절 중에는 약점 노출
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.BEHEMOTH_HEART.get()));
        spawnAtLocation(new ItemStack(Items.NETHERITE_SCRAP, 1 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(Items.MAGMA_CREAM, 4 + random.nextInt(5)));
        spawnAtLocation(new ItemStack(ModItems.COIN_EMERALD.get(), 2));
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
        playSound(SoundEvents.RAVAGER_STEP, 1.6f, 0.5f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RAVAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.RAVAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BEHEMOTH_ROAR.get();
    }

    @Override
    public float getVoicePitch() {
        return 0.5f;
    }

    @Override
    protected float getSoundVolume() {
        return 3f;
    }
}
