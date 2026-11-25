package com.tycooncraft.mech;

import com.tycooncraft.alliance.Alliances;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * v0.27.1 프로토 디스트로이어-정찰: 정찰 코어로 만드는 탑승형 사족 기계. 저글링처럼 빠르고 변칙적.
 * 조작: WASD (빠름, 즉각 반응) · 좌클릭(누르고 있기) 양날 베기 · 우클릭 덮치기(앞으로 크게 도약, 착지 시 주변 피해)
 *       · 스페이스 높은 점프 · 걷는 중 A/D 를 짧게 두 번 = 옆 회피 대시 (클라이언트) · Shift 내리기
 * 탄약이 필요 없음 (근접 기계). 철/강철로 수리, 주인이 웅크리고 빈손 우클릭 → 아이템으로 회수.
 */
public class ProtoScout extends PathfinderMob {
    public static final int ACT_SLASH = 1, ACT_POUNCE = 2, ACT_JUMP = 4, ACT_DASH = 5;
    public static final int MAX_HP = 120, SLASH_CD = 7, POUNCE_CD = 50, DASH_CD = 24;
    public static final float SLASH_DMG = 7f, POUNCE_DMG = 10f;
    public static final double JUMP_VEL = 0.82;
    private static final EntityDataAccessor<Integer> POUNCE_T = SynchedEntityData.defineId(ProtoScout.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLASH_AT = SynchedEntityData.defineId(ProtoScout.class, EntityDataSerializers.INT);
    @Nullable private UUID owner;
    private int slashCd, dashCd;
    private boolean pouncing;
    public static int slashes, pounces, hits;

    public ProtoScout(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, MAX_HP).add(Attributes.ARMOR, 8).add(Attributes.ARMOR_TOUGHNESS, 1)
                .add(Attributes.MOVEMENT_SPEED, 0.36).add(Attributes.KNOCKBACK_RESISTANCE, 0.6).add(Attributes.STEP_HEIGHT, 1.1)
                .add(Attributes.SAFE_FALL_DISTANCE, 16).add(Attributes.FOLLOW_RANGE, 0);
    }

