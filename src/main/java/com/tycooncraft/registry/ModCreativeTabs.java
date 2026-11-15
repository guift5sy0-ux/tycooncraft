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

    private ModCreativeTabs() {}
}
