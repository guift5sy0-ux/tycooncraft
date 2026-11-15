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
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import com.tycooncraft.vehicle.HelicopterEntity;
import net.neoforged.neoforge.network.PacketDistributor;

/** 운전 중 스페이스: 자동차 경적 / 기관차 기적, 3인칭 카메라 거리 */
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
        // 전투기 기관포: 좌클릭을 누르고 있는 동안 계속
        if (v instanceof com.tycooncraft.vehicle.JetEntity jet && jet.getControllingPassenger() == mc.player && mc.options.keyAttack.isDown()
                && mc.player.tickCount % 3 == 0) {
            com.tycooncraft.vehicle.VehicleHooks.client.sendAction(jet, com.tycooncraft.vehicle.VehicleEntity.ACTION_FIRE);
        }
    }

    /** 전차·전투기에 타면 클릭 = 무기 (좌클릭 주포/기관포, 우클릭 미사일) */
    @SubscribeEvent
    public static void onClick(net.neoforged.neoforge.client.event.InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Entity v = mc.player.getVehicle();
        boolean tank = v instanceof com.tycooncraft.vehicle.TankEntity, jet = v instanceof com.tycooncraft.vehicle.JetEntity;
        if (!tank && !jet || mc.player != ((com.tycooncraft.vehicle.VehicleEntity) v).getControllingPassenger()) return;
        if (event.isPickBlock()) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        if (event.isAttack()) com.tycooncraft.vehicle.VehicleHooks.client.sendAction(v, com.tycooncraft.vehicle.VehicleEntity.ACTION_FIRE);
        else if (event.isUseItem() && jet) com.tycooncraft.vehicle.VehicleHooks.client.sendAction(v, com.tycooncraft.vehicle.VehicleEntity.ACTION_FIRE2);
    }

    /** 3인칭 카메라를 탈것 크기에 맞게 멀리 (기본 4칸이면 헬기 동체 안에 박힘) */
    @SubscribeEvent
    public static void cameraDistance(CalculateDetachedCameraDistanceEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Entity v = mc.player.getVehicle();
        if (v instanceof HelicopterEntity) event.setDistance(Math.max(event.getDistance(), 12f));
        else if (v instanceof LocomotiveEntity) event.setDistance(Math.max(event.getDistance(), 8f));
        else if (v instanceof CarEntity) event.setDistance(Math.max(event.getDistance(), 7f));
        else if (v instanceof com.tycooncraft.vehicle.TankEntity) event.setDistance(Math.max(event.getDistance(), 10f));
        else if (v instanceof com.tycooncraft.vehicle.JetEntity) event.setDistance(Math.max(event.getDistance(), 15f));
    }

    private VehicleKeys() {}
}
