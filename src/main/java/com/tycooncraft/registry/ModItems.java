package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.economy.CoinItem;
import com.tycooncraft.machine.MachineType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(TycoonCraft.MODID);

    // ── 화폐 (₩) ──
    public static final DeferredItem<CoinItem> COIN_COPPER = coin("coin_copper", 1);
    public static final DeferredItem<CoinItem> COIN_IRON = coin("coin_iron", 10);
    public static final DeferredItem<CoinItem> COIN_GOLD = coin("coin_gold", 100);
    public static final DeferredItem<CoinItem> COIN_EMERALD = coin("coin_emerald", 1_000);
    public static final DeferredItem<CoinItem> COIN_DIAMOND = coin("coin_diamond", 10_000);

    // ── 1차: 가루 ──
    public static final DeferredItem<Item> IRON_DUST = ITEMS.registerSimpleItem("iron_dust");
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("copper_dust");
    public static final DeferredItem<Item> GOLD_DUST = ITEMS.registerSimpleItem("gold_dust");
    public static final DeferredItem<Item> COAL_DUST = ITEMS.registerSimpleItem("coal_dust");
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");
    // ── 2차: 합금 ──
    public static final DeferredItem<Item> STEEL_INGOT = ITEMS.registerSimpleItem("steel_ingot");
    // ── 3차: 판 / 선 / 봉 ──
    public static final DeferredItem<Item> IRON_PLATE = ITEMS.registerSimpleItem("iron_plate");
    public static final DeferredItem<Item> COPPER_PLATE = ITEMS.registerSimpleItem("copper_plate");
    public static final DeferredItem<Item> GOLD_PLATE = ITEMS.registerSimpleItem("gold_plate");
    public static final DeferredItem<Item> STEEL_PLATE = ITEMS.registerSimpleItem("steel_plate");
    public static final DeferredItem<Item> COPPER_WIRE = ITEMS.registerSimpleItem("copper_wire");
    public static final DeferredItem<Item> GOLD_WIRE = ITEMS.registerSimpleItem("gold_wire");
    public static final DeferredItem<Item> IRON_ROD = ITEMS.registerSimpleItem("iron_rod");
    public static final DeferredItem<Item> STEEL_ROD = ITEMS.registerSimpleItem("steel_rod");
    // ── 4차: 기어 ──
    public static final DeferredItem<Item> IRON_GEAR = ITEMS.registerSimpleItem("iron_gear");
    public static final DeferredItem<Item> STEEL_GEAR = ITEMS.registerSimpleItem("steel_gear");
    // ── 5차: 조립품 ──
    public static final DeferredItem<Item> CIRCUIT = ITEMS.registerSimpleItem("circuit");
    public static final DeferredItem<Item> ADVANCED_CIRCUIT = ITEMS.registerSimpleItem("advanced_circuit");
    public static final DeferredItem<Item> MECHANISM = ITEMS.registerSimpleItem("mechanism");
    public static final DeferredItem<Item> ENGINE = ITEMS.registerSimpleItem("engine");
    public static final DeferredItem<Item> WOODEN_CRATE = ITEMS.registerSimpleItem("wooden_crate");

    // ── 블록 아이템 ──
    public static final DeferredItem<BlockItem> SELL_STATION = ITEMS.registerSimpleBlockItem(ModBlocks.SELL_STATION);
    public static final DeferredItem<BlockItem> EXCHANGE = ITEMS.registerSimpleBlockItem(ModBlocks.EXCHANGE);
    public static final DeferredItem<BlockItem> CONVEYOR = ITEMS.registerSimpleBlockItem(ModBlocks.CONVEYOR);
    public static final DeferredItem<BlockItem> LIFT_UP = ITEMS.registerSimpleBlockItem(ModBlocks.LIFT_UP);
    public static final DeferredItem<BlockItem> LIFT_DOWN = ITEMS.registerSimpleBlockItem(ModBlocks.LIFT_DOWN);
    public static final DeferredItem<BlockItem> BROKEN_MACHINE = ITEMS.registerSimpleBlockItem(ModBlocks.BROKEN_MACHINE);
    public static final List<DeferredItem<BlockItem>> MACHINE_ITEMS = new ArrayList<>();
    static {
        for (MachineType type : MachineType.values()) {
            MACHINE_ITEMS.add(ITEMS.registerSimpleBlockItem(ModBlocks.MACHINES.get(type)));
        }
    }

    public static Item machineItem(MachineType type) {
        return ModBlocks.machine(type).asItem();
    }

    private static DeferredItem<CoinItem> coin(String name, long value) {
        return ITEMS.register(name, () -> new CoinItem(new Item.Properties(), value));
    }

    private ModItems() {}
}
