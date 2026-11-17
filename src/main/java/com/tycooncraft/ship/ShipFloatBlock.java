package com.tycooncraft.ship;

import net.minecraft.world.level.block.Block;

/** 부유체: 운송기에 붙이면 하늘로 띄운다. 1개당 {@link ShipFuel#LIFT_PER_FLOAT} 블록까지. 떠 있는 동안 연료를 더 먹는다 */
public class ShipFloatBlock extends Block {
    public ShipFloatBlock(Properties p) {
        super(p);
    }
}
