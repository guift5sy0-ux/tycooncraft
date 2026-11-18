package com.tycooncraft.client.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tycooncraft.TycoonCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ShieldModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 경찰 방패: 바닐라 방패 모양에 진압 방패 텍스처 */
public class PoliceShieldRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/police_shield.png");
    private ShieldModel model;

    public PoliceShieldRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (model == null) model = new ShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.SHIELD));
        pose.pushPose();
        pose.scale(1.0f, -1.0f, -1.0f);
        VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buffers, model.renderType(TEX), true, stack.hasFoil());
        model.handle().render(pose, vc, light, overlay);
        model.plate().render(pose, vc, light, overlay);
        pose.popPose();
    }
}
