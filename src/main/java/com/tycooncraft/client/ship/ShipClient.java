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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/** 배: 조종 입력 전송, 갑판 위 내 캐릭터 실어 나르기, 조종기 클릭 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ShipClient {
    private static float lastF = 9, lastT = 9, lastV = 9;
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
            // 부유체 운송기: Space 상승 / Ctrl(달리기 키) 하강
            float v = pl.input.jumping ? 1 : mc.options.keySprint.isDown() ? -1 : 0;
            if (f != lastF || t != lastT || v != lastV || ++sendAge >= 5) {
                PacketDistributor.sendToServer(new ShipPayloads.Input(f, t, v));
                lastF = f;
                lastT = t;
                lastV = v;
                sendAge = 0;
            }
        } else {
            lastF = lastT = lastV = 9;
        }
        // 실린 컨베이어 위의 나 (몹·아이템은 서버가 민다)
        if (!pl.isPassenger())
            for (ShipEntity s : ShipCollisions.ships(mc.level))
                if (s.worldBounds().inflate(1, 2, 1).contains(pl.position()) && s.conveyorPush(pl)) break;
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

    // ── 곡괭이로 움직이는 운송기의 부유체·엔진 캐기 ──
    private static int mineShip = -1;
    private static long mineKey;
    private static float mineProgress;

    @SubscribeEvent
    public static void onMineTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer pl = mc.player;
        if (pl == null || mc.level == null || mc.screen != null || !mc.options.keyAttack.isDown() || pl.getVehicle() instanceof ShipEntity) {
            mineShip = -1;
            return;
        }
        Vec3 eye = pl.getEyePosition();
        Vec3 dir = pl.getViewVector(1f);
        double reach = pl.blockInteractionRange();
        var hit = ShipCollisions.clip(mc.level, eye, eye.add(dir.scale(reach)));
        HitResult hr = mc.hitResult;
        if (hit == null || (hr != null && hr.getType() != HitResult.Type.MISS && hr.getLocation().distanceToSqr(eye) + 1e-4 < hit.point().distanceToSqr(eye))) {
            mineShip = -1;
            return;
        }
        ShipEntity s = hit.ship();
        ShipEntity.Cell c = s.cellAtWorld(hit.point().add(dir.scale(0.02)));
        ItemStack tool = pl.getMainHandItem();
        if (c == null || !ShipEntity.minable(c, tool)) {
            mineShip = -1;
            return;
        }
        long k = ShipEntity.key(c.x(), c.y(), c.z());
        if (s.getId() != mineShip || k != mineKey) {
            mineShip = s.getId();
            mineKey = k;
            mineProgress = 0;
        }
        // 바닐라 채굴 속도: 도구 속도(효율·성급함 포함) / 단단함 / 30
        float hardness = Math.max(0.1f, c.state().getDestroySpeed(mc.level, net.minecraft.core.BlockPos.ZERO));
        mineProgress += pl.getDigSpeed(c.state(), null) / hardness / 30f;
        pl.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        if (pl.tickCount % 4 == 0) {
            Vec3 p = hit.point();
            mc.level.addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, c.state()), p.x, p.y, p.z, 0, 0.05, 0);
            mc.level.playLocalSound(p.x, p.y, p.z, c.state().getSoundType().getHitSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 0.6f, false);
        }
        if (mineProgress >= 1f) {
            PacketDistributor.sendToServer(new ShipPayloads.Mine(mineShip, mineKey));
            mineShip = -1;
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
