package com.tycooncraft.ship;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/** 운송기 연료: 아이템 1개 = 연료 단위. 연료통이 가장 효율적, 그 밖엔 화로 연료 시간 그대로 (양동이처럼 빈 그릇이 남는 건 제외) */
public final class ShipFuel {
    /** 엔진 1개가 담을 수 있는 연료 */
    public static final int MOTOR_CAPACITY = 64_000;

    public static int value(ItemStack s) {
        if (s.isEmpty()) return 0;
        if (s.is(ModItems.FUEL_CANISTER.get())) return 12_000;
        if (s.hasCraftingRemainingItem()) return 0;
        return Math.max(0, s.getBurnTime(RecipeType.SMELTING));
    }

    /** 블록 수가 많을수록 많이 먹는다 (틱당) — 추진: 최고속 기준, 부양: 공중에 떠 있는 동안 */
    public static double thrustUse(int blocks) {
        return 0.3 + blocks / 120.0;
    }

    public static double liftUse(int blocks) {
        return 0.2 + blocks / 200.0;
    }

    /** 부유체 1개가 띄울 수 있는 블록 수 (자기 포함) */
    public static final int LIFT_PER_FLOAT = 40;

    private ShipFuel() {}
}
