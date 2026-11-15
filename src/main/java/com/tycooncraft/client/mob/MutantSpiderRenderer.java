package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mob.MutantSpider;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.resources.ResourceLocation;

/** 변이 거미 여왕: 거미 모델 3.2배 + 전용 텍스처 */
public class MutantSpiderRenderer extends SpiderRenderer<MutantSpider> {
    private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/mutant_spider.png");

    public MutantSpiderRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, ModelLayers.SPIDER);
        this.shadowRadius = 2.6f;
    }

    @Override
    protected void scale(MutantSpider e, PoseStack ps, float pt) {
        ps.scale(3.2f, 3.2f, 3.2f);
    }

    @Override
    public ResourceLocation getTextureLocation(MutantSpider e) {
        return TEX;
    }
}
