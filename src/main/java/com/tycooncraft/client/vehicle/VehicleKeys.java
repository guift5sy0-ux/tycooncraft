package com.tycooncraft.client.vehicle;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.network.HornPayload;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.LocomotiveEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 운전 중 스페이스: 자동차 경적 / 기관차 기적 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class VehicleKeys {
    private static boolean wasDown;
    private static int cooldown;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (cooldown > 0) cooldown--;
        if (mc.player == null || mc.screen != null) {
            wasDown = false;
            return;
        }
        Entity v = mc.player.getVehicle();
        boolean horn = v instanceof CarEntity || v instanceof LocomotiveEntity;
        boolean down = horn && mc.options.keyJump.isDown();
        if (down && !wasDown && cooldown == 0) {
            PacketDistributor.sendToServer(HornPayload.INSTANCE);
            cooldown = v instanceof LocomotiveEntity ? 40 : 14;
        }
        wasDown = down;
    }

    private VehicleKeys() {}
}
