package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.block.ConveyorBlock;
import com.tycooncraft.block.ExchangeBlock;
import com.tycooncraft.block.LiftBlock;
import com.tycooncraft.block.MachineBlock;
import com.tycooncraft.block.SellStationBlock;
import com.tycooncraft.machine.MachineType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(TycoonCraft.MODID);

    // ── 판매 & 화폐 ──
    public static final DeferredBlock<SellStationBlock> SELL_STATION = BLOCKS.register("sell_station",
            () -> new SellStationBlock(machineProps().strength(3.0f, 6.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<ExchangeBlock> EXCHANGE = BLOCKS.register("exchange",
            () -> new ExchangeBlock(machineProps().strength(3.0f, 6.0f).requiresCorrectToolForDrops()));

    public static final DeferredBlock<com.tycooncraft.gun.GunWorkbenchBlock> GUN_WORKBENCH = BLOCKS.register("gun_workbench",
            () -> new com.tycooncraft.gun.GunWorkbenchBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD)
                    .strength(2.5f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));

    // ── v0.21.0 산업혁명 1단계: 증기 동력 ──
    public static final DeferredBlock<com.tycooncraft.power.BoilerBlock> BOILER = BLOCKS.register("boiler",
            () -> new com.tycooncraft.power.BoilerBlock(machineProps().strength(3.5f, 8.0f).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(com.tycooncraft.power.BoilerBlock.LIT) ? 10 : 0)));
    public static final DeferredBlock<com.tycooncraft.power.SteamEngineBlock> STEAM_ENGINE = BLOCKS.register("steam_engine",
            () -> new com.tycooncraft.power.SteamEngineBlock(machineProps().strength(3.5f, 8.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.power.ShaftBlock> SHAFT = BLOCKS.register("shaft",
            () -> new com.tycooncraft.power.ShaftBlock(machineProps().strength(1.5f, 4.0f).noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.power.GearboxBlock> POWER_GEARBOX = BLOCKS.register("power_gearbox",
            () -> new com.tycooncraft.power.GearboxBlock(machineProps().strength(2.5f, 6.0f).requiresCorrectToolForDrops()));
    public static final DeferredBlock<com.tycooncraft.cctv.CctvBlock> CCTV = BLOCKS.register("cctv",
            () -> new com.tycooncraft.cctv.CctvBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL)
                    .strength(1.5f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.cctv.ScreenBlock> CCTV_SCREEN = BLOCKS.register("cctv_screen",
            () -> new com.tycooncraft.cctv.ScreenBlock(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.COLOR_BLACK)
                    .strength(1.5f).sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion().lightLevel(s -> 7)));

    // ── 운반 ──
    public static final DeferredBlock<ConveyorBlock> CONVEYOR = BLOCKS.register("conveyor",
            () -> new ConveyorBlock(machineProps().strength(1.0f).noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.block.TransmissionBlock> TRANSMISSION = BLOCKS.register("transmission",
            () -> new com.tycooncraft.block.TransmissionBlock(machineProps().strength(1.5f).noOcclusion()));
    // ── 배 ──
    public static final DeferredBlock<com.tycooncraft.ship.ShipMotorBlock> SHIP_MOTOR = BLOCKS.register("ship_motor",
            () -> new com.tycooncraft.ship.ShipMotorBlock(machineProps().strength(2.0f, 6.0f)));
    public static final DeferredBlock<com.tycooncraft.ship.ShipHelmBlock> SHIP_HELM = BLOCKS.register("ship_helm",
            () -> new com.tycooncraft.ship.ShipHelmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.5f).noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.setter.SetterBlock> SETTER = BLOCKS.register("setter",
            () -> new com.tycooncraft.setter.SetterBlock(machineProps().strength(3.0f, 6.0f)));
    public static final DeferredBlock<com.tycooncraft.ship.ShipBoosterBlock> SHIP_BOOSTER = BLOCKS.register("ship_booster",
            () -> new com.tycooncraft.ship.ShipBoosterBlock(machineProps().strength(2.5f, 6.0f)));
    public static final DeferredBlock<com.tycooncraft.ship.ShipFloatBlock> SHIP_FLOAT = BLOCKS.register("ship_float",
            () -> new com.tycooncraft.ship.ShipFloatBlock(machineProps().strength(1.5f, 4.0f)));
    public static final DeferredBlock<com.tycooncraft.machine.EmpGeneratorBlock> EMP_GENERATOR = BLOCKS.register("emp_generator",
            () -> new com.tycooncraft.machine.EmpGeneratorBlock(machineProps().strength(4.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(s -> s.getValue(com.tycooncraft.machine.EmpGeneratorBlock.ACTIVE) ? 12 : 0)));
    public static final DeferredBlock<LiftBlock> LIFT_UP = BLOCKS.register("lift_up",
            () -> new LiftBlock(machineProps().strength(1.5f), true));
    public static final DeferredBlock<LiftBlock> LIFT_DOWN = BLOCKS.register("lift_down",
            () -> new LiftBlock(machineProps().strength(1.5f), false));

    // ── 기계 ──
    public static final Map<MachineType, DeferredBlock<MachineBlock>> MACHINES = new EnumMap<>(MachineType.class);
    static {
        for (MachineType type : MachineType.values()) {
            MACHINES.put(type, BLOCKS.register(type.getSerializedName(),
                    () -> new MachineBlock(type, machineProps().strength(2.5f, 6.0f).requiresCorrectToolForDrops())));
        }
    }

    // ── 폐공장 장식 ──
    public static final DeferredBlock<com.tycooncraft.turret.CiwsBlock> CIWS = BLOCKS.register("ciws",
            () -> new com.tycooncraft.turret.CiwsBlock(machineProps().strength(5.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<com.tycooncraft.turret.TurretBlock> TURRET = BLOCKS.register("turret",
            () -> new com.tycooncraft.turret.TurretBlock(machineProps().strength(4.0f, 12.0f).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> SULFUR_ORE = BLOCKS.register("sulfur_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(1, 3),
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.COAL_ORE)));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> DEEPSLATE_SULFUR_ORE = BLOCKS.register("deepslate_sulfur_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(1, 3),
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE)));

    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> BAUXITE_ORE = BLOCKS.register("bauxite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2),
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_ORE)));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = BLOCKS.register("deepslate_bauxite_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2),
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE)));
    public static final DeferredBlock<net.minecraft.world.level.block.DropExperienceBlock> ROCK_SALT_ORE = BLOCKS.register("rock_salt_ore",
            () -> new net.minecraft.world.level.block.DropExperienceBlock(net.minecraft.util.valueproviders.UniformInt.of(0, 2),
                    net.minecraft.world.level.block.state.BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.IRON_ORE)));

    public static final DeferredBlock<Block> BROKEN_MACHINE = BLOCKS.registerSimpleBlock("broken_machine",
            machineProps().strength(3.0f, 6.0f).requiresCorrectToolForDrops());

    public static MachineBlock machine(MachineType type) {
        return MACHINES.get(type).get();
    }

    private static BlockBehaviour.Properties machineProps() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).sound(SoundType.METAL);
    }

    private ModBlocks() {}
}
