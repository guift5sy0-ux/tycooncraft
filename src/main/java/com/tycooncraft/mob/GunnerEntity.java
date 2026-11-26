package com.tycooncraft.mob;

import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * 총잡이 몹 3종: 갱단원(권총), 용병(소총·방탄장비), 무법자(산탄총). 총알 판정은 플레이어 총과 같음(방탄조끼로 막힘).
 * 쓰러뜨리면 동전·탄약, 가끔 총.
 */
public class GunnerEntity extends Monster implements RangedAttackMob {
    public enum Kind {
        GANGSTER(26, 0.28, 5.0f, 1, 0.045f, 34, 22, 32),
        MERCENARY(32, 0.27, 6.5f, 1, 0.03f, 18, 26, 40),
        OUTLAW(30, 0.29, 3.2f, 6, 0.10f, 40, 12, 18),
        BOSS(420, 0.27, 7.0f, 1, 0.035f, 7, 30, 44),
        // v0.29 언더시티 변종 갱단 (더 강함)
        ENFORCER(70, 0.25, 5.5f, 1, 0.05f, 6, 24, 40),      // 집행자: 중무장, 빠른 연사
        BRUTE(60, 0.33, 3.6f, 8, 0.11f, 30, 12, 32),        // 돌격대: 빠르게 붙어서 산탄
        MARKSMAN(34, 0.27, 14f, 1, 0.006f, 60, 42, 56);     // 저격수: 멀리서 한 방

        final double health, speed;
        final float damage, spread;
        final int pellets, interval, range, follow;

        Kind(double health, double speed, float damage, int pellets, float spread, int interval, int range, int follow) {
            this.health = health;
            this.speed = speed;
            this.damage = damage;
            this.pellets = pellets;
            this.spread = spread;
            this.interval = interval;
            this.range = range;
            this.follow = follow;
        }
    }

    public static final float MATERIAL_CHANCE = 0.01f, AMMO_CHANCE = 0.02f, GUN_CHANCE = 0.001f;
    protected final Kind kind;

