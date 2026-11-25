package com.tycooncraft.mob.carrier;

import com.tycooncraft.mob.BossBar;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * v0.27.0 공중 디스트로이어 (모체): 높이 떠서 선회하며
 *  - 격납고를 열어 정찰 디스트로이어를 떨어뜨리고 (최대 6)
 *  - 미니 드론 10기를 뽑아 원거리 견제를 시킨다.
 * 본체에는 직접 피해가 들어가지 않고, 맞은 방향에 가까운 동력원(왼쪽·오른쪽·뒤, 각 체력 150)이 깎인다.
 * 동력원 2개가 부서지면 추락 → 땅에 닿으면 대폭발 (충전된 크리퍼의 1.2배).
 */
public class AerialDestroyer extends Monster {
    public static final float CORE_HP = 150f, BLAST = 6f * 1.2f;
    public static final int MAX_SCOUTS = 6, MAX_DRONES = 10, HANGAR_EVERY = 220, DRONE_EVERY = 70;
    /** 동력원 위치 (몸 기준: x 오른쪽, z 앞) */
    public static final Vec3[] CORE_POS = {new Vec3(-2.2, 1.35, 0.15), new Vec3(2.2, 1.35, 0.15), new Vec3(0, 1.4, -2.1)};
    private static final EntityDataAccessor<Float> C0 = SynchedEntityData.defineId(AerialDestroyer.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> C1 = SynchedEntityData.defineId(AerialDestroyer.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> C2 = SynchedEntityData.defineId(AerialDestroyer.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> CRASH = SynchedEntityData.defineId(AerialDestroyer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HATCH = SynchedEntityData.defineId(AerialDestroyer.class, EntityDataSerializers.INT);
    private final BossBar bar = new BossBar(Component.translatable("entity.tycooncraft.aerial_destroyer"), BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_6, 72);
    private final List<ScoutDestroyer> scouts = new ArrayList<>();
    private final List<MiniDrone> drones = new ArrayList<>();
    private int hangarCd = 60, droneCd = 5, crashTicks;
    private float orbit;
    private BlockPos home;
    public static int coresBroken, crashes, scoutsLaunched, dronesLaunched;

    public AerialDestroyer(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        xpReward = 120;
        orbit = random.nextFloat() * 6.28f;
    }

    public static AttributeSupplier.Builder attributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, CORE_HP * 3).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 64).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ARMOR, 6);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(C0, CORE_HP);
        b.define(C1, CORE_HP);
        b.define(C2, CORE_HP);
        b.define(CRASH, 0);
        b.define(HATCH, 0);
    }

    private EntityDataAccessor<Float> acc(int i) {
        return i == 0 ? C0 : i == 1 ? C1 : C2;
    }

    public float core(int i) {
        return entityData.get(acc(i));
    }

    public int coresAlive() {
        int n = 0;
        for (int i = 0; i < 3; i++) if (core(i) > 0) n++;
        return n;
    }

    public boolean crashing() {
        return entityData.get(CRASH) > 0;
    }

    public int crashTime() {
        return entityData.get(CRASH);
    }

