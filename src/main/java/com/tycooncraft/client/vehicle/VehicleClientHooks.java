package com.tycooncraft.client.vehicle;

import com.tycooncraft.registry.ModSounds;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.DroneEntity;
import com.tycooncraft.vehicle.FueledVehicle;
import com.tycooncraft.vehicle.HelicopterEntity;
import com.tycooncraft.vehicle.LocomotiveEntity;
import com.tycooncraft.vehicle.VehicleEntity;
import com.tycooncraft.vehicle.VehicleHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.WeakHashMap;

/** 클라이언트 구현: 키 입력 읽기, 바퀴·로터 애니메이션 값, 엔진 소리, 배기·손상 연기, 하강풍 */
public final class VehicleClientHooks implements VehicleHooks.Client {
    private final Map<Entity, VehicleSound> sounds = new WeakHashMap<>();

    @Override
    public VehicleHooks.Input input() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.screen != null) return VehicleHooks.Input.NONE;
        Input in = p.input;
        boolean down = mc.options.keySprint.isDown() || VehicleKeyBinds.DESCEND.isDown();
        return new VehicleHooks.Input(in.forwardImpulse, in.leftImpulse, in.jumping, down, p.getYRot(), p.getXRot());
    }

    @Override
    public void sendAction(Entity vehicle, int action) {
        net.neoforged.neoforge.network.PacketDistributor.sendToServer(new com.tycooncraft.network.VehicleActionPayload(vehicle.getId(), (byte) action));
    }

    @Override
    public void startPilot(int entityId) {
        DroneClient.start(entityId);
    }

    @Override
    public void tick(Entity v) {
        Level level = v.level();
        RandomSource r = level.random;
        if (v instanceof CarEntity car) {
            car.wheelAngleO = car.wheelAngle;
            car.wheelAngle -= car.forwardSpeed / (15f / 32f);
            car.steerO = car.steer;
            float target = Mth.clamp(car.yawRate * 7f * (car.forwardSpeed >= 0 ? 1 : -1), -28f, 28f);
            car.steer += (target - car.steer) * 0.35f;
            if (car.engineRunning() && r.nextInt(3) == 0) {
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 p = v.position().add(car.toWorld(new Vec3(s * 24 / 32.0, 8 / 32.0, 73 / 32.0)));
                    Vec3 back = car.forward().scale(-0.04);
                    level.addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, back.x, 0.01, back.z);
                }
            }
            damageSmoke(car, car.toWorld(new Vec3(0, 1.1, -1.6)), r);
        } else if (v instanceof HelicopterEntity h) {
            h.rotorAngleO = h.rotorAngle;
            h.rotorAngle += h.rotor * 1.25f;
            h.tailAngleO = h.tailAngle;
            h.tailAngle += h.rotor * 2.1f;
            h.pitchO = h.pitch;
            h.rollO = h.roll;
            boolean air = !h.onGround();
            float tp = air ? Mth.clamp(-h.forwardSpeed * 14f, -14f, 8f) : 0f;
            float tr = air ? Mth.clamp(-h.yawRate * 3.5f * Math.min(1f, Math.abs(h.forwardSpeed) * 2f + 0.3f), -16f, 16f) : 0f;
            h.pitch += (tp - h.pitch) * 0.12f;
            h.roll += (tr - h.roll) * 0.12f;
            if (h.rotor > 0.3f && r.nextInt(4) == 0) {
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 p = v.position().add(h.toWorld(new Vec3(s * 10 / 32.0, 84 / 32.0, 40 / 32.0)));
                    level.addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.03, 0);
                }
            }
            if (h.rotor > 0.7f) downwash(h, r);
            damageSmoke(h, h.toWorld(new Vec3(0, 2.6, 0.2)), r);
        } else if (v instanceof com.tycooncraft.vehicle.TankEntity t) {
            t.wheelAngleO = t.wheelAngle;
            t.wheelAngle -= t.forwardSpeed / (12f / 32f) + t.yawRate * 0.02f;
            t.turretYawO = t.turretYaw;
            float target = t.getControllingPassenger() != null ? t.getControllingPassenger().getYHeadRot() : t.turretYaw;
            if (!t.turretInit) {
                t.turretYaw = t.turretYawO = t.getYRot();
                t.turretInit = true;
            }
            t.turretYaw += Mth.clamp(Mth.wrapDegrees(target - t.turretYaw), -18f, 18f);
            if (t.engineRunning() && r.nextInt(3) == 0) {
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 p = v.position().add(t.toWorld(new Vec3(s * 30 / 32.0, 42 / 32.0, 96 / 32.0)));
                    level.addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.04, 0);
                }
            }
            damageSmoke(t, t.toWorld(new Vec3(0, 1.6, 2.0)), r);
        } else if (v instanceof com.tycooncraft.vehicle.JetEntity j) {
            if (j.engineRunning()) {
                Vec3 back = Vec3.directionFromRotation(j.getXRot(), j.getYRot()).scale(-4.3);
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 side = Vec3.directionFromRotation(0, j.getYRot() + 90).scale(s * 7 / 32.0);
                    Vec3 p = v.position().add(0, 0.9, 0).add(back).add(side);
                    level.addParticle(j.forwardSpeed > 1.2 ? ParticleTypes.FLAME : ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0, 0);
                }
            }
            damageSmoke(j, new Vec3(0, 1.2, 0), r);
        } else if (v instanceof LocomotiveEntity loco) {
            loco.wheelAngleO = loco.wheelAngle;
            double sp = Math.sqrt(Math.pow(v.getX() - v.xo, 2) + Math.pow(v.getZ() - v.zo, 2));
            loco.wheelAngle += (float) (sp / (8.5 / 32.0));
            if (loco.engineRunning() && r.nextInt(sp > 0.05 ? 2 : 4) == 0) {
                double a = Math.toRadians(v.getYRot());
                Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
                for (int s = -1; s <= 1; s += 2) {
                    Vec3 p = v.position().add(dir.scale(s * 40 / 32.0)).add(0, 52 / 32.0, 0);
                    level.addParticle(sp > 0.1 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, p.x, p.y, p.z, 0, 0.06, 0);
                }
            }
            if (loco.healthFraction() < 0.5f && r.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.LARGE_SMOKE, v.getX(), v.getY() + 1.5, v.getZ(), 0, 0.05, 0);
            }
        }
        sound(v);
    }

    private static void damageSmoke(VehicleEntity v, Vec3 offset, RandomSource r) {
        float hp = v.healthFraction();
        if (hp >= 0.5f) return;
        if (r.nextFloat() < (0.5f - hp) * 1.6f) {
            Vec3 p = v.position().add(offset);
            v.level().addParticle(hp < 0.25f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, p.x + r.nextGaussian() * 0.2, p.y, p.z + r.nextGaussian() * 0.2, 0, 0.06, 0);
        }
    }

    /** 로터 바람: 땅에 가까우면 아래에서 먼지가 퍼진다 */
    private static void downwash(HelicopterEntity h, RandomSource r) {
        Level level = h.level();
        BlockPos base = h.blockPosition();
        for (int dy = 0; dy <= 7; dy++) {
            BlockPos p = base.below(dy);
            if (!level.getBlockState(p).getCollisionShape(level, p).isEmpty()) {
                double gy = p.getY() + 1.05;
                for (int i = 0; i < 3; i++) {
                    double ang = r.nextDouble() * Math.PI * 2;
                    double rad = 1.5 + r.nextDouble() * 2.5;
                    double vx = Math.cos(ang) * 0.25, vz = Math.sin(ang) * 0.25;
                    level.addParticle(ParticleTypes.POOF, h.getX() + Math.cos(ang) * rad, gy, h.getZ() + Math.sin(ang) * rad, vx, 0.01, vz);
                }
                return;
            }
        }
    }

    private void sound(Entity v) {
        if (!VehicleSound.engineOn(v)) return;
        VehicleSound s = sounds.get(v);
        Minecraft mc = Minecraft.getInstance();
        if (s == null || s.isStopped()) {
            SoundEvent ev = v instanceof CarEntity ? ModSounds.CAR_ENGINE.get()
                    : v instanceof HelicopterEntity ? ModSounds.HELI_ROTOR.get()
                    : v instanceof DroneEntity ? ModSounds.DRONE_BUZZ.get()
                    : v instanceof com.tycooncraft.vehicle.JetEntity ? ModSounds.JET_ENGINE.get() : ModSounds.TRAIN_ENGINE.get();
            if (!(v instanceof HelicopterEntity) && !(v instanceof DroneEntity) && !(v instanceof com.tycooncraft.vehicle.JetEntity)) {
                v.level().playLocalSound(v.getX(), v.getY(), v.getZ(), ModSounds.ENGINE_START.get(), SoundSource.NEUTRAL, 1.0f, 1.0f, false);
            }
            s = new VehicleSound(v, ev);
            sounds.put(v, s);
            mc.getSoundManager().play(s);
        }
    }

    /** 계기판에 쓰는 정보 */
    public static double speedKmh(Entity v) {
        double dx = v.getX() - v.xo, dz = v.getZ() - v.zo, dy = v instanceof HelicopterEntity ? v.getY() - v.yo : 0;
        return Math.sqrt(dx * dx + dz * dz + dy * dy) * 20 * 3.6;
    }

    public static boolean isVehicle(Entity v) {
        return v instanceof FueledVehicle;
    }
}
