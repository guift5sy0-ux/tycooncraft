package com.tycooncraft.gear;

import com.tycooncraft.TycoonCraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * v0.31: 방어력 40까지.
 * 바닐라는 방어력 효과를 20(피해 -80%)에서 자르므로, 20을 넘는 몫은 여기서 더 깎아 줌.
 * 20까지는 바닐라와 똑같고, 20 → 40 사이는 1당 0.5%씩 → 40이면 최대 -90%.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID)
public final class ExtendedArmor {
    public static final double MAX_ARMOR = 40.0;

    /** 방어력·강도·들어온 피해 → 바닐라식 감소율 (상한만 40으로 늘림) */
    public static float reduction(float armor, float toughness, float damage) {
        float f = Mth.clamp(armor - damage / (2f + toughness / 4f), armor * 0.2f, (float) MAX_ARMOR);
        return f <= 20f ? f / 25f : 0.8f + (f - 20f) * 0.005f;
    }

    @SubscribeEvent
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity le = e.getEntity();
        if (e.getSource().is(DamageTypeTags.BYPASSES_ARMOR)) return;
        boolean bullet = com.tycooncraft.gun.Ballistics.BULLET_HIT;
        if (bullet) {
            // v0.31.3: 총알 - 방어력 60% 무시 + 총알 보호(레벨당 1.1), 총 감소는 최대 70%
            float raw = com.tycooncraft.gun.Ballistics.BULLET_RAW;
            float eff = com.tycooncraft.gun.Ballistics.bulletArmor(le);
            e.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (c, vanillaCut) -> {
                float dmg = c.getNewDamage();
                if (dmg <= 0) return vanillaCut;
                float tough = (float) le.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
                float cut = dmg * reduction(eff, tough, dmg);
                float floor = raw * (1f - com.tycooncraft.gun.Ballistics.BULLET_MAX_CUT);
                return Mth.clamp(cut, 0f, Math.max(0f, dmg - floor));
            });
            return;
        }
        if (le.getArmorValue() <= 20) return;
        e.getContainer().addModifier(DamageContainer.Reduction.ARMOR, (c, vanillaCut) -> {
            float dmg = c.getNewDamage();
            if (dmg <= 0) return vanillaCut;
            float armor = le.getArmorValue(), tough = (float) le.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            float vanillaRate = Mth.clamp(armor - dmg / (2f + tough / 4f), armor * 0.2f, 20f) / 25f;
            if (vanillaRate <= 0) return vanillaCut;
            // 무기의 방어 관통(브리치 등)이 바닐라 몫에 준 비율을 그대로 유지
            float rate = Mth.clamp(vanillaCut / dmg * reduction(armor, tough, dmg) / vanillaRate, 0f, 0.95f);
            return dmg * rate;
        });
    }

    private ExtendedArmor() {}
}
