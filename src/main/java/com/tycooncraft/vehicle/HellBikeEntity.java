package com.tycooncraft.vehicle;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * v0.36: 헬파이어 바이크 — 지옥불 초퍼. 2인승, 자동차보다 빠르고 날렵하게 꺾임.
 * 불에 타지 않고 용암 위를 달림, 들이받은 적에게 불을 붙임. 바퀴와 배기구에서 불꽃.
 */
public class HellBikeEntity extends CarEntity {
    private static final Vec3[] SEATS = {new Vec3(0, 0.42, 0.08), new Vec3(0, 0.5, 0.58)};

    public HellBikeEntity(EntityType<? extends HellBikeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected int seats() {
        return 2;
    }

    @Override
    protected Vec3 seatLocal(int index) {
        return SEATS[Math.min(index, 1)];
    }

    @Override
    public Item dropItem() {
        return ModItems.HELL_BIKE.get();
    }

    @Override
    public int maxFuel() {
        return 24000;
    }

    @Override
    public float maxHealth() {
        return 55f;
    }

    @Override
    public String hudName() {
        return "hud.tycooncraft.name.hell_bike";
    }

    @Override
    protected double maxSpeed() {
        return 1.25;
    }

    @Override
    protected double accel() {
        return 0.034;
    }

    @Override
    protected float turnBase() {
        return 8.5f;
    }

    /** 용암 위를 달림 */
    @Override
    protected double liftY(double vy) {
        return isInLava() ? Math.max(vy, 0.12) : vy;
    }

    @Override
    protected void onRam(Entity e) {
        e.igniteForSeconds(6);
    }

    /** 타고 있으면 지옥불에 타지 않음 */
    @Override
    protected void serverTick(Vec3 moved) {
        super.serverTick(moved);
        if (tickCount % 20 == 0) for (Entity p : getPassengers())
            if (p instanceof net.minecraft.world.entity.LivingEntity le) {
                le.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.FIRE_RESISTANCE, 60, 0, true, false, true));
                le.clearFire();
            }
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
