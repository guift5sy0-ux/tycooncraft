package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.village.VillageGuard;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** v0.25.0 마을 경비병: 남색 제복 + 헬멧·방탄조끼, 적을 보면 소총을 겨눔 */
public class GuardRenderer extends HumanoidMobRenderer<VillageGuard, GuardRenderer.Model> {
    private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/village_guard.png");

    public GuardRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new Model(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(VillageGuard e) {
        return TEX;
    }

    @Override
    protected void scale(VillageGuard e, PoseStack ps, float pt) {
        ps.scale(0.9375f, 0.9375f, 0.9375f);
    }

    public static class Model extends HumanoidModel<VillageGuard> {
        public Model(ModelPart root) {
            super(root);
        }

        @Override
        public void prepareMobModel(VillageGuard e, float limbSwing, float amount, float pt) {
            super.prepareMobModel(e, limbSwing, amount, pt);
            if (e.isAggressive()) {
                rightArmPose = e.getMainHandItem().getItem() instanceof com.tycooncraft.gun.GunItem
                        ? com.tycooncraft.client.GunPoses.poseFor(e.getMainHandItem()) : ArmPose.BOW_AND_ARROW;
            } else rightArmPose = e.getMainHandItem().isEmpty() ? ArmPose.EMPTY : ArmPose.ITEM;
            leftArmPose = ArmPose.EMPTY;
        }
    }
}
