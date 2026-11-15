package com.tycooncraft.vehicle;

import com.tycooncraft.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 헬리콥터 "TC AIR": 4인승. 시동 후 로터가 다 돌아야 이륙.
 * W/S 전진·후진, A/D 방향 전환, 스페이스 상승, Ctrl 하강, 손 떼면 제자리 비행.
 * 연료가 떨어지면 로터가 천천히 멈추며 자동 회전으로 천천히 내려온다.
 */
public class HelicopterEntity extends VehicleEntity {
    private static final Vec3[] SEATS = {
            new Vec3(-12 / 32.0, 24 / 32.0, -26 / 32.0), new Vec3(12 / 32.0, 24 / 32.0, -26 / 32.0),
            new Vec3(-12 / 32.0, 24 / 32.0, 6 / 32.0), new Vec3(12 / 32.0, 24 / 32.0, 6 / 32.0)};

    /** 로터 회전 (0~1), 클라이언트 표시용 각도·기울기 */
    public float rotor;
    public float rotorAngle, rotorAngleO, tailAngle, tailAngleO;
    public float pitch, pitchO, roll, rollO;

    public HelicopterEntity(EntityType<? extends HelicopterEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected int seats() {
        return 4;
    }

    @Override
    protected Vec3 seatLocal(int index) {
        return SEATS[Math.min(index, SEATS.length - 1)];
    }

    @Override
    public Item dropItem() {
        return ModItems.HELICOPTER.get();
    }

    @Override
    public int maxFuel() {
        return 18000;
    }

    @Override
    public float maxHealth() {
        return 60f;
    }

    @Override
    public String hudName() {
        return "hud.tycooncraft.name.helicopter";
    }

    @Override
    public boolean showAltitude() {
        return true;
    }

    @Override
    protected boolean burning(Vec3 moved) {
        return rotor > 0.05f;
    }

    @Override
    protected int fuelPerTick() {
        return 2;
    }

    @Override
    public float maxUpStep() {
        return 0.6f;
    }

    @Override
    protected void preDrive() {
        rotor = Mth.clamp(rotor + (engineRunning() ? 0.011f : -0.006f), 0f, 1f);
    }

    /** 벽에 빠르게 부딪히거나 바닥에 세게 떨어지면 폭발 (충전 크리퍼급) */
    @Override
    protected void afterMove(Vec3 v) {
        double h = Math.sqrt(v.x * v.x + v.z * v.z);
        if ((horizontalCollision && h > 0.5) || (verticalCollision && v.y < -0.6)) crash();
    }

    @Override
    public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        Vec3 g = groundDismount(passenger);
        return g != null ? g : super.getDismountLocationForPassenger(passenger);
    }

    public boolean hasLift() {
        return rotor > 0.85f;
    }

    @Override
    protected void drive(VehicleHooks.Input in, boolean hasFuel) {
        Vec3 dm = getDeltaMovement();
        double vx = dm.x, vy = dm.y, vz = dm.z;
        if (hasLift() && hasFuel) {
            // 기수는 조종자가 보는 방향으로 (마우스로 방향 전환), A/D = 옆으로 이동
            boolean manned = getControllingPassenger() != null;
            float dy = manned ? Mth.wrapDegrees(in.lookYaw() - getYRot()) : 0;
            float yawIn = Mth.clamp(dy * 0.16f, -5.5f, 5.5f);
            deltaRotation = 0;
            setYRot(getYRot() + yawIn);
            Vec3 f = forward();
            Vec3 right = toWorld(new Vec3(1, 0, 0));
            double target = in.forward() > 0 ? 1.2 : in.forward() < 0 ? -0.5 : 0;
            double cur = vx * f.x + vz * f.z;
            double ns = cur + (target - cur) * 0.04;
            double lat = vx * right.x + vz * right.z;
            double nl = lat + (-in.strafe() * 0.5 - lat) * 0.06;
            vx = f.x * ns + right.x * nl;
            vz = f.z * ns + right.z * nl;
            double ty = in.up() ? 0.36 : in.down() ? -0.42 : 0.0;
            vy += (ty - vy) * 0.14;
        } else {
            deltaRotation = 0;
            vy = (vy - 0.04 * (1 - rotor * 0.75)) * 0.98;
            vy = Math.max(vy, -0.9 + rotor * 0.7);
            double drag = onGround() ? 0.6 : 0.985;
            vx *= drag;
            vz *= drag;
            if (isInWater()) {
                vx *= 0.8;
                vz *= 0.8;
                vy = Math.max(vy, -0.05);
            }
        }
        setDeltaMovement(vx, vy, vz);
    }
}
