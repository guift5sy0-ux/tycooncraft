package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** v0.20.0: 마크 기본 연기(네모 픽셀) 대신 부드러운 연기 */
public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TycoonCraft.MODID);
    /** 옅은 회색 연기 (총구·먼지·배기) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE = PARTICLES.register("soft_smoke", () -> new SimpleParticleType(false));
    /** 크고 짙은 연기 (폭발·화재·엔진) */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SMOKE_LARGE = PARTICLES.register("soft_smoke_large", () -> new SimpleParticleType(false));
    /** 불꽃 튐 */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARK = PARTICLES.register("spark", () -> new SimpleParticleType(false));

    private ModParticles() {}
}
