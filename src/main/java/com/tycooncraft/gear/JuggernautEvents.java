package com.tycooncraft.gear;

import com.tycooncraft.registry.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** 중장갑 세트 효과: 전투화 낙하 피해 -40%, 4부위 세트 폭발 피해 -25% */
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
        if (event.getSource().is(DamageTypeTags.IS_FALL) && e.getItemBySlot(EquipmentSlot.FEET).is(ModItems.COMBAT_BOOTS.get()))
            event.setAmount(event.getAmount() * 0.6f);
        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION) && fullSet(e))
            event.setAmount(event.getAmount() * 0.75f);
    }

    private JuggernautEvents() {}
}
