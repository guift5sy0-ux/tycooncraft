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
        event.register(ModMenus.SELL.get(), SellScreen::new);
        event.register(ModMenus.GUN_WORKBENCH.get(), GunWorkbenchScreen::new);
    }

    /** v0.32: 총 동작 훅 (공용 코드 → 클라이언트) */
    @SubscribeEvent
    public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        com.tycooncraft.gun.GunItem.CLIENT_SHOT = com.tycooncraft.client.gun.GunViewModel::onLocalShot;
        com.tycooncraft.gun.GunItem.CLIENT_RELOAD = com.tycooncraft.client.gun.GunViewModel::onLocalReload;
    }

    @SubscribeEvent
    public static void reloadListeners(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) rm -> com.tycooncraft.client.gun.GunRig.clear());
    }

    private ClientModEvents() {}
}
