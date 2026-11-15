package com.tycooncraft.entity;

import com.tycooncraft.registry.ModEffects;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** 수류탄 (파편 / 섬광 / 연막): 튕기며 굴러가다 신관 시간 뒤 작동 */
public class GrenadeEntity extends ThrowableItemProjectile {
    public static final int FRAG = 0, FLASH = 1, SMOKE = 2;
    private static final EntityDataAccessor<Integer> SMOKE_T = SynchedEntityData.defineId(GrenadeEntity.class, EntityDataSerializers.INT);
    public static final int SMOKE_TIME = 400;
    private int fuse = 50;
    public float spin, spinO;
    /** 테스트용 카운터 */
    public static int detonations;

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, Level level) {
        super(type, level);
    }

    public GrenadeEntity(EntityType<? extends GrenadeEntity> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(SMOKE_T, 0);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.FRAG_GRENADE.get();
    }

    public int kind() {
        if (getItem().is(ModItems.FLASH_GRENADE.get())) return FLASH;
        if (getItem().is(ModItems.SMOKE_GRENADE.get())) return SMOKE;
        return FRAG;
    }

    public int smokeTicks() {
        return entityData.get(SMOKE_T);
    }

    @Override
    public void tick() {
        baseTick();
        spinO = spin;
        Vec3 v = getDeltaMovement();
        if (smokeTicks() == 0 || !onGround()) {
            v = v.add(0, -0.05, 0);
            Vec3 before = position();
            move(MoverType.SELF, v);
            Vec3 moved = position().subtract(before);
            double vx = v.x, vy = v.y, vz = v.z;
            if (Math.abs(moved.x - v.x) > 1e-4) vx = -v.x * 0.35;
            if (Math.abs(moved.z - v.z) > 1e-4) vz = -v.z * 0.35;
            if (Math.abs(moved.y - v.y) > 1e-4) {
                vy = v.y < -0.12 ? -v.y * 0.3 : 0;
                vx *= 0.6;
                vz *= 0.6;
            }
            v = new Vec3(vx, vy, vz).scale(isInWater() ? 0.8 : 0.985);
            if (onGround() && v.horizontalDistanceSqr() < 0.0004) v = new Vec3(0, v.y, 0);
            setDeltaMovement(v);
            spin += (float) v.length() * 60f;
        }
        if (level().isClientSide) {
            int st = smokeTicks();
            if (st > 0) smokeParticles(st);
            else if (tickCount % 2 == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.15, getZ(), 0, 0.01, 0);
            return;
        }
        int st = smokeTicks();
        if (st > 0) {
            entityData.set(SMOKE_T, st + 1);
            if (st % 20 == 0) blindMobsInSmoke(st);
            if (st >= SMOKE_TIME) discard();
            return;
        }
        if (--fuse <= 0) detonate();
    }

    private void smokeParticles(int st) {
        float grow = Math.min(1f, st / 60f);
        float fade = st > SMOKE_TIME - 80 ? (SMOKE_TIME - st) / 80f : 1f;
        double r = 1.2 + 3.3 * grow;
        int n = (int) (10 * fade) + 1;
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = Math.sqrt(random.nextDouble()) * r;
            double y = random.nextDouble() * (1.2 + 2.2 * grow);
            level().addAlwaysVisibleParticle(i % 3 == 0 ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.WHITE_SMOKE, true,
                    getX() + Math.cos(a) * d, getY() + y, getZ() + Math.sin(a) * d,
                    random.nextGaussian() * 0.01, i % 3 == 0 ? 0.008 : 0.02, random.nextGaussian() * 0.01);
        }
    }

    private void blindMobsInSmoke(int st) {
        double r = 1.5 + 3.0 * Math.min(1f, st / 60f);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(r, 3, r))) {
            if (e instanceof Mob m) {
                m.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
                if (m.getTarget() != null && m.getTarget().distanceTo(this) < r + 1) m.setTarget(null);
            }
        }
        // 연기 밖의 몹이 연기 속 대상을 노리고 있으면 놓침
        for (Mob m : level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(32))) {
            LivingEntity t = m.getTarget();
            if (t != null && t.distanceTo(this) < r && m.distanceTo(this) > r) m.setTarget(null);
        }
    }

    private void detonate() {
        detonations++;
        ServerLevel sl = (ServerLevel) level();
        switch (kind()) {
            case FLASH -> flash(sl);
            case SMOKE -> {
                entityData.set(SMOKE_T, 1);
                setDeltaMovement(Vec3.ZERO);
                sl.playSound(null, getX(), getY(), getZ(), ModSounds.SMOKE_HISS.get(), SoundSource.PLAYERS, 1.5f, 1f);
            }
            default -> {
                sl.playSound(null, getX(), getY(), getZ(), ModSounds.GRENADE_EXPLODE.get(), SoundSource.PLAYERS, 4f, 1f);
                sl.explode(this, getX(), getY() + 0.1, getZ(), 3.2f, Level.ExplosionInteraction.MOB);
                discard();
            }
        }
    }

    private void flash(ServerLevel sl) {
        Vec3 c = position().add(0, 0.2, 0);
        sl.playSound(null, getX(), getY(), getZ(), ModSounds.FLASHBANG.get(), SoundSource.PLAYERS, 4f, 1f);
        sl.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 3, 0.1, 0.1, 0.1, 0);
        sl.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 40, 0.2, 0.2, 0.2, 0.35);
        for (LivingEntity e : sl.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(20))) {
            Vec3 eye = e.getEyePosition();
            double dist = eye.distanceTo(c);
            if (dist > 20) continue;
            if (sl.clip(new ClipContext(c, eye, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this)).getType() != HitResult.Type.MISS) continue;
            Vec3 toFlash = c.subtract(eye).normalize();
            double facing = e.getViewVector(1f).dot(toFlash);       // 1 = 정면
            double f = (1 - dist / 20.0) * (facing > 0.3 ? 1.0 : facing > -0.2 ? 0.6 : 0.3);
            if (dist < 4) f = Math.max(f, 0.6);
            int ticks = (int) (30 + 130 * f);
            e.addEffect(new MobEffectInstance(ModEffects.FLASHED, ticks, 0, false, false, false));
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks / 2, 0, false, false));
            if (e instanceof ServerPlayer sp) {
                sp.playNotifySound(ModSounds.TINNITUS.get(), SoundSource.PLAYERS, (float) Math.min(1, 0.3 + f), 1f);
            } else if (e instanceof Mob m) {
                m.setTarget(null);
                m.getNavigation().stop();
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 3, false, false));
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuse", fuse);
        tag.putInt("Smoke", smokeTicks());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        fuse = tag.getInt("Fuse");
        entityData.set(SMOKE_T, tag.getInt("Smoke"));
    }
}
