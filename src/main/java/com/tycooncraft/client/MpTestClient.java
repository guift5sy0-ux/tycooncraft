package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.devtest.MpTest;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.DisconnectedScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** CI 전용 멀티플레이 시험 (클라이언트 쪽): 서버 접속 → 드론 소환 → 비행 → 클릭 폭파를 두 번 반복 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class MpTestClient {
    static int t = -1, round;
    static boolean reported;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        if (!MpTest.ON) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof DisconnectedScreen ds && !reported) {
            reported = true;
            System.out.println("[MPTEST] client DISCONNECTED: " + ds.getTitle().getString() + " / " + ds.getNarrationMessage().getString());
            System.out.println("[MPTEST] finished");
            mc.stop();
            return;
        }
        if (mc.player == null || mc.level == null) return;
        if (t < 0) {
            t = 0;
            System.out.println("[MPTEST] client joined");
        }
        t++;
        var o = mc.options;
        int k = t % 200;
        if (t > 60 && round < 3) {
            if (k == 0) System.out.println("[MPTEST] client round " + round + " start");
            if (k == 10) {
                mc.player.getInventory().selected = round == 2 ? 4 : round;
                mc.player.setXRot(0);
            }
            if (k == 20) KeyMapping.click(o.keyUse.getKey());
            if (k == 60) System.out.println("[MPTEST] client round " + round + " piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting()
                    + " camera=" + mc.getCameraEntity().getType().toShortString());
            if (k == 60) KeyMapping.set(o.keyUp.getKey(), true);
            if (k == 90) KeyMapping.set(o.keyUp.getKey(), false);
            if (k == 100) KeyMapping.click(o.keyAttack.getKey());       // 클릭 폭파
            if (k == 140) {
                System.out.println("[MPTEST] client round " + round + " after click piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting()
                        + " camera=" + mc.getCameraEntity().getType().toShortString() + " drones=" + mc.level.getEntitiesOfClass(
                        com.tycooncraft.vehicle.DroneEntity.class, mc.player.getBoundingBox().inflate(64)).size() + " hp=" + mc.player.getHealth());
                round++;
            }
        }
        // 4라운드: 소환 직후 바로 클릭 / 5라운드: Shift 로 내려놓고 드론을 우클릭해 다시 조종 → 클릭
        if (round == 3) {
            if (k == 10) mc.player.getInventory().selected = 3;
            if (k == 20) KeyMapping.click(o.keyUse.getKey());
            if (k == 40) KeyMapping.click(o.keyAttack.getKey());
            if (k == 90) {
                System.out.println("[MPTEST] client round 3 instant-click hp=" + mc.player.getHealth() + " piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting());
                round++;
            }
            return;
        }
        if (round == 4) {
            if (k == 10) mc.player.getInventory().selected = 2;
            if (k == 20) KeyMapping.click(o.keyUse.getKey());
            if (k == 50) KeyMapping.set(o.keyShift.getKey(), true);           // 조종 끝 (드론은 제자리)
            if (k == 54) KeyMapping.set(o.keyShift.getKey(), false);
            if (k == 70) {
                var ds = mc.level.getEntitiesOfClass(com.tycooncraft.vehicle.DroneEntity.class, mc.player.getBoundingBox().inflate(8));
                System.out.println("[MPTEST] client round 4 released piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting() + " nearbyDrones=" + ds.size());
                if (!ds.isEmpty()) {
                    var d = ds.get(0);
                    mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, d.position().add(0, 0.2, 0));
                }
            }
            if (k == 74) KeyMapping.click(o.keyUse.getKey());                 // 드론 우클릭 → 다시 조종
            if (k == 100) {
                System.out.println("[MPTEST] client round 4 re-pilot piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting());
                KeyMapping.click(o.keyAttack.getKey());
            }
            if (k == 150) {
                System.out.println("[MPTEST] client round 4 after click piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting() + " hp=" + mc.player.getHealth());
                round++;
            }
            return;
        }
        if (round >= 5 && k == 180) {
            System.out.println("[MPTEST] client still connected, finished OK");
            System.out.println("[MPTEST] finished");
            mc.stop();
        }
    }

    private MpTestClient() {}
}
