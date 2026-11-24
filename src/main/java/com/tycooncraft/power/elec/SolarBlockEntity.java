package com.tycooncraft.power.elec;

import com.tycooncraft.power.Layer;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** 태양광 발전기: 낮에 하늘이 보이면 8 ⚡, 비 오면 3, 밤엔 0 */
public class SolarBlockEntity extends GridBlockEntity {
    public static final int OUTPUT = 8;
    private int out;

    public SolarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_GENERATOR.get(), pos, state);
    }

    @Override
    public int supply(Layer l) {
        return l == Layer.ELEC ? out : 0;
    }

    public int output() {
        return out;
    }

    @Override
    public void serverTick() {
        if (level == null || (level.getGameTime() + worldPosition.asLong()) % 20 != 0) return;
        long t = level.getDayTime() % 24000;
        boolean day = t < 12300 || t > 23700;
        boolean sky = level.canSeeSky(worldPosition.above());
        out = !day || !sky ? 0 : level.isRaining() ? 3 : OUTPUT;
    }
}
