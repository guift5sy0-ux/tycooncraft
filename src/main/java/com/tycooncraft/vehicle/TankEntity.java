package com.tycooncraft.vehicle;

import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 전차 "TC-201": W/S 전진·후진, A/D 차체 회전(제자리 회전 가능), 포탑은 마우스(보는 방향)를 따라감.
 * 좌클릭 = 주포 (전차 포탄 소모, 2.5초 재장전). 총알엔 거의 안 다치고 폭발에 약함.
 */
public class TankEntity extends VehicleEntity {
    /** 몸은 렌더링하지 않고, 시점만 포탑 위(지휘관 해치 높이)에 둔다 */
    private static final Vec3[] SEATS = {new Vec3(0, 44 / 32.0, 8 / 32.0), new Vec3(0, 40 / 32.0, 40 / 32.0)};
    public static final Vec3 TURRET = new Vec3(0, 46 / 32.0, 8 / 32.0);
    private int reload;
    /** 클라이언트: 포탑 각도, 포신 반동, 바퀴 */
    public float turretYaw, turretYawO, recoil, recoilO, wheelAngle, wheelAngleO;
    public boolean turretInit;

    public TankEntity(EntityType<? extends TankEntity> type, Level level) {
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
        return ModItems.TANK.get();
    }

    @Override
    public int maxFuel() {
        return 24000;
    }

    @Override
    public float maxHealth() {
        return 250f;
    }

    @Override
    public String hudName() {
        return "hud.tycooncraft.name.tank";
    }

    @Override
    protected float crashPower() {
        return 4f;
    }

    @Override
    protected void drive(VehicleHooks.Input in, boolean hasFuel) {
        Vec3 dm = getDeltaMovement();
        Vec3 fwd = forward();
        double speed = dm.x * fwd.x + dm.z * fwd.z;
        float th = hasFuel ? in.forward() : 0;
        if (th > 0) speed += (0.42 - speed) * 0.04;
        else if (th < 0) speed += (-0.2 - speed) * 0.05;
        else speed *= 0.85;
        if (Math.abs(speed) < 0.004) speed = 0;
        // 포탄 반동·폭발 넉백으로 생긴 초과 속도가 4%씩만 줄어 시속 90km 까지 나오던 버그: 최고 속도(전진 0.42, 후진 0.2)로 즉시 제한
        speed = Mth.clamp(speed, -0.22, 0.44);
        float turn = hasFuel ? -in.strafe() * 3.6f : 0;
        deltaRotation = 0;
        setYRot(getYRot() + turn);
        Vec3 nf = forward();
        double vy = (dm.y - 0.08) * 0.98;
        setDeltaMovement(nf.x * speed, vy, nf.z * speed);
    }

    /** 총알·근접엔 매우 튼튼, 폭발엔 그대로 */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // v0.19.6: RT-20(대물 저격총) 탄은 전차의 총알 피해 감소를 무시
        if (!source.is(DamageTypeTags.IS_EXPLOSION) && !com.tycooncraft.gun.Ballistics.nextAntiMaterial && !(source.getDirectEntity() instanceof Player p && !(p.getMainHandItem().getItem() instanceof com.tycooncraft.gun.GunItem))) {
            amount *= 0.12f;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();
        if (reload > 0) reload--;
        if (level().isClientSide) {
            recoilO = recoil;
            recoil *= 0.8f;
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 60) recoil = 1f;
        else super.handleEntityEvent(id);
    }

    /** 포신 끝 위치와 방향 (조종자가 보는 방향) */
    public Vec3 aimDir(LivingEntity gunner) {
        float pitch = Mth.clamp(gunner.getXRot(), -20f, 6f);
        return Vec3.directionFromRotation(pitch, gunner.getYHeadRot());
    }

    @Override
    public void handleAction(ServerPlayer player, int action) {
        super.handleAction(player, action);
        if (action != ACTION_FIRE || getControllingPassenger() != player) return;
        if (reload > 0) return;
        if (!player.getAbilities().instabuild) {
            int slot = -1;
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                if (player.getInventory().getItem(i).is(ModItems.TANK_SHELL.get())) {
                    slot = i;
                    break;
                }
            }
            if (slot < 0) {
                {} /* v0.18.9: 전투 안내 문구 제거 */
                return;
            }
            player.getInventory().getItem(slot).shrink(1);
        }
        reload = 50;
        // 화면 가운데(조준점)가 가리키는 곳을 찾아, 포신 끝에서 그곳을 향해 쏜다
        Vec3 look = player.getViewVector(1f);
        Vec3 eye = player.getEyePosition();
        var hit = level().clip(new net.minecraft.world.level.ClipContext(eye, eye.add(look.scale(250)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, player));
        Vec3 target = hit.getLocation();
        Vec3 pivot = position().add(toWorld(new Vec3(0, 0, TURRET.z))).add(0, TURRET.y + 12 / 32.0, 0);
        Vec3 dir = aimDir(player);
        Vec3 muzzle = pivot.add(dir.scale(5.4));
        Vec3 shot = target.subtract(muzzle);
        if (shot.lengthSqr() > 4 && shot.normalize().dot(dir) > 0.8) dir = shot.normalize();
        level().addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level(), player, muzzle, dir.scale(4.0), RocketEntity.SHELL, 5.0f));   // v0.19.4 버프 (4 → 5)
        level().playSound(null, muzzle.x, muzzle.y, muzzle.z, ModSounds.TANK_FIRE.get(), SoundSource.NEUTRAL, 6.0f, 0.95f + random.nextFloat() * 0.1f);
        if (level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.EXPLOSION, muzzle.x, muzzle.y, muzzle.z, 2, 0.2, 0.2, 0.2, 0);
            sl.sendParticles(com.tycooncraft.registry.ModParticles.SMOKE_LARGE.get(), muzzle.x, muzzle.y, muzzle.z, 20, 0.5, 0.3, 0.5, 0.05);
        }
        level().broadcastEntityEvent(this, (byte) 60);
        // 반동: 차체가 살짝 뒤로
        setDeltaMovement(getDeltaMovement().subtract(dir.x * 0.08, 0, dir.z * 0.08));
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 g = groundDismount(passenger);
        return g != null ? g : super.getDismountLocationForPassenger(passenger);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.TANK.get());
    }
}
