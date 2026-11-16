package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.WitherArmorLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.boss.wither.WitherBoss;

import java.util.Map;

/**
 * 심연의 위더 외형: 바닐라와 같은 뼈대·관절 움직임 + 뿔 왕관·가시 견갑·갈비뼈 속 영혼 수정,
 * 4배 해상도 흑요석 해골 텍스처와 빛나는 성난 눈.
 */
public final class AbyssalWither {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TycoonCraft.id("abyssal_wither"), "main");
    private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/abyssal_wither.png");
    private static final ResourceLocation TEX_INV = TycoonCraft.id("textures/entity/abyssal_wither_invulnerable.png");
    private static final ResourceLocation GLOW = TycoonCraft.id("textures/entity/abyssal_wither_glow.png");

    public static class Model extends HierarchicalModel<WitherBoss> {
        private final ModelPart root;
        private final ModelPart center, right, left, ribcage, tail;

        public Model(ModelPart root) {
            this.root = root;
            Map<String, ModelPart> b = AbyssalWitherGeo.bones(root);
            center = b.get("center_head");
            right = b.get("right_head");
            left = b.get("left_head");
            ribcage = b.get("ribcage");
            tail = b.get("tail");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void prepareMobModel(WitherBoss e, float limb, float amt, float pt) {
            head(e, right, 0);
            head(e, left, 1);
        }

        private static void head(WitherBoss e, ModelPart part, int i) {
            part.yRot = (e.getHeadYRot(i) - e.yBodyRot) * Mth.DEG_TO_RAD;
            part.xRot = e.getHeadXRot(i) * Mth.DEG_TO_RAD;
        }

        @Override
        public void setupAnim(WitherBoss e, float limb, float amt, float age, float yaw, float pitch) {
            float f = Mth.cos(age * 0.1f);
            ribcage.xRot = (0.065f + 0.05f * f) * Mth.PI;
            tail.setPos(-2.0f, 6.9f + Mth.cos(ribcage.xRot) * 10.0f, -0.5f + Mth.sin(ribcage.xRot) * 10.0f);
            tail.xRot = (0.265f + 0.1f * f) * Mth.PI;
            center.yRot = yaw * Mth.DEG_TO_RAD;
            center.xRot = pitch * Mth.DEG_TO_RAD;
        }
    }

    public static class Renderer extends MobRenderer<WitherBoss, Model> {
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Renderer(EntityRendererProvider.Context ctx) {
            super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.0f);
            addLayer(new GlowLayer<>(this, GLOW));
            addLayer((RenderLayer) new WitherArmorLayer((RenderLayerParent) this, ctx.getModelSet()));   // 각성 시 푸른 보호막 (바닐라 효과)
        }

        @Override
        protected int getBlockLightLevel(WitherBoss e, BlockPos pos) {
            return 15;
        }

        @Override
        public ResourceLocation getTextureLocation(WitherBoss e) {
            int i = e.getInvulnerableTicks();
            return i > 0 && (i > 80 || i / 5 % 2 != 1) ? TEX_INV : TEX;
        }

        @Override
        protected void scale(WitherBoss e, PoseStack ps, float pt) {
            float f = 2.0f;
            int i = e.getInvulnerableTicks();
            if (i > 0) f -= ((float) i - pt) / 220.0f * 0.5f;
            ps.scale(f, f, f);
        }
    }

    private AbyssalWither() {}
}