    @Override
    protected void registerGoals() {}

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(POUNCE_T, 0).define(SLASH_AT, -100);
    }

    public int pounceCd() {
        return entityData.get(POUNCE_T);
    }

    /** 마지막 베기 시각 (tickCount) — 모델 애니메이션 */
    public int slashAt() {
        return entityData.get(SLASH_AT);
    }

    @Nullable public UUID owner() { return owner; }
    public void setOwner(@Nullable UUID u) { owner = u; }

    public boolean mayRide(Player p) {
        if (owner == null || owner.equals(p.getUUID())) return true;
        return p.getServer() != null && Alliances.friendly(p.getServer(), owner, p.getUUID());
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack st = player.getItemInHand(hand);
        if (isVehicle()) return InteractionResult.PASS;
        if (st.is(net.minecraft.world.item.Items.IRON_INGOT) || st.is(ModItems.STEEL_INGOT.get())) {
            if (getHealth() >= getMaxHealth()) return InteractionResult.PASS;
            if (!level().isClientSide) {
                heal(st.is(ModItems.STEEL_INGOT.get()) ? 30 : 12);
                st.consume(1, player);
                playSound(SoundEvents.ANVIL_USE, 0.8f, 1.5f);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (!mayRide(player)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tycooncraft.proto.not_owner"), true);
            return InteractionResult.FAIL;
        }
        if (player.isSecondaryUseActive() && st.isEmpty() && (owner == null || owner.equals(player.getUUID()))) {
            player.setItemInHand(hand, toItem());
            playSound(SoundEvents.IRON_GOLEM_REPAIR, 1f, 1.2f);
            discard();
            return InteractionResult.CONSUME;
        }
        return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public ItemStack toItem() {
        ItemStack s = new ItemStack(ModItems.PROTO_SCOUT.get());
        CompoundTag t = new CompoundTag();
        t.putFloat("Health", getHealth());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
        if (getHealth() < getMaxHealth()) {
            s.set(DataComponents.MAX_DAMAGE, 100);
            s.set(DataComponents.DAMAGE, Math.round(100 * (1 - getHealth() / getMaxHealth())));
        }
        return s;
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        float d = Mth.wrapDegrees(rider.getYRot() - getYRot());
        setYRot(getYRot() + Mth.clamp(d, -16f, 16f));   // 날렵하게 돎
        yRotO = getYRot();
        yBodyRot = yHeadRot = getYRot();
        setXRot(rider.getXRot() * 0.5f);
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 travel) {
        float fwd = rider.zza < 0 ? rider.zza * 0.6f : rider.zza;
        return new Vec3(rider.xxa * 0.8f, 0, fwd);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return (float) getAttributeValue(Attributes.MOVEMENT_SPEED);
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return new Vec3(0, 1.35, -0.2).yRot(-yBodyRot * Mth.DEG_TO_RAD);
    }

    @Override
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public Vec3 forward() {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    public void action(ServerPlayer rider, int act) {
        if (getControllingPassenger() != rider || !isAlive()) return;
        switch (act) {
            case ACT_SLASH -> slash(rider);
            case ACT_POUNCE -> {
                if (pounceCd() > 0 || !onGround()) return;
                entityData.set(POUNCE_T, POUNCE_CD);
                pouncing = true;
                pounces++;
                playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.8f);
            }
            case ACT_JUMP -> playSound(SoundEvents.PISTON_EXTEND, 1f, 1.6f);
            case ACT_DASH -> {
                if (dashCd > 0) return;
                dashCd = DASH_CD;
                playSound(SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.8f);
            }
            default -> {}
        }
    }

    /** 앞쪽 반원(3.2칸)의 적을 양날로 벰 */
    private void slash(ServerPlayer rider) {
        if (slashCd > 0) return;
        slashCd = SLASH_CD;
        slashes++;
        entityData.set(SLASH_AT, tickCount);
        Vec3 f = forward();
        AABB box = getBoundingBox().inflate(3.2, 1.0, 3.2);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e != rider && e.isAlive() && !e.isAlliedTo(rider))) {
            Vec3 to = e.position().subtract(position());
            if (to.horizontalDistance() > 3.4 || to.normalize().dot(f) < 0.1) continue;
            e.invulnerableTime = 0;   // 연속 베기가 무적 시간에 막히지 않게
            if (e.hurt(damageSources().playerAttack(rider), SLASH_DMG)) {
                hits++;
                e.knockback(0.12, -f.x, -f.z);
            }
        }
        playSound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.2f, 0.7f + random.nextFloat() * 0.2f);
    }

    @Override
    public boolean causeFallDamage(float dist, float mult, DamageSource src) {
        if (pouncing && !level().isClientSide) landPounce();
        else if (dist > 1.2f) playSound(SoundEvents.ANVIL_LAND, 0.4f, 1.5f);
        return false;
    }

    private void landPounce() {
        pouncing = false;
        Player rider = getControllingPassenger() instanceof Player p ? p : null;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(3.2, 1.5, 3.2),
                e -> e != this && e != rider && e.isAlive() && (rider == null || !e.isAlliedTo(rider)))) {
            e.invulnerableTime = 0;
            if (e.hurt(rider != null ? damageSources().playerAttack(rider) : damageSources().mobAttack(this), POUNCE_DMG)) {
                hits++;
                Vec3 k = e.position().subtract(position()).normalize();
                e.knockback(0.8, -k.x, -k.z);
            }
        }
        if (level() instanceof ServerLevel sl)
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, getX(), getY() + 0.2, getZ(), 2, 0.8, 0.1, 0.8, 0);
        playSound(SoundEvents.ANVIL_LAND, 1.2f, 0.8f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (slashCd > 0) slashCd--;
        if (dashCd > 0) dashCd--;
        if (pounceCd() > 0) entityData.set(POUNCE_T, pounceCd() - 1);
        if (pouncing && onGround() && pounceCd() < POUNCE_CD - 6) landPounce();
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        // 탑승자는 기체가 막아 줌 (일부만 전달)
        boolean r = super.hurt(src, amount);
        if (r && getControllingPassenger() instanceof Player p && !src.is(net.minecraft.tags.DamageTypeTags.IS_FALL))
            p.hurt(src, amount * 0.15f);
        return r;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel l, DamageSource s, boolean hit) {
        super.dropCustomDeathLoot(l, s, hit);
        spawnAtLocation(new ItemStack(ModItems.SCOUT_CORE.get(), 2));
    }

    @Override
    public void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (owner != null) t.putUUID("Owner", owner);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        if (t.hasUUID("Owner")) owner = t.getUUID("Owner");
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource s) {
        return SoundEvents.IRON_GOLEM_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.5f;
    }
}
