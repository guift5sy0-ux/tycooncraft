package com.tycooncraft.client.ship;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.ship.ShipCollisions;
import com.tycooncraft.ship.ShipEntity;
import com.tycooncraft.ship.ShipPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/** 배: 조종 입력 전송, 갑판 위 내 캐릭터 실어 나르기, 조종기 클릭 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ShipClient {
    private static float lastF = 9, lastT = 9;
    private static int sendAge;

    static {
        ShipEntity.LOCAL_PLAYER = () -> Minecraft.getInstance().player;
    }

    public static void init() {
        ShipEntity.LOCAL_PLAYER = () -> Minecraft.getInstance().player;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null || mc.level == null) return;
        // 조종 입력
        if (pl.getVehicle() instanceof ShipEntity ship && ship.pilot() == pl) {
            float f = pl.input.forwardImpulse, t = pl.input.leftImpulse;
            if (f != lastF || t != lastT || ++sendAge >= 5) {
                PacketDistributor.sendToServer(new ShipPayloads.Input(f, t));
                lastF = f;
                lastT = t;
                sendAge = 0;
            }
        } else {
            lastF = lastT = 9;
        }
        // 갑판 위: 배가 움직인 만큼 함께 이동·회전 (모든 엔티티 틱이 끝난 뒤라 화면이 부드럽다)
        for (ShipEntity s : ShipCollisions.ships(mc.level)) {
            if (s.carryFromForClient() == null) continue;
            Vec3 np = s.carryPointForClient(pl.position());
            pl.setPos(np.x, np.y, np.z);
            float dy = s.carryYawForClient();
            pl.setYRot(pl.getYRot() + dy);
            pl.setYHeadRot(pl.getYHeadRot() + dy);
            s.clearCarry();
        }
    }

    /** 배의 블록을 바라보고 우클릭: 조종기면 탑승/정박, 그 밖엔 뒤의 블록을 건드리지 않게 막는다 */
    @SubscribeEvent
    public static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null || mc.level == null || pl.getVehicle() instanceof ShipEntity) return;
        Vec3 eye = pl.getEyePosition();
        double reach = pl.blockInteractionRange();
        Vec3 end = eye.add(pl.getViewVector(1f).scale(reach));
        double best = Double.MAX_VALUE;
        ShipEntity bestShip = null;
        boolean helm = false;
        for (ShipEntity s : ShipCollisions.ships(mc.level)) {
            if (!s.worldBounds().inflate(reach).contains(eye)) continue;
            AABB[] boxes = s.boxes();
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i] == null) continue;
                Optional<Vec3> hit = boxes[i].clip(eye, end);
                if (hit.isEmpty()) continue;
                double d = hit.get().distanceToSqr(eye);
                if (d < best) {
                    best = d;
                    bestShip = s;
                    helm = s.isHelmBox(i);
                }
            }
        }
        if (bestShip == null) return;
        HitResult hr = mc.hitResult;
        if (hr != null && hr.getType() != HitResult.Type.MISS && hr.getLocation().distanceToSqr(eye) + 1e-4 < best) return;
        if (event.isUseItem() && helm) {
            PacketDistributor.sendToServer(new ShipPayloads.Use(bestShip.getId(), pl.isShiftKeyDown()));
            event.setSwingHand(true);
            event.setCanceled(true);
        } else if (event.isUseItem() || event.isAttack()) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private ShipClient() {}
}
