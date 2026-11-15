package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.vehicle.CarEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 자동차: 차체 + 바퀴 4개(굴러감, 앞바퀴 조향), 회전할 때 차체 기울어짐 */
public class CarRenderer extends EntityRenderer<CarEntity> {
    private static final float K = 2f;
    /** 바퀴 중심 (단위 1/32 블록): 앞 = -Z */
    private static final float[][] WHEELS = {{31, 15, -46}, {-31, 15, -46}, {31, 15, 46}, {-31, 15, 46}};

    public CarRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.3f;
    }

    @Override
    public void render(CarEntity car, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, car.yRotO, car.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        VehicleModels.hurtWobble(ps, car.getHurtTime(), pt);
        VertexConsumer vc = VehicleModels.buffer(buffers);

        float steer = Mth.lerp(pt, car.steerO, car.steer);
        float wheel = Mth.lerp(pt, car.wheelAngleO, car.wheelAngle);
        // 차체만 바깥으로 살짝 기울임 (바퀴는 땅에)
        ps.pushPose();
        float lean = Mth.clamp(steer * Math.abs(car.forwardSpeed) * 0.35f, -3.5f, 3.5f);
        ps.translate(0, 0.45f, 0);
        ps.mulPose(Axis.ZP.rotationDegrees(lean));
        ps.translate(0, -0.45f, 0);
        VehicleModels.body(ps, vc, VehicleModels.CAR_BODY, K, light);
        ps.popPose();

        for (int i = 0; i < 4; i++) {
            float[] w = WHEELS[i];
            ps.pushPose();
            VehicleModels.translateUnits(ps, w[0], w[1], w[2]);
            if (w[2] < 0) ps.mulPose(Axis.YP.rotationDegrees(-steer));
            ps.mulPose(Axis.XP.rotation(wheel));
            VehicleModels.part(ps, vc, VehicleModels.CAR_WHEEL, K, light);
            ps.popPose();
        }
        ps.popPose();
        super.render(car, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(CarEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
