package com.tycooncraft.power.elec;

import com.tycooncraft.power.Layer;
import com.tycooncraft.power.PowerGrid;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 전동기: 전기 32 ⚡ 로 축 동력 28 P 를 만든다 — 멀리 떨어진 옛 기계(축 방식)를 전선으로 돌릴 때.
 * 발전기와 맞물려도 손실이 있어 무한 동력은 안 됨.
 */
public class MotorBlockEntity extends GridBlockEntity {
    public static final int DRAW = 32, OUTPUT = 28;
    private int draw, out;

    public MotorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_MOTOR.get(), pos, state);
    }

    @Override
    public boolean in(Layer l) {
        return true;
    }

    /** 축 쪽: 증기기관처럼 여섯 면 모두로 동력을 냄. 전선 쪽: 소비자 */
    @Override
    public boolean conducts(Layer l, Direction d) {
        return l == Layer.MECH;
    }

    @Override
    public boolean consumer(Layer l) {
        return l == Layer.ELEC;
    }

    @Override
    public int demand(Layer l) {
        return l == Layer.ELEC ? draw : 0;
    }

    @Override
    public int supply(Layer l) {
        return l == Layer.MECH ? out : 0;
    }

    public int output() {
        return out;
    }

    @Override
    public void serverTick() {
        if (level == null) return;
        var m = PowerGrid.net(level, worldPosition, Layer.MECH);
        float er = PowerGrid.ratio(level, worldPosition, Layer.ELEC);
        out = draw <= 0 ? 0 : Math.round(OUTPUT * (draw / (float) DRAW) * er);
        int others = m == null ? 0 : m.supply - out;
        int need = m == null ? 0 : m.demand - Math.max(0, others);
        draw = need <= 0 ? 0 : Math.min(DRAW, (int) Math.ceil(need * DRAW / (float) OUTPUT));
        BlockState s = getBlockState();
        boolean on = out > 0;
        if (s.hasProperty(GeneratorBlock.ACTIVE) && s.getValue(GeneratorBlock.ACTIVE) != on)
            level.setBlock(worldPosition, s.setValue(GeneratorBlock.ACTIVE, on), 3);
    }
}
