package com.tycooncraft.vehicle;

import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 전투기 "TC-F1": W/S 출력(스로틀) 올림·내림, 기수는 마우스(보는 방향)를 따라감.
 * 빨라야(약 65km/h 이상) 떠오르고, 느려지면 가라앉음. 좌클릭(누르고 있기) = 기관포(중탄약), 우클릭 = 미사일(로켓).
 * 세게 부딪히면 폭발.
 */
public class JetEntity extends VehicleEntity {
    // 눈이 캐노피 위로 오도록 (1인칭에서 기수·동체에 시야가 가리지 않게)
    private static final Vec3 SEAT = new Vec3(0, 49 / 32.0, -64 / 32.0);
    /** 최고 속도 130 km/h (= 130 / 72 블록/틱) */
    public static final double MAX_SPEED = 130.0 / 72.0, LIFT_SPEED = 0.85;
    private float yawVel, pitchVel;
    /** 조종자 클라이언트의 스로틀 (0~1) */
    public float throttle;
    public float roll, rollO;
    private int gunCooldown, missileCooldown;

    public JetEntity(EntityType<? extends JetEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected int seats() {
        return 1;
    }

    /** 조종석: 눈높이가 캐노피 안에 오도록 (몸은 렌더링하지 않음) */
    @Override
    protected Vec3 seatLocal(int index) {
        return new Vec3(0, 9 / 32.0, -60 / 32.0);
    }

    public boolean airbrake;

    /**
     * 조종석 위치를 기체의 피치(기수 상하)까지 반영해 계산 — 렌더러는 기체를 (0, 1, 0) 을 축으로 피치 회전시키므로
     * 좌석(=시점)도 같이 돌려야 위아래로 기울 때 시점이 동체 밖/안으로 튀지 않는다.
     */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dims, float pt) {
        Vec3 s = seatLocal(0);
        double th = -getXRot() * Mth.DEG_TO_RAD;
        double y = s.y - 1.0, z = s.z;
        double y2 = y * Math.cos(th) - z * Math.sin(th), z2 = y * Math.sin(th) + z * Math.cos(th);
        return toWorld(new Vec3(s.x, y2 + 1.0, z2));
    }

    /** 배럴 롤 (클라이언트 표시): 남은 틱 */
    public int rollTicks;
    public static final int ROLL_TIME = 22;

