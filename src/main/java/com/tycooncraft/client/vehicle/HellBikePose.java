package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.vehicle.HellBikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.joml.Vector3f;

/**
 * 헬파이어 바이크 탑승 자세.
 * 운전자: 두 손은 조향에 따라 움직이는 에이프행어 손잡이를, 두 발은 포워드 발판을 짚음 (팔·다리를 그 점으로 향하게 계산).
 * 동승자: 운전자 허리를 잡음. 둘 다 바이크와 같이 코너에서 눕고 윌리 때 뒤로 젖혀짐.
 * 모델 단위가 1/16 블록이라 플레이어 모델 픽셀과 같은 크기.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class HellBikePose {
    /** 바이크 모델 좌표: 엉덩이(운전석), 손잡이, 발판 */
    private static final float HIP_Y = 16.6f, HIP_Z = 14.5f;
    private static final float GRIP_Y = 29.83f, GRIP_Z = 6.44f, GRIP_L = 2.2f, GRIP_R = 13.8f;
    private static final float PEG_Y = 8.5f, PEG_Z = 5.1f, PEG_L = 3.9f, PEG_R = 12.1f;

    /** HumanoidModel.setupAnim 끝에서 불림 (mixin) */
    public static void apply(HumanoidModel<?> m, LivingEntity e) {
        if (!(e.getVehicle() instanceof HellBikeEntity bike)) return;
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        if (bike.getFirstPassenger() == e) {
            float steer = HellBikeRenderer.steerRad(bike, pt);
            m.body.xRot = 0.06f;
            // 어깨 관절: 엉덩이에서 10px 위, 좌우 5px
            aim(m.rightArm, new Vector3f(13f, HIP_Y + 10f, HIP_Z), grip(GRIP_R, steer));
            aim(m.leftArm, new Vector3f(3f, HIP_Y + 10f, HIP_Z), grip(GRIP_L, steer));
            aim(m.rightLeg, new Vector3f(9.9f, HIP_Y, HIP_Z), new Vector3f(PEG_R, PEG_Y, PEG_Z));
            aim(m.leftLeg, new Vector3f(6.1f, HIP_Y, HIP_Z), new Vector3f(PEG_L, PEG_Y, PEG_Z));
            m.rightArm.zRot = m.leftArm.zRot = 0;
            m.rightLeg.zRot = m.leftLeg.zRot = 0;
        } else {
            // 동승자: 앞사람 허리를 감싸 잡음
            m.rightArm.xRot = -0.95f;
            m.rightArm.yRot = -0.35f;
            m.leftArm.xRot = -0.95f;
            m.leftArm.yRot = 0.35f;
            m.rightArm.zRot = m.leftArm.zRot = 0;
            m.rightLeg.xRot = -1.15f;
            m.leftLeg.xRot = -1.15f;
            m.rightLeg.yRot = 0.25f;
            m.leftLeg.yRot = -0.25f;
        }
    }

    /** 조향축으로 돈 손잡이 위치 (바이크 모델 좌표) */
    private static Vector3f grip(float x, float steer) {
        Vector3f head = new Vector3f(8f, HellBikeRenderer.HEAD_Y, HellBikeRenderer.HEAD_Z);
        Vector3f p = new Vector3f(x, GRIP_Y, GRIP_Z).sub(head);
        HellBikeRenderer.steerQuat(steer).transform(p);
        return p.add(head);
    }

    /**
     * 관절 from 에서 to 를 향하도록 팔·다리 회전 (바이크 모델 좌표 → 플레이어 모델 공간:
     * 플레이어 모델은 x 가 반대, y 가 아래, z 는 같음). 팔다리는 기본이 아래(+y) 방향.
     */
    private static void aim(ModelPart part, Vector3f from, Vector3f to) {
        Vector3f v = new Vector3f(to).sub(from);
        v.set(-v.x, -v.y, v.z).normalize();
        float x = (float) -Math.acos(Mth.clamp(v.y, -1f, 1f));
        float sx = Mth.sin(x);
        float y = Math.abs(sx) < 1e-4f ? 0f : (float) Math.atan2(v.x / sx, v.z / sx);
        part.xRot = x;
        part.yRot = y;
    }

    // ── 탑승자도 바이크와 같이 기울기 ──
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void pre(RenderLivingEvent.Pre<?, ?> ev) {
        if (ev.isCanceled() || !(ev.getEntity().getVehicle() instanceof HellBikeEntity bike)) return;
        float pt = ev.getPartialTick();
        PoseStack ps = ev.getPoseStack();
        ps.pushPose();
        Vec3 d = bike.getPosition(pt).subtract(ev.getEntity().getPosition(pt));
        float yaw = Mth.rotLerp(pt, bike.yRotO, bike.getYRot());
        ps.translate(d.x, d.y, d.z);
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        HellBikeRenderer.tilt(ps, bike, pt);
        ps.mulPose(Axis.YP.rotationDegrees(-(180f - yaw)));
        ps.translate(-d.x, -d.y, -d.z);
    }

    @SubscribeEvent
    public static void post(RenderLivingEvent.Post<?, ?> ev) {
        if (ev.getEntity().getVehicle() instanceof HellBikeEntity) ev.getPoseStack().popPose();
    }

    private HellBikePose() {}
}
