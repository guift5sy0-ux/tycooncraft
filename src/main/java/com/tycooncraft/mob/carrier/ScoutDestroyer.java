package com.tycooncraft.mob.carrier;

import com.tycooncraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.27.0 정찰 디스트로이어: 작고 빠른 사족 기계. 곧장 오지 않고 좌우로 꺾고 튀어 오르며 파고들어 날로 벤다.
 * 떼로 다니고, 공중 디스트로이어 격납고에서도 떨어진다 (격납고 출신은 코어를 안 떨어뜨림).
 */
public class ScoutDestroyer extends Monster {
    public static final float CORE_CHANCE = 0.08f;
    private boolean fromCarrier;
    private int jukeCd;
    /** 클라이언트 애니메이션: 마지막 베기 시각 */
    public long slashAt = -100;

    public ScoutDestroyer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 24).add(Attributes.MOVEMENT_SPEED, 0.38)
                .add(Attributes.ATTACK_DAMAGE, 5).add(Attributes.ARMOR, 4).add(Attributes.FOLLOW_RANGE, 36)
                .add(Attributes.STEP_HEIGHT, 1.1);
    }

    public void setFromCarrier(boolean b) {
        fromCarrier = b;
    }

    public boolean fromCarrier() {
        return fromCarrier;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.35, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, ScoutDestroyer.class, AerialDestroyer.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, com.tycooncraft.village.VillageGuard.class, true));
    }

    /** 저글링처럼: 쫓아갈 때 좌우로 꺾어 뛰고, 가까우면 덮치듯 도약 */
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (jukeCd > 0) jukeCd--;
        if (t == null || !onGround() || jukeCd > 0) return;
        Vec3 to = t.position().subtract(position());
        double d = to.horizontalDistance();
        if (d < 0.5) return;
        Vec3 fwd = new Vec3(to.x / d, 0, to.z / d);
        Vec3 side = new Vec3(-fwd.z, 0, fwd.x).scale(random.nextBoolean() ? 1 : -1);
        if (d > 4 && d < 20) {
            // 옆으로 꺾으며 짧게 점프
            Vec3 v = fwd.scale(0.6).add(side.scale(0.35 + random.nextDouble() * 0.2));
            setDeltaMovement(v.x, 0.28 + random.nextDouble() * 0.1, v.z);
            jukeCd = 18 + random.nextInt(16);
        } else if (d <= 4 && d > 1.6) {
            // 덮치기
            setDeltaMovement(fwd.x * 0.75, 0.38, fwd.z * 0.75);
            jukeCd = 18 + random.nextInt(10);
        }
    }

    @Override
    public boolean doHurtTarget(Entity e) {
        boolean ok = super.doHurtTarget(e);
        if (ok) level().broadcastEntityEvent(this, (byte) 4);
        return ok;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) slashAt = level().getGameTime();
        else super.handleEntityEvent(id);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean hit) {
        super.dropCustomDeathLoot(level, src, hit);
        if (!fromCarrier && random.nextFloat() < CORE_CHANCE) spawnAtLocation(new ItemStack(ModItems.SCOUT_CORE.get()));
        if (!fromCarrier && random.nextFloat() < 0.3f) spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.IRON_NUGGET, 2 + random.nextInt(5)));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putBoolean("FromCarrier", fromCarrier);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        fromCarrier = t.getBoolean("FromCarrier");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SILVERFISH_AMBIENT;
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
        return 1.8f;
    }
}
