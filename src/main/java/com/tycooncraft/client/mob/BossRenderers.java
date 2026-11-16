package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mob.Behemoth;
import com.tycooncraft.mob.DestroyerGolem;
import com.tycooncraft.mob.StormWyvern;
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

    public static class Beast extends MobRenderer<Behemoth, BossModels.Beast> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/behemoth.png");

        public Beast(EntityRendererProvider.Context ctx) {
            super(ctx, new BossModels.Beast(ctx.bakeLayer(BossModels.BEHEMOTH)), 2.4f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/behemoth_glow.png")));
        }

        @Override
        protected void scale(Behemoth e, PoseStack ps, float pt) {
            ps.scale(1.6f, 1.6f, 1.6f);
        }

        @Override
        public ResourceLocation getTextureLocation(Behemoth e) {
            return TEX;
        }
    }

    public static class Wyvern extends MobRenderer<StormWyvern, BossModels.Wyvern> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/storm_wyvern.png");

        public Wyvern(EntityRendererProvider.Context ctx) {
            super(ctx, new BossModels.Wyvern(ctx.bakeLayer(BossModels.WYVERN)), 2.0f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/storm_wyvern_glow.png")));
        }

        @Override
        protected void scale(StormWyvern e, PoseStack ps, float pt) {
            ps.scale(1.8f, 1.8f, 1.8f);
        }

        @Override
        public ResourceLocation getTextureLocation(StormWyvern e) {
            return TEX;
        }
    }

    private BossRenderers() {}
}
