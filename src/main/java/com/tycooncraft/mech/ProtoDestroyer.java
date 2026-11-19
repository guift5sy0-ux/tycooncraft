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
 * v0.19.1 프로토 디스트로이어: 플레이어가 타는 역관절 보행 병기.
 * 조작: WASD 이동(묵직·느림) · 마우스 방향 · 좌클릭(누르고 있기) 양팔 개틀링 [중탄 소모] · 우클릭 로켓 3연발 [로켓 소모]
 *       · 스페이스 점프 (높이 떨어지면 착지 충격파) · Shift 내리기
 * 주인(과 동맹)만 탈 수 있음. 주인이 웅크리고 빈손 우클릭 → 아이템으로 회수 (체력 유지). 철/강철 주괴로 수리.
 */
public class ProtoDestroyer extends PathfinderMob {
    public static final int IDLE = 0, FIRING = 1, ROCKETS = 2, SLAM = 3;
    public static final int ACT_FIRE = 1, ACT_ROCKETS = 2, ACT_JUMP = 4;
    public static final int MAX_HEAT = 100, ROCKET_CD = 120, SLAM_CD = 40;
    public static final float GUN_DMG = 3.5f;
    public static final int MAX_HP = 200;
    /** 탑승자도 받는 피해 비율 (완전 무적이면 보스를 너무 쉽게 잡음) */
    public static final float PILOT_SHARE = 0.15f;
    /** 걸음 위상 계수: 클수록 빠른 걸음. 모델·카메라 흔들림과 공유 */
    public static final float GAIT = 0.2f;
    private float throttle;
    public static final double JUMP_VEL = 0.62;
    private static final EntityDataAccessor<Integer> ATTACK = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEAT = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OVERHEAT = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ROCKET_T = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLAM_T = SynchedEntityData.defineId(ProtoDestroyer.class, EntityDataSerializers.INT);
    @Nullable private UUID owner;
    private int attackT, gunCd, lastFire = -100;
    public int animT, lastAttack;
    public static int shots, rockets, slams, dryFires;
    private int rocketsLeft;

