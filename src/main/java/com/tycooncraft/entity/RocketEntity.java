package com.tycooncraft.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** RPG 로켓 / 전차 포탄 / 전투기 미사일: 맞는 순간 폭발 */
public class RocketEntity extends Projectile {
    public static final int ROCKET = 0, SHELL = 1, MISSILE = 2;
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private float power = 3.5f;
    private int life;
    /** 클라이언트 렌더용: 마지막으로 알던 비행 방향 */
    public net.minecraft.world.phys.Vec3 lastDir = new net.minecraft.world.phys.Vec3(0, 0, -1);

    public RocketEntity(EntityType<? extends RocketEntity> type, Level level) {
        super(type, level);
    }

    public static RocketEntity create(EntityType<? extends RocketEntity> type, Level level, Entity owner, Vec3 pos, Vec3 velocity, int kind, float power) {
        RocketEntity r = new RocketEntity(type, level);
        r.setOwner(owner);
        r.setPos(pos.x, pos.y, pos.z);
        r.setDeltaMovement(velocity);
        r.entityData.set(KIND, kind);
        r.power = power;
        ProjectileUtil.rotateTowardsMovement(r, 1.0f);
        r.yRotO = r.getYRot();
        r.xRotO = r.getXRot();
        return r;
    }

    public int kind() {
        return entityData.get(KIND);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, ROCKET);
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 v = getDeltaMovement();
        HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        var shipHit = level().isClientSide ? null : com.tycooncraft.ship.ShipCollisions.clip(level(), position(), position().add(v));
        if (shipHit != null && (hit.getType() == HitResult.Type.MISS || shipHit.point().distanceToSqr(position()) < hit.getLocation().distanceToSqr(position()))) {
            setPos(shipHit.point().x, shipHit.point().y, shipHit.point().z);
            explode();
            return;
        }
        if (hit.getType() != HitResult.Type.MISS) {
            setPos(hit.getLocation().x, hit.getLocation().y, hit.getLocation().z);
            explode();
            return;
        }
        setPos(getX() + v.x, getY() + v.y, getZ() + v.z);
        double grav = kind() == SHELL ? 0.002 : kind() == MISSILE ? 0.0 : 0.006;
        setDeltaMovement(v.scale(kind() == ROCKET ? 1.015 : kind() == SHELL ? 1.0 : 0.998).add(0, -grav, 0));
        if (getDeltaMovement().length() > 4.0) setDeltaMovement(getDeltaMovement().normalize().scale(4.0));
        ProjectileUtil.rotateTowardsMovement(this, 0.5f);
        if (level().isClientSide) {
            Vec3 back = position().subtract(getDeltaMovement().normalize().scale(0.6));
            if (kind() != SHELL) {
                level().addParticle(ParticleTypes.FLAME, back.x, back.y, back.z, 0, 0, 0);
                level().addParticle(ParticleTypes.LARGE_SMOKE, back.x, back.y, back.z, 0, 0.02, 0);
            } else {
                level().addParticle(ParticleTypes.SMOKE, back.x, back.y, back.z, 0, 0, 0);
            }
        } else if (++life > 200 || isInWater()) {
            explode();
        }
    }

    /** 요격당함: 탄두가 공중에서 약하게 터진다 */
    public void intercept() {
        if (level().isClientSide || isRemoved()) return;
        power *= 0.5f;
        explode();
    }

    private void explode() {
        if (level().isClientSide || isRemoved()) return;
        level().explode(this, damageSources().explosion(this, getOwner()), null, getX(), getY(), getZ(), power, false, Level.ExplosionInteraction.MOB);
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kind", kind());
        tag.putFloat("Power", power);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(KIND, tag.getInt("Kind"));
        power = tag.getFloat("Power");
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 256 * 256;
    }
}
