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
 * v0.28.1: 정찰 디스트로이어와 같은 크기 · 타지 않고 드론처럼 원격 조종 (최대 80블록, 빈손 우클릭 시작 · Shift 끝)
 * v0.27.1 프로토 디스트로이어-정찰: 정찰 코어로 만드는 탑승형 사족 기계. 저글링처럼 빠르고 변칙적.
 * 조작: WASD (빠름, 즉각 반응) · 좌클릭(누르고 있기) 양날 베기 · 우클릭 덮치기(앞으로 크게 도약, 착지 시 주변 피해)
 *       · 스페이스 높은 점프 · 걷는 중 A/D 를 짧게 두 번 = 옆 회피 대시 (클라이언트) · Shift 내리기
 * 탄약이 필요 없음 (근접 기계). 철/강철로 수리, 주인이 웅크리고 빈손 우클릭 → 아이템으로 회수.
 */
public class ProtoScout extends PathfinderMob {
    public static final int ACT_SLASH = 1, ACT_POUNCE = 2, ACT_JUMP = 4, ACT_DASH = 5;
    public static final int MAX_HP = 120, SLASH_CD = 7, POUNCE_CD = 50, DASH_CD = 24;
    public static final float SLASH_DMG = 7f, POUNCE_DMG = 10f;
    public static final double JUMP_VEL = 0.62, RANGE = 80;
    private static final EntityDataAccessor<Integer> PILOT = SynchedEntityData.defineId(ProtoScout.class, EntityDataSerializers.INT);
    /** 원격 조종자 */
    @Nullable private ServerPlayer pilot;
    private float inFwd, inStrafe, inYaw, inPitch;
    private boolean inJump, inSlash, jumpWas;
    private int lastInput;
    private static final EntityDataAccessor<Integer> POUNCE_T = SynchedEntityData.defineId(ProtoScout.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SLASH_AT = SynchedEntityData.defineId(ProtoScout.class, EntityDataSerializers.INT);
    @Nullable private UUID owner;
    private int slashCd, dashCd;
    private boolean pouncing;
    public static int slashes, pounces, hits;

    public ProtoScout(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        // 조종 중에는 기본 이동 제어가 입력(zza)을 지우지 않게
        moveControl = new net.minecraft.world.entity.ai.control.MoveControl(this) {
            @Override
            public void tick() {
                if (pilot == null) super.tick();
            }
        };
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
        b.define(POUNCE_T, 0).define(SLASH_AT, -100).define(PILOT, -1);
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
        startRemote((ServerPlayer) player);
        return InteractionResult.CONSUME;
    }

    // ── 원격 조종 ──
    public static int links;

    public void startRemote(ServerPlayer sp) {
        if (pilot != null && pilot != sp) stopRemote();
        pilot = sp;
        lastInput = tickCount;
        entityData.set(PILOT, sp.getId());
        links++;
        ScoutRemote.link(sp, getId());
        playSound(SoundEvents.BEACON_ACTIVATE, 0.8f, 1.8f);
    }

    public void stopRemote() {
        if (pilot != null) ScoutRemote.link(pilot, -1);
        pilot = null;
        entityData.set(PILOT, -1);
        inFwd = inStrafe = 0;
        inJump = inSlash = false;
        setZza(0);
        setXxa(0);
    }

    @Nullable
    public ServerPlayer pilot() {
        return pilot;
    }

    /** 클라이언트: 조종자 엔티티 id */
    public int pilotId() {
        return entityData.get(PILOT);
    }

    public void remoteInput(ServerPlayer sp, ScoutRemote.Input in) {
        if (sp != pilot) return;
        if (in.act() == ScoutRemote.Input.STOP) {
            stopRemote();
            return;
        }
        lastInput = tickCount;
        if (tickCount < testHold) return;
        inFwd = Mth.clamp(in.fwd(), -1, 1);
        inStrafe = Mth.clamp(in.strafe(), -1, 1);
        inJump = in.jump();
        inYaw = in.yaw();
        inPitch = in.pitch();
        inSlash = in.slash();
        if (in.act() != 0) action(sp, in.act());
    }

    /** 테스트용: 입력 직접 넣기 */
    private int testHold;

    public void testInput(float fwd, float strafe, float yaw) {
        lastInput = tickCount;
        testHold = tickCount + 4;
        inFwd = fwd;
        inStrafe = strafe;
        inYaw = yaw;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (pilot == null) return;
        if (!pilot.isAlive() || pilot.level() != level() || pilot.distanceTo(this) > RANGE || tickCount - lastInput > 40) {
            if (pilot.isAlive() && pilot.distanceTo(this) > RANGE)
                pilot.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tycooncraft.proto_scout.signal_lost"), true);
            stopRemote();
            return;
        }
        float d = Mth.wrapDegrees(inYaw - getYRot());
        setYRot(getYRot() + Mth.clamp(d, -18f, 18f));   // 날렵하게 돎
        yBodyRot = yHeadRot = getYRot();
        setXRot(inPitch * 0.5f);
        setSpeed((float) getAttributeValue(Attributes.MOVEMENT_SPEED));
        setZza(inFwd < 0 ? inFwd * 0.6f : inFwd);
        setXxa(inStrafe * 0.8f);
        if (inJump && !jumpWas && onGround()) {
            Vec3 v = getDeltaMovement();
            setDeltaMovement(v.x, JUMP_VEL, v.z);
            hasImpulse = true;
            playSound(SoundEvents.PISTON_EXTEND, 0.8f, 1.7f);
        }
        jumpWas = inJump;
        if (inSlash) slash(pilot);
    }

    @Override
    public void remove(RemovalReason r) {
        if (pilot != null) ScoutRemote.link(pilot, -1);
        super.remove(r);
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
    public boolean canBeLeashed() { return false; }

    @Override
    public boolean removeWhenFarAway(double d) { return false; }

    public Vec3 forward() {
        float yaw = yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    public void action(ServerPlayer rider, int act) {
        if (pilot != rider || !isAlive()) return;
        switch (act) {
            case ACT_SLASH -> slash(rider);
            case ACT_POUNCE -> {
                if (pounceCd() > 0 || !onGround()) return;
                Vec3 f = forward();
                setDeltaMovement(f.x * 1.3, 0.55, f.z * 1.3);   // 바라보는 쪽으로 크게 도약
                hasImpulse = true;
                entityData.set(POUNCE_T, POUNCE_CD);
                pouncing = true;
                pounces++;
                playSound(SoundEvents.RAVAGER_ROAR, 1.2f, 1.8f);
            }
            case ACT_DASH, ACT_DASH + 10 -> {
                if (dashCd > 0 || !onGround()) return;
                dashCd = DASH_CD;
                int side = act == ACT_DASH ? 1 : -1;   // 5 = 왼쪽, 15 = 오른쪽
                float yaw = getYRot() * Mth.DEG_TO_RAD;
                setDeltaMovement(Mth.cos(yaw) * 1.3 * side, 0.22, Mth.sin(yaw) * 1.3 * side);
                hasImpulse = true;
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
        AABB box = getBoundingBox().inflate(2.6, 1.0, 2.6);
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box, e -> e != this && e != rider && e.isAlive() && !e.isAlliedTo(rider))) {
            Vec3 to = e.position().subtract(position());
            if (to.horizontalDistance() > 2.8 || to.normalize().dot(f) < 0.1) continue;
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
        Player rider = pilot;
        for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(2.6, 1.3, 2.6),
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
        return super.hurt(src, amount);
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
