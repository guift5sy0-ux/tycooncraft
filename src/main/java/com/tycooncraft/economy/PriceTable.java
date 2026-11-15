package com.tycooncraft.economy;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 판매 기준가 (₩). 가공할수록 비싸게:
 * 원석 < 가루(2배 생산) < 주괴 < 판·선·봉 < 기어 < 조립품
 */
public final class PriceTable {
    private static Map<Item, Double> prices;

    public static Map<Item, Double> all() {
        if (prices == null) {
            Map<Item, Double> p = new LinkedHashMap<>();
            // 원자재
            p.put(Items.COBBLESTONE, 0.1);
            p.put(Items.STONE, 0.2);
            p.put(Items.GRAVEL, 0.3);
            p.put(Items.SAND, 0.5);
            p.put(Items.CLAY_BALL, 0.5);
            for (Item log : new Item[]{Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
                    Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.CHERRY_LOG, Items.MANGROVE_LOG}) p.put(log, 1.0);
            for (Item plank : new Item[]{Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS, Items.JUNGLE_PLANKS,
                    Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS, Items.CHERRY_PLANKS, Items.MANGROVE_PLANKS}) p.put(plank, 0.3);
            p.put(Items.COAL, 2.0);
            p.put(Items.RAW_COPPER, 2.0);
            p.put(Items.RAW_IRON, 5.0);
            p.put(Items.RAW_GOLD, 12.0);
            p.put(Items.REDSTONE, 3.0);
            p.put(Items.LAPIS_LAZULI, 4.0);
            p.put(Items.QUARTZ, 4.0);
            p.put(Items.EMERALD, 80.0);
            p.put(Items.DIAMOND, 150.0);
            // 1차: 가루
            p.put(ModItems.COAL_DUST.get(), 1.5);
            p.put(ModItems.COPPER_DUST.get(), 1.5);
            p.put(ModItems.IRON_DUST.get(), 3.5);
            p.put(ModItems.GOLD_DUST.get(), 8.0);
            p.put(ModItems.FLOUR.get(), 0.8);
            // 2차: 주괴
            p.put(Items.COPPER_INGOT, 4.0);
            p.put(Items.IRON_INGOT, 8.0);
            p.put(Items.GOLD_INGOT, 20.0);
            p.put(ModItems.STEEL_INGOT.get(), 22.0);
            p.put(Items.GLASS, 1.0);
            p.put(Items.BRICK, 1.0);
            // 3차: 판·선·봉
            p.put(ModItems.COPPER_PLATE.get(), 7.0);
            p.put(ModItems.IRON_PLATE.get(), 14.0);
            p.put(ModItems.GOLD_PLATE.get(), 34.0);
            p.put(ModItems.STEEL_PLATE.get(), 38.0);
            p.put(ModItems.COPPER_WIRE.get(), 2.0);
            p.put(ModItems.GOLD_WIRE.get(), 9.0);
            p.put(ModItems.IRON_ROD.get(), 6.0);
            p.put(ModItems.STEEL_ROD.get(), 15.0);
            // 4차: 기어
            p.put(ModItems.IRON_GEAR.get(), 22.0);
            p.put(ModItems.STEEL_GEAR.get(), 60.0);
            // 5차: 조립품
            p.put(ModItems.CIRCUIT.get(), 45.0);
            p.put(ModItems.MECHANISM.get(), 160.0);
            p.put(ModItems.ADVANCED_CIRCUIT.get(), 230.0);
            p.put(ModItems.ENGINE.get(), 700.0);
            p.put(ModItems.WOODEN_CRATE.get(), 15.0);
            // 총기 부품·탄약·총
            p.put(ModItems.GUN_BARREL.get(), 40.0);
            p.put(ModItems.GUN_RECEIVER.get(), 300.0);
            p.put(ModItems.GUN_STOCK.get(), 10.0);
            p.put(ModItems.LIGHT_AMMO.get(), 1.0);
            p.put(ModItems.HEAVY_AMMO.get(), 3.0);
            p.put(ModItems.SHOTGUN_SHELL.get(), 2.0);
            p.put(ModItems.PISTOL.get(), 400.0);
            p.put(ModItems.SMG.get(), 600.0);
            p.put(ModItems.SHOTGUN.get(), 900.0);
            p.put(ModItems.RIFLE.get(), 1200.0);
            p.put(ModItems.SNIPER.get(), 1600.0);
            // 농산물·식품
            p.put(Items.WHEAT, 1.0);
            p.put(Items.POTATO, 0.8);
            p.put(Items.CARROT, 0.8);
            p.put(Items.SUGAR_CANE, 0.5);
            p.put(Items.SUGAR, 0.5);
            p.put(Items.EGG, 1.0);
            p.put(Items.COCOA_BEANS, 1.0);
            p.put(Items.BREAD, 4.0);
            p.put(Items.BAKED_POTATO, 2.0);
            p.put(Items.COOKIE, 1.5);
            p.put(Items.PUMPKIN_PIE, 8.0);
            p.put(Items.CAKE, 40.0);
            prices = p;
        }
        return prices;
    }

    /** 0이면 판매 불가 */
    public static double basePrice(Item item) {
        return all().getOrDefault(item, 0.0);
    }

    private PriceTable() {}
}
