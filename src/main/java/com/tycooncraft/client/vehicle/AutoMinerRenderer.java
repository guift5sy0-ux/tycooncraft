package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.miner.AutoMinerEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 자동 굴착기: 차체 + 바퀴 4 + 회전 드릴, 가까이 가면 연료·상태 표시 */
public class AutoMinerRenderer extends EntityRenderer<AutoMinerEntity> {
    private static final float K = 2f;
    private static final float[][] WHEELS = {{30, 11, -16}, {-30, 11, -16}, {30, 11, 28}, {-30, 11, 28}};

    public AutoMinerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.1f;
    }

    @Override
    public void render(AutoMinerEntity m, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, m.yRotO, m.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        if (m.isWorking()) ps.translate((m.tickCount % 2 == 0 ? 0.008f : -0.008f), 0, 0);   // 진동
        VertexConsumer vc = VehicleModels.buffer(buffers);
        VehicleModels.body(ps, vc, VehicleModels.MINER_BODY, K, light);
        float wheel = Mth.lerp(pt, m.wheelAngleO, m.wheelAngle);
        for (float[] w : WHEELS) {
            ps.pushPose();
            VehicleModels.translateUnits(ps, w[0], w[1], w[2]);
            ps.mulPose(Axis.XP.rotation(-wheel));
            VehicleModels.part(ps, vc, VehicleModels.MINER_WHEEL, K, light);
            ps.popPose();
        }
        ps.pushPose();
        VehicleModels.translateUnits(ps, 0, 22, -42);
        ps.mulPose(Axis.ZP.rotation(Mth.lerp(pt, m.drillAngleO, m.drillAngle)));
        VehicleModels.part(ps, vc, VehicleModels.MINER_DRILL, K, light);
        ps.popPose();
        ps.popPose();
        var player = Minecraft.getInstance().player;
        if (player != null && player.distanceToSqr(m) < 100) {
            int pct = m.getFuel() * 100 / AutoMinerEntity.MAX_FUEL;
            Component label = Component.translatable(m.isRunning() ? "hud.tycooncraft.miner.running" : "hud.tycooncraft.miner.stopped", pct)
                    .withStyle(m.isRunning() ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
            ps.pushPose();
            ps.translate(0, 0.35, 0);
            renderNameTag(m, label, ps, buffers, light, pt);
            ps.popPose();
        }
    }

    @Override
    public ResourceLocation getTextureLocation(AutoMinerEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
