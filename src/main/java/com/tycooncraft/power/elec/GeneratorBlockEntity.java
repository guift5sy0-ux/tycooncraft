package com.tycooncraft.power.elec;

import com.tycooncraft.power.Layer;
import com.tycooncraft.power.PowerGrid;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 발전기: 회전축·증기기관에 붙이면 동력 32 P 를 전기 30 ⚡ 로 바꾼다 (손실 약 6%).
 * 전력망에 쓸 곳(기계·빈 축전지)이 있을 때만 동력을 끌어 쓴다.
 */
public class GeneratorBlockEntity extends GridBlockEntity {
    public static final int DRAW = 32, OUTPUT = 30;
    private int draw, out;

    public GeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GENERATOR.get(), pos, state);
    }

    @Override
    public boolean in(Layer l) {
        return true;
    }

    @Override
    public boolean conducts(Layer l, Direction d) {
        return l == Layer.ELEC;
    }

    /** 축 쪽에선 기계처럼 동력을 받는 쪽 */
    @Override
    public boolean consumer(Layer l) {
        return l == Layer.MECH;
    }

    @Override
    public int demand(Layer l) {
        return l == Layer.MECH ? draw : 0;
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
        if (level == null) return;
        var e = PowerGrid.net(level, worldPosition, Layer.ELEC);
        // 이번 틱 낸 전기 = 지난 틱 끌어온 동력 × 충족 비율
        float mr = PowerGrid.ratio(level, worldPosition, Layer.MECH);
        out = draw <= 0 ? 0 : Math.round(OUTPUT * (draw / (float) DRAW) * mr);
        // 다른 발전원이 못 채우는 만큼만 끌어옴
        int others = e == null ? 0 : e.supply - e.discharged - out;
        int need = e == null ? 0 : e.want - Math.max(0, others);
        draw = need <= 0 ? 0 : Math.min(DRAW, (int) Math.ceil(need * DRAW / (float) OUTPUT));
        BlockState s = getBlockState();
        boolean on = out > 0;
        if (s.hasProperty(GeneratorBlock.ACTIVE) && s.getValue(GeneratorBlock.ACTIVE) != on)
            level.setBlock(worldPosition, s.setValue(GeneratorBlock.ACTIVE, on), 3);
    }
}
