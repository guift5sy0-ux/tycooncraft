package com.tycooncraft.machine;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 기계 종류. 레시피는 MachineRecipes 에 정의 */
public enum MachineType implements StringRepresentable {
    CRUSHER("crusher", 20),
    SMELTER("smelter", 20),
    ALLOY_FURNACE("alloy_furnace", 40),
    PRESS("press", 20),
    WIRE_DRAWER("wire_drawer", 20),
    LATHE("lathe", 30),
    ASSEMBLER("assembler", 40),
    CHEMICAL_REACTOR("chemical_reactor", 40),
    ELECTROLYZER("electrolyzer", 60),
    // v0.22.0 전기 시대: 전기로만 도는 고급 기계
    INDUCTION_FURNACE("induction_furnace", 12),
    ARC_FURNACE("arc_furnace", 30);

    public static final Codec<MachineType> CODEC = StringRepresentable.fromEnum(MachineType::values);

    private final String name;
    private final int processTime;
    private List<MachineRecipe> recipes;
    private Set<Item> accepted;

    MachineType(String name, int processTime) {
        this.name = name;
        this.processTime = processTime;
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /** 1개 가공 시간 (틱) */
    public int processTime() {
        return processTime;
    }

    public List<MachineRecipe> recipes() {
        if (recipes == null) recipes = MachineRecipes.build(this);
        return recipes;
    }

    public boolean accepts(Item item) {
        if (accepted == null) {
            Set<Item> set = new HashSet<>();
            for (MachineRecipe r : recipes()) set.addAll(r.inputs().keySet());
            accepted = set;
        }
        return accepted.contains(item);
    }

    public String translationKey() {
        return "block.tycooncraft." + name;
    }

    /** v0.21.0: 일할 때 필요한 동력 (P/t). 증기기관 하나 = 32 */
    public int power() {
        return switch (this) {
            case CRUSHER, SMELTER, WIRE_DRAWER -> 8;
            case PRESS, LATHE -> 12;
            case ALLOY_FURNACE, ASSEMBLER, CHEMICAL_REACTOR -> 16;
            case ELECTROLYZER -> 24;
            case INDUCTION_FURNACE -> 32;
            case ARC_FURNACE -> 48;
        };
    }

    /** v0.22.0: 전선으로만 돌릴 수 있는 기계 */
    public boolean electricOnly() {
        return this == INDUCTION_FURNACE || this == ARC_FURNACE;
    }
}
