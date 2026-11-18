package com.tycooncraft.mob;

import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 갱단 무장 트럭 ("테크니컬"): 운전수·기관총 사수·조직원 2명을 태우고 순찰하다가
 * 플레이어를 보면 추격 → 사거리에 들면 멈춰 짐칸 기관총으로 점사, 가까워지면 조직원을 내려 보낸다.
 * 들이받으면 피해, 파괴되면 폭발. 플레이어가 운전할 수는 없다.
 */
public class GangTruck extends Monster {
    private static final EntityDataAccessor<Float> MG_YAW = SynchedEntityData.defineId(GangTruck.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> MG_PITCH = SynchedEntityData.defineId(GangTruck.class, EntityDataSerializers.FLOAT);

    /** 모델 단위(1/32 블록), 앞 = -Z */
    public static final Vec3 MG_PIVOT = new Vec3(0, 50, 40);
    private static final Vec3[] SEATS = {new Vec3(13, 21, -4), new Vec3(0, 27, 54), new Vec3(22, 27, 46), new Vec3(-22, 27, 46)};

    public static int SHOTS, DEPLOYS, RAMS;

    private int burst, mgCd = 40, rammed;
    private boolean deployed;
    private final Map<Integer, Integer> ramCd = new HashMap<>();
    // 클라이언트 표시
    public float wheelAngle, wheelAngleO, mgYawO, mgPitchO;

    public GangTruck(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new TruckMove(this);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 160).add(Attributes.ARMOR, 8).add(Attributes.ARMOR_TOUGHNESS, 4)
                .add(Attributes.MOVEMENT_SPEED, 0.34).add(Attributes.FOLLOW_RANGE, 64).add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.STEP_HEIGHT, 1.1).add(Attributes.ATTACK_DAMAGE, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(MG_YAW, 0f);
        b.define(MG_PITCH, 0f);
    }

    public float mgYaw() {
        return entityData.get(MG_YAW);
    }

