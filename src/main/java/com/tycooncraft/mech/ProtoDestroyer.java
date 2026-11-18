package com.tycooncraft.mech;

import com.tycooncraft.alliance.Alliances;
import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * v0.19.0 프로토 디스트로이어: 플레이어가 타는 야수형 기계.
 * 조작: WASD 이동 · 마우스 방향 · 좌클릭(누르고 있기) 개틀링 · 우클릭 로켓 3연발 · 스페이스 내려찍기 · Shift 내리기
 * 주인(과 동맹)만 탈 수 있음. 주인이 웅크리고 빈손 우클릭 → 아이템으로 회수 (체력 유지). 철/강철 주괴로 수리.
 */
public class ProtoDestroyer extends PathfinderMob {
    public static final int IDLE = 0, FIRING = 1, ROCKETS = 2, SLAM = 3;
    public static final int ACT_FIRE = 1, ACT_ROCKETS = 2, ACT_SLAM = 3;
    public static final int MAX_HEAT = 100, ROCKET_CD = 120, SLAM_CD = 70;
    public static final float GUN_DMG = 3.5f;
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEAT = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OVERHEAT = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ROCKET_T = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLAM_T = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    @Nullable private UUID owner;
    private int attackT, gunCd, lastFire = -100;
    public int animT, lastAttack;
    public static int shots, rockets, slams;

