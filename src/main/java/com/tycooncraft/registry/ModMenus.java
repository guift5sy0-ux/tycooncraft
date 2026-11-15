package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.economy.SellMenu;
import com.tycooncraft.machine.MachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, TycoonCraft.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE =
            MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SellMenu>> SELL =
            MENUS.register("sell_station", () -> IMenuTypeExtension.create(SellMenu::new));

    private ModMenus() {}
}
