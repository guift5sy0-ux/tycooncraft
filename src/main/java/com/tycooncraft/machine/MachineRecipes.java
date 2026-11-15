package com.tycooncraft.machine;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * 기계 레시피.
 * 분쇄기: 원석 → 가루 2 / 제련기: 가루 → 주괴 / 합금로: 철 주괴 + 석탄 가루 2 → 강철
 * 압착기: 주괴 → 판 / 인발기: 주괴 → 선·봉 / 절삭기: 판 → 기어, 원목 → 판자 / 조립기: 조립품
 */
public final class MachineRecipes {

    static List<MachineRecipe> build(MachineType type) {
        List<MachineRecipe> r = new ArrayList<>();
        switch (type) {
            case CRUSHER -> {
                r.add(MachineRecipe.of(ModItems.IRON_DUST.get(), 2).in(Items.RAW_IRON).build());
                r.add(MachineRecipe.of(ModItems.COPPER_DUST.get(), 2).in(Items.RAW_COPPER).build());
                r.add(MachineRecipe.of(ModItems.GOLD_DUST.get(), 2).in(Items.RAW_GOLD).build());
                r.add(MachineRecipe.of(ModItems.IRON_DUST.get(), 2).in(Items.IRON_ORE).build());
                r.add(MachineRecipe.of(ModItems.IRON_DUST.get(), 2).in(Items.DEEPSLATE_IRON_ORE).build());
                r.add(MachineRecipe.of(ModItems.COPPER_DUST.get(), 4).in(Items.COPPER_ORE).build());
                r.add(MachineRecipe.of(ModItems.COPPER_DUST.get(), 4).in(Items.DEEPSLATE_COPPER_ORE).build());
                r.add(MachineRecipe.of(ModItems.GOLD_DUST.get(), 2).in(Items.GOLD_ORE).build());
                r.add(MachineRecipe.of(ModItems.GOLD_DUST.get(), 2).in(Items.DEEPSLATE_GOLD_ORE).build());
                r.add(MachineRecipe.of(ModItems.COAL_DUST.get(), 2).in(Items.COAL).build());
                r.add(MachineRecipe.of(ModItems.FLOUR.get(), 2).in(Items.WHEAT).build());
                r.add(MachineRecipe.of(Items.BONE_MEAL, 6).in(Items.BONE).build());
                r.add(MachineRecipe.of(Items.GRAVEL, 1).in(Items.COBBLESTONE).build());
                r.add(MachineRecipe.of(Items.SAND, 1).in(Items.GRAVEL).build());
            }
            case SMELTER -> {
                r.add(MachineRecipe.of(Items.IRON_INGOT, 1).in(ModItems.IRON_DUST.get()).build());
                r.add(MachineRecipe.of(Items.COPPER_INGOT, 1).in(ModItems.COPPER_DUST.get()).build());
                r.add(MachineRecipe.of(Items.GOLD_INGOT, 1).in(ModItems.GOLD_DUST.get()).build());
                r.add(MachineRecipe.of(Items.IRON_INGOT, 1).in(Items.RAW_IRON).build());
                r.add(MachineRecipe.of(Items.COPPER_INGOT, 1).in(Items.RAW_COPPER).build());
                r.add(MachineRecipe.of(Items.GOLD_INGOT, 1).in(Items.RAW_GOLD).build());
                r.add(MachineRecipe.of(Items.BREAD, 1).in(ModItems.FLOUR.get()).build());
                r.add(MachineRecipe.of(Items.GLASS, 1).in(Items.SAND).build());
                r.add(MachineRecipe.of(Items.STONE, 1).in(Items.COBBLESTONE).build());
                r.add(MachineRecipe.of(Items.BRICK, 1).in(Items.CLAY_BALL).build());
                r.add(MachineRecipe.of(Items.BAKED_POTATO, 1).in(Items.POTATO).build());
            }
            case ALLOY_FURNACE ->
                    r.add(MachineRecipe.of(ModItems.STEEL_INGOT.get(), 1).in(Items.IRON_INGOT).in(ModItems.COAL_DUST.get(), 2).build());
            case PRESS -> {
                r.add(MachineRecipe.of(ModItems.IRON_PLATE.get(), 1).in(Items.IRON_INGOT).build());
                r.add(MachineRecipe.of(ModItems.COPPER_PLATE.get(), 1).in(Items.COPPER_INGOT).build());
                r.add(MachineRecipe.of(ModItems.GOLD_PLATE.get(), 1).in(Items.GOLD_INGOT).build());
                r.add(MachineRecipe.of(ModItems.STEEL_PLATE.get(), 1).in(ModItems.STEEL_INGOT.get()).build());
            }
            case WIRE_DRAWER -> {
                r.add(MachineRecipe.of(ModItems.COPPER_WIRE.get(), 3).in(Items.COPPER_INGOT).build());
                r.add(MachineRecipe.of(ModItems.GOLD_WIRE.get(), 3).in(Items.GOLD_INGOT).build());
                r.add(MachineRecipe.of(ModItems.IRON_ROD.get(), 2).in(Items.IRON_INGOT).build());
                r.add(MachineRecipe.of(ModItems.STEEL_ROD.get(), 2).in(ModItems.STEEL_INGOT.get()).build());
            }
            case LATHE -> {
                r.add(MachineRecipe.of(ModItems.IRON_GEAR.get(), 1).in(ModItems.IRON_PLATE.get()).build());
                r.add(MachineRecipe.of(ModItems.STEEL_GEAR.get(), 1).in(ModItems.STEEL_PLATE.get()).build());
                Item[][] wood = {
                        {Items.OAK_LOG, Items.OAK_PLANKS}, {Items.SPRUCE_LOG, Items.SPRUCE_PLANKS},
                        {Items.BIRCH_LOG, Items.BIRCH_PLANKS}, {Items.JUNGLE_LOG, Items.JUNGLE_PLANKS},
                        {Items.ACACIA_LOG, Items.ACACIA_PLANKS}, {Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS},
                        {Items.CHERRY_LOG, Items.CHERRY_PLANKS}, {Items.MANGROVE_LOG, Items.MANGROVE_PLANKS}};
                for (Item[] w : wood) r.add(MachineRecipe.of(w[1], 6).in(w[0]).build());
            }
            case ASSEMBLER -> {
                // 순서 = 우선순위 (상위 부품 먼저)
                r.add(MachineRecipe.of(ModItems.ENGINE.get(), 1)
                        .in(ModItems.STEEL_PLATE.get(), 4).in(ModItems.STEEL_GEAR.get(), 2).in(ModItems.MECHANISM.get()).build());
                r.add(MachineRecipe.of(ModItems.ADVANCED_CIRCUIT.get(), 1)
                        .in(ModItems.CIRCUIT.get(), 2).in(ModItems.GOLD_WIRE.get(), 2).in(ModItems.STEEL_PLATE.get()).build());
                r.add(MachineRecipe.of(ModItems.MECHANISM.get(), 1)
                        .in(ModItems.IRON_GEAR.get(), 2).in(ModItems.STEEL_ROD.get()).in(ModItems.CIRCUIT.get()).build());
                r.add(MachineRecipe.of(ModItems.CIRCUIT.get(), 1)
                        .in(ModItems.COPPER_WIRE.get(), 3).in(ModItems.IRON_PLATE.get()).in(Items.REDSTONE).build());
                r.add(MachineRecipe.of(Items.CAKE, 1).in(ModItems.FLOUR.get(), 3).in(Items.SUGAR, 2).in(Items.EGG).build());
                r.add(MachineRecipe.of(Items.COOKIE, 8).in(ModItems.FLOUR.get(), 2).in(Items.COCOA_BEANS).build());
                for (Item planks : new Item[]{Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS}) {
                    r.add(MachineRecipe.of(ModItems.WOODEN_CRATE.get(), 1).in(planks, 4).in(ModItems.IRON_ROD.get()).build());
                }
            }
        }
        return List.copyOf(r);
    }

    private MachineRecipes() {}
}
