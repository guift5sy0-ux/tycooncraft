package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 상태 효과: 섬광 (화면이 하얗게) */
public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, TycoonCraft.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> FLASHED = EFFECTS.register("flashed", Flashed::new);

    /** v0.26.0 추적: 암상인을 죽인 대가. 이동속도 -10%(레벨당 -2%), 재장전 +20%, 갱단 추격. 우유로 안 지워짐 */
    public static final DeferredHolder<MobEffect, MobEffect> TRACKED = EFFECTS.register("tracked", () -> new Tracked()
            .addAttributeModifier(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED, TycoonCraft.id("tracked_slow"), -0.02,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    static final class Tracked extends MobEffect {
        Tracked() {
            super(MobEffectCategory.HARMFUL, 0x8A1010);
        }

        @Override
        public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures, net.minecraft.world.effect.MobEffectInstance inst) {
            // 어떤 것으로도 치료되지 않음
        }
    }

    static final class Flashed extends MobEffect {
        Flashed() {
            super(MobEffectCategory.HARMFUL, 0xFFFFFF);
        }
    }

    private ModEffects() {}
}
