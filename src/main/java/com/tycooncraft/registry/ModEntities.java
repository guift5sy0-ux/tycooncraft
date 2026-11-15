package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.TankEntity;
import com.tycooncraft.vehicle.JetEntity;
import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.mob.GunnerEntity;
import com.tycooncraft.mob.CartelBoss;
import com.tycooncraft.mob.MutantSpider;
import com.tycooncraft.vehicle.DroneEntity;
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

    public static final DeferredHolder<EntityType<?>, EntityType<DroneEntity>> DRONE = ENTITIES.register("drone",
            () -> EntityType.Builder.<DroneEntity>of(DroneEntity::new, MobCategory.MISC)
                    .sized(0.8f, 0.45f).clientTrackingRange(12).updateInterval(1).build("drone"));

    public static final DeferredHolder<EntityType<?>, EntityType<TankEntity>> TANK = ENTITIES.register("tank",
            () -> EntityType.Builder.<TankEntity>of(TankEntity::new, MobCategory.MISC)
                    .sized(3.4f, 2.4f).clientTrackingRange(10).updateInterval(1).build("tank"));
    public static final DeferredHolder<EntityType<?>, EntityType<JetEntity>> FIGHTER_JET = ENTITIES.register("fighter_jet",
            () -> EntityType.Builder.<JetEntity>of(JetEntity::new, MobCategory.MISC)
                    .sized(3.4f, 2.0f).clientTrackingRange(16).updateInterval(1).build("fighter_jet"));
    public static final DeferredHolder<EntityType<?>, EntityType<RocketEntity>> ROCKET = ENTITIES.register("rocket",
            () -> EntityType.Builder.<RocketEntity>of(RocketEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(10).updateInterval(1).build("rocket"));

    // 몹
    public static final DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> GANGSTER = mob("gangster", GunnerEntity::gangster);
    public static final DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> MERCENARY = mob("mercenary", GunnerEntity::mercenary);
    public static final DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> OUTLAW = mob("outlaw", GunnerEntity::outlaw);
    public static final DeferredHolder<EntityType<?>, EntityType<CartelBoss>> CARTEL_BOSS = ENTITIES.register("cartel_boss",
            () -> EntityType.Builder.<CartelBoss>of(CartelBoss::new, MobCategory.MONSTER)
                    .sized(0.84f, 2.7f).eyeHeight(2.3f).clientTrackingRange(10).fireImmune().build("cartel_boss"));
    public static final DeferredHolder<EntityType<?>, EntityType<MutantSpider>> MUTANT_SPIDER = ENTITIES.register("mutant_spider",
            () -> EntityType.Builder.<MutantSpider>of(MutantSpider::new, MobCategory.MONSTER)
                    .sized(4.2f, 2.7f).clientTrackingRange(10).build("mutant_spider"));

    private static DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> mob(String name, EntityType.EntityFactory<GunnerEntity> f) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(f, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build(name));
    }

    private ModEntities() {}
}