    public ProtoDestroyer(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 320).add(Attributes.ARMOR, 18).add(Attributes.ARMOR_TOUGHNESS, 8)
                .add(Attributes.MOVEMENT_SPEED, 0.27).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.6)
                .add(Attributes.SAFE_FALL_DISTANCE, 12).add(Attributes.FOLLOW_RANGE, 0);
    }

    @Override
    protected void registerGoals() {}   // 스스로는 움직이지 않음

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(ATTACK, IDLE).define(HEAT, 0).define(OVERHEAT, false).define(ROCKET_T, 0).define(SLAM_T, 0);
    }

    public int attack() { return entityData.get(ATTACK); }
    public int heat() { return entityData.get(HEAT); }
    public boolean overheated() { return entityData.get(OVERHEAT); }
    public int rocketCd() { return entityData.get(ROCKET_T); }
    public int slamCd() { return entityData.get(SLAM_T); }

    private void setAttack(int a) {
        if (attack() != a) entityData.set(ATTACK, a);
        attackT = 0;
    }

    @Nullable public UUID owner() { return owner; }
    public void setOwner(@Nullable UUID u) { owner = u; }

    public boolean mayRide(Player p) {
        if (owner == null || owner.equals(p.getUUID())) return true;
        return p.getServer() != null && Alliances.friendly(p.getServer(), owner, p.getUUID());
    }

    // ── 탑승 ─────────────────────────────────────────────────────
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (isVehicle()) return InteractionResult.PASS;
        if (st.is(net.minecraft.world.item.Items.IRON_INGOT) || st.is(ModItems.STEEL_INGOT.get())) {
            if (getHealth() >= getMaxHealth()) return InteractionResult.PASS;
            if (!level().isClientSide) {
                heal(st.is(ModItems.STEEL_INGOT.get()) ? 40 : 15);
                st.consume(1, player);
                playSound(SoundEvents.ANVIL_USE, 0.8f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!mayRide(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tycooncraft.proto.not_owner"), true);
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive() && st.isEmpty() && (owner == null || owner.equals(player.getUUID()))) {
            player.setItemInHand(hand, toItem());
            playSound(SoundEvents.IRON_GOLEM_REPAIR, 1f, 0.6f);
            discard();
            return InteractionResult.CONSUME;
        }
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public ItemStack toItem() {
        ItemStack s = new ItemStack(ModItems.PROTO_DESTROYER.get());
        CompoundTag t = new CompoundTag();
        t.putFloat("Health", getHealth());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        if (getHealth() < getMaxHealth()) s.set(DataComponents.MAX_DAMAGE, 100);
        if (getHealth() < getMaxHealth()) s.set(DataComponents.DAMAGE, Math.round(100 * (1 - getHealth() / getMaxHealth())));
        return s;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        float yaw = rider.getYRot();
        float d = Mth.wrapDegrees(yaw - getYRot());
        setYRot(getYRot() + Mth.clamp(d, -14f, 14f));   // 육중하게 천천히 돎
        yRotO = getYRot();
        yBodyRot = yHeadRot = getYRot();
        setXRot(rider.getXRot() * 0.5f);
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travel) {
        float f = rider.zza, s = rider.xxa * 0.45f;
        if (f < 0) f *= 0.45f;
        return new Vec3(s, 0, f);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(0, 2.72, 0.05).yRot(-yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    @Override
    public boolean isPushable() { return false; }

    // ── 무기 ─────────────────────────────────────────────────────
    /** 몸 기준 국소 좌표(오른쪽+, 위, 앞)를 월드로 */
    public Vec3 local(double right, double up, double fwd) {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw), fz = Mth.cos(yaw);
        double rx = -fz, rz = fx;
        return position().add(fx * fwd + rx * right, up, fz * fwd + rz * right);
    }

    /** 탑승자 시선이 닿는 곳 (조준점) */
    private Vec3 aimPoint(Player rider) {
        Vec3 eye = rider.getEyePosition(), look = rider.getLookAngle();
        var hit = level().clip(new ClipContext(eye.add(look.scale(2)), eye.add(look.scale(80)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        return hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(80)) : hit.getLocation();
    }

    public void action(ServerPlayer rider, int act) {
        if (getControllingPassenger() != rider || !isAlive()) return;
        switch (act) {
            case ACT_FIRE -> fire(rider);
            case ACT_ROCKETS -> {
                if (rocketCd() > 0 || attack() == SLAM) return;
                setAttack(ROCKETS);
                entityData.set(ROCKET_T, ROCKET_CD);
            }
            case ACT_SLAM -> {
                if (slamCd() > 0 || !onGround()) return;
                setAttack(SLAM);
                entityData.set(SLAM_T, SLAM_CD);
                playSound(SoundEvents.RAVAGER_ROAR, 2f, 0.7f);
            }
            default -> {}
        }
    }

    private void fire(ServerPlayer rider) {
        if (gunCd > 0 || overheated() || attack() == SLAM) return;
        gunCd = 2;
        lastFire = tickCount;
        if (attack() == IDLE) setAttack(FIRING);
        int h = heat() + 3;
        entityData.set(HEAT, Math.min(MAX_HEAT, h));
        if (h >= MAX_HEAT) {
            entityData.set(OVERHEAT, true);
            playSound(SoundEvents.FIRE_EXTINGUISH, 1.5f, 0.6f);
        }
        ServerLevel sl = (ServerLevel) level();
        Vec3 muzzle = local(1.2, 2.0, 2.0);
        Vec3 aim = aimPoint(rider).subtract(muzzle).normalize();
        aim = aim.add(random.nextGaussian() * 0.03, random.nextGaussian() * 0.02, random.nextGaussian() * 0.03).normalize();
        Ballistics.shoot(sl, rider, muzzle, aim, 60, GUN_DMG, 0, 1.3f, false, true, damageSources().playerAttack(rider), e -> e != this && e != rider);
        sl.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 1, 0.05, 0.05, 0.05, 0.01);
        playSound(ModSounds.SMG_FIRE.get(), 2.2f, 0.62f + random.nextFloat() * 0.06f);
        shots++;
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
            return;
        }
        if (gunCd > 0) gunCd--;
        if (rocketCd() > 0) entityData.set(ROCKET_T, rocketCd() - 1);
        if (slamCd() > 0) entityData.set(SLAM_T, slamCd() - 1);
        if (tickCount - lastFire > 6 && heat() > 0) entityData.set(HEAT, Math.max(0, heat() - (overheated() ? 2 : 3)));
        if (overheated() && heat() == 0) entityData.set(OVERHEAT, false);
        attackT++;
        Player rider = getControllingPassenger() instanceof Player p ? p : null;
        switch (attack()) {
            case FIRING -> { if (tickCount - lastFire > 8) setAttack(IDLE); }
            case ROCKETS -> {
                if (rider != null && (attackT == 6 || attackT == 12 || attackT == 18)) {
                    Vec3 from = local(-1.3, 3.1, 0.6);
                    Vec3 dir = aimPoint(rider).subtract(from).normalize();
                    level().addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level(), rider, from.add(dir.scale(1.6)), dir.scale(1.3), RocketEntity.ROCKET, 2.4f));
                    playSound(ModSounds.RPG_FIRE.get(), 2.5f, 1.0f);
                    rockets++;
                }
                if (attackT >= 24) setAttack(IDLE);
            }
            case SLAM -> {
                if (attackT == 12) slamImpact(rider);
                if (attackT >= 22) setAttack(IDLE);
            }
            default -> {}
        }
    }

    private void slamImpact(@Nullable Player rider) {
        slams++;
        ServerLevel sl = (ServerLevel) level();
        Vec3 c = local(0, 0, 1.8);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2f, 0.5f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.5f, 0.45f);
        var floor = sl.getBlockState(BlockPos.containing(c.x, c.y - 0.5, c.z));
        if (!floor.isAir()) sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, floor), c.x, c.y + 0.2, c.z, 90, 2.2, 0.2, 2.2, 0.3);
        sl.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 1.0, 0.1, 1.0, 0);
        DamageSource src = rider != null ? damageSources().playerAttack(rider) : damageSources().mobAttack(this);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(5, 1.5, 5))) {
            if (e == this || e == rider || e.getVehicle() == this) continue;
            if (rider != null && e instanceof Player p && sl.getServer() != null && Alliances.friendly(sl.getServer(), rider.getUUID(), p.getUUID())) continue;
            double d = e.position().distanceTo(c);
            if (d > 5.5) continue;
            e.hurt(src, (float) (16 - d * 1.8));
            Vec3 push = e.position().subtract(c).normalize().scale(1.3);
            e.push(push.x, 0.8, push.z);
            e.hurtMarked = true;
        }
    }

    // ── 피해 ─────────────────────────────────────────────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && hasPassenger(source.getEntity())) return false;   // 내 로켓·총알
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.6f;
        if (source.is(DamageTypeTags.IS_FALL)) return false;
        return super.hurt(source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        spawnAtLocation(new ItemStack(ModItems.DESTROYER_CORE.get()));   // 잔해에서 코어만 건짐
    }

    @Override
    public void die(DamageSource source) {
        ejectPassengers();
        if (!level().isClientSide) level().explode(this, getX(), getY() + 1.5, getZ(), 3f, Level.ExplosionInteraction.NONE);
        super.die(source);
    }

    @Override
    protected boolean shouldDropLoot() { return true; }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (owner != null) tag.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
    }

    @Override
    protected void playStepSound(BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        playSound(SoundEvents.RAVAGER_STEP, 1.4f, 0.6f);
        if (random.nextInt(3) == 0) playSound(ModSounds.SERVO.get(), 0.6f, 0.8f + random.nextFloat() * 0.2f);
    }

    @Override
    protected SoundEvent getAmbientSound() { return isVehicle() ? null : SoundEvents.RAVAGER_AMBIENT; }

    @Override
    public int getAmbientSoundInterval() { return 400; }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }

    @Override
    public float getVoicePitch() { return 0.55f; }
}
