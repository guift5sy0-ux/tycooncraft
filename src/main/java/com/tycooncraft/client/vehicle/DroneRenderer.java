package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.vehicle.DroneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 자폭 드론: 본체 + 프로펠러 4개 (대각선끼리 반대로 회전), 비행 방향으로 기울어짐 */
public class DroneRenderer extends EntityRenderer<DroneEntity> {
    private static final float K = 1f;
    private static final float[][] MOTORS = {{12, 9.6f, -12, 1}, {-12, 9.6f, -12, -1}, {12, 9.6f, 12, -1}, {-12, 9.6f, 12, 1}};

    public DroneRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.4f;
    }

    @Override
    public void render(DroneEntity d, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, d.yRotO, d.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.translate(0, 0.2f, 0);
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, d.tiltO, d.tilt)));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, d.rollO, d.roll)));
        ps.translate(0, -0.2f, 0);
        VertexConsumer vc = VehicleModels.buffer(buffers);
        VehicleModels.body(ps, vc, VehicleModels.DRONE_BODY, K, light);
        float spin = Mth.lerp(pt, d.propAngleO, d.propAngle);
        for (float[] m : MOTORS) {
            ps.pushPose();
            VehicleModels.translateUnits(ps, m[0], m[1], m[2]);
            ps.mulPose(Axis.YP.rotation(spin * m[3] + m[0] * 0.1f));
            VehicleModels.part(ps, vc, VehicleModels.DRONE_PROP, K, light);
            ps.popPose();
        }
        ps.popPose();
        super.render(d, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(DroneEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
