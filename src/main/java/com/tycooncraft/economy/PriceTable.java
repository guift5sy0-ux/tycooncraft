package com.tycooncraft.economy;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 판매 기준가 (₩).
 * v0.18.5: 원자재·바닐라 아이템만 손으로 정하고, 모드 아이템은 전부 제작법에서 자동 계산한다.
 *   가격 = (재료 가격 합 ÷ 나오는 개수) × 가공 배율
 *   가공 배율: 기계 1.6, 작업대 1.15, 총기 제작대 1.25 (기계 가공이 돈이 되게 → 공장을 지어야 함)
 *   원자재는 RAW_SCALE(0.25)배: 손으로 캐서 파는 것만으로는 시간당 약 500원  → 가공 단계마다 꾸준히 비싸지고, 재료보다 싸게 팔리는 일이 없음.
 * 계산은 서버 시작·레시피 동기화 때 한 번. 제작법이 없는 것은 아래 표 값을 그대로 쓴다.
 */
public final class PriceTable {
    /** v0.23.2: 동력 비용 보정 — 기계 가공 1회마다 (필요 동력 × 가공 시간 ÷ 1000)원을 원가에 더함 */
    public static final double ENERGY_DIV = 1000;
    /** v0.26.0: 경제 규모 10배 (판매가·상점가·보상 전부 이 배율을 따름) */
    public static final double ECON = 10;
    public static final double MACHINE_MARKUP = 1.5, CRAFT_MARKUP = 1.15, GUN_MARKUP = 1.25;
    /** 손으로 캐고 파는 원자재·바닐라 값 배율 (v0.18.6: 손노동 수입 ≈ 시간당 500원) */
    public static final double RAW_SCALE = 0.25;
    /** 기준: 공장 없이 손으로 1시간 벌면 이 정도 */
    public static final long MANUAL_HOUR = Math.round(500 * ECON);
    private static Map<Item, Double> prices;
    private static Map<Item, Double> computed;

    public static Map<Item, Double> all() {
        return computed != null ? computed : table();
    }

