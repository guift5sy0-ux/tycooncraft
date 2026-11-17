package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mob.DestroyerGolem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public final class BossRenderers {
    public static class Golem extends MobRenderer<DestroyerGolem, BossModels.Golem> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/destroyer_golem.png");

        public Golem(EntityRendererProvider.Context ctx) {
            super(ctx, new BossModels.Golem(ctx.bakeLayer(BossModels.GOLEM)), 1.3f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/destroyer_golem_glow.png")));
        }

        @Override
        public ResourceLocation getTextureLocation(DestroyerGolem e) {
            return TEX;
        }
    }

    private BossRenderers() {}
}
