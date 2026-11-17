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
                r.add(MachineRecipe.of(ModItems.BAUXITE_DUST.get(), 2).in(ModItems.RAW_BAUXITE.get()).build());
                r.add(MachineRecipe.of(ModItems.SALT.get(), 3).in(ModItems.ROCK_SALT.get()).build());
                r.add(MachineRecipe.of(ModItems.LIMESTONE_DUST.get(), 2).in(Items.CALCITE).build());
                r.add(MachineRecipe.of(ModItems.SILICA_DUST.get(), 2).in(Items.QUARTZ).build());
                r.add(MachineRecipe.of(ModItems.SILICA_DUST.get(), 1).in(Items.SAND).build());
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
                r.add(MachineRecipe.of(ModItems.COKE.get(), 1).in(Items.COAL).build());
                r.add(MachineRecipe.of(ModItems.GRAPHITE.get(), 1).in(ModItems.COKE.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.QUICKLIME.get(), 1).in(ModItems.LIMESTONE_DUST.get()).build());
                r.add(MachineRecipe.of(Items.GLASS, 1).in(ModItems.SILICA_DUST.get()).build());
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
            case ALLOY_FURNACE -> {
                r.add(MachineRecipe.of(ModItems.STEEL_INGOT.get(), 2).in(Items.IRON_INGOT, 2).in(ModItems.COKE.get()).in(ModItems.QUICKLIME.get()).build());
                r.add(MachineRecipe.of(ModItems.SILICON.get(), 1).in(ModItems.SILICA_DUST.get(), 2).in(ModItems.COKE.get()).build());
                r.add(MachineRecipe.of(ModItems.RUBBER.get(), 2).in(ModItems.LATEX.get(), 2).in(ModItems.SULFUR.get()).build());
                r.add(MachineRecipe.of(ModItems.CEMENT.get(), 3).in(ModItems.LIMESTONE_DUST.get(), 2).in(Items.CLAY_BALL).build());
                r.add(MachineRecipe.of(ModItems.STEEL_INGOT.get(), 1).in(Items.IRON_INGOT).in(ModItems.COAL_DUST.get(), 2).build());
            }
            case PRESS -> {
                r.add(MachineRecipe.of(ModItems.ALUMINUM_PLATE.get(), 1).in(ModItems.ALUMINUM_INGOT.get()).build());
                r.add(MachineRecipe.of(ModItems.ALUMINUM_FOIL.get(), 4).in(ModItems.ALUMINUM_PLATE.get()).build());
                r.add(MachineRecipe.of(ModItems.COPPER_FOIL.get(), 4).in(ModItems.COPPER_PLATE.get()).build());
                r.add(MachineRecipe.of(ModItems.REBAR.get(), 1).in(ModItems.STEEL_ROD.get()).build());
                r.add(MachineRecipe.of(ModItems.H_BEAM.get(), 1).in(ModItems.STEEL_PLATE.get(), 3).build());
                r.add(MachineRecipe.of(ModItems.GASKET.get(), 4).in(ModItems.RUBBER.get()).build());
                r.add(MachineRecipe.of(ModItems.LATEX.get(), 2).in(Items.JUNGLE_LOG).build());
                r.add(MachineRecipe.of(ModItems.IRON_PLATE.get(), 1).in(Items.IRON_INGOT).build());
                r.add(MachineRecipe.of(ModItems.COPPER_PLATE.get(), 1).in(Items.COPPER_INGOT).build());
                r.add(MachineRecipe.of(ModItems.GOLD_PLATE.get(), 1).in(Items.GOLD_INGOT).build());
                r.add(MachineRecipe.of(ModItems.STEEL_PLATE.get(), 1).in(ModItems.STEEL_INGOT.get()).build());
            }
            case WIRE_DRAWER -> {
                r.add(MachineRecipe.of(ModItems.FIBERGLASS.get(), 3).in(Items.GLASS).build());
                r.add(MachineRecipe.of(ModItems.STEEL_PIPE.get(), 2).in(ModItems.STEEL_PLATE.get()).build());
                r.add(MachineRecipe.of(ModItems.COPPER_WIRE.get(), 3).in(Items.COPPER_INGOT).build());
                r.add(MachineRecipe.of(ModItems.GOLD_WIRE.get(), 3).in(Items.GOLD_INGOT).build());
                r.add(MachineRecipe.of(ModItems.IRON_ROD.get(), 2).in(Items.IRON_INGOT).build());
                r.add(MachineRecipe.of(ModItems.STEEL_ROD.get(), 2).in(ModItems.STEEL_INGOT.get()).build());
            }
            case LATHE -> {
                r.add(MachineRecipe.of(ModItems.BOLT.get(), 4).in(ModItems.IRON_ROD.get()).build());
                r.add(MachineRecipe.of(ModItems.BALL_BEARING.get(), 2).in(ModItems.STEEL_INGOT.get()).build());
                r.add(MachineRecipe.of(ModItems.PISTON.get(), 1).in(ModItems.ALUMINUM_INGOT.get()).build());
                r.add(MachineRecipe.of(ModItems.SILICON_WAFER.get(), 4).in(ModItems.SILICON.get()).build());
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
                // ── 산업 완제품·부품 (신규 재료가 들어가 기존 레시피와 겹치지 않음) ──
                r.add(MachineRecipe.of(ModItems.INDUSTRIAL_ROBOT_ARM.get(), 1).in(ModItems.ELECTRIC_MOTOR.get(), 3).in(ModItems.GEARBOX.get(), 2).in(ModItems.CONTROL_BOARD.get()).in(ModItems.HYDRAULIC_CYLINDER.get(), 2).in(ModItems.ALUMINUM_PLATE.get(), 4).in(ModItems.BOLT.get(), 8).build());
                r.add(MachineRecipe.of(ModItems.TRANSFORMER.get(), 1).in(ModItems.COPPER_COIL.get(), 2).in(ModItems.STEEL_PLATE.get(), 4).in(ModItems.FIBERGLASS.get(), 2).in(ModItems.INSULATED_CABLE.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.SOLAR_PANEL.get(), 1).in(ModItems.SILICON_WAFER.get(), 4).in(ModItems.COPPER_FOIL.get()).in(ModItems.ALUMINUM_PLATE.get(), 2).in(Items.GLASS_PANE, 2).in(ModItems.EPOXY_RESIN.get()).build());
                r.add(MachineRecipe.of(ModItems.CONTROL_BOARD.get(), 1).in(ModItems.PCB.get()).in(ModItems.MICROCHIP.get()).in(ModItems.RESISTOR.get(), 4).in(ModItems.CAPACITOR.get(), 2).in(Items.REDSTONE).build());
                r.add(MachineRecipe.of(ModItems.ELECTRIC_MOTOR.get(), 1).in(ModItems.COPPER_COIL.get(), 2).in(ModItems.STEEL_PLATE.get(), 2).in(ModItems.DRIVE_SHAFT.get()).in(ModItems.GRAPHITE.get()).build());
                r.add(MachineRecipe.of(ModItems.GEARBOX.get(), 1).in(ModItems.STEEL_GEAR.get(), 4).in(ModItems.DRIVE_SHAFT.get()).in(ModItems.ALUMINUM_PLATE.get(), 2).in(ModItems.BOLT.get(), 4).build());
                r.add(MachineRecipe.of(ModItems.HYDRAULIC_CYLINDER.get(), 1).in(ModItems.STEEL_PIPE.get()).in(ModItems.PISTON.get()).in(ModItems.GASKET.get(), 2).in(ModItems.STEEL_ROD.get()).in(ModItems.VALVE.get()).build());
                r.add(MachineRecipe.of(ModItems.BATTERY_CELL.get(), 2).in(ModItems.ALUMINUM_FOIL.get()).in(ModItems.COPPER_FOIL.get()).in(ModItems.GRAPHITE.get(), 2).in(ModItems.PLASTIC_PELLET.get()).in(ModItems.CAUSTIC_SODA.get()).build());
                r.add(MachineRecipe.of(ModItems.GAS_CYLINDER.get(), 1).in(ModItems.STEEL_PIPE.get(), 2).in(ModItems.VALVE.get()).build());
                r.add(MachineRecipe.of(ModItems.VALVE.get(), 1).in(ModItems.STEEL_PIPE.get()).in(ModItems.BOLT.get(), 2).in(ModItems.GASKET.get()).build());
                r.add(MachineRecipe.of(ModItems.DRIVE_SHAFT.get(), 1).in(ModItems.STEEL_ROD.get(), 2).in(ModItems.BALL_BEARING.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.COPPER_COIL.get(), 1).in(ModItems.COPPER_WIRE.get(), 8).in(ModItems.EPOXY_RESIN.get()).build());
                r.add(MachineRecipe.of(ModItems.PRECAST_PANEL.get(), 2).in(ModItems.CEMENT.get(), 2).in(ModItems.REBAR.get(), 2).in(Items.GRAVEL, 2).in(Items.SAND).build());
                r.add(MachineRecipe.of(ModItems.CAPACITOR.get(), 4).in(ModItems.ALUMINUM_FOIL.get(), 2).in(Items.PAPER).in(ModItems.COPPER_WIRE.get()).build());
                r.add(MachineRecipe.of(ModItems.RESISTOR.get(), 8).in(ModItems.GRAPHITE.get()).in(ModItems.COPPER_WIRE.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.INSULATED_CABLE.get(), 4).in(ModItems.COPPER_WIRE.get(), 4).in(ModItems.PLASTIC_PELLET.get()).build());
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
                // 중장갑 세트
                r.add(MachineRecipe.of(ModItems.HELMET_4.get(), 1).in(ModItems.HELMET_3.get()).in(ModItems.STEEL_PLATE.get(), 6).in(Items.NETHERITE_SCRAP).in(Items.GLOWSTONE_DUST, 2).build());
                r.add(MachineRecipe.of(ModItems.VEST_4.get(), 1).in(ModItems.VEST_3.get()).in(ModItems.STEEL_PLATE.get(), 10).in(Items.NETHERITE_SCRAP, 2).build());
                r.add(MachineRecipe.of(ModItems.JUGGERNAUT_LEGGINGS.get(), 1).in(ModItems.STEEL_PLATE.get(), 7).in(Items.LEATHER, 3).in(Items.DIAMOND).build());
                r.add(MachineRecipe.of(ModItems.COMBAT_BOOTS.get(), 1).in(ModItems.STEEL_PLATE.get(), 4).in(Items.LEATHER, 4).in(Items.SLIME_BALL, 2).build());
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
            case CHEMICAL_REACTOR -> {
                r.add(MachineRecipe.of(ModItems.PCB.get(), 2).in(ModItems.FIBERGLASS.get(), 2).in(ModItems.EPOXY_RESIN.get()).in(ModItems.COPPER_FOIL.get()).in(ModItems.SULFURIC_ACID.get()).build());
                r.add(MachineRecipe.of(ModItems.MICROCHIP.get(), 2).in(ModItems.SILICON_WAFER.get()).in(ModItems.SULFURIC_ACID.get()).in(ModItems.GOLD_WIRE.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.ALUMINA.get(), 2).in(ModItems.BAUXITE_DUST.get(), 2).in(ModItems.CAUSTIC_SODA.get()).build());
                r.add(MachineRecipe.of(ModItems.EPOXY_RESIN.get(), 8).in(ModItems.PLASTIC_PELLET.get(), 2).in(ModItems.CHLORINE_CYLINDER.get()).build());
                r.add(MachineRecipe.of(ModItems.PLASTIC_PELLET.get(), 4).in(ModItems.FUEL_CANISTER.get()).in(ModItems.COAL_DUST.get(), 2).build());
                r.add(MachineRecipe.of(ModItems.SULFURIC_ACID.get(), 1).in(ModItems.SULFUR.get(), 2).in(Items.GLASS_BOTTLE).build());
            }
            case ELECTROLYZER -> {
                r.add(MachineRecipe.of(ModItems.CHLORINE_CYLINDER.get(), 1).in(ModItems.SALT.get(), 3).in(ModItems.GAS_CYLINDER.get()).build());
                r.add(MachineRecipe.of(ModItems.CAUSTIC_SODA.get(), 1).in(ModItems.SALT.get(), 2).in(Items.GLASS_BOTTLE).build());
                r.add(MachineRecipe.of(ModItems.ALUMINUM_INGOT.get(), 1).in(ModItems.ALUMINA.get(), 2).in(ModItems.COKE.get()).build());
            }
        }
        return List.copyOf(r);
    }

    private MachineRecipes() {}
}
