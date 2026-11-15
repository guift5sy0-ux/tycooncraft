package com.tycooncraft.vehicle;

import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 자동차·헬리콥터 공통: 연료, 내구도, 좌석, 탑승, 운전석 클라이언트가 직접 움직이는 방식(보트와 같음).
 * 로컬 좌표: 앞 = -Z, 오른쪽 = +X (모델과 같음)
 */
public abstract class VehicleEntity extends Entity implements FueledVehicle {
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> HURT = SynchedEntityData.defineId(VehicleEntity.class, EntityDataSerializers.INT);

    protected float deltaRotation;
    private Vec3 lastServerPos;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;
    private float lerpYRot, lerpXRot;

    /** 클라이언트 표시용: 앞 방향 속도(블록/틱), 회전 속도(도/틱) */
    public float forwardSpeed, forwardSpeedO, yawRate;

    protected VehicleEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    // ── 차종별 사양 ───────────────────────────────────────────────
    protected abstract int seats();

    /** 좌석 위치(블록 단위, 로컬 좌표) — 탑승자 엉덩이 높이 */
    protected abstract Vec3 seatLocal(int index);

    public abstract Item dropItem();

    /** 조종하는 쪽(운전자 클라이언트 / 무인일 땐 서버)에서 속도와 방향을 정한다 */
    protected abstract void drive(VehicleHooks.Input in, boolean hasFuel);

    /** 이번 틱에 연료를 태우는지 (서버) */
    protected boolean burning(Vec3 moved) {
        return moved.horizontalDistanceSqr() > 1.0E-4;
    }

    protected int fuelPerTick() {
        return 1;
    }

    public abstract float maxHealth();

