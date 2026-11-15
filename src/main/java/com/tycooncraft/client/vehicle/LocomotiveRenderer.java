package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.vehicle.LocomotiveEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 기관차: 레일 방향·경사를 따라가는 차체(바닐라 광산 수레와 같은 계산) + 바퀴축 4개 */
public class LocomotiveRenderer extends EntityRenderer<LocomotiveEntity> {
    private static final float K = 2f;
    private static final float[] AXLES = {-47, -25, 25, 47};

    public LocomotiveRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 1.0f;
    }

    @Override
    public boolean shouldRender(LocomotiveEntity e, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        AABB box = e.getBoundingBox().inflate(2.2, 0.5, 2.2);
        return frustum.isVisible(box) || super.shouldRender(e, frustum, x, y, z);
    }

    @Override
    public void render(LocomotiveEntity cart, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        double x = Mth.lerp(pt, cart.xOld, cart.getX());
        double y = Mth.lerp(pt, cart.yOld, cart.getY());
        double z = Mth.lerp(pt, cart.zOld, cart.getZ());
        float pitch = Mth.lerp(pt, cart.xRotO, cart.getXRot());
        Vec3 on = cart.getPos(x, y, z);
        if (on != null) {
            Vec3 a = cart.getPosOffs(x, y, z, 0.3F);
            Vec3 b = cart.getPosOffs(x, y, z, -0.3F);
            if (a == null) a = on;
            if (b == null) b = on;
            ps.translate(on.x - x, (a.y + b.y) / 2.0 - y, on.z - z);
            Vec3 d = b.add(-a.x, -a.y, -a.z);
            if (d.length() != 0.0) {
                d = d.normalize();
                entityYaw = (float) (Math.atan2(d.z, d.x) * 180.0 / Math.PI);
                pitch = (float) (Math.atan(d.y) * 73.0);
            }
        }
        ps.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        ps.mulPose(Axis.ZP.rotationDegrees(-pitch));
        ps.mulPose(Axis.YP.rotationDegrees(90.0F));   // 모델 길이 방향(Z)을 레일 방향(X)에 맞춤

        float hurt = cart.getHurtTime() - pt;
        if (hurt > 0) ps.mulPose(Axis.XP.rotationDegrees(Mth.sin(hurt) * hurt * cart.getDamage() / 40f * cart.getHurtDir()));

        VertexConsumer vc = VehicleModels.buffer(buffers);
        VehicleModels.body(ps, vc, VehicleModels.LOCO_BODY, K, light);
        float wheel = Mth.lerp(pt, cart.wheelAngleO, cart.wheelAngle);
        for (float az : AXLES) {
            ps.pushPose();
            VehicleModels.translateUnits(ps, 0, 8, az);
            ps.mulPose(Axis.XP.rotation(wheel));
            VehicleModels.part(ps, vc, VehicleModels.LOCO_WHEELS, K, light);
            ps.popPose();
        }
        ps.popPose();
        super.render(cart, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(LocomotiveEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
