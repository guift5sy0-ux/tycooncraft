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
