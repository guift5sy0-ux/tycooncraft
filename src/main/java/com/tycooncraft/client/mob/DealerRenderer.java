package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** v0.26.0 암상인: 밀수꾼(검붉은 가죽), 브로커(보라 정장), 장물아비(갈색 코트) */
public class DealerRenderer extends HumanoidMobRenderer<com.tycooncraft.blackmarket.BlackMarketDealer, DealerRenderer.Model> {
    private static final ResourceLocation[] TEX = {TycoonCraft.id("textures/entity/bm_arms.png"), TycoonCraft.id("textures/entity/bm_broker.png"),
            TycoonCraft.id("textures/entity/bm_fence.png")};

    public DealerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(com.tycooncraft.blackmarket.BlackMarketDealer e) {
        return TEX[Math.floorMod(e.kind(), 3)];
    }

    @Override
    protected void scale(com.tycooncraft.blackmarket.BlackMarketDealer e, PoseStack ps, float pt) {
        ps.scale(0.9375f, 0.9375f, 0.9375f);
    }

    public static class Model extends HumanoidModel<com.tycooncraft.blackmarket.BlackMarketDealer> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(com.tycooncraft.blackmarket.BlackMarketDealer e, float limbSwing, float amount, float pt) {
            super.prepareMobModel(e, limbSwing, amount, pt);
            com.tycooncraft.client.GunPoses.mobArms(this, e);
        }
    }
}
