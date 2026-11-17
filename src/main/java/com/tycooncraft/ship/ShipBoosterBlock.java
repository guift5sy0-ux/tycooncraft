package com.tycooncraft.ship;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;

/** 운송기 부스터: 엔진 2.5개만큼 밀고 최고 속도 한계를 올린다 — 대신 연료를 3배 먹는다 (탱크·호퍼는 엔진과 같음) */
public class ShipBoosterBlock extends ShipMotorBlock {
    public static final MapCodec<ShipBoosterBlock> CODEC = simpleCodec(ShipBoosterBlock::new);

    public ShipBoosterBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }
}
