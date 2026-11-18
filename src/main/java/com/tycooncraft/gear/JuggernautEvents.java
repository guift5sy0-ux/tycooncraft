package com.tycooncraft.gear;

import com.tycooncraft.registry.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 중장갑 세트 효과: 전투화 낙하 피해 -30%, 4부위 세트 폭발 피해 -25% */
public final class JuggernautEvents {
    public static boolean fullSet(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.HELMET_4.get())
                && e.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.VEST_4.get())
                && e.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.JUGGERNAUT_LEGGINGS.get())
                && e.getItemBySlot(EquipmentSlot.FEET).is(ModItems.COMBAT_BOOTS.get());
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
            var ft = e.getItemBySlot(EquipmentSlot.FEET);
            float m = ft.is(ModItems.COMBAT_BOOTS.get()) ? 0.7f : ft.is(ModItems.BOOTS_3.get()) ? 0.75f : ft.is(ModItems.BOOTS_2.get()) ? 0.8f : ft.is(ModItems.BOOTS_1.get()) ? 0.85f : 1f;
            event.setAmount(event.getAmount() * m);
        }
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION) && fullSet(e))
            event.setAmount(event.getAmount() * 0.75f);
    }

    private JuggernautEvents() {}
}