    /** 격납고 해치가 열려 있는 남은 틱 (클라 애니메이션) */
    public int hatch() {
        return entityData.get(HATCH);
    }

    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, com.tycooncraft.village.VillageGuard.class, false));
    }

    // ── 피해: 맞은 방향 쪽 동력원으로 ──
    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(src, amount);   // /kill 등
        if (level().isClientSide || isInvulnerableTo(src) || !isAlive()) return false;
        if (crashing() || src.is(DamageTypeTags.IS_FALL) || src.is(DamageTypeTags.IS_DROWNING) || src.is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return false;
        if (src.getEntity() instanceof MiniDrone || src.getEntity() instanceof ScoutDestroyer || src.getEntity() == this) return false;
        Vec3 from = src.getSourcePosition() != null ? src.getSourcePosition() : src.getEntity() != null ? src.getEntity().position() : position();
        int idx = nearestCore(from);
        if (idx < 0) return false;
        float dmg = amount * (1 - Math.min(0.5f, (float) getAttributeValue(Attributes.ARMOR) * 0.02f));
        float left = core(idx) - dmg;
        entityData.set(acc(idx), Math.max(0, left));
        level().broadcastDamageEvent(this, src);
        if (src.getEntity() instanceof LivingEntity le && le != this) {
            setLastHurtByMob(le);
            if (getTarget() == null) setTarget(le);
        }
        if (left <= 0) coreDestroyed(idx);
        syncHealth();
        return true;
    }

    int nearestCore(Vec3 from) {
        Vec3 d = from.subtract(position().add(0, getBbHeight() / 2, 0));
        double yaw = Math.toRadians(getYRot());
        // 세계 → 몸 기준 (x 오른쪽, z 앞): 마크의 yaw 0 = 남쪽(+z)을 바라봄
        double lx = d.x * Math.cos(yaw) + d.z * Math.sin(yaw);
        double lz = -d.x * Math.sin(yaw) + d.z * Math.cos(yaw);
        Vec3 local = new Vec3(-lx, d.y, lz).normalize();
        int best = -1;
        double bd = -9;
        for (int i = 0; i < 3; i++) {
            if (core(i) <= 0) continue;
            double s = CORE_POS[i].normalize().dot(local);
            if (s > bd) {
                bd = s;
                best = i;
            }
        }
        return best;
    }

    /** 동력원 i 의 세계 좌표 */
    public Vec3 coreWorld(int i) {
        Vec3 c = CORE_POS[i];
        double yaw = Math.toRadians(getYRot());
        double wx = -c.x * Math.cos(yaw) - c.z * Math.sin(yaw);
        double wz = -c.x * Math.sin(yaw) + c.z * Math.cos(yaw);
        return position().add(wx, getBbHeight() / 2 + c.y - 0.6, wz);
    }

    private void coreDestroyed(int i) {
        coresBroken++;
        if (level() instanceof ServerLevel sl) {
            Vec3 p = coreWorld(i);
            sl.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y, p.z, 3, 0.6, 0.6, 0.6, 0);
            sl.sendParticles(ParticleTypes.LAVA, p.x, p.y, p.z, 12, 0.5, 0.5, 0.5, 0);
            sl.playSound(null, p.x, p.y, p.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5f, 1.2f);
        }
        if (coresAlive() <= 1) startCrash();
    }

    private void startCrash() {
        if (crashing()) return;
        crashes++;
        entityData.set(CRASH, 1);
        setNoGravity(false);
        bar.setName(Component.translatable("entity.tycooncraft.aerial_destroyer.crashing"));
        playSound(SoundEvents.WITHER_HURT, 4f, 0.5f);
    }

    private void syncHealth() {
        float sum = core(0) + core(1) + core(2);
        setHealth(Math.max(1f, sum));
        bar.setName(Component.translatable("entity.tycooncraft.aerial_destroyer.cores", coresAlive()));
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            // 부서진 동력원에서 연기·불꽃
            for (int i = 0; i < 3; i++) {
                if (core(i) > 0 && !crashing()) continue;
                if (random.nextInt(2) != 0) continue;
                Vec3 p = coreWorld(i);
                level().addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, 0, 0.05, 0);
                if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, 0, 0.02, 0);
            }
            return;
        }
        if (home == null) home = blockPosition();
        bar.tick(this);
        bar.setProgress(Math.max(0f, (core(0) + core(1) + core(2)) / (CORE_HP * 3)));
        if (hatch() > 0) entityData.set(HATCH, hatch() - 1);
        scouts.removeIf(s -> !s.isAlive());
        drones.removeIf(d -> !d.isAlive());
        if (crashing()) {
            crashTick();
            return;
        }
        LivingEntity t = getTarget();
        if (t != null && (!t.isAlive() || t.distanceToSqr(this) > 90 * 90 || (t instanceof Player p && (p.isCreative() || p.isSpectator())))) {
            setTarget(null);
            t = null;
        }
        fly(t);
        if (t != null) {
            if (--hangarCd <= 0) {
                hangarCd = HANGAR_EVERY;
                if (scouts.size() < MAX_SCOUTS) entityData.set(HATCH, 40);
            }
            if (hatch() == 20) launchScouts(t);
            if (--droneCd <= 0) {
                droneCd = DRONE_EVERY;
                for (int k = 0; k < 3 && drones.size() < MAX_DRONES; k++) launchDrone();
            }
        }
    }

    private void fly(LivingEntity t) {
        Vec3 goal;
        orbit += t != null ? 0.012f : 0.006f;
        if (t != null) {
            goal = t.position().add(Math.cos(orbit) * 14, 15 + Math.sin(orbit * 2) * 2, Math.sin(orbit) * 14);
        } else {
            goal = Vec3.atCenterOf(home).add(Math.cos(orbit) * 10, 0, Math.sin(orbit) * 10);
        }
        // 땅에서 최소 10칸 위
        int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(goal.x), (int) Math.floor(goal.z));
        goal = new Vec3(goal.x, Math.max(goal.y, ground + 10), goal.z);
        Vec3 v = goal.subtract(position());
        double d = v.length();
        Vec3 want = d > 0.01 ? v.scale(Math.min(0.22, d * 0.05) / d) : Vec3.ZERO;
        setDeltaMovement(getDeltaMovement().scale(0.85).add(want.scale(0.15)));
        Vec3 mv = getDeltaMovement();
        if (mv.horizontalDistanceSqr() > 0.0004) {
            float want2 = (float) (Math.atan2(mv.z, mv.x) * 180 / Math.PI) - 90f;
            float cur = getYRot();
            float diff = net.minecraft.util.Mth.wrapDegrees(want2 - cur);
            setYRot(cur + net.minecraft.util.Mth.clamp(diff, -2.5f, 2.5f));
            yBodyRot = getYRot();
            yHeadRot = getYRot();
        }
    }

    private void launchScouts(LivingEntity t) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (int k = 0; k < 2 && scouts.size() < MAX_SCOUTS; k++) {
            ScoutDestroyer s = ModEntities.SCOUT_DESTROYER.get().create(sl);
            if (s == null) return;
            s.moveTo(getX() + random.nextGaussian() * 0.6, getY() - 0.8, getZ() + random.nextGaussian() * 0.6, getYRot(), 0);
            s.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            s.setFromCarrier(true);
            s.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
            s.setTarget(t);
            sl.addFreshEntity(s);
            scouts.add(s);
            scoutsLaunched++;
        }
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_DOOR_OPEN, SoundSource.HOSTILE, 3f, 0.6f);
    }

    private void launchDrone() {
        if (!(level() instanceof ServerLevel sl)) return;
        MiniDrone d = ModEntities.MINI_DRONE.get().create(sl);
        if (d == null) return;
        d.moveTo(getX(), getY() - 0.5, getZ(), getYRot(), 0);
        d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        d.setCarrier(this);
        sl.addFreshEntity(d);
        drones.add(d);
        dronesLaunched++;
        sl.playSound(null, getX(), getY(), getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.HOSTILE, 1.5f, 1.4f);
    }

    private void crashTick() {
        crashTicks++;
        entityData.set(CRASH, crashTicks + 1);
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x * 0.98, Math.max(-1.6, v.y - 0.04), v.z * 0.98);
        setYRot(getYRot() + 4f);
        if (level() instanceof ServerLevel sl && crashTicks % 3 == 0)
            sl.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 1, getZ(), 4, 1.5, 0.5, 1.5, 0.02);
        if ((onGround() || horizontalCollision || isInWater() || crashTicks > 200) && crashTicks > 5) {
            // 마을(경비병 근처)·블랙마켓에선 블록은 부수지 않음
            boolean safe = !level().getEntitiesOfClass(com.tycooncraft.village.VillageGuard.class, getBoundingBox().inflate(64)).isEmpty()
                    || com.tycooncraft.blackmarket.BlackMarket.isBM(level());
            level().explode(this, getX(), getY() + 1, getZ(), BLAST, false, safe ? Level.ExplosionInteraction.NONE : Level.ExplosionInteraction.MOB);
            DamageSource last = getLastDamageSource();
            dieFromCrash(last);
        }
    }

    private void dieFromCrash(DamageSource last) {
        setHealth(0);
        die(last != null ? last : damageSources().generic());
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource src, boolean hit) {
        super.dropCustomDeathLoot(level, src, hit);
        var core = spawnAtLocation(new ItemStack(ModItems.FLIGHT_CORE.get()));
        if (core != null) core.setInvulnerable(true);   // 추락 폭발에 휘말려도 남게
        spawnAtLocation(new ItemStack(com.tycooncraft.economy.Coins.scaled(ModItems.COIN_GOLD.get()), 2 + random.nextInt(3)));
        spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 6 + random.nextInt(8)));
    }

    @Override
    public void remove(RemovalReason r) {
        super.remove(r);
        bar.clear();
    }

    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
    }

    @Override
    public boolean causeFallDamage(float d, float m, DamageSource s) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return d > 160 * 160;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putFloat("Core0", core(0));
        t.putFloat("Core1", core(1));
        t.putFloat("Core2", core(2));
        if (home != null) t.putLong("Home", home.asLong());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.contains("Core0")) {
            entityData.set(C0, t.getFloat("Core0"));
            entityData.set(C1, t.getFloat("Core1"));
            entityData.set(C2, t.getFloat("Core2"));
        }
        if (t.contains("Home")) home = BlockPos.of(t.getLong("Home"));
        if (coresAlive() <= 1) startCrash();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BEACON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GENERIC_EXPLODE.value();
    }

    @Override
    protected float getSoundVolume() {
        return 3f;
    }

    @Override
    public float getVoicePitch() {
        return 0.6f;
    }
}
