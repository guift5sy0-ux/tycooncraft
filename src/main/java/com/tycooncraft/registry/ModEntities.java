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

    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.miner.AutoMinerEntity>> AUTO_MINER = ENTITIES.register("auto_miner",
            () -> EntityType.Builder.<com.tycooncraft.miner.AutoMinerEntity>of(com.tycooncraft.miner.AutoMinerEntity::new, MobCategory.MISC)
                    .sized(1.9f, 1.9f).fireImmune().clientTrackingRange(10).build("auto_miner"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.entity.GrenadeEntity>> GRENADE = ENTITIES.register("grenade",
            () -> EntityType.Builder.<com.tycooncraft.entity.GrenadeEntity>of(com.tycooncraft.entity.GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(8).updateInterval(2).build("grenade"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.ship.ShipEntity>> SHIP = ENTITIES.register("ship",
            () -> EntityType.Builder.<com.tycooncraft.ship.ShipEntity>of(com.tycooncraft.ship.ShipEntity::new, MobCategory.MISC)
                    .sized(1f, 1f).clientTrackingRange(12).updateInterval(1).fireImmune().build("ship"));

    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.entity.SpiderShot>> SPIDER_SHOT = ENTITIES.register("spider_shot",
            () -> EntityType.Builder.<com.tycooncraft.entity.SpiderShot>of(com.tycooncraft.entity.SpiderShot::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(8).updateInterval(1).build("spider_shot"));

    // 몹
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.village.VillageGuard>> VILLAGE_GUARD = ENTITIES.register("village_guard",
            () -> EntityType.Builder.<com.tycooncraft.village.VillageGuard>of(com.tycooncraft.village.VillageGuard::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).build("village_guard"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.blackmarket.BlackMarketDealer>> BM_DEALER = ENTITIES.register("bm_dealer",
            () -> EntityType.Builder.<com.tycooncraft.blackmarket.BlackMarketDealer>of(com.tycooncraft.blackmarket.BlackMarketDealer::new, MobCategory.MISC)
                    .sized(0.6f, 1.95f).clientTrackingRange(10).build("bm_dealer"));
    // v0.27.0 디스트로이어 변종: 공중 모체 + 정찰 + 미니 드론
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mob.carrier.ScoutDestroyer>> SCOUT_DESTROYER = ENTITIES.register("scout_destroyer",
            () -> EntityType.Builder.<com.tycooncraft.mob.carrier.ScoutDestroyer>of(com.tycooncraft.mob.carrier.ScoutDestroyer::new, MobCategory.MONSTER)
                    .sized(0.95f, 0.85f).clientTrackingRange(10).build("scout_destroyer"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mob.carrier.AerialDestroyer>> AERIAL_DESTROYER = ENTITIES.register("aerial_destroyer",
            () -> EntityType.Builder.<com.tycooncraft.mob.carrier.AerialDestroyer>of(com.tycooncraft.mob.carrier.AerialDestroyer::new, MobCategory.MONSTER)
                    .sized(5.0f, 2.4f).clientTrackingRange(16).fireImmune().build("aerial_destroyer"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mech.PilotBody>> PILOT_BODY = ENTITIES.register("pilot_body",
            () -> EntityType.Builder.<com.tycooncraft.mech.PilotBody>of(com.tycooncraft.mech.PilotBody::new, MobCategory.MISC)
                    .sized(0.6f, 1.8f).clientTrackingRange(10).noSave().build("pilot_body"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mob.carrier.MiniDrone>> MINI_DRONE = ENTITIES.register("mini_drone",
            () -> EntityType.Builder.<com.tycooncraft.mob.carrier.MiniDrone>of(com.tycooncraft.mob.carrier.MiniDrone::new, MobCategory.MONSTER)
                    .sized(0.6f, 0.35f).clientTrackingRange(10).build("mini_drone"));
    public static final DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> GANGSTER = mob("gangster", GunnerEntity::gangster);
    public static final DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> MERCENARY = mob("mercenary", GunnerEntity::mercenary);
    public static final DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> OUTLAW = mob("outlaw", GunnerEntity::outlaw);
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mob.GangTruck>> GANG_TRUCK = ENTITIES.register("gang_truck",
            () -> EntityType.Builder.<com.tycooncraft.mob.GangTruck>of(com.tycooncraft.mob.GangTruck::new, MobCategory.MONSTER)
                    .sized(2.4f, 2.0f).eyeHeight(1.8f).passengerAttachments(2.0f).clientTrackingRange(10).build("gang_truck"));
    public static final DeferredHolder<EntityType<?>, EntityType<CartelBoss>> CARTEL_BOSS = ENTITIES.register("cartel_boss",
            () -> EntityType.Builder.<CartelBoss>of(CartelBoss::new, MobCategory.MONSTER)
                    .sized(0.84f, 2.7f).eyeHeight(2.3f).clientTrackingRange(10).fireImmune().build("cartel_boss"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mob.DestroyerGolem>> DESTROYER_GOLEM = ENTITIES.register("destroyer_golem",
            () -> EntityType.Builder.<com.tycooncraft.mob.DestroyerGolem>of(com.tycooncraft.mob.DestroyerGolem::new, MobCategory.MONSTER)
                    .sized(2.2f, 3.4f).clientTrackingRange(10).build("destroyer_golem"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mech.ProtoDestroyer>> PROTO_DESTROYER = ENTITIES.register("proto_destroyer",
            () -> EntityType.Builder.<com.tycooncraft.mech.ProtoDestroyer>of(com.tycooncraft.mech.ProtoDestroyer::new, MobCategory.MISC)
                    .sized(2.8f, 4.3f).eyeHeight(3.6f).clientTrackingRange(10).fireImmune().build("proto_destroyer"));
    public static final DeferredHolder<EntityType<?>, EntityType<com.tycooncraft.mech.ProtoScout>> PROTO_SCOUT = ENTITIES.register("proto_scout",
            () -> EntityType.Builder.<com.tycooncraft.mech.ProtoScout>of(com.tycooncraft.mech.ProtoScout::new, MobCategory.MISC)
                    .sized(0.95f, 0.85f).clientTrackingRange(10).fireImmune().build("proto_scout"));
    public static final DeferredHolder<EntityType<?>, EntityType<MutantSpider>> MUTANT_SPIDER = ENTITIES.register("mutant_spider",
            () -> EntityType.Builder.<MutantSpider>of(MutantSpider::new, MobCategory.MONSTER)
                    .sized(4.2f, 2.7f).clientTrackingRange(10).build("mutant_spider"));

    private static DeferredHolder<EntityType<?>, EntityType<GunnerEntity>> mob(String name, EntityType.EntityFactory<GunnerEntity> f) {
        return ENTITIES.register(name, () -> EntityType.Builder.of(f, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8).build(name));
    }

    private ModEntities() {}
}
