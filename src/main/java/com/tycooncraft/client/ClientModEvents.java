package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** 클라이언트 전용 등록 (화면) */
@EventBusSubscriber(modid = TycoonCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.MACHINE.get(), MachineScreen::new);
        event.register(ModMenus.SELL.get(), SellScreen::new);
        event.register(ModMenus.GUN_WORKBENCH.get(), GunWorkbenchScreen::new);
    }

    private ClientModEvents() {}
}
