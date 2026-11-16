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
    static boolean reported, advance;

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
        // 라운드는 언제나 새 200틱 주기의 처음부터 시작
        if (k == 0 && advance) {
            advance = false;
            round++;
        }
        if (t > 199 && round < 3) {
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
                advance = true;
            }
        }
        // 4라운드: 소환 직후 바로 클릭 / 5라운드: Shift 로 내려놓고 드론을 우클릭해 다시 조종 → 클릭
        if (round == 3 && t > 199) {
            if (k == 10) mc.player.getInventory().selected = 3;
            if (k == 20) KeyMapping.click(o.keyUse.getKey());
            if (k == 40) KeyMapping.click(o.keyAttack.getKey());
            if (k == 90) {
                System.out.println("[MPTEST] client round 3 instant-click hp=" + mc.player.getHealth() + " piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting());
                advance = true;
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
                System.out.println("[MPTEST] client round 4 after-shift (must still pilot) piloting=" + com.tycooncraft.client.vehicle.DroneClient.piloting() + " nearbyDrones=" + ds.size());
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
                advance = true;
            }
            return;
        }
        // 서버가 1300틱·1500틱에 마을·폐공장 위로 보내 준다 → 촬영
        if (t == 1440) shot(mc, "mp_village");
        if (t == 1640) shot(mc, "mp_ruin");
        if (round >= 5 && t >= 1700) {
            System.out.println("[MPTEST] client still connected, finished OK");
            System.out.println("[MPTEST] finished");
            mc.stop();
        }
    }

    static void shot(Minecraft mc, String name) {
        net.minecraft.client.Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(), msg -> {});
        System.out.println("[MPTEST] client screenshot " + name + String.format(" at %.0f,%.0f,%.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }

    private MpTestClient() {}
}