    public GunnerEntity(EntityType<? extends Monster> type, Level level, Kind kind) {
        super(type, level);
        this.kind = kind;
        this.xpReward = kind == Kind.BOSS ? 200 : 8;
        // registerGoals() 는 부모 생성자에서 먼저 불리므로(kind 가 아직 null) 사격 목표는 여기서 추가
        goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, kind.interval, kind.interval + 10, kind.range));
        goalSelector.addGoal(1, com.tycooncraft.turret.TurretHuntGoal.ranged(this, kind.damage * kind.pellets, kind.range, kind.interval, this::fireSound));   // 포탑 먼저
    }

    public Kind kind() {
        return kind;
    }

    public static AttributeSupplier.Builder attributes(Kind k) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, k.health)
                .add(Attributes.MOVEMENT_SPEED, k.speed)
                .add(Attributes.FOLLOW_RANGE, k.follow)
                .add(Attributes.ARMOR, k == Kind.BOSS ? 14 : k == Kind.ENFORCER ? 12 : k == Kind.BRUTE ? 8 : k == Kind.MERCENARY ? 4 : 2)
                .add(Attributes.ARMOR_TOUGHNESS, k == Kind.ENFORCER ? 4 : 0)
                .add(Attributes.KNOCKBACK_RESISTANCE, k == Kind.BOSS ? 1.0 : k == Kind.ENFORCER || k == Kind.BRUTE ? 0.6 : 0.1)
                .add(Attributes.ATTACK_DAMAGE, 4);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, GunnerEntity.class));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, DestroyerGolem.class, false));   // 갱단 ↔ 디스트로이어 적대
    }

    protected Item weapon() {
        return switch (kind) {
            case GANGSTER -> ModItems.PISTOL.get();
            case MERCENARY, BOSS, ENFORCER -> ModItems.RIFLE.get();
            case OUTLAW, BRUTE -> ModItems.SHOTGUN.get();
            case MARKSMAN -> ModItems.SNIPER.get();
        };
    }

    protected SoundEvent fireSound() {
        return switch (kind) {
            case GANGSTER -> ModSounds.PISTOL_FIRE.get();
            case MERCENARY, BOSS, ENFORCER, MARKSMAN -> ModSounds.RIFLE_FIRE.get();
            case OUTLAW, BRUTE -> ModSounds.SHOTGUN_FIRE.get();
        };
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, @Nullable SpawnGroupData data) {
        SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(weapon()));
        setDropChance(EquipmentSlot.MAINHAND, kind == Kind.BOSS ? 1.0f : GUN_CHANCE);
        if (kind == Kind.ENFORCER) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.VEST_2.get()));
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HELMET_1.get()));
            setDropChance(EquipmentSlot.CHEST, 0.03f);
            setDropChance(EquipmentSlot.HEAD, 0.03f);
        } else if (kind == Kind.BRUTE) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.VEST_1.get()));
            setDropChance(EquipmentSlot.CHEST, 0.03f);
        }
        if (kind == Kind.MERCENARY) {
            setItemSlot(EquipmentSlot.CHEST, new ItemStack(random.nextFloat() < 0.3f ? ModItems.VEST_2.get() : ModItems.VEST_1.get()));
            setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.HELMET_1.get()));
            setDropChance(EquipmentSlot.CHEST, MATERIAL_CHANCE);
            setDropChance(EquipmentSlot.HEAD, MATERIAL_CHANCE);
        }
        return d;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        setAggressive(getTarget() != null);
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pull) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = getEyePosition();
        Vec3 aim = target.getBoundingBox().getCenter().add(0, target.getBbHeight() * (random.nextFloat() < 0.15f ? 0.35 : 0.05), 0);
        Vec3 base = aim.subtract(from).normalize();
        float spread = kind.spread * (level().getDifficulty().getId() >= 3 ? 0.8f : 1f);
        for (int i = 0; i < kind.pellets; i++) {
            Vec3 dir = base.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread).normalize();
            Ballistics.shoot(sl, this, from.add(dir.scale(0.6)), dir, kind.range + 8, kind.damage, 0, 1.5f, false, true,
                    damageSources().mobAttack(this), e -> !(e instanceof GunnerEntity));
        }
        sl.playSound(null, getX(), getY(), getZ(), fireSound(), SoundSource.HOSTILE, 3.0f, 0.95f + random.nextFloat() * 0.1f);
        Vec3 m = from.add(base.scale(1.0)).add(0, -0.25, 0);
        // v0.20.0: 총구 섬광·연기는 BulletFx 가 그림
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (kind == Kind.BOSS) return;   // 보스는 자기 전리품(확정 총)을 따로 떨어뜨림
        if (random.nextFloat() < 0.03f) spawnAtLocation(new ItemStack(ModItems.BLACK_MARKET_PASS.get()));   // v0.26.0 암시장 출입증 3%
        // 갱단원 전리품은 아주 드물게: 재료(동전 포함) 1%, 탄약 2%, 총은 0.1% (장비 드롭 확률)
        if (random.nextFloat() < MATERIAL_CHANCE) spawnAtLocation(new ItemStack(com.tycooncraft.economy.Coins.scaled(ModItems.COIN_IRON.get()), 1 + random.nextInt(kind == Kind.MERCENARY ? 4 : 3)));
        if (random.nextFloat() < MATERIAL_CHANCE) spawnAtLocation(new ItemStack(com.tycooncraft.economy.Coins.scaled(ModItems.COIN_COPPER.get()), 2 + random.nextInt(8)));
        Item ammo = kind == Kind.OUTLAW || kind == Kind.BRUTE ? ModItems.SHOTGUN_SHELL.get() : kind == Kind.GANGSTER ? ModItems.LIGHT_AMMO.get() : ModItems.HEAVY_AMMO.get();
        boolean variant = variant();
        if (random.nextFloat() < (variant ? 0.25f : AMMO_CHANCE)) spawnAtLocation(new ItemStack(ammo, 2 + random.nextInt(6)));
        // v0.29 언더시티: 갱단원이 금고 열쇠를 지니고 다님 (일반 20%, 변종 60%) · 변종은 현금도
        if (com.tycooncraft.blackmarket.BlackMarket.isBM(level)) {
            if (random.nextFloat() < (variant ? 0.6f : 0.2f)) spawnAtLocation(new ItemStack(ModItems.UNDERCITY_KEY.get()));
            if (variant) for (ItemStack c : com.tycooncraft.economy.Coins.toStacks(2000 + random.nextInt(6000))) spawnAtLocation(c);
            com.tycooncraft.blackmarket.Undercity.kills++;
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }

    public boolean variant() {
        return kind == Kind.ENFORCER || kind == Kind.BRUTE || kind == Kind.MARKSMAN;
    }

    // 엔티티 타입별 생성자
    public static GunnerEntity enforcer(EntityType<? extends Monster> t, Level l) {
        return new GunnerEntity(t, l, Kind.ENFORCER);
    }

    public static GunnerEntity brute(EntityType<? extends Monster> t, Level l) {
        return new GunnerEntity(t, l, Kind.BRUTE);
    }

    public static GunnerEntity marksman(EntityType<? extends Monster> t, Level l) {
        return new GunnerEntity(t, l, Kind.MARKSMAN);
    }

    public static GunnerEntity gangster(EntityType<? extends Monster> t, Level l) {
        return new GunnerEntity(t, l, Kind.GANGSTER);
    }

    public static GunnerEntity mercenary(EntityType<? extends Monster> t, Level l) {
        return new GunnerEntity(t, l, Kind.MERCENARY);
    }

    public static GunnerEntity outlaw(EntityType<? extends Monster> t, Level l) {
        return new GunnerEntity(t, l, Kind.OUTLAW);
    }

    static EntityType<? extends GunnerEntity> minionType() {
        return ModEntities.GANGSTER.get();
    }
}