    // ── 동기화 데이터 ─────────────────────────────────────────────
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUEL, 0);
        builder.define(DAMAGE, 0f);
        builder.define(HURT, 0);
    }

    @Override
    public int getFuel() {
        return entityData.get(FUEL);
    }

    public void setFuel(int fuel) {
        entityData.set(FUEL, Mth.clamp(fuel, 0, maxFuel()));
    }

    public float getDamage() {
        return entityData.get(DAMAGE);
    }

    public void setDamage(float d) {
        entityData.set(DAMAGE, Math.max(0, d));
    }

    public int getHurtTime() {
        return entityData.get(HURT);
    }

    @Override
    public float healthFraction() {
        return Mth.clamp(1f - getDamage() / maxHealth(), 0f, 1f);
    }

    /** 운전자가 있고 연료가 있으면 시동이 걸린 상태 (양쪽에서 같게 계산됨) */
    public boolean engineRunning() {
        return getFirstPassenger() instanceof Player && getFuel() > 0;
    }

    // ── 좌표 ──────────────────────────────────────────────────────
    public Vec3 toWorld(Vec3 local) {
        return new Vec3(-local.x, local.y, -local.z).yRot(-getYRot() * Mth.DEG_TO_RAD);
    }

    public Vec3 forward() {
        return toWorld(new Vec3(0, 0, -1));
    }

    // ── 틱 ────────────────────────────────────────────────────────
    @Override
    public void tick() {
        if (getHurtTime() > 0) entityData.set(HURT, getHurtTime() - 1);
        super.tick();
        tickLerp();
        preDrive();
        if (isControlledByLocalInstance()) {
            boolean manned = getControllingPassenger() != null;
            VehicleHooks.Input in = manned && level().isClientSide ? VehicleHooks.client.input() : VehicleHooks.Input.NONE;
            drive(in, getFuel() > 0);
            Vec3 before = getDeltaMovement();
            move(MoverType.SELF, before);
            afterMove(before);
        } else {
            deltaRotation = 0;
        }
        // 서버는 운전자 클라이언트가 보낸 위치로 순간 이동하므로(xo도 같이 바뀜) 직접 기억한 위치와 비교
        Vec3 moved;
        if (level().isClientSide) {
            moved = new Vec3(getX() - xo, getY() - yo, getZ() - zo);
        } else {
            Vec3 now = position();
            moved = lastServerPos == null ? Vec3.ZERO : now.subtract(lastServerPos);
            lastServerPos = now;
        }
        Vec3 fwd = forward();
        forwardSpeedO = forwardSpeed;
        forwardSpeed = (float) (moved.x * fwd.x + moved.z * fwd.z);
        yawRate = Mth.wrapDegrees(getYRot() - yRotO);
        if (level().isClientSide) {
            VehicleHooks.client.tick(this);
        } else {
            if (getControllingPassenger() != null && getFuel() > 0 && burning(moved)) setFuel(getFuel() - fuelPerTick());
            serverTick(moved);
        }
    }

    // ── 충돌·행동 ─────────────────────────────────────────────────
    public static final int ACTION_CRASH = 1, ACTION_FIRE = 2, ACTION_FIRE2 = 3;
    private boolean crashed;

    /** 이동 직후(조종하는 쪽): 충돌 판정 등 */
    protected void afterMove(Vec3 velocityBefore) {
    }

    /** 큰 충돌: 운전자 클라이언트면 서버에 알리고, 서버면 바로 폭발 */
    protected void crash() {
        if (crashed) return;
        crashed = true;
        if (level().isClientSide) VehicleHooks.client.sendAction(this, ACTION_CRASH);
        else explodeWreck();
    }

    public void explodeWreck() {
        if (level().isClientSide || isRemoved()) return;
        level().explode(this, getX(), getY() + 1.0, getZ(), crashPower(), Level.ExplosionInteraction.MOB);
        discard();
    }

    protected float crashPower() {
        return 6.0f;
    }

    /** 서버: 탑승자가 보낸 행동 */
    public void handleAction(net.minecraft.server.level.ServerPlayer player, int action) {
        if (action == ACTION_CRASH && getControllingPassenger() == player) explodeWreck();
    }

    /** 하늘에 떠 있어도 내릴 때는 옆 땅바닥으로 (아래로 내려가며 설 자리를 찾음) */
    protected Vec3 groundDismount(LivingEntity passenger) {
        int seat = Math.max(0, getPassengers().indexOf(passenger));
        double side = seatLocal(seat).x <= 0 ? -1 : 1;
        for (double s : new double[]{side, -side}) {
            Vec3 base = position().add(toWorld(new Vec3(s * (getBbWidth() / 2 + 0.8), 0, seatLocal(seat).z)));
            BlockPos p = BlockPos.containing(base.x, getY() + 1, base.z);
            for (int i = 0; i < 320 && p.getY() > level().getMinBuildHeight(); i++, p = p.below()) {
                BlockPos below = p.below();
                if (level().getBlockState(below).getCollisionShape(level(), below).isEmpty() && level().getFluidState(below).isEmpty()) continue;
                Vec3 at = new Vec3(base.x, p.getY(), base.z);
                if (level().noCollision(passenger, passenger.getDimensions(Pose.STANDING).makeBoundingBox(at))) return at;
                break;
            }
        }
        return null;
    }

    /** 모든 쪽에서 운전 계산 전에 (헬기 로터 회전 등) */
    protected void preDrive() {
    }

    protected void serverTick(Vec3 moved) {
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 3;
    }

    @Override
    public double lerpTargetX() {
        return lerpSteps > 0 ? lerpX : getX();
    }

    @Override
    public double lerpTargetY() {
        return lerpSteps > 0 ? lerpY : getY();
    }

    @Override
    public double lerpTargetZ() {
        return lerpSteps > 0 ? lerpZ : getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return lerpSteps > 0 ? lerpXRot : getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return lerpSteps > 0 ? lerpYRot : getYRot();
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            lerpPositionAndRotationStep(lerpSteps, lerpX, lerpY, lerpZ, lerpYRot, lerpXRot);
            lerpSteps--;
        }
    }

    // ── 탑승 ──────────────────────────────────────────────────────
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < seats();
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float partialTick) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        return toWorld(seatLocal(i));
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        super.positionRider(passenger, callback);
        if (!hasPassenger(passenger)) return;
        passenger.setYRot(passenger.getYRot() + deltaRotation);
        passenger.setYHeadRot(passenger.getYHeadRot() + deltaRotation);
        clampRotation(passenger);
    }

    protected void clampRotation(Entity passenger) {
        passenger.setYBodyRot(getYRot());
        float f = Mth.wrapDegrees(passenger.getYRot() - getYRot());
        float g = Mth.clamp(f, -120.0F, 120.0F);
        passenger.yRotO += g - f;
        passenger.setYRot(passenger.getYRot() + g - f);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        clampRotation(passenger);
    }

    /** 내릴 때는 차 옆으로 */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        int seat = Math.max(0, getPassengers().indexOf(passenger));
        double side = seatLocal(seat).x <= 0 ? -1 : 1;
        for (double s : new double[]{side, -side}) {
            Vec3 base = position().add(toWorld(new Vec3(s * (getBbWidth() / 2 + 0.7), 0, seatLocal(seat).z)));
            for (double dy : new double[]{0, 1, -1}) {
                Vec3 p = base.add(0, dy, 0);
                AABB box = passenger.getDimensions(Pose.STANDING).makeBoundingBox(p);
                if (level().noCollision(passenger, box) && !level().getBlockState(BlockPos.containing(p).below()).isAir()) return p;
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Fuel.value(stack) > 0) {
            if (!level().isClientSide) Fuel.refuel(this, player, hand, getFuel(), maxFuel(), this::setFuel);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (stack.is(ModItems.STEEL_PLATE.get()) && getDamage() > 0) {
            if (!level().isClientSide) {
                setDamage(getDamage() - maxHealth() * 0.25f);
                if (!player.getAbilities().instabuild) stack.shrink(1);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.6f, 1.4f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) return InteractionResult.PASS;
        if (!level().isClientSide) return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        return canAddPassenger(player) ? InteractionResult.SUCCESS : InteractionResult.PASS;
    }

    // ── 피해 ──────────────────────────────────────────────────────
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (level().isClientSide || isRemoved()) return true;
        Entity attacker = source.getEntity();
        if (attacker != null && hasPassenger(attacker)) return false;
        boolean melee = attacker instanceof Player p && source.getDirectEntity() == p && !(p.getMainHandItem().getItem() instanceof GunItem);
        if (melee && ((Player) attacker).getAbilities().instabuild) {
            ejectPassengers();
            discard();
            return true;
        }
        entityData.set(HURT, 10);
        setDamage(getDamage() + (melee ? amount * 6f + 2f : amount));
        gameEvent(GameEvent.ENTITY_DAMAGE, attacker);
        if (getDamage() >= maxHealth()) {
            ejectPassengers();
            if (melee) {
                spawnAtLocation(new ItemStack(dropItem()));
            } else {
                level().explode(this, getX(), getY() + 0.6, getZ(), 2.5f, Level.ExplosionInteraction.NONE);
            }
            discard();
        }
        return true;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return 1.0f;
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(dropItem());
    }

    // ── 저장 ──────────────────────────────────────────────────────
    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(FUEL, tag.getInt("Fuel"));
        setDamage(tag.getFloat("Damage"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuel", getFuel());
        tag.putFloat("Damage", getDamage());
    }
}
