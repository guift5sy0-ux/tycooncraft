package com.tycooncraft.client.vehicle;

import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.DroneEntity;
import com.tycooncraft.vehicle.HelicopterEntity;
import com.tycooncraft.vehicle.LocomotiveEntity;
import com.tycooncraft.vehicle.VehicleEntity;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

/** 엔진·로터 반복음: 탈것을 따라다니며 속도에 따라 음높이·크기가 변한다. 시동이 꺼지면 서서히 사라짐 */
public class VehicleSound extends AbstractTickableSoundInstance {
    private final Entity vehicle;
    private float level;

    public VehicleSound(Entity vehicle, SoundEvent event) {
        super(event, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.vehicle = vehicle;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.01f;
        this.pitch = 0.7f;
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.z = vehicle.getZ();
    }

    public static boolean engineOn(Entity v) {
        if (v instanceof VehicleEntity ve) return ve.engineRunning() || (v instanceof HelicopterEntity h && h.rotor > 0.02f);
        if (v instanceof LocomotiveEntity loco) return loco.engineRunning();
        if (v instanceof DroneEntity d) return d.battery() > 0 && (d.isPiloted() || !d.onGround());
        return false;
    }

    @Override
    public void tick() {
        if (vehicle.isRemoved()) {
            stop();
            return;
        }
        x = vehicle.getX();
        y = vehicle.getY() + 0.8;
        z = vehicle.getZ();
        boolean on = engineOn(vehicle);
        level = Mth.clamp(level + (on ? 0.08f : -0.05f), 0f, 1f);
        if (level <= 0f && !on) {
            stop();
            return;
        }
        double speed = Math.sqrt(Math.pow(vehicle.getX() - vehicle.xo, 2) + Math.pow(vehicle.getZ() - vehicle.zo, 2));
        if (vehicle instanceof CarEntity) {
            pitch = (float) Mth.clamp(0.75 + speed * 1.35, 0.6, 2.0);
            volume = level * (float) (0.55 + Math.min(0.45, speed));
        } else if (vehicle instanceof DroneEntity) {
            double sp3 = Math.sqrt(Math.pow(vehicle.getX() - vehicle.xo, 2) + Math.pow(vehicle.getY() - vehicle.yo, 2) + Math.pow(vehicle.getZ() - vehicle.zo, 2));
            pitch = (float) Mth.clamp(0.9 + sp3 * 0.9, 0.8, 1.9);
            volume = level * 0.7f;
        } else if (vehicle instanceof HelicopterEntity h) {
            pitch = 0.45f + 0.6f * h.rotor;
            volume = level * (0.25f + 1.6f * h.rotor);
        } else {
            pitch = (float) Mth.clamp(0.75 + speed * 1.1, 0.6, 1.8);
            volume = level * (float) (0.8 + Math.min(0.6, speed));
        }
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }
}