    public ProtoDestroyer(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HP).add(Attributes.ARMOR, 10).add(Attributes.ARMOR_TOUGHNESS, 2)
                .add(Attributes.MOVEMENT_SPEED, 0.15).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.1)
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
        setYRot(getYRot() + Mth.clamp(d, -3.5f, 3.5f));   // 육중하게 천천히 돎
        yRotO = getYRot();
        yBodyRot = yHeadRot = getYRot();
        setXRot(rider.getXRot() * 0.5f);
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travel) {
        float want = rider.zza < 0 ? rider.zza * 0.4f : rider.zza;
        throttle += Mth.clamp(want - throttle, -0.035f, 0.025f);   // 관성: 천천히 출발하고 천천히 멈춤
        // 걸음마다 발을 디딜 때 거의 멈췄다가 다시 밀고 나감 → 쿵, 쿵 끊기는 육중한 걸음
        float stomp = 1f - Math.abs(Mth.sin(walkAnimation.position() * GAIT));
        float lurch = 1f - 0.6f * stomp * stomp;
        return new Vec3(rider.xxa * 0.25f * lurch, 0, throttle * lurch);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(0, 2.45, 0.55).yRot(-yBodyRot * Mth.DEG_TO_RAD);   // 조종석 안 (기수 뒤)
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
        Vec3 from = eye.add(look.scale(2)), to = eye.add(look.scale(80));
        var hit = level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, rider));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        // 조준선에 걸린 생명체가 있으면 그 지점으로 수렴 (양팔 총구가 옆으로 떨어져 있어도 맞도록)
        var eh = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(level(), rider, from, end,
                new net.minecraft.world.phys.AABB(from, end).inflate(1), e -> e != this && e != rider && e instanceof LivingEntity && e.isAlive() && !e.isSpectator(), 0.3f);
        return eh != null ? eh.getLocation() : end;
    }

    public void action(ServerPlayer rider, int act) {
        if (getControllingPassenger() != rider || !isAlive()) return;
        switch (act) {
            case ACT_FIRE -> fire(rider);
            case ACT_ROCKETS -> {
                if (rocketCd() > 0 || attack() == ROCKETS) return;
                int have = rider.getAbilities().instabuild ? 3 : Math.min(3, count(rider, ModItems.ROCKET.get()));
                if (have == 0) { dry(); return; }
                rocketsLeft = have;
                setAttack(ROCKETS);
                entityData.set(ROCKET_T, ROCKET_CD);
            }
            case ACT_JUMP -> playSound(SoundEvents.PISTON_EXTEND, 1.6f, 0.5f);   // 점프 자체는 조종하는 클라이언트가 처리
            default -> {}
        }
    }

    public static int count(Player p, net.minecraft.world.item.Item it) {
        int n = 0;
        for (ItemStack st : p.getInventory().items) if (st.is(it)) n += st.getCount();
        return n;
    }

    private static boolean take(Player p, net.minecraft.world.item.Item it) {
        if (p.getAbilities().instabuild) return true;
        for (ItemStack st : p.getInventory().items) if (st.is(it)) { st.shrink(1); return true; }
        return false;
    }

    private void dry() {
        dryFires++;
        playSound(SoundEvents.DISPENSER_FAIL, 1f, 0.6f);
    }

    private void fire(ServerPlayer rider) {
        if (gunCd > 0 || overheated()) return;
        if (!take(rider, ModItems.HEAVY_AMMO.get())) { gunCd = 10; dry(); return; }
        gunCd = 3;
        lastFire = tickCount;
        if (attack() == IDLE) setAttack(FIRING);
        int h = heat() + 5;
        entityData.set(HEAT, Math.min(MAX_HEAT, h));
        if (h >= MAX_HEAT) {
            entityData.set(OVERHEAT, true);
            playSound(SoundEvents.FIRE_EXTINGUISH, 1.5f, 0.6f);
        }
        ServerLevel sl = (ServerLevel) level();
        Vec3 muzzle = local(shots % 2 == 0 ? 1.75 : -1.75, 3.4, 2.5);   // 오른팔·왼팔 번갈아
        Vec3 aim = aimPoint(rider).subtract(muzzle).normalize();
        aim = aim.add(random.nextGaussian() * 0.02, random.nextGaussian() * 0.015, random.nextGaussian() * 0.02).normalize();
        Ballistics.shoot(sl, rider, muzzle, aim, 70, GUN_DMG, 1, 1.3f, false, true, damageSources().playerAttack(rider), e -> e != this && e != rider);
        sl.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.06, 0.06, 0.06, 0.01);
        sl.sendParticles(ParticleTypes.LARGE_SMOKE, muzzle.x, muzzle.y, muzzle.z, 1, 0.05, 0.05, 0.05, 0.01);
        playSound(ModSounds.SMG_FIRE.get(), 2.6f, 0.6f + random.nextFloat() * 0.06f);
        shots++;
    }

    /** 점프 착지: 높이에 비례한 충격파 (조종 클라이언트가 움직여도 서버 move 에서 호출됨) */
    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        if (dist > 1.4f && slamCd() == 0 && !level().isClientSide) {
            setAttack(SLAM);
            entityData.set(SLAM_T, SLAM_CD);
            slamImpact(getControllingPassenger() instanceof Player p ? p : null, Math.min(1.6f, 0.6f + dist * 0.2f));
        } else if (dist > 0.8f) playSound(SoundEvents.ANVIL_LAND, 0.6f, 0.5f);
        return false;
    }

    @Override
    protected float nextStep() {
        return moveDist + 2.3f;   // 큰 보폭 → 발소리 간격 넓게
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
                if (rider != null && rocketsLeft > 0 && (attackT == 6 || attackT == 12 || attackT == 18) && take(rider, ModItems.ROCKET.get())) {
                    rocketsLeft--;
                    Vec3 from = local(-1.75, 4.4, 1.3);
                    Vec3 dir = aimPoint(rider).subtract(from).normalize();
                    level().addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level(), rider, from.add(dir.scale(1.6)), dir.scale(1.3), RocketEntity.ROCKET, 2.4f));
                    playSound(ModSounds.RPG_FIRE.get(), 2.5f, 1.0f);
                    rockets++;
                }
                if (attackT >= 24) setAttack(IDLE);
            }
            case SLAM -> { if (attackT >= 14) setAttack(IDLE); }
            default -> {}
        }
    }

    private void slamImpact(@Nullable Player rider, float power) {
        slams++;
        ServerLevel sl = (ServerLevel) level();
        Vec3 c = position();
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
            e.hurt(src, (float) (11 - d * 1.4) * power);
            Vec3 push = e.position().subtract(c).normalize().scale(1.2 * power);
            e.push(push.x, 0.7 * power, push.z);
            e.hurtMarked = true;
        }
    }

    // ── 피해 ─────────────────────────────────────────────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() != null && hasPassenger(source.getEntity())) return false;   // 내 로켓·총알
        if (source.is(DamageTypeTags.IS_EXPLOSION)) amount *= 0.6f;
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
        playSound(SoundEvents.IRON_GOLEM_STEP, 2.6f, 0.32f);                    // 쿵
        playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.25f, 0.35f);            // 땅 울림
        playSound(SoundEvents.ANVIL_LAND, 0.25f, 0.45f + random.nextFloat() * 0.1f);
        playSound(ModSounds.SERVO.get(), 0.7f, 0.65f + random.nextFloat() * 0.15f);   // 유압
        if (level() instanceof ServerLevel sl && !state.isAir())
            sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), getX(), getY() + 0.1, getZ(), 14, 1.0, 0.05, 1.0, 0.1);
    }

    @Override
    protected SoundEvent getAmbientSound() { return null; }

    @Override
    public int getAmbientSoundInterval() { return 400; }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.IRON_GOLEM_HURT; }

    @Override
    protected SoundEvent getDeathSound() { return SoundEvents.IRON_GOLEM_DEATH; }

    @Override
    public float getVoicePitch() { return 0.55f; }
}
