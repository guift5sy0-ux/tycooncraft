package com.tycooncraft.gun;

import com.tycooncraft.TycoonCraft;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 총 전용 인챈트 (data/tycooncraft/enchantment/*.json, 효과는 GunItem 에서 처리).
 * 화력·속사·확장 탄창·빠른 장전·관통탄·정밀 사격·소이탄·탄약 절약·헤드헌터
 */
public final class GunEnchants {
    public static final ResourceKey<Enchantment> FIREPOWER = key("firepower");
    public static final ResourceKey<Enchantment> RAPID_FIRE = key("rapid_fire");
    public static final ResourceKey<Enchantment> EXTENDED_MAG = key("extended_mag");
    public static final ResourceKey<Enchantment> QUICK_RELOAD = key("quick_reload");
    public static final ResourceKey<Enchantment> PIERCING = key("piercing_rounds");
    public static final ResourceKey<Enchantment> PRECISION = key("precision");
    public static final ResourceKey<Enchantment> INCENDIARY = key("incendiary");
    public static final ResourceKey<Enchantment> AMMO_SAVER = key("ammo_saver");
    public static final ResourceKey<Enchantment> HEADHUNTER = key("headhunter");
    /** v0.31: 방어구용 - 총알 피해 감소 (부위·레벨당 0.6%) */
    public static final ResourceKey<Enchantment> BULLET_PROTECTION = key("bullet_protection");

    private static ResourceKey<Enchantment> key(String name) {
        return ResourceKey.create(Registries.ENCHANTMENT, TycoonCraft.id(name));
    }

    /** 레지스트리 없이(클라이언트 내구도 바 등에서도) 레벨 읽기 */
    public static int level(ItemStack stack, ResourceKey<Enchantment> key) {
        int part = GunParts.bonus(stack, key);   // v0.17: 총기 파츠가 인챈트를 대신함
        for (Object2IntMap.Entry<Holder<Enchantment>> e : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
            if (e.getKey().is(key)) return Math.max(part, e.getIntValue());
        }
        return part;
    }

    private GunEnchants() {}
}
