package com.tycooncraft.vehicle;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 자동차 "TC 타이쿤 GT": 4인승. W/S 가속·브레이크(후진), A/D 조향(움직일 때만), 스페이스 경적.
 * 1칸 턱은 그대로 올라감. 빠르게 들이받으면 몹에게 피해.
 */
public class CarEntity extends VehicleEntity {
    public static final double MAX_SPEED = 0.95, MAX_REVERSE = 0.3, ACCEL = 0.022;
    private static final Vec3[] SEATS = {
            new Vec3(-14 / 32.0, 14 / 32.0, 8 / 32.0), new Vec3(14 / 32.0, 14 / 32.0, 8 / 32.0),
            new Vec3(-14 / 32.0, 14 / 32.0, 34 / 32.0), new Vec3(14 / 32.0, 14 / 32.0, 34 / 32.0)};

    /** 클라이언트 표시: 바퀴 회전각, 조향각 */
    public float wheelAngle, wheelAngleO, steer, steerO;

    public CarEntity(EntityType<? extends CarEntity> type, Level level) {
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
        return ModItems.CAR.get();
    }

    @Override
    public int maxFuel() {
        return 18000;
    }

    @Override
    public float maxHealth() {
        return 40f;
    }

    @Override
    public String hudName() {
        return "hud.tycooncraft.name.car";
    }

    @Override
    protected void drive(VehicleHooks.Input in, boolean hasFuel) {
        Vec3 dm = getDeltaMovement();
        Vec3 fwd = forward();
        double speed = dm.x * fwd.x + dm.z * fwd.z;
        float throttle = hasFuel ? in.forward() : 0;
        if (throttle > 0) {
            speed = speed < 0 ? Math.min(0, speed + 0.06) : speed + accel() * (1 - speed / maxSpeed());
        } else if (throttle < 0) {
            speed = speed > 0 ? Math.max(0, speed - 0.06) : speed - ACCEL * 0.6 * (1 + speed / MAX_REVERSE);
        } else {
            speed *= onGround() ? 0.975 : 0.995;
        }
        if (isInWater()) speed *= 0.85;
        speed = Math.min(speed, maxSpeed() * 1.05);
        if (Math.abs(speed) < 0.003) speed = 0;

        float grip = (float) Math.min(1.0, Math.abs(speed) / 0.05);
        float turnRate = turnBase() - 2.6f * (float) Math.min(1.0, Math.abs(speed) / maxSpeed());
        float turn = onGround() ? -in.strafe() * turnRate * grip * (speed >= 0 ? 1 : -1) : 0;
        deltaRotation = turn;
        setYRot(getYRot() + turn);

        Vec3 nf = forward();
        double vy = (dm.y - 0.08) * 0.98;
        if (isInWater()) vy = Math.max(vy, -0.06);
        vy = liftY(vy);
        // 약간의 미끄러짐: 옆 방향 속도를 조금 남긴다
        double along = dm.x * fwd.x + dm.z * fwd.z;
        double latX = (dm.x - fwd.x * along) * 0.25, latZ = (dm.z - fwd.z * along) * 0.25;
        setDeltaMovement(nf.x * speed + latX, vy, nf.z * speed + latZ);
    }

    protected void onRam(Entity e) {
    }

    protected double maxSpeed() {
        return MAX_SPEED;
    }

    protected double accel() {
        return ACCEL;
    }

    protected float turnBase() {
        return 6.8f;
    }

    protected double liftY(double vy) {
        return vy;
    }

    /** 빠르게 달리다 부딪힌 생명체에게 피해 */
    @Override
    protected void serverTick(Vec3 moved) {
        double sp = moved.horizontalDistance();
        LivingEntity driver = getControllingPassenger();
        if (driver == null || sp < 0.35) return;
        Vec3 dir = new Vec3(moved.x, 0, moved.z).normalize();
        for (Entity e : level().getEntities(this, getBoundingBox().inflate(0.3), e -> e instanceof LivingEntity && e.isAlive() && !hasPassenger(e))) {
            if (e.hurt(damageSources().mobAttack(driver), (float) (sp * 14))) {
                onRam(e);
                e.push(dir.x * sp * 1.6, 0.35, dir.z * sp * 1.6);
            }
        }
    }
}
