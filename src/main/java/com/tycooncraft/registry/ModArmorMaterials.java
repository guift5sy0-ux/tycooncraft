package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/** 전술 방탄 장비 3등급 + 낙하산 (낙하산은 방어력 없음, 다리 칸에 메는 배낭) */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, TycoonCraft.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TACTICAL_LIGHT = tactical("tactical_light", 2, 5, 3, 1, 0.5f, 0f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TACTICAL_MEDIUM = tactical("tactical_medium", 3, 6, 4, 2, 1f, 0.03f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TACTICAL_HEAVY = tactical("tactical_heavy", 3, 7, 4, 2, 1.5f, 0.06f);
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> PARACHUTE = MATERIALS.register("parachute", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                for (ArmorItem.Type t : ArmorItem.Type.values()) m.put(t, 0);
            }), 1, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.EMPTY,
            List.of(new ArmorMaterial.Layer(TycoonCraft.id("parachute"))), 0f, 0f));

    /** 중장갑(저거넛) 세트: 4부위 모두 방어력 */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> JUGGERNAUT = MATERIALS.register("juggernaut", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.HELMET, 3);
                m.put(ArmorItem.Type.CHESTPLATE, 8);
                m.put(ArmorItem.Type.LEGGINGS, 5);
                m.put(ArmorItem.Type.BOOTS, 2);
                m.put(ArmorItem.Type.BODY, 0);
            }), 12, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(ModItems.STEEL_PLATE.get()),
            List.of(new ArmorMaterial.Layer(TycoonCraft.id("juggernaut"))), 2f, 0.09f));

    private static DeferredHolder<ArmorMaterial, ArmorMaterial> tactical(String name, int helmet, int chest, int legs, int boots, float toughness, float kb) {
        return MATERIALS.register(name, () -> new ArmorMaterial(
                Util.make(new EnumMap<>(ArmorItem.Type.class), m -> {
                    m.put(ArmorItem.Type.HELMET, helmet);
                    m.put(ArmorItem.Type.CHESTPLATE, chest);
                    m.put(ArmorItem.Type.LEGGINGS, legs);
                    m.put(ArmorItem.Type.BOOTS, boots);
                    m.put(ArmorItem.Type.BODY, 0);
                }), 10, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(ModItems.STEEL_PLATE.get()),
                List.of(new ArmorMaterial.Layer(TycoonCraft.id(name))), toughness, kb));
    }

    private ModArmorMaterials() {}
}
