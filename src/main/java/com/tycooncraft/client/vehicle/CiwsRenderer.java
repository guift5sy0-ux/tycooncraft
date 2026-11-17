package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.turret.CiwsBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

/** 방공 요격 포대: 받침 위에 회전 마운트(레이더 돔)와 위아래로 움직이는 6열 개틀링 */
public class CiwsRenderer implements BlockEntityRenderer<com.tycooncraft.turret.CiwsBlockEntity> {

    public CiwsRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(CiwsBlockEntity t, float pt, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer vc = VehicleModels.cutout(buffers);
        float yaw = Mth.rotLerp(pt, t.yawO * Mth.RAD_TO_DEG, t.yaw * Mth.RAD_TO_DEG);
        float pitch = Mth.lerp(pt, t.pitchO, t.pitch) * Mth.RAD_TO_DEG;
        ps.pushPose();
        ps.translate(0.5, 14 / 32.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        VehicleModels.part(ps, vc, VehicleModels.CIWS_HEAD, 1f, light);
        ps.translate(0, 6 / 32.0, 0);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        VehicleModels.part(ps, vc, VehicleModels.CIWS_GUN, 1f, light);
        ps.popPose();
    }
}
