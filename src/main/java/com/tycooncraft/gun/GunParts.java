package com.tycooncraft.gun;

import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 총에 단 파츠 (데이터 컴포넌트: 슬롯 이름 → 파츠 id, "Order" 로 단 순서) */
public final class GunParts {
    public static String gunId(ItemStack gun) {
        return BuiltInRegistries.ITEM.getKey(gun.getItem()).getPath();
    }

    public static Set<GunPart> supported(ItemStack gun) {
        return GunPart.SUPPORT.getOrDefault(gunId(gun), EnumSet.noneOf(GunPart.class));
    }

    public static List<GunPart> installed(ItemStack gun) {
        CompoundTag t = gun.get(ModComponents.GUN_PARTS.get());
        if (t == null) return Collections.emptyList();
        List<GunPart> out = new ArrayList<>();
        for (GunPart.Slot s : GunPart.Slot.values()) {
            GunPart p = t.contains(s.name()) ? GunPart.byId(t.getString(s.name())) : null;
            if (p != null) out.add(p);
        }
        return out;
    }

    public static boolean has(ItemStack gun, GunPart part) {
        CompoundTag t = gun.get(ModComponents.GUN_PARTS.get());
        return t != null && part.id.equals(t.getString(part.slot.name()));
    }

    /** 장착: 같은 부위에 있던 파츠(없으면 null)를 돌려줌 */
    public static GunPart install(ItemStack gun, GunPart part) {
        CompoundTag t = gun.getOrDefault(ModComponents.GUN_PARTS.get(), new CompoundTag()).copy();
        GunPart old = t.contains(part.slot.name()) ? GunPart.byId(t.getString(part.slot.name())) : null;
        t.putString(part.slot.name(), part.id);
        String order = t.getString("Order").replace(part.slot.name() + ",", "") + part.slot.name() + ",";
        t.putString("Order", order);
        gun.set(ModComponents.GUN_PARTS.get(), t);
        return old;
    }

    /** 마지막으로 단 파츠 하나를 뗌 */
    public static GunPart removeLast(ItemStack gun) {
        CompoundTag t = gun.get(ModComponents.GUN_PARTS.get());
        if (t == null) return null;
        t = t.copy();
        String[] order = t.getString("Order").split(",");
        for (int i = order.length - 1; i >= 0; i--) {
            String s = order[i];
            if (s.isEmpty() || !t.contains(s)) continue;
            GunPart p = GunPart.byId(t.getString(s));
            t.remove(s);
            t.putString("Order", t.getString("Order").replace(s + ",", ""));
            if (installedCount(t) == 0) gun.remove(ModComponents.GUN_PARTS.get());
            else gun.set(ModComponents.GUN_PARTS.get(), t);
            return p;
        }
        return null;
    }

    private static int installedCount(CompoundTag t) {
        int n = 0;
        for (GunPart.Slot s : GunPart.Slot.values()) if (t.contains(s.name())) n++;
        return n;
    }

    public static Item item(GunPart p) {
        return ModItems.GUN_PART_ITEMS.get(p).get();
    }

    /** 예전 인챈트 수치와 같은 단위로 파츠 효과 */
    public static int bonus(ItemStack gun, net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> key) {
        if (gun.get(ModComponents.GUN_PARTS.get()) == null) return 0;
        if (key.equals(GunEnchants.FIREPOWER)) return has(gun, GunPart.HEAVY_BARREL) ? 2 : 0;
        if (key.equals(GunEnchants.RAPID_FIRE)) return has(gun, GunPart.RAPID_TRIGGER) ? 2 : 0;
        if (key.equals(GunEnchants.EXTENDED_MAG)) return has(gun, GunPart.EXTENDED_MAG) ? 2 : 0;
        if (key.equals(GunEnchants.QUICK_RELOAD)) return has(gun, GunPart.MAG_COUPLER) ? 2 : 0;
        if (key.equals(GunEnchants.PIERCING)) return has(gun, GunPart.AP_ROUNDS) ? 2 : 0;
        if (key.equals(GunEnchants.INCENDIARY)) return has(gun, GunPart.INCENDIARY_ROUNDS) ? 1 : 0;
        if (key.equals(GunEnchants.AMMO_SAVER)) return has(gun, GunPart.BRASS_CATCHER) ? 2 : 0;
        if (key.equals(GunEnchants.HEADHUNTER)) return has(gun, GunPart.SCOPE) ? 2 : 0;
        return 0;
    }

    /** 탄 퍼짐 배율 (레드닷·조준경·손잡이) */
    public static float spreadMult(ItemStack gun) {
        float m = 1f;
        if (has(gun, GunPart.RED_DOT)) m *= 0.6f;
        if (has(gun, GunPart.SCOPE)) m *= 0.75f;
        if (has(gun, GunPart.VERTICAL_GRIP)) m *= 0.9f;
        return m;
    }

    /** 반동 배율 */
    public static float recoilMult(ItemStack gun) {
        float m = 1f;
        if (has(gun, GunPart.VERTICAL_GRIP)) m *= 0.6f;
        if (has(gun, GunPart.RED_DOT)) m *= 0.9f;
        if (has(gun, GunPart.HEAVY_BARREL)) m *= 1.1f;
        return m;
    }

    private GunParts() {}
}
