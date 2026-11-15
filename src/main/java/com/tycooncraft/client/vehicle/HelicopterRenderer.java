package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.vehicle.HelicopterEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** 헬기: 동체 + 메인 로터(회전) + 꼬리 로터(회전), 전진하면 기수가 숙여지고 선회하면 기울어짐 */
public class HelicopterRenderer extends EntityRenderer<HelicopterEntity> {
    private static final float K = 5f;
    private static final float[] MAST = {0, 100, -6};
    private static final float[] TAIL = {6, 70, 190};

    public HelicopterRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.6f;
    }

    @Override
    public boolean shouldRender(HelicopterEntity e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        // 꼬리와 로터가 히트박스 밖으로 길게 나가므로 넉넉한 상자로 판정
        AABB box = e.getBoundingBox().inflate(7.0, 2.0, 7.0);
        return frustum.isVisible(box) || super.shouldRender(e, frustum, x, y, z);
    }

    @Override
    public void render(HelicopterEntity heli, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, heli.yRotO, heli.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        VehicleModels.hurtWobble(ps, heli.getHurtTime(), pt);
        // 무게중심(로터 마스트 아래) 기준으로 기울이기
        ps.translate(0, 1.4f, -0.2f);
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, heli.pitchO, heli.pitch)));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, heli.rollO, heli.roll)));
        ps.translate(0, -1.4f, 0.2f);
        VertexConsumer vc = VehicleModels.buffer(buffers);
        VehicleModels.body(ps, vc, VehicleModels.HELI_BODY, K, light);

        ps.pushPose();
        VehicleModels.translateUnits(ps, MAST[0], MAST[1], MAST[2]);
        ps.mulPose(Axis.YP.rotation(Mth.lerp(pt, heli.rotorAngleO, heli.rotorAngle)));
        VehicleModels.part(ps, vc, VehicleModels.HELI_ROTOR, K, light);
        ps.popPose();

        ps.pushPose();
        VehicleModels.translateUnits(ps, TAIL[0], TAIL[1], TAIL[2]);
        ps.mulPose(Axis.XP.rotation(Mth.lerp(pt, heli.tailAngleO, heli.tailAngle)));
        VehicleModels.part(ps, vc, VehicleModels.HELI_TAIL_ROTOR, K, light);
        ps.popPose();

        ps.popPose();
        super.render(heli, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(HelicopterEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
