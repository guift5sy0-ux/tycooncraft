package com.tycooncraft.vehicle;

import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * 자폭 드론 (FPV). 조종자는 드론 카메라로 화면이 바뀌고, 조종자의 클라이언트가 직접 드론을 움직인다.
 * W = 보는 방향(위아래 포함)으로 비행, A/D 옆으로, 스페이스 상승, Ctrl 하강.
 * 좌·우클릭 = 폭파, 빠르게 부딪히면 폭발, Shift = 조종 끝(천천히 내려앉음, 맨손으로 치면 회수).
 * 배터리 45초, 조종 거리 100블록. 배터리가 다 되면 추락 → 충돌 폭발.
 */
public class DroneEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Boolean> PILOTED = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> BATTERY = SynchedEntityData.defineId(DroneEntity.class, EntityDataSerializers.INT);
    public static final int MAX_BATTERY = 900;
    public static final double MAX_RANGE = 100;
    public static final float POWER = 3.2f;

    /** 조종 중인 내 클라이언트에서만 true: 서버 위치로 덮어쓰지 않는다 */
    public boolean localPilot;
    public float propAngle, propAngleO, tilt, tiltO, roll, rollO;
    private int idleTicks;

    public DroneEntity(EntityType<? extends DroneEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, Optional.empty());
        builder.define(PILOTED, false);
        builder.define(BATTERY, MAX_BATTERY);
    }

    public void setOwner(Player p) {
        entityData.set(OWNER, Optional.of(p.getUUID()));
    }

    @Nullable
    public UUID ownerId() {
        return entityData.get(OWNER).orElse(null);
    }

    @Nullable
    public Player owner() {
        UUID id = ownerId();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    public boolean isPiloted() {
        return entityData.get(PILOTED);
    }

    public void setPiloted(boolean b) {
        entityData.set(PILOTED, b);
    }

    public int battery() {
        return entityData.get(BATTERY);
    }

    // ── 조종 (조종자 클라이언트) ──────────────────────────────────
    /** 입력으로 한 틱 비행. 충돌로 터져야 하면 true */
    public boolean flyLocal(VehicleHooks.Input in, float yaw, float pitch) {
        setYRot(yaw);
        setXRot(pitch);
        boolean powered = battery() > 0;
        Vec3 v = getDeltaMovement();
        if (powered) {
            Vec3 look = Vec3.directionFromRotation(pitch, yaw);
            Vec3 flat = Vec3.directionFromRotation(0, yaw);
            Vec3 right = new Vec3(-flat.z, 0, flat.x);
            Vec3 target = look.scale(in.forward() * 1.05)
                    .add(right.scale(-in.strafe() * 0.6))
                    .add(0, (in.up() ? 0.55 : 0) - (in.down() ? 0.55 : 0), 0);
            v = v.add(target.subtract(v).scale(0.14));
        } else {
            v = v.add(0, -0.06, 0).scale(0.98);
        }
        double speed = v.length();
        setDeltaMovement(v);
        move(MoverType.SELF, v);
        tickVisuals(in);
        return (horizontalCollision || verticalCollision) && speed > 0.38;
    }

    private void tickVisuals(VehicleHooks.Input in) {
        tiltO = tilt;
        rollO = roll;
        tilt += (-in.forward() * 18f - tilt) * 0.2f;
        roll += (in.strafe() * 16f - roll) * 0.2f;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (localPilot) return;
        super.lerpTo(x, y, z, yRot, xRot, steps);
    }

    // ── 틱 ────────────────────────────────────────────────────────
    @Override
    public void tick() {
        super.tick();
        propAngleO = propAngle;
        if (level().isClientSide) {
            propAngle += (battery() > 0 && (isPiloted() || !onGround())) ? 1.9f : 0f;
            if (!localPilot) {
                tiltO = tilt;
                rollO = roll;
                Vec3 f = Vec3.directionFromRotation(0, getYRot());
                double fw = (getX() - xo) * f.x + (getZ() - zo) * f.z;
                double side = (getX() - xo) * -f.z + (getZ() - zo) * f.x;
                tilt += (Mth.clamp((float) (-fw * 40), -22f, 22f) - tilt) * 0.2f;
                roll += (Mth.clamp((float) (-side * 40), -20f, 20f) - roll) * 0.2f;
            }
            VehicleHooks.client.tick(this);
            if (isPiloted() && tickCount % 10 == 0) {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.1, getZ(), 0, -0.02, 0);
            }
            return;
        }
        if (battery() > 0 && (isPiloted() || !onGround())) entityData.set(BATTERY, battery() - 1);
        Player owner = owner();
        if (isPiloted()) {
            if (owner == null || !owner.isAlive() || owner.level() != level() || distanceTo(owner) > MAX_RANGE + 20) {
                setPiloted(false);
                if (owner != null) owner.displayClientMessage(Component.translatable("message.tycooncraft.drone.lost"), true);
            }
            // 생명체와 부딪히면 폭발
            for (Entity e : level().getEntities(this, getBoundingBox().inflate(0.25), e -> e instanceof LivingEntity && e.isAlive() && e != owner)) {
                detonate();
                return;
            }
        } else {
            // 조종 안 할 때: 배터리가 있으면 천천히 내려앉고, 없으면 추락 (충돌 시 폭발)
            Vec3 v = getDeltaMovement();
            double fallCap = battery() > 0 ? -0.12 : -1.5;
            v = new Vec3(v.x * 0.9, Math.max(fallCap, v.y - 0.05), v.z * 0.9);
            double speed = v.length();
            setDeltaMovement(v);
            move(MoverType.SELF, v);
            if ((horizontalCollision || verticalCollision) && speed > 0.38) {
                detonate();
                return;
            }
            if (onGround() && battery() <= 0 && ++idleTicks > 1200) discard();
        }
    }

    /** 서버: 조종자 클라이언트가 보낸 위치·방향 반영 */
    public void applyPilotUpdate(ServerPlayer from, double x, double y, double z, float yaw, float pitch) {
        if (!isPiloted() || !from.getUUID().equals(ownerId())) return;
        Vec3 to = new Vec3(x, y, z);
        if (to.distanceToSqr(position()) > 25 * 25) return;   // 말도 안 되는 순간이동 무시
        Vec3 from0 = position();
        setPos(x, y, z);
        setDeltaMovement(to.subtract(from0));
        setYRot(yaw);
        setXRot(pitch);
        if (from.distanceTo(this) > MAX_RANGE) {
            setPiloted(false);
            from.displayClientMessage(Component.translatable("message.tycooncraft.drone.lost"), true);
        }
    }

    public void detonate() {
        if (level().isClientSide || isRemoved()) return;
        Player owner = owner();
        DamageSource src = damageSources().explosion(this, owner);
        level().explode(this, src, null, getX(), getY() + 0.2, getZ(), POWER, false, Level.ExplosionInteraction.MOB);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        }
        discard();
    }

    // ── 상호작용 ──────────────────────────────────────────────────
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (isPiloted() || !player.getUUID().equals(ownerId()) || battery() <= 0) return InteractionResult.PASS;
        if (!level().isClientSide && player instanceof ServerPlayer sp) DroneItem.startPiloting(sp, this);
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (level().isClientSide || isRemoved()) return true;
        Entity attacker = source.getEntity();
        // 주인이 맨손으로 치면 회수, 그 외 피해는 폭발
        if (attacker instanceof Player p && p.getUUID().equals(ownerId()) && source.getDirectEntity() == p && !isPiloted()) {
            if (!p.getAbilities().instabuild && battery() > 0) spawnAtLocation(new ItemStack(ModItems.DRONE.get()));
            discard();
            return true;
        }
        detonate();
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public void playerTouch(Player player) {
    }

    public void armSound() {
        level().playSound(null, getX(), getY(), getZ(), ModSounds.DRONE_ARM.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    public float batteryFraction() {
        return Mth.clamp(battery() / (float) MAX_BATTERY, 0f, 1f);
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.DRONE.get());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) entityData.set(OWNER, Optional.of(tag.getUUID("Owner")));
        entityData.set(BATTERY, tag.getInt("Battery"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID id = ownerId();
        if (id != null) tag.putUUID("Owner", id);
        tag.putInt("Battery", battery());
    }
}
