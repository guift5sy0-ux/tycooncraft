package com.tycooncraft.client.mech;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.mech.PilotBody;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** v0.28.2 원격 조종 중 남겨진 몸: 주인 스킨 그대로. 주인 자신의 화면에는 안 그림(내 몸이 이미 그 자리에 있음) */
public class PilotBodyRenderer extends LivingEntityRenderer<PilotBody, PlayerModel<PilotBody>> {
    public PilotBodyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
        addLayer(new ItemInHandLayer<>(this, ctx.getItemInHandRenderer()));
    }

    @Override
    public void render(PilotBody e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && e.owner().map(u -> u.equals(mc.player.getUUID())).orElse(false)) return;
        getModel().head.xRot = 0.5f;   // 고개 숙이고 조종기를 보는 자세
        super.render(e, yaw, pt, ps, buf, light);
    }

    @Override
    public ResourceLocation getTextureLocation(PilotBody e) {
        var mc = Minecraft.getInstance();
        var u = e.owner().orElse(null);
        if (u == null) return DefaultPlayerSkin.getDefaultTexture();
        var info = mc.getConnection() == null ? null : mc.getConnection().getPlayerInfo(u);
        return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(u).texture();
    }
}
