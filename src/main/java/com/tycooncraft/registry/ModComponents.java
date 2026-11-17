package com.tycooncraft.registry;

import com.mojang.serialization.Codec;
import com.tycooncraft.TycoonCraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 아이템에 붙는 데이터 (총의 탄창 잔탄 수, 소음기) */
public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, TycoonCraft.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> AMMO =
            COMPONENTS.register("ammo", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** 총기 파츠 (슬롯 → 파츠 id) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.nbt.CompoundTag>> GUN_PARTS =
            COMPONENTS.register("gun_parts", () -> DataComponentType.<net.minecraft.nbt.CompoundTag>builder()
                    .persistent(net.minecraft.nbt.CompoundTag.CODEC)
                    .networkSynchronized(ByteBufCodecs.COMPOUND_TAG)
                    .build());

    /** 소음기 장착 여부 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> SILENCED =
            COMPONENTS.register("silenced", () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /** 소음기 등급 (0 없음, 1 소음기, 2 상위 소음기) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SILENCER_TIER =
            COMPONENTS.register("silencer_tier", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** 사격 모드 (VSS: 0 단발·조준, 1 연사) */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FIRE_MODE =
            COMPONENTS.register("fire_mode", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ModComponents() {}
}
