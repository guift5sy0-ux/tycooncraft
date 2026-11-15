package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.vehicle.JetEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** 전투기: 기수 방향(요·피치) + 선회 시 롤 */
public class JetRenderer extends EntityRenderer<JetEntity> {
    private static final float K = 6f;

    public JetRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.5f;
    }

    @Override
    public boolean shouldRender(JetEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        AABB box = e.getBoundingBox().inflate(6.0, 2.0, 6.0);
        return f.isVisible(box) || super.shouldRender(e, f, x, y, z);
    }

    @Override
    public void render(JetEntity j, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, j.yRotO, j.getYRot());
        float pitch = Mth.lerp(pt, j.xRotO, j.getXRot());
        ps.translate(0, 1.0f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        ps.mulPose(Axis.XP.rotationDegrees(-pitch));
        ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, j.rollO, j.roll) - j.barrelRoll(pt)));
        ps.translate(0, -1.0f, 0);
        VehicleModels.hurtWobble(ps, j.getHurtTime(), pt);
        VehicleModels.body(ps, VehicleModels.buffer(buffers), VehicleModels.JET_BODY, K, light);
        ps.popPose();
        super.render(j, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(JetEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
