package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.turret.TurretBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

/** 자동 포탑: 받침(블록 모델) 위에 회전하는 몸통과 위아래로 움직이는 쌍열 기관총 */
public class TurretRenderer implements BlockEntityRenderer<TurretBlockEntity> {

    public TurretRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(TurretBlockEntity t, float pt, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        VertexConsumer vc = VehicleModels.cutout(buffers);
        float yaw = Mth.rotLerp(pt, t.yawO * Mth.RAD_TO_DEG, t.yaw * Mth.RAD_TO_DEG);
        float pitch = Mth.lerp(pt, t.pitchO, t.pitch) * Mth.RAD_TO_DEG;
        ps.pushPose();
        ps.translate(0.5, 14 / 32.0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        VehicleModels.part(ps, vc, VehicleModels.TURRET_HEAD, 1f, light);
        ps.translate(0, 6 / 32.0, 0);
        ps.mulPose(Axis.XP.rotationDegrees(pitch));
        VehicleModels.part(ps, vc, VehicleModels.TURRET_GUN, 1f, light);
        ps.popPose();
    }
}
