package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.blockentity.LiftBlockEntity;
import com.tycooncraft.blockentity.MachineBlockEntity;
import com.tycooncraft.blockentity.SellStationBlockEntity;
import com.tycooncraft.machine.MachineType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TycoonCraft.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.turret.CiwsBlockEntity>> CIWS =
            BLOCK_ENTITY_TYPES.register("ciws",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.turret.CiwsBlockEntity::new, ModBlocks.CIWS.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.BoilerBlockEntity>> BOILER =
            BLOCK_ENTITY_TYPES.register("boiler", () -> BlockEntityType.Builder.of(com.tycooncraft.power.BoilerBlockEntity::new, ModBlocks.BOILER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.SteamEngineBlockEntity>> STEAM_ENGINE =
            BLOCK_ENTITY_TYPES.register("steam_engine", () -> BlockEntityType.Builder.of(com.tycooncraft.power.SteamEngineBlockEntity::new, ModBlocks.STEAM_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.elec.GeneratorBlockEntity>> GENERATOR =
            BLOCK_ENTITY_TYPES.register("generator", () -> BlockEntityType.Builder.of(com.tycooncraft.power.elec.GeneratorBlockEntity::new, ModBlocks.GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.elec.MotorBlockEntity>> ELECTRIC_MOTOR =
            BLOCK_ENTITY_TYPES.register("power_motor", () -> BlockEntityType.Builder.of(com.tycooncraft.power.elec.MotorBlockEntity::new, ModBlocks.ELECTRIC_MOTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.elec.CableBlockEntity>> POWER_CABLE =
            BLOCK_ENTITY_TYPES.register("power_cable", () -> BlockEntityType.Builder.of(com.tycooncraft.power.elec.CableBlockEntity::new, ModBlocks.POWER_CABLE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.elec.SolarBlockEntity>> SOLAR_GENERATOR =
            BLOCK_ENTITY_TYPES.register("solar_generator", () -> BlockEntityType.Builder.of(com.tycooncraft.power.elec.SolarBlockEntity::new, ModBlocks.SOLAR_GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.elec.AccumulatorBlockEntity>> ACCUMULATOR =
            BLOCK_ENTITY_TYPES.register("accumulator", () -> BlockEntityType.Builder.of(com.tycooncraft.power.elec.AccumulatorBlockEntity::new, ModBlocks.ACCUMULATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.ShaftBlockEntity>> SHAFT =
            BLOCK_ENTITY_TYPES.register("shaft", () -> BlockEntityType.Builder.of(com.tycooncraft.power.ShaftBlockEntity::new, ModBlocks.SHAFT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.power.GearboxBlockEntity>> GEARBOX =
            BLOCK_ENTITY_TYPES.register("power_gearbox", () -> BlockEntityType.Builder.of(com.tycooncraft.power.GearboxBlockEntity::new, ModBlocks.POWER_GEARBOX.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.cctv.CctvBlockEntity>> CCTV =
            BLOCK_ENTITY_TYPES.register("cctv",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.cctv.CctvBlockEntity::new, ModBlocks.CCTV.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.tycooncraft.cctv.ScreenBlockEntity>> CCTV_SCREEN =
            BLOCK_ENTITY_TYPES.register("cctv_screen",
                    () -> BlockEntityType.Builder.of(com.tycooncraft.cctv.ScreenBlockEntity::new, ModBlocks.CCTV_SCREEN.get()).build(null));
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
                    () -> BlockEntityType.Builder.of(com.tycooncraft.ship.ShipMotorBlockEntity::new, ModBlocks.SHIP_MOTOR.get(), ModBlocks.SHIP_BOOSTER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SellStationBlockEntity>> SELL_STATION =
            BLOCK_ENTITY_TYPES.register("sell_station",
                    () -> BlockEntityType.Builder.of(SellStationBlockEntity::new, ModBlocks.SELL_STATION.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LiftBlockEntity>> LIFT =
            BLOCK_ENTITY_TYPES.register("lift",
                    () -> BlockEntityType.Builder.of(LiftBlockEntity::new, ModBlocks.LIFT_UP.get(), ModBlocks.LIFT_DOWN.get()).build(null));

    /** 모든 기계가 하나의 블록 엔티티 타입을 공유 (블록이 기계 종류를 알려줌) */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE =
            BLOCK_ENTITY_TYPES.register("machine", () -> {
                Block[] blocks = new Block[MachineType.values().length];
                for (int i = 0; i < blocks.length; i++) blocks[i] = ModBlocks.machine(MachineType.values()[i]);
                return BlockEntityType.Builder.of(MachineBlockEntity::new, blocks).build(null);
            });

    private ModBlockEntities() {}
}
