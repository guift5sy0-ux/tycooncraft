package com.tycooncraft.power.elec;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 전선: 전기만 이어 준다 (손실 없음) */
public class CableBlockEntity extends GridBlockEntity {
    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_CABLE.get(), pos, state);
    }
}
