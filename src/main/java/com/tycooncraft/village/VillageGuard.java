package com.tycooncraft.village;

import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/**
 * v0.25.0 마을 경비병: 소총으로 무장. 폭발 없이 총알만 씀.
 * - 마을(근무지 반경 40칸)에 들어온 적대 몹을 쏨
 * - 마을에서 총을 쏜 플레이어: 1번째 경고, 15초 안에 또 쏘면 60초 동안 수배 → 사격
 * - 주민·상인·경비병·골렘을 때린 플레이어는 바로 수배
 */
public class VillageGuard extends PathfinderMob implements RangedAttackMob {
    public static final float DAMAGE = 7f, PLAYER_DAMAGE = 5f, SPREAD = 0.025f;
    public static final int INTERVAL = 18, RANGE = 28, HOME_RADIUS = 46;
    private BlockPos home;

    public VillageGuard(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 60).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, 36).add(Attributes.ARMOR, 6).add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, INTERVAL, INTERVAL + 6, RANGE));
        goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 10f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, VillageGuard.class).setAlertOthers());
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                p -> p instanceof Player pl && GuardAlert.wanted(pl)));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
                m -> m instanceof Enemy && !(m instanceof VillageGuard) && inVillage(m)));
    }

    /** 경비 구역 안인가 (근무지 반경) */
    public boolean inVillage(LivingEntity e) {
        return home == null || e.blockPosition().distSqr(home) <= (double) HOME_RADIUS * HOME_RADIUS;
    }

    public void setHome(BlockPos p) {
        home = p.immutable();
        restrictTo(home, HOME_RADIUS - 8);
    }

    @Nullable
    public BlockPos home() {
        return home;
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance d, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData r = super.finalizeSpawn(level, d, reason, data);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.RIFLE.get()));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.VEST_1.get()));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HELMET_1.get()));
        for (EquipmentSlot s : EquipmentSlot.values()) setDropChance(s, 0f);
        if (home == null) setHome(blockPosition());
        return r;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        // 수배가 풀렸거나 마을 밖으로 멀리 간 대상은 놓아줌
        if (t instanceof Player p && !GuardAlert.wanted(p)) setTarget(null);
        else if (t != null && !(t instanceof Player) && home != null && t.blockPosition().distSqr(home) > (HOME_RADIUS + 12.0) * (HOME_RADIUS + 12.0)) setTarget(null);
        setAggressive(getTarget() != null);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pull) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().add(0, target.getBbHeight() * 0.1, 0);
        Vec3 dir = aim.subtract(from).normalize().add(random.nextGaussian() * SPREAD, random.nextGaussian() * SPREAD, random.nextGaussian() * SPREAD).normalize();
        // 주민·경비병·골렘, 수배 안 된 플레이어는 총알이 지나감
        Ballistics.shoot(sl, this, from.add(dir.scale(0.6)), dir, RANGE + 8, target instanceof Player ? PLAYER_DAMAGE : DAMAGE * (target instanceof Creeper ? 2f : 1f), 0, 1.5f, false, true,
                damageSources().mobAttack(this),
                e -> !(e instanceof Villager) && !(e instanceof VillageGuard) && !(e instanceof net.minecraft.world.entity.animal.IronGolem)
                        && (!(e instanceof Player p) || GuardAlert.wanted(p)));
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.RIFLE_FIRE.get(), SoundSource.NEUTRAL, 3.0f, 0.95f + random.nextFloat() * 0.1f);
        GuardAlert.shotsFired++;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (home != null) tag.put("GuardHome", NbtUtils.writeBlockPos(home));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "GuardHome").ifPresent(this::setHome);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }
}
