package com.tycooncraft.power;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class ShaftBlockEntity extends RotatingBlockEntity {
    public ShaftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHAFT.get(), pos, state);
    }

    @Override
    public boolean conducts(Direction d) {
        return d.getAxis() == getBlockState().getValue(ShaftBlock.AXIS);
    }

    @Override
    public boolean drives(Direction d) {
        return true;
    }
}
