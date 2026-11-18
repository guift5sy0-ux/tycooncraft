package com.tycooncraft.gun;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 총기 제작대 전용 레시피 (총기·탄약). 칸 하나 = 재료 1개, 배치 모양은 상관없음. */
public final class GunCrafting {
    public record Recipe(ItemStack result, Map<Item, Integer> needs) {
        public int size() {
            return needs.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private static List<Recipe> recipes;

    private static Recipe r(Item result, int count, Object... in) {
        Map<Item, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < in.length; i += 2) m.merge((Item) in[i], (Integer) in[i + 1], Integer::sum);
        return new Recipe(new ItemStack(result, count), m);
    }

    public static List<Recipe> all() {
        if (recipes == null) {
            Item rec = ModItems.GUN_RECEIVER.get(), bar = ModItems.GUN_BARREL.get(), stock = ModItems.GUN_STOCK.get();
            List<Recipe> l = new ArrayList<>();
            // 총기 부품 (조립기·선반에서도 만들 수 있음)
            l.add(r(rec, 1, ModItems.STEEL_PLATE.get(), 2, ModItems.MECHANISM.get(), 1, ModItems.IRON_GEAR.get(), 1));
            l.add(r(bar, 1, ModItems.STEEL_ROD.get(), 1));
            l.add(r(stock, 1, Items.OAK_PLANKS, 3, Items.LEATHER, 1));
            l.add(r(ModItems.SILENCER.get(), 1, ModItems.STEEL_PLATE.get(), 1, ModItems.STEEL_ROD.get(), 1, Items.WHITE_WOOL, 1));
            // 총기
            l.add(r(ModItems.PISTOL.get(), 1, rec, 1, bar, 1, ModItems.IRON_PLATE.get(), 1));
            l.add(r(ModItems.DUAL_PISTOLS.get(), 1, ModItems.PISTOL.get(), 2, ModItems.MECHANISM.get(), 1));
            l.add(r(ModItems.SMG.get(), 1, rec, 1, bar, 1, stock, 1, ModItems.CIRCUIT.get(), 1));
            l.add(r(ModItems.SHOTGUN.get(), 1, rec, 1, bar, 2, stock, 1, ModItems.STEEL_PLATE.get(), 1));
            l.add(r(ModItems.RIFLE.get(), 1, rec, 1, bar, 2, stock, 1, ModItems.ADVANCED_CIRCUIT.get(), 1));
            l.add(r(ModItems.SNIPER.get(), 1, rec, 1, bar, 2, stock, 1, ModItems.ADVANCED_CIRCUIT.get(), 1, Items.GLASS, 1, Items.SPYGLASS, 1));
            l.add(r(ModItems.VSS.get(), 1, rec, 1, bar, 1, ModItems.SILENCER.get(), 1, stock, 1, Items.SPYGLASS, 1, ModItems.STEEL_PLATE.get(), 2));
            l.add(r(ModItems.RPG.get(), 1, rec, 1, ModItems.STEEL_PLATE.get(), 4, ModItems.STEEL_ROD.get(), 2, stock, 1, Items.SPYGLASS, 1));
            l.add(r(ModItems.JUDGE.get(), 1, Items.BEACON, 2, Items.NETHERITE_INGOT, 5, Items.DRAGON_EGG, 1, ModItems.PISTOL.get(), 1));
            // 총기 파츠 (총기 제작대 전용, 한 칸 = 1개라 9칸 안에 들어가게)
            l.add(r(part(GunPart.SCOPE), 1, ModItems.ALUMINUM_PLATE.get(), 2, Items.GLASS_PANE, 2, ModItems.STEEL_ROD.get(), 1, ModItems.CIRCUIT.get(), 1));
            l.add(r(part(GunPart.RED_DOT), 1, ModItems.ALUMINUM_PLATE.get(), 1, Items.GLASS_PANE, 1, Items.REDSTONE, 1, ModItems.BATTERY_CELL.get(), 1, ModItems.CIRCUIT.get(), 1));
            l.add(r(part(GunPart.HEAVY_BARREL), 1, bar, 1, ModItems.STEEL_PLATE.get(), 2, ModItems.ALUMINUM_PLATE.get(), 1));
            l.add(r(part(GunPart.RAPID_TRIGGER), 1, ModItems.STEEL_ROD.get(), 1, ModItems.MECHANISM.get(), 1, Items.GOLD_INGOT, 1));
            l.add(r(part(GunPart.EXTENDED_MAG), 1, ModItems.STEEL_PLATE.get(), 2, ModItems.PLASTIC_PELLET.get(), 3));
            l.add(r(part(GunPart.MAG_COUPLER), 1, ModItems.PLASTIC_PELLET.get(), 2, ModItems.RUBBER.get(), 2, ModItems.STEEL_PLATE.get(), 1));
            l.add(r(part(GunPart.VERTICAL_GRIP), 1, ModItems.PLASTIC_PELLET.get(), 4, ModItems.ALUMINUM_PLATE.get(), 1, ModItems.RUBBER.get(), 1));
            l.add(r(part(GunPart.AP_ROUNDS), 1, ModItems.HEAVY_AMMO.get(), 4, ModItems.STEEL_INGOT.get(), 2, ModItems.COKE.get(), 1));
            l.add(r(part(GunPart.INCENDIARY_ROUNDS), 1, ModItems.LIGHT_AMMO.get(), 4, ModItems.SULFUR.get(), 2, Items.BLAZE_POWDER, 1));
            l.add(r(part(GunPart.BRASS_CATCHER), 1, Items.LEATHER, 3, ModItems.ALUMINUM_PLATE.get(), 1, Items.STRING, 2));
            l.add(r(ModItems.SILENCER_ADVANCED.get(), 1, ModItems.SILENCER.get(), 1, ModItems.STEEL_PLATE.get(), 2, Items.BLACK_WOOL, 2, ModItems.GOLD_PLATE.get(), 1));
            // 탄약 (가공 재료로 만들면 더 많이 나옴)
            l.add(r(ModItems.LIGHT_AMMO.get(), 16, ModItems.COPPER_PLATE.get(), 1, Items.GUNPOWDER, 1, ModItems.IRON_ROD.get(), 1));
            l.add(r(ModItems.LIGHT_AMMO.get(), 6, Items.COPPER_INGOT, 1, Items.GUNPOWDER, 1, Items.IRON_NUGGET, 1));
            l.add(r(ModItems.HEAVY_AMMO.get(), 8, ModItems.COPPER_PLATE.get(), 1, Items.GUNPOWDER, 2, ModItems.STEEL_ROD.get(), 1));
            l.add(r(ModItems.HEAVY_AMMO.get(), 4, Items.COPPER_INGOT, 1, Items.GUNPOWDER, 2, Items.IRON_INGOT, 1));
            l.add(r(ModItems.SHOTGUN_SHELL.get(), 8, ModItems.COPPER_PLATE.get(), 1, Items.GUNPOWDER, 1, ModItems.IRON_DUST.get(), 2));
            l.add(r(ModItems.SHOTGUN_SHELL.get(), 4, Items.PAPER, 1, Items.GUNPOWDER, 1, Items.IRON_NUGGET, 2));
            l.add(r(ModItems.ROCKET.get(), 2, ModItems.IRON_PLATE.get(), 2, Items.GUNPOWDER, 4, Items.TNT, 1, ModItems.COPPER_WIRE.get(), 1));
            recipes = List.copyOf(l);
        }
        return recipes;
    }

