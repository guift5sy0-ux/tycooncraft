package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.blockentity.LiftBlockEntity;
import com.tycooncraft.blockentity.SellStationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TycoonCraft.MODID);

    public static final java.util.function.Supplier<BlockEntityType<com.tycooncraft.blackmarket.LockedChestBlockEntity>> LOCKED_CHEST =
            BLOCK_ENTITY_TYPES.register("locked_chest", () -> BlockEntityType.Builder.of(com.tycooncraft.blackmarket.LockedChestBlockEntity::new, ModBlocks.LOCKED_CHEST.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.turret.CiwsBlockEntity>> CIWS =
            BLOCK_ENTITY_TYPES.register("ciws",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.turret.CiwsBlockEntity::new, ModBlocks.CIWS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.cctv.CctvBlockEntity>> CCTV =
            BLOCK_ENTITY_TYPES.register("cctv",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.cctv.CctvBlockEntity::new, ModBlocks.CCTV.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.cctv.ScreenBlockEntity>> CCTV_SCREEN =
            BLOCK_ENTITY_TYPES.register("cctv_screen",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.cctv.ScreenBlockEntity::new, ModBlocks.CCTV_SCREEN.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.ship.DroneBayBlockEntity>> DRONE_BAY =
            BLOCK_ENTITY_TYPES.register("drone_bay",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.ship.DroneBayBlockEntity::new, ModBlocks.DRONE_SWARM.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.turret.TurretBlockEntity>> TURRET =
            BLOCK_ENTITY_TYPES.register("turret",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.turret.TurretBlockEntity::new, ModBlocks.TURRET.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.machine.EmpGeneratorBlockEntity>> EMP_GENERATOR =
            BLOCK_ENTITY_TYPES.register("emp_generator",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.machine.EmpGeneratorBlockEntity::new, ModBlocks.EMP_GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.setter.SetterBlockEntity>> SETTER =
            BLOCK_ENTITY_TYPES.register("setter",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.setter.SetterBlockEntity::new, ModBlocks.SETTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.ship.ShipMotorBlockEntity>> SHIP_MOTOR =
            BLOCK_ENTITY_TYPES.register("ship_motor",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.ship.ShipMotorBlockEntity::new, ModBlocks.SHIP_MOTOR.get(), ModBlocks.SHIP_BOOSTER.get(), ModBlocks.SHIP_CORE_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SellStationBlockEntity>> SELL_STATION =
            BLOCK_ENTITY_TYPES.register("sell_station",
                    () -> BlockEntityType.Builder.of(SellStationBlockEntity::new, ModBlocks.SELL_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiftBlockEntity>> LIFT =
            BLOCK_ENTITY_TYPES.register("lift",
                    () -> BlockEntityType.Builder.of(LiftBlockEntity::new, ModBlocks.LIFT_UP.get(), ModBlocks.LIFT_DOWN.get()).build(null));


    private ModBlockEntities() {}
}
