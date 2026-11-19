package com.tycooncraft.client.cctv;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.cctv.CctvBlock;
import com.tycooncraft.cctv.CctvBlockEntity;
import com.tycooncraft.client.vehicle.VehicleModels;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;

/** CCTV 머리: 벽 브래킷 끝에서 보는 방향대로 좌우·위아래로 돎 */
public class CctvRenderer implements BlockEntityRenderer<CctvBlockEntity> {
    public CctvRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(CctvBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
        var st = be.getBlockState();
        if (!st.hasProperty(CctvBlock.FACING)) return;
        float facingRot = switch (st.getValue(CctvBlock.FACING)) {   // 블록 상태 y 회전 (북 0, 동 90, 남 180, 서 270)
            case EAST -> 90f;
            case SOUTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
        float yaw = Mth.lerp(pt, be.shownYawO, be.shownYaw), pitch = Mth.lerp(pt, be.shownPitchO, be.shownPitch);
        ps.pushPose();
        ps.translate(0.5, 0, 0.5);
        ps.mulPose(Axis.YP.rotationDegrees(-facingRot));
        ps.translate(-0.5, 0, -0.5);
        ps.translate(8 / 16.0, 9 / 16.0, 9.5 / 16.0);           // 팔 끝 (회전축)
        ps.mulPose(Axis.YP.rotationDegrees(-yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        VehicleModels.part(ps, VehicleModels.cutout(buf), VehicleModels.CCTV_HEAD, 1f, light);
        ps.popPose();
    }
}
