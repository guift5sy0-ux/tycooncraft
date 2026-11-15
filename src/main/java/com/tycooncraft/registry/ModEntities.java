package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.HelicopterEntity;
import com.tycooncraft.vehicle.LocomotiveEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 탈것 엔티티 */
public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, TycoonCraft.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<CarEntity>> CAR = ENTITIES.register("car",
            () -> EntityType.Builder.<CarEntity>of(CarEntity::new, MobCategory.MISC)
                    .sized(2.4f, 1.8f).clientTrackingRange(10).updateInterval(1).build("car"));

    public static final DeferredHolder<EntityType<?>, EntityType<HelicopterEntity>> HELICOPTER = ENTITIES.register("helicopter",
            () -> EntityType.Builder.<HelicopterEntity>of(HelicopterEntity::new, MobCategory.MISC)
                    .sized(2.2f, 2.8f).clientTrackingRange(16).updateInterval(1).build("helicopter"));

    public static final DeferredHolder<EntityType<?>, EntityType<LocomotiveEntity>> LOCOMOTIVE = ENTITIES.register("locomotive",
            () -> EntityType.Builder.<LocomotiveEntity>of(LocomotiveEntity::new, MobCategory.MISC)
                    .sized(0.98f, 2.6f).passengerAttachments(0.94f).clientTrackingRange(10).build("locomotive"));

    private ModEntities() {}
}
