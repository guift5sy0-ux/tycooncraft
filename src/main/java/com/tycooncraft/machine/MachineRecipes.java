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
                r.add(MachineRecipe.of(Items.GUNPOWDER, 1).in(Items.FLINT).build());
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
                r.add(MachineRecipe.of(ModItems.GUN_BARREL.get(), 1).in(ModItems.STEEL_ROD.get()).build());
                Item[][] wood = {
                        {Items.OAK_LOG, Items.OAK_PLANKS}, {Items.SPRUCE_LOG, Items.SPRUCE_PLANKS},
                        {Items.BIRCH_LOG, Items.BIRCH_PLANKS}, {Items.JUNGLE_LOG, Items.JUNGLE_PLANKS},
                        {Items.ACACIA_LOG, Items.ACACIA_PLANKS}, {Items.DARK_OAK_LOG, Items.DARK_OAK_PLANKS},
                        {Items.CHERRY_LOG, Items.CHERRY_PLANKS}, {Items.MANGROVE_LOG, Items.MANGROVE_PLANKS}};
                for (Item[] w : wood) r.add(MachineRecipe.of(w[1], 6).in(w[0]).build());
            }
            case ASSEMBLER -> {
                // 순서 = 우선순위 (상위 부품 먼저) — 탈것이 맨 앞이어야 엔진 레시피가 재료를 가로채지 않음
                // 대형 병기 (엔진 레시피보다 먼저)
                r.add(MachineRecipe.of(ModItems.FIGHTER_JET.get(), 1)
                        .in(ModItems.ENGINE.get(), 4).in(ModItems.STEEL_PLATE.get(), 16).in(ModItems.ADVANCED_CIRCUIT.get(), 4)
                        .in(ModItems.MECHANISM.get(), 4).in(Items.GLASS, 6).in(ModItems.GUN_BARREL.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.TANK.get(), 1)
                        .in(ModItems.ENGINE.get(), 3).in(ModItems.STEEL_PLATE.get(), 24).in(ModItems.STEEL_GEAR.get(), 12)
                        .in(ModItems.GUN_BARREL.get(), 2).in(ModItems.MECHANISM.get(), 3).in(ModItems.ADVANCED_CIRCUIT.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.TURRET.get(), 1)
                        .in(ModItems.MECHANISM.get(), 2).in(ModItems.CIRCUIT.get(), 2).in(ModItems.STEEL_PLATE.get(), 4)
                        .in(ModItems.GUN_RECEIVER.get()).in(ModItems.GUN_BARREL.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.DRONE_MK2.get(), 1)
                        .in(ModItems.DRONE.get()).in(ModItems.ADVANCED_CIRCUIT.get(), 2).in(Items.TNT, 2).in(ModItems.STEEL_PLATE.get(), 2).in(ModItems.MECHANISM.get()).build());
                // 신형 총기·탄
                r.add(MachineRecipe.of(ModItems.VSS.get(), 1)
                        .in(ModItems.GUN_RECEIVER.get()).in(ModItems.GUN_BARREL.get()).in(ModItems.SILENCER.get()).in(ModItems.GUN_STOCK.get())
                        .in(Items.SPYGLASS).in(ModItems.STEEL_PLATE.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.RPG.get(), 1)
                        .in(ModItems.GUN_RECEIVER.get()).in(ModItems.STEEL_PLATE.get(), 4).in(ModItems.STEEL_ROD.get(), 2).in(ModItems.GUN_STOCK.get()).in(Items.SPYGLASS).build());
                r.add(MachineRecipe.of(ModItems.DUAL_PISTOLS.get(), 1).in(ModItems.PISTOL.get(), 2).in(ModItems.MECHANISM.get()).build());
                r.add(MachineRecipe.of(ModItems.ROCKET.get(), 2)
                        .in(ModItems.IRON_PLATE.get(), 2).in(Items.GUNPOWDER, 4).in(Items.TNT).in(ModItems.COPPER_WIRE.get()).build());
                r.add(MachineRecipe.of(ModItems.TANK_SHELL.get(), 2)
                        .in(ModItems.STEEL_PLATE.get()).in(Items.GUNPOWDER, 3).in(Items.TNT).in(ModItems.COPPER_PLATE.get()).build());
                r.add(MachineRecipe.of(ModItems.SILENCER_ADVANCED.get(), 1)
                        .in(ModItems.SILENCER.get()).in(ModItems.STEEL_PLATE.get(), 2).in(Items.BLACK_WOOL, 2).in(ModItems.GOLD_PLATE.get()).build());
                // 방탄 장비 & 낙하산 & 신호탄
                r.add(MachineRecipe.of(ModItems.VEST_3.get(), 1).in(ModItems.VEST_2.get()).in(ModItems.STEEL_PLATE.get(), 8).in(Items.DIAMOND, 2).build());
                r.add(MachineRecipe.of(ModItems.VEST_2.get(), 1).in(ModItems.VEST_1.get()).in(ModItems.STEEL_PLATE.get(), 4).in(Items.LEATHER, 2).build());
                r.add(MachineRecipe.of(ModItems.VEST_1.get(), 1).in(ModItems.IRON_PLATE.get(), 4).in(Items.LEATHER, 4).in(Items.STRING, 2).build());
                r.add(MachineRecipe.of(ModItems.HELMET_3.get(), 1).in(ModItems.HELMET_2.get()).in(ModItems.STEEL_PLATE.get(), 5).in(Items.GLASS_PANE, 2).in(Items.DIAMOND).build());
                r.add(MachineRecipe.of(ModItems.HELMET_2.get(), 1).in(ModItems.HELMET_1.get()).in(ModItems.STEEL_PLATE.get(), 3).build());
                r.add(MachineRecipe.of(ModItems.HELMET_1.get(), 1).in(ModItems.IRON_PLATE.get(), 3).in(Items.LEATHER, 2).build());
                r.add(MachineRecipe.of(ModItems.PARACHUTE.get(), 1).in(Items.STRING, 8).in(Items.WHITE_WOOL, 4).in(Items.RED_WOOL, 2).in(Items.LEATHER, 2).build());
                r.add(MachineRecipe.of(ModItems.CARTEL_FLARE.get(), 1).in(ModItems.GOLD_PLATE.get(), 2).in(Items.GUNPOWDER, 4).in(Items.REDSTONE, 2).in(ModItems.COIN_GOLD.get(), 5).build());
                // 탈것 (부품 6종까지)
                r.add(MachineRecipe.of(ModItems.HELICOPTER.get(), 1)
                        .in(ModItems.ENGINE.get(), 2).in(ModItems.STEEL_PLATE.get(), 10).in(ModItems.ADVANCED_CIRCUIT.get(), 3)
                        .in(ModItems.MECHANISM.get(), 3).in(ModItems.STEEL_ROD.get(), 8).in(Items.GLASS, 6).build());
                r.add(MachineRecipe.of(ModItems.LOCOMOTIVE.get(), 1)
                        .in(ModItems.ENGINE.get(), 2).in(ModItems.STEEL_PLATE.get(), 12).in(ModItems.STEEL_GEAR.get(), 8)
                        .in(ModItems.MECHANISM.get(), 2).in(ModItems.CIRCUIT.get(), 2).in(Items.GLASS, 4).build());
                r.add(MachineRecipe.of(ModItems.CAR.get(), 1)
                        .in(ModItems.ENGINE.get()).in(ModItems.STEEL_PLATE.get(), 6).in(ModItems.STEEL_GEAR.get(), 4)
                        .in(ModItems.CIRCUIT.get(), 2).in(Items.GLASS, 4).in(Items.BLACK_DYE, 4).build());
                r.add(MachineRecipe.of(ModItems.DRONE.get(), 1)
                        .in(ModItems.MECHANISM.get()).in(ModItems.CIRCUIT.get(), 2).in(ModItems.IRON_PLATE.get(), 4)
                        .in(Items.TNT).in(Items.GLASS_PANE).in(ModItems.COPPER_WIRE.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.SILENCER.get(), 1)
                        .in(ModItems.STEEL_PLATE.get()).in(ModItems.STEEL_ROD.get()).in(Items.WHITE_WOOL).build());
                r.add(MachineRecipe.of(ModItems.FUEL_CANISTER.get(), 2)
                        .in(ModItems.IRON_PLATE.get()).in(ModItems.COAL_DUST.get(), 4).build());
                r.add(MachineRecipe.of(ModItems.ENGINE.get(), 1)
                        .in(ModItems.STEEL_PLATE.get(), 4).in(ModItems.STEEL_GEAR.get(), 2).in(ModItems.MECHANISM.get()).build());
                r.add(MachineRecipe.of(ModItems.ADVANCED_CIRCUIT.get(), 1)
                        .in(ModItems.CIRCUIT.get(), 2).in(ModItems.GOLD_WIRE.get(), 2).in(ModItems.STEEL_PLATE.get()).build());
                r.add(MachineRecipe.of(ModItems.MECHANISM.get(), 1)
                        .in(ModItems.IRON_GEAR.get(), 2).in(ModItems.STEEL_ROD.get()).in(ModItems.CIRCUIT.get()).build());
                r.add(MachineRecipe.of(ModItems.CIRCUIT.get(), 1)
                        .in(ModItems.COPPER_WIRE.get(), 3).in(ModItems.IRON_PLATE.get()).in(Items.REDSTONE).build());
                // 총기 부품 & 탄약
                r.add(MachineRecipe.of(ModItems.GUN_RECEIVER.get(), 1)
                        .in(ModItems.STEEL_PLATE.get(), 2).in(ModItems.MECHANISM.get()).in(ModItems.IRON_GEAR.get()).build());
                r.add(MachineRecipe.of(ModItems.HEAVY_AMMO.get(), 8)
                        .in(ModItems.COPPER_PLATE.get()).in(Items.GUNPOWDER, 2).in(ModItems.STEEL_ROD.get()).build());
                r.add(MachineRecipe.of(ModItems.LIGHT_AMMO.get(), 16)
                        .in(ModItems.COPPER_PLATE.get()).in(Items.GUNPOWDER).in(ModItems.IRON_ROD.get()).build());
                r.add(MachineRecipe.of(ModItems.SHOTGUN_SHELL.get(), 8)
                        .in(ModItems.COPPER_PLATE.get()).in(Items.GUNPOWDER).in(ModItems.IRON_DUST.get(), 2).build());
                for (Item planks : new Item[]{Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS}) {
                    r.add(MachineRecipe.of(ModItems.GUN_STOCK.get(), 1).in(planks, 3).in(Items.LEATHER).build());
                }
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
