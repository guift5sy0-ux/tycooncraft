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

    // ── 운반 ──
    public static final DeferredBlock<ConveyorBlock> CONVEYOR = BLOCKS.register("conveyor",
            () -> new ConveyorBlock(machineProps().strength(1.0f).noOcclusion()));
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
