package com.tycooncraft.power;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class GearboxBlockEntity extends RotatingBlockEntity {
    public GearboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GEARBOX.get(), pos, state);
    }

    @Override
    public boolean conducts(Direction d) {
        return true;
    }
}
