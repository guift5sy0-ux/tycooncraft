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
    /** 좌석: 운전자 엉덩이가 시트(높이 16.6/16) 위, 동승자는 뒷 패드 위 */
    private static final Vec3[] SEATS = {new Vec3(0, 0.8875, 0.406), new Vec3(0, 0.96, 0.79)};

    // ── 클라이언트 표시 상태 ──
    /** 코너 기울기(도, + = 왼쪽), 윌리(앞바퀴 들림, 도) */
    public float lean, leanO, wheelie, wheelieO;
    /** 시동이 걸린 틱 (불길이 확 치솟는 연출) */
    public int ignitedAt = -9999;
    public boolean wasOn;
    /** 땅에 남는 불자국: {x, y, z, 생긴 게임 시간} */
    public final java.util.ArrayDeque<double[]> trail = new java.util.ArrayDeque<>();
    /** 자동 테스트 전용: 플레이어가 아닌 탑승자도 시동 켠 것으로 보고, 기울기·윌리를 고정 */
    public static boolean demo;
    public static float demoLean = Float.NaN, demoWheelie = Float.NaN;

    private int revCd;
    public int revAt = -9999;
    public static final byte EV_REV = 61;

    /** 스페이스: 스로틀 블립 — 배기구에서 불기둥이 뿜어져 뒤쪽 적에게 불을 붙임 */
    public void rev(net.minecraft.world.entity.player.Player p) {
        if (getControllingPassenger() != p || getFuel() <= 0 || tickCount - revCd < 24) return;
        revCd = tickCount;
        level().playSound(null, getX(), getY(), getZ(), com.tycooncraft.registry.ModSounds.HELL_BIKE_REV.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 3.0f, 0.95f + random.nextFloat() * 0.1f);
        level().broadcastEntityEvent(this, EV_REV);
        Vec3 tail = position().add(toWorld(new Vec3(0.3, 0.6, 1.9)));
        for (Entity e : level().getEntities(this, new net.minecraft.world.phys.AABB(tail, tail).inflate(1.6), e -> e instanceof net.minecraft.world.entity.LivingEntity && !hasPassenger(e)))
            e.igniteForSeconds(4);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EV_REV) revAt = tickCount;
        else super.handleEntityEvent(id);
    }

    /** 플레이어가 아닌 탑승자(몹)도 플레이어와 같은 높이에 앉게 보정 */
    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, net.minecraft.world.entity.EntityDimensions dim, float pt) {
        Vec3 base = super.getPassengerAttachmentPoint(passenger, dim, pt);
        return base.add(0, passenger.getVehicleAttachmentPoint(this).y - 0.6, 0);
    }

    @Override
    public boolean engineRunning() {
        return super.engineRunning() || (demo && getFirstPassenger() != null && getFuel() > 0);
    }

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
