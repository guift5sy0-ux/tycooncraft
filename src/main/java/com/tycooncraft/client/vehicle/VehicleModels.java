package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;

/**
 * 탈것 부품 모델(models/vehicle/*.json): 블록 모델 형식이라 크기 제한(-16~32)이 있어
 * 작게 만들어 두고 그릴 때 K배 확대한다. 단위: 1 = 1/32 블록.
 */
public final class VehicleModels {
    public static final ModelResourceLocation CAR_BODY = id("car_body");
    public static final ModelResourceLocation CAR_WHEEL = id("car_wheel");
    public static final ModelResourceLocation LOCO_BODY = id("loco_body");
    public static final ModelResourceLocation LOCO_WHEELS = id("loco_wheels");
    public static final ModelResourceLocation HELI_BODY = id("heli_body");
    public static final ModelResourceLocation HELI_ROTOR = id("heli_rotor");
    public static final ModelResourceLocation HELI_TAIL_ROTOR = id("heli_tail_rotor");
    public static final ModelResourceLocation[] ALL = {CAR_BODY, CAR_WHEEL, LOCO_BODY, LOCO_WHEELS, HELI_BODY, HELI_ROTOR, HELI_TAIL_ROTOR};

    private static ModelResourceLocation id(String name) {
        return ModelResourceLocation.standalone(TycoonCraft.id("vehicle/" + name));
    }

    public static VertexConsumer buffer(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityTranslucentCull(TextureAtlas.LOCATION_BLOCKS));
    }

    /** 차체: 모델 원점(8,0,8)이 엔티티 바닥 중심 */
    public static void body(PoseStack ps, VertexConsumer vc, ModelResourceLocation id, float k, int light) {
        draw(ps, vc, id, k, false, light);
    }

    /** 부품: 모델 중심(8,8,8)이 현재 위치 */
    public static void part(PoseStack ps, VertexConsumer vc, ModelResourceLocation id, float k, int light) {
        draw(ps, vc, id, k, true, light);
    }

    private static void draw(PoseStack ps, VertexConsumer vc, ModelResourceLocation id, float k, boolean centred, int light) {
        BakedModel model = Minecraft.getInstance().getModelManager().getModel(id);
        ps.pushPose();
        ps.scale(k, k, k);
        ps.translate(-0.5f, centred ? -0.5f : 0f, -0.5f);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer()
                .renderModel(ps.last(), vc, null, model, 1f, 1f, 1f, light, OverlayTexture.NO_OVERLAY);
        ps.popPose();
    }

    /** 맞았을 때 흔들림 (보트와 같은 방식) */
    public static void hurtWobble(PoseStack ps, int hurtTime, float partialTick) {
        float h = hurtTime - partialTick;
        if (h > 0) ps.mulPose(Axis.XP.rotationDegrees(Mth.sin(h) * h * 0.6f));
    }

    public static void translateUnits(PoseStack ps, double x, double y, double z) {
        ps.translate(x / 32.0, y / 32.0, z / 32.0);
    }

    private VehicleModels() {}
}
