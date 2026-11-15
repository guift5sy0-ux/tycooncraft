package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.entity.RocketEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 로켓·포탄·미사일: 날아가는 방향을 향하게 */
public class RocketRenderer extends EntityRenderer<RocketEntity> {

    public RocketRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(RocketEntity r, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        ps.mulPose(Axis.YP.rotationDegrees(Mth.lerp(pt, r.yRotO, r.getYRot()) + 180f));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, r.xRotO, r.getXRot())));
        boolean shell = r.kind() == RocketEntity.SHELL;
        VehicleModels.part(ps, VehicleModels.cutout(buffers), shell ? VehicleModels.SHELL : VehicleModels.ROCKET, r.kind() == RocketEntity.MISSILE ? 1.6f : 1f, 0xF000F0);
        ps.popPose();
        super.render(r, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(RocketEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
