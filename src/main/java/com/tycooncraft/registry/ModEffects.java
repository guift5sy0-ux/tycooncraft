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

    static final class Flashed extends MobEffect {
        Flashed() {
            super(MobEffectCategory.HARMFUL, 0xFFFFFF);
        }
    }

    private ModEffects() {}
}
