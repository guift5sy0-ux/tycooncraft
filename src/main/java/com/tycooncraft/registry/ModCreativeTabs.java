package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TycoonCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tycooncraft"))
                    .icon(() -> new ItemStack(ModItems.COIN_GOLD.get()))
                    .displayItems((params, out) -> ModItems.ITEMS.getEntries().forEach(holder -> out.accept(holder.get())))
                    .build());

    /** 탈것·총기 전용 탭: 크리에이티브에서 바로 꺼내기 쉽게 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VEHICLES = TABS.register("vehicles",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tycooncraft.vehicles"))
                    .icon(() -> new ItemStack(ModItems.HELICOPTER.get()))
                    .withTabsBefore(MAIN.getId())
                    .displayItems((params, out) -> {
                        out.accept(ModItems.CAR.get());
                        out.accept(ModItems.HELICOPTER.get());
                        out.accept(ModItems.LOCOMOTIVE.get());
                        out.accept(ModItems.FUEL_CANISTER.get());
                        out.accept(ModItems.PISTOL.get());
                        out.accept(ModItems.SMG.get());
                        out.accept(ModItems.RIFLE.get());
                        out.accept(ModItems.SHOTGUN.get());
                        out.accept(ModItems.SNIPER.get());
                        out.accept(ModItems.LIGHT_AMMO.get());
                        out.accept(ModItems.HEAVY_AMMO.get());
                        out.accept(ModItems.SHOTGUN_SHELL.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