    /** 제작법으로 모드 아이템 가격을 다시 계산 */
    public static synchronized void compute(net.minecraft.world.item.crafting.RecipeManager rm, net.minecraft.core.HolderLookup.Provider reg) {
        Map<Item, Double> p = new LinkedHashMap<>(table());
        for (Item c : Coins.denominations()) p.put(c, 0.0001);   // 제작법에 든 동전은 설치비(되팔아도 돌려받지 못함)
        record R(Map<Item, Integer> in, Item out, int count, double markup, double energy) {}
        java.util.List<R> rs = new java.util.ArrayList<>();
        // v0.30: 기계 삭제 → 화로·용광로 제작법(이 모드 것)으로 강철 등 값 계산 (연료 값 조금 더함)
        for (var type : java.util.List.of(net.minecraft.world.item.crafting.RecipeType.SMELTING, net.minecraft.world.item.crafting.RecipeType.BLASTING))
            for (var h : rm.getAllRecipesFor(type)) {
                if (!h.id().getNamespace().equals(com.tycooncraft.TycoonCraft.MODID)) continue;
                var out = h.value().getResultItem(reg);
                var ings = h.value().getIngredients();
                if (out.isEmpty() || ings.isEmpty() || ings.get(0).getItems().length == 0) continue;
                rs.add(new R(java.util.Map.of(ings.get(0).getItems()[0].getItem(), 1), out.getItem(), out.getCount(), CRAFT_MARKUP, 0.5 * ECON * RAW_SCALE));
            }
        for (var r : com.tycooncraft.gun.GunCrafting.all()) rs.add(new R(r.needs(), r.result().getItem(), r.result().getCount(), GUN_MARKUP, 0));
        // 작업대 제작법 (이 모드 것만; 재료 선택지가 여럿이면 가장 싼 것)
        java.util.List<java.util.Map.Entry<java.util.List<net.minecraft.world.item.crafting.Ingredient>, net.minecraft.world.item.ItemStack>> crafts = new java.util.ArrayList<>();
        for (var h : rm.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING)) {
            if (!h.id().getNamespace().equals(com.tycooncraft.TycoonCraft.MODID)) continue;
            var out = h.value().getResultItem(reg);
            if (out.isEmpty() || Coins.isCoin(out)) continue;
            crafts.add(java.util.Map.entry(h.value().getIngredients(), out));
        }
        for (int pass = 0; pass < 20; pass++) {
            Map<Item, Double> best = new java.util.HashMap<>();
            for (R r : rs) {
                if (!derivable(r.out())) continue;
                double sum = 0;
                boolean ok = true;
                for (var e : r.in().entrySet()) {
                    Double v = p.get(e.getKey());
                    if (v == null || v <= 0) { ok = false; break; }
                    sum += v * e.getValue();
                }
                if (ok) best.merge(r.out(), (sum + r.energy()) / r.count() * r.markup(), Math::min);
            }
            for (var c : crafts) {
                Item out = c.getValue().getItem();
                if (!derivable(out)) continue;
                double sum = 0;
                boolean ok = true;
                for (var ing : c.getKey()) {
                    if (ing.isEmpty()) continue;
                    double b = Double.MAX_VALUE;
                    for (var st : ing.getItems()) {
                        Double v = p.get(st.getItem());
                        if (v != null && v > 0) b = Math.min(b, v);
                    }
                    if (b == Double.MAX_VALUE) { ok = false; break; }
                    sum += b;
                }
                if (ok) best.merge(out, sum / c.getValue().getCount() * CRAFT_MARKUP, Math::min);
            }
            boolean changed = false;
            for (var e : best.entrySet()) {
                Double old = p.get(e.getKey());
                if (old == null || Math.abs(old - e.getValue()) > 1e-6) changed = true;
                p.put(e.getKey(), e.getValue());
            }
            if (!changed) break;
        }
        for (Item c : Coins.denominations()) p.remove(c);
        p.replaceAll((k, v) -> round(v));
        computed = p;
        SOURCE_RECIPES = rs.size() + crafts.size();
    }

    public static int SOURCE_RECIPES;

    /** 바닐라 아이템과 동전은 표 값 고정, 모드 아이템만 계산 */
    private static boolean derivable(Item it) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it);
        return id.getNamespace().equals(com.tycooncraft.TycoonCraft.MODID) && !(it instanceof CoinItem) && it != ModItems.DESTROYER_CORE.get() && it != ModItems.VENOM_SPIT.get()
                && it != ModItems.SCOUT_CORE.get() && it != ModItems.FLIGHT_CORE.get() && it != ModItems.PROTO_SCOUT.get();
    }

    static double round(double v) {
        if (v < 10) return Math.round(v * 10) / 10.0;
        if (v < 1000) return Math.round(v);
        return Math.round(v / 10) * 10.0;
    }

    private static Map<Item, Double> table() {
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
            p.put(ModItems.DESTROYER_CORE.get(), 4000.0);   // v0.26.2 보스 소재 대폭 인상 (×2.5 = ₩10,000)
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
            p.put(ModItems.SHIP_MOTOR.get(), 900.0);
            p.put(ModItems.SHIP_BOOSTER.get(), 2400.0);
            p.put(ModItems.SETTER.get(), 650.0);
            p.put(ModItems.SHIP_HELM.get(), 300.0);
            p.put(ModItems.SHIP_FLOAT.get(), 420.0);
            p.put(ModItems.EMP_GENERATOR.get(), 2500.0);
            // 5차: 조립품
            p.put(ModItems.CIRCUIT.get(), 45.0);
            p.put(ModItems.MECHANISM.get(), 160.0);
            p.put(ModItems.ADVANCED_CIRCUIT.get(), 230.0);
            p.put(ModItems.ENGINE.get(), 700.0);
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
            p.put(ModItems.FUEL_CANISTER.get(), 12.0);
            p.put(ModItems.SILENCER.get(), 120.0);
            for (var e : java.util.Map.of(com.tycooncraft.gun.GunPart.HEAVY_BARREL, 260.0, com.tycooncraft.gun.GunPart.RED_DOT, 180.0,
                    com.tycooncraft.gun.GunPart.SCOPE, 320.0, com.tycooncraft.gun.GunPart.VERTICAL_GRIP, 90.0, com.tycooncraft.gun.GunPart.EXTENDED_MAG, 140.0,
                    com.tycooncraft.gun.GunPart.MAG_COUPLER, 110.0, com.tycooncraft.gun.GunPart.RAPID_TRIGGER, 220.0, com.tycooncraft.gun.GunPart.AP_ROUNDS, 200.0,
                    com.tycooncraft.gun.GunPart.INCENDIARY_ROUNDS, 190.0, com.tycooncraft.gun.GunPart.BRASS_CATCHER, 80.0).entrySet())
                p.put(ModItems.GUN_PART_ITEMS.get(e.getKey()).get(), e.getValue());
            p.put(ModItems.DRONE.get(), 450.0);
            p.put(ModItems.DRONE_MK2.get(), 1100.0);
            p.put(ModItems.SILENCER_ADVANCED.get(), 300.0);
            p.put(ModItems.VSS.get(), 1500.0);
            p.put(ModItems.DUAL_PISTOLS.get(), 950.0);
            p.put(ModItems.RPG.get(), 1400.0);
            p.put(ModItems.ROCKET.get(), 60.0);
            p.put(ModItems.TANK_SHELL.get(), 50.0);
            p.put(ModItems.VEST_1.get(), 120.0);
            p.put(ModItems.VEST_2.get(), 450.0);
            p.put(ModItems.VEST_3.get(), 1200.0);
            p.put(ModItems.HELMET_1.get(), 80.0);
            p.put(ModItems.HELMET_2.get(), 300.0);
            p.put(ModItems.HELMET_3.get(), 900.0);
            p.put(ModItems.PANTS_1.get(), 100.0);
            p.put(ModItems.PANTS_2.get(), 380.0);
            p.put(ModItems.PANTS_3.get(), 1000.0);
            p.put(ModItems.BOOTS_1.get(), 70.0);
            p.put(ModItems.BOOTS_2.get(), 260.0);
            p.put(ModItems.BOOTS_3.get(), 700.0);
            p.put(ModItems.PARACHUTE.get(), 150.0);
            p.put(ModItems.HELMET_4.get(), 2200.0);
            p.put(ModItems.VEST_4.get(), 2800.0);
            p.put(ModItems.JUGGERNAUT_LEGGINGS.get(), 1300.0);
            p.put(ModItems.COMBAT_BOOTS.get(), 700.0);
            p.put(ModItems.TURRET.get(), 1600.0);
            p.put(ModItems.CIWS.get(), 3200.0);
            p.put(ModItems.TANK.get(), 9000.0);
            p.put(ModItems.FIGHTER_JET.get(), 12000.0);
            p.put(ModItems.CAR.get(), 2000.0);
            p.put(ModItems.LOCOMOTIVE.get(), 4000.0);
            p.put(ModItems.HELICOPTER.get(), 5500.0);
            // v0.16 산업 체인
            p.put(ModItems.RUBBER.get(), 3.2);
            p.put(Items.CALCITE, 0.6);
            // 제작 재료로 쓰이는 바닐라 아이템 (v0.18.5)
            p.put(Items.IRON_NUGGET, 0.9);
            p.put(Items.GOLD_NUGGET, 2.2);
            p.put(Items.IRON_ORE, 5.0);
            p.put(Items.DEEPSLATE_IRON_ORE, 5.0);
            p.put(Items.COPPER_ORE, 2.0);
            p.put(Items.DEEPSLATE_COPPER_ORE, 2.0);
            p.put(Items.GOLD_ORE, 12.0);
            p.put(Items.DEEPSLATE_GOLD_ORE, 12.0);
            p.put(Items.FLINT, 0.5);
            p.put(Items.STRING, 1.0);
            p.put(Items.LEATHER, 3.0);
            p.put(Items.PAPER, 0.6);
            p.put(Items.GUNPOWDER, 3.0);
            p.put(Items.SLIME_BALL, 3.0);
            p.put(Items.BONE, 1.0);
            p.put(Items.BONE_MEAL, 0.4);
            p.put(Items.BLAZE_POWDER, 6.0);
            p.put(Items.GLOWSTONE_DUST, 2.0);
            p.put(Items.WHITE_WOOL, 2.0);
            p.put(Items.BLACK_WOOL, 2.5);
            p.put(Items.RED_WOOL, 2.5);
            p.put(Items.BLACK_DYE, 1.0);
            p.put(Items.GLASS_PANE, 0.4);
            p.put(Items.GLASS_BOTTLE, 1.0);
            p.put(Items.BRICKS, 4.0);
            p.put(Items.DRIED_KELP_BLOCK, 2.0);
            p.put(Items.CRAFTING_TABLE, 1.3);
            p.put(Items.FURNACE, 1.0);
            p.put(Items.CHEST, 2.6);
            p.put(Items.STONECUTTER, 10.0);
            p.put(Items.PISTON, 12.0);
            p.put(Items.DISPENSER, 6.0);
            p.put(Items.COMPARATOR, 12.0);
            p.put(Items.REDSTONE_BLOCK, 27.0);
            p.put(Items.HOPPER, 43.0);
            p.put(Items.CAULDRON, 56.0);
            p.put(Items.TNT, 20.0);
            p.put(Items.SPYGLASS, 30.0);
            p.put(Items.NETHERITE_SCRAP, 200.0);
            p.put(Items.NETHERITE_INGOT, 900.0);
            p.put(Items.BEACON, 2500.0);
            p.put(Items.DRAGON_EGG, 60000.0);
            // v0.26.2 보스 소재 (판매가 = 표값 × 2.5)
            p.put(Items.NETHER_STAR, 12000.0);
            p.put(Items.WITHER_SKELETON_SKULL, 1600.0);
            p.put(Items.DRAGON_BREATH, 400.0);
            p.put(Items.ELYTRA, 12000.0);
            p.put(Items.DRAGON_HEAD, 4000.0);
            p.put(ModItems.VENOM_SPIT.get(), 200.0);
            p.put(ModItems.SCOUT_CORE.get(), 800.0);      // v0.27.0 ₩2,000
            p.put(ModItems.FLIGHT_CORE.get(), 10000.0);   // v0.27.0 ₩25,000
            p.put(ModItems.PROTO_SCOUT.get(), 9000.0);    // v0.27.1
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
            p.replaceAll((k, v) -> v * RAW_SCALE * ECON);
            prices = p;
        }
        return prices;
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent e) {
        compute(e.getServer().getRecipeManager(), e.getServer().registryAccess());
    }

    /** 0이면 판매 불가 */
    public static double basePrice(Item item) {
        return all().getOrDefault(item, 0.0);
    }

    private PriceTable() {}
}
