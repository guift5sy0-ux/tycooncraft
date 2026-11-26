package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mob.GunnerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** 갱단원·용병·무법자·카르텔 두목 (사람 모델 + 방탄장비 표시 + 손에 든 총) */
public class GunnerRenderer extends HumanoidMobRenderer<GunnerEntity, GunnerModel<GunnerEntity>> {

    public GunnerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GunnerModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(GunnerEntity e) {
        String name = switch (e.kind()) {
            case GANGSTER -> "gangster";
            case MERCENARY -> "mercenary";
            case OUTLAW -> "outlaw";
            case BOSS -> "cartel_boss";
            case ENFORCER -> "gang_enforcer";
            case BRUTE -> "gang_brute";
            case MARKSMAN -> "gang_marksman";
        };
        return TycoonCraft.id("textures/entity/" + name + ".png");
    }

    @Override
    protected void scale(GunnerEntity e, PoseStack ps, float pt) {
        if (e.kind() == GunnerEntity.Kind.BOSS) ps.scale(1.4f, 1.4f, 1.4f);
        else if (e.kind() == GunnerEntity.Kind.BRUTE) ps.scale(1.1f, 1.1f, 1.1f);
        else if (e.kind() == GunnerEntity.Kind.ENFORCER) ps.scale(1.02f, 1.02f, 1.02f);
        else ps.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