    private static Item part(GunPart p) {
        return ModItems.GUN_PART_ITEMS.get(p).get();
    }

    /** 0 총기, 1 탄약, 2 총기 부품, 3 총기 파츠 */
    public static int category(Recipe rc) {
        Item it = rc.result().getItem();
        if (it instanceof GunItem) return 0;
        if (it == ModItems.LIGHT_AMMO.get() || it == ModItems.HEAVY_AMMO.get() || it == ModItems.SHOTGUN_SHELL.get() || it == ModItems.ROCKET.get()) return 1;
        if (it == ModItems.GUN_RECEIVER.get() || it == ModItems.GUN_BARREL.get() || it == ModItems.GUN_STOCK.get() || it == ModItems.SILENCER.get()) return 2;
        return 3;
    }

    /** 판자는 종류 상관없이 하나로 셈 */
    public static Item key(ItemStack s) {
        return s.is(net.minecraft.tags.ItemTags.PLANKS) ? Items.OAK_PLANKS : s.getItem();
    }

    /** 가진 재료로 몇 번 만들 수 있는지 */
    public static int sets(Recipe rc, Map<Item, Integer> have) {
        int n = Integer.MAX_VALUE;
        for (Map.Entry<Item, Integer> e : rc.needs().entrySet()) n = Math.min(n, have.getOrDefault(e.getKey(), 0) / e.getValue());
        return n == Integer.MAX_VALUE ? 0 : n;
    }

    /** 격자 내용과 정확히 맞는 레시피 */
    public static Recipe match(Container grid) {
        Map<Item, Integer> have = new LinkedHashMap<>();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.getItem(i);
            if (!s.isEmpty()) have.merge(key(s), 1, Integer::sum);
        }
        if (have.isEmpty()) return null;
        for (Recipe rc : all()) if (rc.needs().equals(have)) return rc;
        return null;
    }

    /** 총기 제작대 전용 (총·탄약). 부품은 기계로도 만들 수 있으므로 제외 */
    public static boolean isGunOrAmmo(Item item) {
        if (item == ModItems.GUN_RECEIVER.get() || item == ModItems.GUN_BARREL.get() || item == ModItems.GUN_STOCK.get() || item == ModItems.SILENCER.get()) return false;
        for (Recipe rc : all()) if (rc.result().is(item)) return true;
        return false;
    }

    private GunCrafting() {}
}
