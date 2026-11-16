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
        // 모델의 탄두는 -Z 쪽 → 실제 속도 방향으로 정확히 돌린다 (예전엔 남북·동서가 뒤집혀 보였음)
        net.minecraft.world.phys.Vec3 v = r.getDeltaMovement();
        if (v.lengthSqr() < 1e-8) v = r.lastDir;
        else r.lastDir = v;
        double hz = Math.sqrt(v.x * v.x + v.z * v.z);
        ps.mulPose(Axis.YP.rotation((float) Math.atan2(-v.x, -v.z)));
        ps.mulPose(Axis.XP.rotation((float) Math.atan2(v.y, hz)));
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
