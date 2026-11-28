package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.HellBikeEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** 헬파이어 바이크: 차체 + 불타는 바퀴 2개, 코너에서 안쪽으로 크게 눕고, 불꽃 테·엔진 불빛은 어둠 속에서도 빛남 */
public class HellBikeRenderer extends EntityRenderer<HellBikeEntity> {
    private static final float K = 2f;
    /** 바퀴 중심 (1/32 블록), 앞 = -Z */
    private static final float FRONT_Z = -32f, REAR_Z = 26f, WHEEL_Y = 13.6f;

    public HellBikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.6f;
    }

    @Override
    public void render(HellBikeEntity bike, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        ps.pushPose();
        float yaw = Mth.rotLerp(pt, bike.yRotO, bike.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        VehicleModels.hurtWobble(ps, bike.getHurtTime(), pt);
        float steer = Mth.lerp(pt, bike.steerO, bike.steer);
        float wheel = Mth.lerp(pt, bike.wheelAngleO, bike.wheelAngle);
        // 코너 안쪽으로 눕기 (바퀴 접지점 기준)
        float lean = Mth.clamp(-steer * Math.min(1f, Math.abs(bike.forwardSpeed) * 1.4f) * 1.1f, -32f, 32f);
        if (bike.getControllingPassenger() == null) lean = 8f;   // 세워 두면 킥스탠드로 살짝 기움
        ps.mulPose(Axis.ZP.rotationDegrees(lean));
        VertexConsumer vc = VehicleModels.buffer(buffers);
        int full = LightTexture.FULL_BRIGHT;
        VehicleModels.body(ps, vc, VehicleModels.HELL_BODY, K, light);
        VehicleModels.body(ps, vc, VehicleModels.HELL_BODY_GLOW, K, full);
        for (int i = 0; i < 2; i++) {
            ps.pushPose();
            VehicleModels.translateUnits(ps, 0, WHEEL_Y, i == 0 ? FRONT_Z : REAR_Z);
            if (i == 0) ps.mulPose(Axis.YP.rotationDegrees(-steer * 0.8f));
            ps.mulPose(Axis.XP.rotation(wheel * 1.3f));
            VehicleModels.part(ps, vc, VehicleModels.HELL_WHEEL, K, light);
            boolean on = bike.engineRunning();
            if (on) VehicleModels.part(ps, vc, VehicleModels.HELL_GLOW, K, full);
            ps.popPose();
        }
        ps.popPose();
        super.render(bike, entityYaw, pt, ps, buffers, light);
    }

    /** 바퀴 불꽃·배기 불길·달릴 때 남는 불자국 */
    public static void fx(CarEntity bike, RandomSource r) {
        if (!bike.engineRunning()) return;
        var level = bike.level();
        float sp = Math.abs(bike.forwardSpeed);
        Vec3 pos = bike.position();
        Vec3 back = bike.forward().scale(-0.05 - sp * 0.3);
        for (float z : new float[]{FRONT_Z, REAR_Z}) {
            int n = 1 + (int) (sp * 4);
            for (int k = 0; k < n; k++) {
                double a = r.nextDouble() * Math.PI * 2;
                Vec3 p = pos.add(bike.toWorld(new Vec3((r.nextDouble() - 0.5) * 0.25, WHEEL_Y / 32.0 + Math.sin(a) * 0.42, z / 32.0 + Math.cos(a) * 0.42)));
                level.addParticle(r.nextInt(3) == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, p.x, p.y, p.z, back.x * 0.5, 0.02 + r.nextDouble() * 0.03, back.z * 0.5);
            }
        }
        // 배기구 불길
        for (int i = 0; i < 2; i++) {
            Vec3 p = pos.add(bike.toWorld(new Vec3((10.6 - 8) / 8.0, (6.0 + i * 1.2) / 8.0, (17.4 - 8) / 8.0)));
            level.addParticle(ParticleTypes.FLAME, p.x, p.y, p.z, back.x * 2, 0.03, back.z * 2);
            if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, p.x, p.y, p.z, back.x, 0.04, back.z);
        }
        // 땅에 남는 불자국
        if (sp > 0.35 && bike.onGround()) {
            Vec3 p = pos.add(bike.toWorld(new Vec3(0, 0.05, REAR_Z / 32.0 + 0.4)));
            for (int k = 0; k < 3; k++)
                level.addParticle(ParticleTypes.FLAME, p.x + (r.nextDouble() - 0.5) * 0.3, p.y, p.z + (r.nextDouble() - 0.5) * 0.3, 0, 0.01, 0);
            if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.LAVA, p.x, p.y, p.z, 0, 0, 0);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(HellBikeEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
