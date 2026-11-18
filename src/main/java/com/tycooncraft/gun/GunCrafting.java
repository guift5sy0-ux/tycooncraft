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

    /** 격자 내용과 정확히 맞는 레시피 */
    public static Recipe match(Container grid) {
        Map<Item, Integer> have = new LinkedHashMap<>();
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.getItem(i);
            if (!s.isEmpty()) have.merge(s.getItem(), 1, Integer::sum);
        }
        if (have.isEmpty()) return null;
        for (Recipe rc : all()) if (rc.needs().equals(have)) return rc;
        return null;
    }

    public static boolean isGunOrAmmo(Item item) {
        for (Recipe rc : all()) if (rc.result().is(item)) return true;
        return false;
    }

    private GunCrafting() {}
}
