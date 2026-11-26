package com.tycooncraft.mech;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.UUID;

/**
 * v0.28.2 원격 조종 중 제자리에 남는 "내 몸". 조종자 스킨으로 보이고, 저장되지 않음.
 * 몸이 공격받으면 그 피해가 조종자에게 가고 원격 조종이 끊김 (몸으로 돌아옴).
 */
public class PilotBody extends PathfinderMob {
    private static final EntityDataAccessor<Optional<UUID>> OWNER = SynchedEntityData.defineId(PilotBody.class, EntityDataSerializers.OPTIONAL_UUID);
    private int mechId = -1;
    public static int hitsForwarded;

    public PilotBody(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder attributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, 20).add(Attributes.MOVEMENT_SPEED, 0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(OWNER, Optional.empty());
    }

    @Override
    protected void registerGoals() {}

    public void setup(ServerPlayer p, ProtoScout mech) {
        entityData.set(OWNER, Optional.of(p.getUUID()));
        mechId = mech.getId();
        moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot());
        setYBodyRot(p.getYRot());
        setYHeadRot(p.getYHeadRot());
        for (EquipmentSlot s : EquipmentSlot.values()) {
            setItemSlot(s, p.getItemBySlot(s).copy());
            setDropChance(s, 0);
        }
        setCustomName(Component.translatable("entity.tycooncraft.pilot_body.name", p.getGameProfile().getName()).withStyle(ChatFormatting.GRAY));
        setCustomNameVisible(true);
    }

    public Optional<UUID> owner() {
        return entityData.get(OWNER);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || tickCount % 10 != 0) return;
        ProtoScout m = owner().map(ProtoScout.ACTIVE::get).orElse(null);
        if (m == null || m.isRemoved() || m.pilot() == null) discard();
    }

    @Override
    public boolean hurt(DamageSource src, float amount) {
        if (level().isClientSide || isInvulnerableTo(src)) return false;
        ProtoScout m = owner().map(ProtoScout.ACTIVE::get).orElse(null);
        if (m != null && m.pilot() != null) {
            ServerPlayer p = m.pilot();
            m.stopRemote();
            p.displayClientMessage(Component.translatable("message.tycooncraft.proto_scout.body_hit").withStyle(ChatFormatting.RED), true);
            hitsForwarded++;
            p.hurt(src, amount);
        }
        discard();
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double d) {
        return false;
    }
}
