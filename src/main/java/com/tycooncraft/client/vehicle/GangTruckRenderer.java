package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.mob.GangTruck;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 갱단 무장 트럭: 차체 + 바퀴 4개(굴러감) + 짐칸 기관총(좌우·상하 조준) */
public class GangTruckRenderer extends EntityRenderer<GangTruck> {
    private static final float K = 2f;
    private static final float[][] WHEELS = {{34, 16, -50}, {-34, 16, -50}, {34, 16, 52}, {-34, 16, 52}};

    public GangTruckRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.4f;
    }

    @Override
    public void render(GangTruck t, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, t.yRotO, t.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        VehicleModels.hurtWobble(ps, t.hurtTime, pt);
        if (t.deathTime > 0) ps.mulPose(Axis.ZP.rotationDegrees(Math.min(t.deathTime + pt, 12f) * 1.2f));
        VertexConsumer vc = VehicleModels.buffer(buffers);
        VehicleModels.body(ps, vc, VehicleModels.GANG_TRUCK_BODY, K, light);
        float wheel = Mth.lerp(pt, t.wheelAngleO, t.wheelAngle);
        for (float[] w : WHEELS) {
            ps.pushPose();
            VehicleModels.translateUnits(ps, w[0], w[1], w[2]);
            ps.mulPose(Axis.XP.rotation(-wheel));
            VehicleModels.part(ps, vc, VehicleModels.GANG_TRUCK_WHEEL, K, light);
            ps.popPose();
        }
        ps.pushPose();
        VehicleModels.translateUnits(ps, GangTruck.MG_PIVOT.x, GangTruck.MG_PIVOT.y, GangTruck.MG_PIVOT.z);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(pt, t.mgYawO, t.mgYaw())));
        ps.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(pt, t.mgPitchO, t.mgPitch())));
        VehicleModels.part(ps, vc, VehicleModels.GANG_MG, K, light);
        ps.popPose();
        ps.popPose();
        super.render(t, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(GangTruck t) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