    public void startRoll() {
        if (!onGround() && !level().isClientSide) level().broadcastEntityEvent(this, (byte) 61);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 61) rollTicks = ROLL_TIME;
        else super.handleEntityEvent(id);
    }

    /** 0~1 진행도 → 롤 각도 */
    public float barrelRoll(float pt) {
        if (rollTicks <= 0) return 0;
        float prog = 1f - (rollTicks - pt) / ROLL_TIME;
        return Mth.clamp(prog, 0f, 1f) * 360f;
    }

    @Override
    public Item dropItem() {
        return ModItems.FIGHTER_JET.get();
    }

    @Override
    public int maxFuel() {
        return 18000;
    }

    @Override
    public float maxHealth() {
        return 80f;
    }

    @Override
    public String hudName() {
        return "hud.tycooncraft.name.jet";
    }

    @Override
    public boolean showAltitude() {
        return true;
    }

    @Override
    protected int fuelPerTick() {
        return 2;
    }

    @Override
    protected float crashPower() {
        return 7f;
    }

    @Override
    public float maxUpStep() {
        return 0.6f;
    }

    @Override
    protected boolean burning(Vec3 moved) {
        return true;
    }

    @Override
    protected void drive(VehicleHooks.Input in, boolean hasFuel) {
        boolean manned = getControllingPassenger() != null;
        if (hasFuel && manned) throttle = Mth.clamp(throttle + in.forward() * 0.012f, 0f, 1f);
        else throttle = Math.max(0, throttle - 0.01f);
        boolean brake = manned && in.down();
        if (brake) throttle = Math.max(0, throttle - 0.03f);
        Vec3 dm = getDeltaMovement();
        double speed = dm.length();
        double target = throttle * MAX_SPEED;
        speed += (target - speed) * (target > speed ? 0.02 : 0.012);
        // 에어브레이크 (Ctrl / Z): 강하게 감속, 지상에선 바퀴 브레이크
        if (brake) speed *= onGround() ? 0.88 : 0.95;
        airbrake = brake;
        // 기수 방향: 조종자가 보는 쪽으로 천천히
        float yaw = getYRot(), pitch = getXRot();
        if (manned) {
            float dy = Mth.wrapDegrees(in.lookYaw() - yaw);
            float dp = in.lookPitch() - pitch;
            float agility = (float) Mth.clamp(speed / LIFT_SPEED, 0.25, 1.0);
            // 회전 속도 자체를 부드럽게 보간 → 급격한 꺾임 없이 미끄러지듯
            float agility2 = Math.max(agility, 0.45f);
            yawVel += (Mth.clamp(dy * 0.16f, -7.5f, 7.5f) * agility2 - yawVel) * 0.32f;
            pitchVel += (Mth.clamp(dp * 0.16f, -6.0f, 6.0f) * agility2 - pitchVel) * 0.32f;
        } else {
            yawVel *= 0.8f;
            pitchVel *= 0.8f;
        }
        yaw += yawVel;
        pitch += pitchVel;
        boolean flying = speed > LIFT_SPEED * 0.8;
        if (onGround() && !flying) pitch = Math.min(0, pitch) * 0.5f + Math.max(0, pitch) * 0f;
        pitch = Mth.clamp(pitch, -70f, 70f);
        deltaRotation = 0;
        setYRot(yaw);
        setXRot(pitch);
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        double lift = Mth.clamp(speed / LIFT_SPEED, 0, 1);
        // 속도 방향은 기수 방향을 살짝 늦게 따라감 (관성)
        Vec3 cur = dm.lengthSqr() > 1e-4 ? dm.normalize() : dir;
        Vec3 v = cur.scale(0.25).add(dir.scale(0.75)).normalize().scale(speed);
        double sink = (1 - lift) * 0.5;
        if (onGround() && !flying) v = new Vec3(v.x, Math.min(v.y, 0), v.z);
        setDeltaMovement(v.x, v.y - sink * (onGround() ? 0 : 1) - (onGround() ? 0.04 : 0), v.z);
        if (onGround() && throttle < 0.05) setDeltaMovement(getDeltaMovement().multiply(0.9, 1, 0.9));
    }

    @Override
    protected void afterMove(Vec3 v) {
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        if ((horizontalCollision && h > 0.7) || (verticalCollision && v.y < -0.7)) crash();
    }

    @Override
    public void tick() {
        super.tick();
        if (gunCooldown > 0) gunCooldown--;
        if (missileCooldown > 0) missileCooldown--;
        if (level().isClientSide) {
            if (rollTicks > 0) rollTicks--;
            rollO = roll;
            roll += (Mth.clamp(-yawRate * 9f, -60f, 60f) - roll) * 0.15f;
        }
    }

    @Override
    public void handleAction(ServerPlayer player, int action) {
        super.handleAction(player, action);
        if (getControllingPassenger() != player) return;
        Vec3 dir = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 nose = position().add(0, 1.0, 0).add(dir.scale(4.5));
        if (action == ACTION_FIRE && gunCooldown == 0) {
            if (!player.getAbilities().instabuild && !take(player, ModItems.HEAVY_AMMO.get())) {
                player.displayClientMessage(Component.translatable("message.tycooncraft.jet.no_ammo"), true);
                return;
            }
            gunCooldown = 2;
            for (int k = -1; k <= 1; k += 2) {
                Vec3 side = Vec3.directionFromRotation(0, getYRot() + 90).scale(k * 0.6);
                Vec3 spread = dir.add(random.nextGaussian() * 0.012, random.nextGaussian() * 0.012, random.nextGaussian() * 0.012).normalize();
                Ballistics.nextKind = 3;
                Ballistics.nextSpeed = 34f;
                Ballistics.shoot((ServerLevel) level(), player, nose.add(side), spread, 120, 8f * GunItem.DAMAGE_SCALE, 0, 1.5f, false, true,
                        level().damageSources().playerAttack(player), e -> e != this && !hasPassenger(e));
            }
            level().playSound(null, nose.x, nose.y, nose.z, ModSounds.RIFLE_FIRE.get(), SoundSource.PLAYERS, 3.0f, 1.35f);
        } else if (action == ACTION_FIRE2 && missileCooldown == 0) {
            if (!player.getAbilities().instabuild && !take(player, ModItems.ROCKET.get())) {
                player.displayClientMessage(Component.translatable("message.tycooncraft.jet.no_missile"), true);
                return;
            }
            missileCooldown = 30;
            Vec3 v = getDeltaMovement().add(dir.scale(2.2));
            level().addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level(), player, nose.add(0, -0.8, 0), v, RocketEntity.MISSILE, 4.0f));
            level().playSound(null, nose.x, nose.y, nose.z, ModSounds.RPG_FIRE.get(), SoundSource.PLAYERS, 3.0f, 1.2f);
        }
    }

    private static boolean take(ServerPlayer p, Item item) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 g = airDismount(passenger);
        return g != null ? g : super.getDismountLocationForPassenger(passenger);
    }
}
