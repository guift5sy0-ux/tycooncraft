package com.tycooncraft.vehicle;

import com.tycooncraft.registry.ModItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 디젤 기관차 "TC-01": 레일 위에서만 달린다. W = 바라보는 방향으로 가속, S = 브레이크, 스페이스 = 기적.
 * 웅크리고 우클릭 = 화물칸(27칸).
 */
public class LocomotiveEntity extends AbstractMinecart implements FueledVehicle {
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(LocomotiveEntity.class, EntityDataSerializers.INT);
    public static final int MAX_FUEL = 24000;

    private final SimpleContainer cargo = new SimpleContainer(27);
    /** 클라이언트 표시: 바퀴 회전 */
    public float wheelAngle, wheelAngleO;

    public LocomotiveEntity(EntityType<? extends LocomotiveEntity> type, Level level) {
        super(type, level);
    }

    @Override
    public Type getMinecartType() {
        return Type.RIDEABLE;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.LOCOMOTIVE.get();
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.LOCOMOTIVE.get());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FUEL, 0);
    }

    @Override
    public int getFuel() {
        return entityData.get(FUEL);
    }

    public void setFuel(int fuel) {
        entityData.set(FUEL, Mth.clamp(fuel, 0, MAX_FUEL));
    }

    @Override
    public int maxFuel() {
        return MAX_FUEL;
    }

    @Override
    public float healthFraction() {
        return Mth.clamp(1f - getDamage() / 40f, 0f, 1f);
    }

    @Override
    public String hudName() {
        return "entity.tycooncraft.locomotive";
    }

    public boolean engineRunning() {
        return getFirstPassenger() instanceof Player && getFuel() > 0;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && getFirstPassenger() instanceof Player p && getFuel() > 0) {
            Vec3 dm = getDeltaMovement();
            if (p.zza > 0) {
                Vec3 look = p.getLookAngle();
                Vec3 dir = new Vec3(look.x, 0, look.z);
                if (dir.lengthSqr() > 1.0E-4) setDeltaMovement(dm.add(dir.normalize().scale(0.02)));
            } else if (p.zza < 0) {
                setDeltaMovement(dm.scale(0.85));
            }
            if (dm.horizontalDistanceSqr() > 1.0E-4) setFuel(getFuel() - 1);
        }
        super.tick();
        if (level().isClientSide) VehicleHooks.client.tick(this);
    }

    /** 연료가 있고 사람이 타고 있으면 일반 카트보다 빠르게 */
    @Override
    public double getMaxSpeedWithRail() {
        return engineRunning() ? 0.7 : super.getMaxSpeedWithRail();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (Fuel.value(stack) > 0) {
            if (!level().isClientSide) Fuel.refuel(this, player, hand, getFuel(), MAX_FUEL, this::setFuel);
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (player.isSecondaryUseActive()) {
            if (!level().isClientSide) {
                player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, cargo),
                        Component.translatable("container.tycooncraft.locomotive_cargo")));
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide) return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide && reason.shouldDestroy()) Containers.dropContents(level(), this, cargo);
        super.remove(reason);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Fuel", getFuel());
        tag.put("Cargo", cargo.createTag(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setFuel(tag.getInt("Fuel"));
        cargo.fromTag(tag.getList("Cargo", Tag.TAG_COMPOUND), registryAccess());
    }
}
