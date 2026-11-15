package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.vehicle.TankEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;

/** 전차: 차체 + 바퀴(굴러감) + 포탑(조종자가 보는 쪽) + 포신(위아래, 발사 반동) */
public class TankRenderer extends EntityRenderer<TankEntity> {
    private static final float K = 3f;
    private static final float[][] WHEELS;
    static {
        float[] zs = {-72, -46, -20, 6, 32, 58};
        WHEELS = new float[16][];
        int k = 0;
        for (int s = -1; s <= 1; s += 2) {
            for (float z : zs) WHEELS[k++] = new float[]{s * 48, 13, z};
            WHEELS[k++] = new float[]{s * 48, 17, -86};
            WHEELS[k++] = new float[]{s * 48, 17, 84};
        }
    }

    public TankRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 2.2f;
    }

    @Override
    public boolean shouldRender(TankEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        AABB box = e.getBoundingBox().inflate(4.0, 1.0, 4.0);
        return f.isVisible(box) || super.shouldRender(e, f, x, y, z);
    }

    @Override
    public void render(TankEntity t, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float hull = Mth.rotLerp(pt, t.yRotO, t.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - hull));
        VehicleModels.hurtWobble(ps, t.getHurtTime(), pt);
        VertexConsumer vc = VehicleModels.buffer(buffers);
        VehicleModels.body(ps, vc, VehicleModels.TANK_HULL, K, light);
        float wheel = Mth.lerp(pt, t.wheelAngleO, t.wheelAngle);
        for (float[] w : WHEELS) {
            ps.pushPose();
            VehicleModels.translateUnits(ps, w[0], w[1], w[2]);
            ps.mulPose(Axis.XP.rotation(wheel));
            VehicleModels.part(ps, vc, VehicleModels.TANK_WHEEL, K, light);
            ps.popPose();
        }
        // 포탑
        float turret = Mth.rotLerp(pt, t.turretYawO, t.turretYaw);
        float elev = 0;
        if (t.getControllingPassenger() instanceof LivingEntity g) elev = Mth.clamp(-g.getViewXRot(pt), -6f, 20f);
        ps.pushPose();
        VehicleModels.translateUnits(ps, 0, 46, 8);
        ps.mulPose(Axis.YP.rotationDegrees(hull - turret));
        VehicleModels.part(ps, vc, VehicleModels.TANK_TURRET, K, light);
        VehicleModels.translateUnits(ps, 0, 12, -30);
        ps.mulPose(Axis.XP.rotationDegrees(elev));
        float rec = Mth.lerp(pt, t.recoilO, t.recoil);
        VehicleModels.translateUnits(ps, 0, 0, rec * 10);
        VehicleModels.part(ps, vc, VehicleModels.TANK_BARREL, K, light);
        ps.popPose();
        ps.popPose();
        super.render(t, entityYaw, pt, ps, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(TankEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