    public float mgPitch() {
        return entityData.get(MG_PITCH);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new ChaseGoal(this));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.55) {
            @Override
            public boolean canUse() {
                return hasDriver() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return hasDriver() && super.canContinueToUse();
            }
        });
        targetSelector.addGoal(1, new HurtByTargetGoal(this, GunnerEntity.class, GangTruck.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, DestroyerGolem.class, false));
    }

    /** 운전석(첫 번째 좌석)에 갱단원이 있어야 움직인다 */
    public boolean hasDriver() {
        return !getPassengers().isEmpty() && getPassengers().get(0).isAlive();
    }

    /** 몸통(운전) AI 는 트럭 자신이 — 탄 갱단원이 조종하지 않게 */
    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return null;
    }

    // ── 좌표 ──
    public Vec3 toWorld(Vec3 units) {
        return new Vec3(-units.x / 32.0, units.y / 32.0, -units.z / 32.0).yRot(-getYRot() * Mth.DEG_TO_RAD).add(position());
    }

    public Vec3 muzzle() {
        float yaw = getYRot() + mgYaw(), pitch = mgPitch();
        Vec3 dir = Vec3.directionFromRotation(pitch, yaw);
        return toWorld(MG_PIVOT).add(dir.scale(1.45));
    }

    // ── 탑승 ──
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < SEATS.length;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        int i = Math.max(0, getPassengers().indexOf(passenger));
        Vec3 w = toWorld(SEATS[Math.min(i, SEATS.length - 1)]).subtract(position());
        return w.add(0, -0.55, 0).add(passenger.getVehicleAttachmentPoint(this));
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        super.positionRider(passenger, move);
        if (passenger instanceof LivingEntity le) {
            le.yBodyRot = getYRot();
            if (getPassengers().indexOf(passenger) <= 1) le.setYHeadRot(getYRot() + (getPassengers().indexOf(passenger) == 1 ? mgYaw() : 0));
        }
    }

    // ── 스폰: 승무원 태우기 ──
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance diff, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, diff, reason, data);
        List<EntityType<? extends GunnerEntity>> crew = List.of(ModEntities.GANGSTER.get(), ModEntities.MERCENARY.get(),
                ModEntities.OUTLAW.get(), random.nextBoolean() ? ModEntities.GANGSTER.get() : ModEntities.MERCENARY.get());
        for (EntityType<? extends GunnerEntity> t : crew) {
            GunnerEntity g = t.create(level.getLevel());
            if (g == null) continue;
            g.moveTo(getX(), getY(), getZ(), getYRot(), 0);
            g.finalizeSpawn(level, diff, MobSpawnType.JOCKEY, null);
            if (isPersistenceRequired()) g.setPersistenceRequired();
            g.startRiding(this, true);
        }
        return d;
    }

    @Override
    public void setPersistenceRequired() {
        super.setPersistenceRequired();
        for (Entity p : getPassengers()) if (p instanceof net.minecraft.world.entity.Mob m && !m.isPersistenceRequired()) m.setPersistenceRequired();
    }

    // ── 틱 ──
    @Override
    public void tick() {
        wheelAngleO = wheelAngle;
        mgYawO = mgYaw();
        mgPitchO = mgPitch();
        super.tick();
        yBodyRot = getYRot();
        yHeadRot = getYRot();
        if (level().isClientSide) {
            Vec3 v = new Vec3(getX() - xo, 0, getZ() - zo);
            Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
            wheelAngle += (float) (v.dot(fwd) / 0.5);
            if (getHealth() < getMaxHealth() * 0.4f && tickCount % 3 == 0) {
                Vec3 h = toWorld(new Vec3(0, 42, -56));
                level().addParticle(getHealth() < getMaxHealth() * 0.2f ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, h.x, h.y, h.z, 0, 0.06, 0);
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity t = getTarget();
        if (t != null && !t.isAlive()) t = null;
        aimAndFire(t);
        // 가까워지면 조직원 하차
        if (t != null && !deployed && distanceTo(t) < 22) {
            List<Entity> ps = List.copyOf(getPassengers());
            for (int i = 2; i < ps.size(); i++) ps.get(i).stopRiding();
            deployed = true;
            DEPLOYS++;
            playSound(SoundEvents.IRON_DOOR_OPEN, 1.2f, 0.7f);
        }
        // 들이받기
        double sp = getDeltaMovement().horizontalDistance();
        if (sp > 0.14) {
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.4))) {
                if (e == this || e.getVehicle() == this || e instanceof GunnerEntity || e instanceof GangTruck) continue;
                if (ramCd.getOrDefault(e.getId(), 0) > tickCount) continue;
                ramCd.put(e.getId(), tickCount + 20);
                Vec3 fwd = Vec3.directionFromRotation(0, getYRot());
                if (e.hurt(damageSources().mobAttack(this), (float) (6 + sp * 14))) RAMS++;
                e.push(fwd.x * 1.6, 0.45, fwd.z * 1.6);
                e.hurtMarked = true;
                playSound(SoundEvents.ANVIL_LAND, 0.8f, 0.6f);
            }
        }
        if (tickCount % 200 == 0) ramCd.entrySet().removeIf(en -> en.getValue() < tickCount);
    }

    /** 짐칸 기관총: 대상 쪽으로 천천히 돌려 조준 → 점사 (10발) */
    private void aimAndFire(@Nullable LivingEntity t) {
        if (mgCd > 0) mgCd--;
        float yaw = mgYaw(), pitch = mgPitch();
        if (t == null || getPassengers().size() < 2) {             // 사수가 없으면 사격 불가
            entityData.set(MG_YAW, yaw + Mth.wrapDegrees(0 - yaw) * 0.08f);
            entityData.set(MG_PITCH, pitch * 0.9f);
            burst = 0;
            return;
        }
        Vec3 pivot = toWorld(MG_PIVOT);
        Vec3 to = t.getBoundingBox().getCenter().subtract(pivot);
        float wantYaw = Mth.wrapDegrees((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90f - getYRot());
        float wantPitch = (float) -(Mth.atan2(to.y, to.horizontalDistance()) * Mth.RAD_TO_DEG);
        yaw += Mth.clamp(Mth.wrapDegrees(wantYaw - yaw), -9f, 9f);
        pitch += Mth.clamp(wantPitch - pitch, -6f, 6f);
        pitch = Mth.clamp(pitch, -35f, 20f);
        entityData.set(MG_YAW, yaw);
        entityData.set(MG_PITCH, pitch);
        boolean aligned = Math.abs(Mth.wrapDegrees(wantYaw - yaw)) < 7 && Math.abs(wantPitch - pitch) < 7;
        double dist = to.length();
        if (burst == 0 && mgCd == 0 && aligned && dist < 44 && hasLineOfSight(t)) burst = 10;
        if (burst > 0 && tickCount % 2 == 0 && level() instanceof ServerLevel sl) {
            burst--;
            Vec3 m = muzzle();
            Vec3 dir = t.getBoundingBox().getCenter().subtract(m).normalize()
                    .add(random.nextGaussian() * 0.035, random.nextGaussian() * 0.03, random.nextGaussian() * 0.035).normalize();
            Ballistics.shoot(sl, this, m, dir, 48, 3.5f, 0, 1.5f, false, true, damageSources().mobAttack(this),
                    e -> e != this && !(e instanceof GunnerEntity) && !(e instanceof GangTruck) && e.getVehicle() != this);
            sl.sendParticles(ParticleTypes.FLASH, m.x, m.y, m.z, 1, 0, 0, 0, 0);
            sl.sendParticles(ParticleTypes.SMOKE, m.x, m.y, m.z, 2, 0.05, 0.05, 0.05, 0.01);
            SoundEvent s = ModSounds.RIFLE_FIRE.get();
            level().playSound(null, m.x, m.y, m.z, s, SoundSource.HOSTILE, 2.2f, 0.75f + random.nextFloat() * 0.1f);
            SHOTS++;
            if (burst == 0) mgCd = 30 + random.nextInt(20);
        }
    }

    // ── 피해 / 파괴 ──
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof GunnerEntity || source.getEntity() instanceof GangTruck) return false;   // 아군 오사 무시
        boolean r = super.hurt(source, amount);
        if (r && !level().isClientSide && getHealth() < getMaxHealth() * 0.35f && !deployed) {
            List<Entity> ps = List.copyOf(getPassengers());
            for (int i = 2; i < ps.size(); i++) ps.get(i).stopRiding();
            deployed = true;
        }
        return r;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide && !isRemoved()) {
            ejectPassengers();
            Vec3 c = position().add(0, 1, 0);
            level().explode(this, c.x, c.y, c.z, 2.4f, Level.ExplosionInteraction.NONE);
            ((ServerLevel) level()).sendParticles(ParticleTypes.LARGE_SMOKE, c.x, c.y + 1, c.z, 40, 1.2, 0.8, 1.2, 0.05);
            ((ServerLevel) level()).sendParticles(ParticleTypes.FLAME, c.x, c.y, c.z, 30, 1.0, 0.5, 1.0, 0.05);
        }
        super.die(source);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recent) {
        super.dropCustomDeathLoot(level, source, recent);
        float m = GunnerEntity.MATERIAL_CHANCE;   // 갱단 전리품: 재료 1%, 탄약 2%
        if (random.nextFloat() < m) spawnAtLocation(new ItemStack(ModItems.COIN_GOLD.get(), 2 + random.nextInt(3)));
        if (random.nextFloat() < m) spawnAtLocation(new ItemStack(ModItems.STEEL_PLATE.get(), 2 + random.nextInt(3)));
        if (random.nextFloat() < m) spawnAtLocation(new ItemStack(ModItems.WHEEL.get(), 1 + random.nextInt(2)));
        if (random.nextFloat() < GunnerEntity.AMMO_CHANCE) spawnAtLocation(new ItemStack(ModItems.HEAVY_AMMO.get(), 16 + random.nextInt(17)));
        if (random.nextFloat() < m) spawnAtLocation(new ItemStack(ModItems.ENGINE.get()));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource src) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE.value();
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    // ── 자동차처럼 움직이기: 제자리 회전 없이, 꺾을수록 느려짐 ──
    static class TruckMove extends MoveControl {
        TruckMove(GangTruck mob) {
            super(mob);
        }

        @Override
        public void tick() {
            if (operation != Operation.MOVE_TO || (mob instanceof GangTruck g && !g.hasDriver())) {
                operation = Operation.WAIT;
                mob.setZza(0);
                mob.setSpeed(0);
                return;
            }
            operation = Operation.WAIT;
            double dx = wantedX - mob.getX(), dz = wantedZ - mob.getZ();
            if (dx * dx + dz * dz < 0.3) {
                mob.setZza(0);
                mob.setSpeed(0);
                return;
            }
            float target = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90f;
            float diff = Mth.wrapDegrees(target - mob.getYRot());
            mob.setYRot(mob.getYRot() + Mth.clamp(diff, -7f, 7f));
            mob.yBodyRot = mob.getYRot();
            float sp = (float) (speedModifier * mob.getAttributeValue(Attributes.MOVEMENT_SPEED));
            float f = (float) Math.max(0.3, Math.cos(Math.toRadians(Math.min(90, Math.abs(diff)))));
            mob.setSpeed(sp * f);
        }
    }

    /** 대상 추격: 멀면 따라가고, 사거리(18칸) 안 + 시야 확보면 멈춰서 사격 */
    static class ChaseGoal extends Goal {
        private final GangTruck truck;
        private int repath;

        ChaseGoal(GangTruck truck) {
            this.truck = truck;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return truck.hasDriver() && truck.getTarget() != null && truck.getTarget().isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void stop() {
            truck.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity t = truck.getTarget();
            if (t == null) return;
            double d = truck.distanceTo(t);
            boolean see = truck.hasLineOfSight(t);
            if (d < 16 && see && truck.getPassengers().size() >= 2) {
                truck.getNavigation().stop();
                return;
            }
            if (--repath <= 0) {
                repath = 15;
                truck.getNavigation().moveTo(t, d > 30 ? 1.15 : 0.9);
            }
        }
    }
}
