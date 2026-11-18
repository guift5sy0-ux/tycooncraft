package com.tycooncraft.client.mech;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.client.mob.GlowLayer;
import com.tycooncraft.client.mob.ProtoDestroyerGeo;
import com.tycooncraft.mech.MechActionPayload;
import com.tycooncraft.mech.ProtoDestroyer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

/** 프로토 디스트로이어: 모델·애니메이션, 렌더러, 조종 입력, HUD */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ProtoClient {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TycoonCraft.id("proto_destroyer"), "main");

    private static float pt() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    public static class Model extends HierarchicalModel<ProtoDestroyer> {
        private final ModelPart root;
        private final Map<String, ModelPart> b;

        public Model(ModelPart root) {
            this.root = root;
            this.b = ProtoDestroyerGeo.bones(root);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(ProtoDestroyer e, float limb, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            float w = Mth.cos(limb * 0.5f) * 0.85f * amount;
            b.get("left_leg").xRot += w;
            b.get("right_leg").xRot -= w;
            ModelPart body = b.get("body"), head = b.get("head"), jaw = b.get("jaw"), ra = b.get("right_arm"), la = b.get("left_arm");
            body.yRot += Mth.cos(limb * 0.5f) * 0.09f * amount;
            body.y += Math.abs(Mth.sin(limb * 0.5f)) * 1.2f * amount;   // 쿵쿵 걷는 들썩임
            body.xRot += Mth.sin(age * 0.07f) * 0.02f;                   // 숨쉬기
            head.xRot += Mth.sin(age * 0.07f + 1) * 0.04f;
            head.yRot += Mth.sin(age * 0.031f) * 0.12f;
            jaw.xRot += 0.06f + Mth.sin(age * 0.07f) * 0.04f;
            ra.xRot -= w * 0.55f;
            la.xRot += w * 0.55f;
            float t = e.animT + pt();
            float aimPitch = Mth.clamp(e.getXRot(), -40, 40) * Mth.DEG_TO_RAD;
            switch (e.attack()) {
                case ProtoDestroyer.FIRING -> {
                    float raise = Math.min(1f, t / 4f);
                    ra.xRot = Mth.lerp(raise, ra.xRot, -Mth.HALF_PI - 0.28f + aimPitch);
                    b.get("barrels").yRot = age * 1.4f;
                    ra.z += Mth.sin(age * 3f) * 0.35f;
                    jaw.xRot += 0.35f;                                    // 포효
                }
                case ProtoDestroyer.ROCKETS -> {
                    b.get("pod").xRot -= 0.5f * Math.min(1f, t / 4f) - aimPitch * 0.5f;
                    la.xRot -= 0.25f;
                    jaw.xRot += 0.25f;
                }
                case ProtoDestroyer.SLAM -> {
                    float up = t < 10 ? t / 10f : Math.max(0f, 1f - (t - 10) / 2f);
                    float slam = t >= 10 ? Math.min(1f, (t - 10) / 2f) : 0f;
                    ra.xRot += -2.9f * up - 0.5f * slam;
                    la.xRot += -2.9f * up - 0.5f * slam;
                    body.xRot += 0.4f * slam - 0.25f * up;
                    jaw.xRot += 0.7f * up;
                    head.xRot -= 0.4f * up;
                }
                default -> {
                    ra.zRot += 0.1f + Mth.sin(age * 0.07f) * 0.03f;
                    la.zRot -= 0.1f + Mth.sin(age * 0.07f) * 0.03f;
                }
            }
        }
    }

    public static class Renderer extends MobRenderer<ProtoDestroyer, Model> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/proto_destroyer.png");

        public Renderer(EntityRendererProvider.Context ctx) {
            super(ctx, new Model(ctx.bakeLayer(LAYER)), 1.4f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/proto_destroyer_glow.png")));
        }

        @Override
        public ResourceLocation getTextureLocation(ProtoDestroyer e) {
            return TEX;
        }
    }

    // ── 조종 입력 ────────────────────────────────────────────────
    private static boolean jumpWas;

    private static ProtoDestroyer piloting() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getVehicle() instanceof ProtoDestroyer m && m.getControllingPassenger() == mc.player ? m : null;
    }

    public static void send(ProtoDestroyer m, int act) {
        PacketDistributor.sendToServer(new MechActionPayload(m.getId(), (byte) act));
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        ProtoDestroyer m = piloting();
        if (m == null || mc.screen != null) {
            jumpWas = false;
            return;
        }
        if (mc.options.keyAttack.isDown() && !m.overheated()) send(m, ProtoDestroyer.ACT_FIRE);
        boolean j = mc.options.keyJump.isDown();
        if (j && !jumpWas) send(m, ProtoDestroyer.ACT_SLAM);
        jumpWas = j;
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered e) {
        ProtoDestroyer m = piloting();
        if (m == null || e.isPickBlock()) return;
        e.setCanceled(true);
        e.setSwingHand(false);
        if (e.isUseItem()) send(m, ProtoDestroyer.ACT_ROCKETS);
    }

    @SubscribeEvent
    public static void camera(CalculateDetachedCameraDistanceEvent e) {
        if (piloting() != null) e.setDistance(Math.max(e.getDistance(), 9f));
    }

    @SubscribeEvent
    public static void hud(RenderGuiEvent.Post e) {
        ProtoDestroyer m = piloting();
        Minecraft mc = Minecraft.getInstance();
        if (m == null || mc.options.hideGui) return;
        GuiGraphics g = e.getGuiGraphics();
        int w = g.guiWidth(), h = g.guiHeight();
        int x = 8, y = h - 44;   // 왼쪽 아래 (체력 하트·핫바와 겹치지 않게)
        var font = mc.font;
        g.fill(x - 4, y - 4, x + 186, y + 30, 0x90000000);
        g.drawString(font, Component.translatable("entity.tycooncraft.proto_destroyer"), x, y - 1, 0xFFE0A050);
        bar(g, x, y + 9, m.getHealth() / m.getMaxHealth(), 0xFFD03A2A, Component.translatable("hud.tycooncraft.proto.hp", Math.round(m.getHealth())), font);
        int hc = m.overheated() ? ((m.tickCount / 4) % 2 == 0 ? 0xFFFF3020 : 0xFF802010) : 0xFFFF9A30;
        bar(g, x, y + 19, m.heat() / (float) ProtoDestroyer.MAX_HEAT, hc,
                Component.translatable(m.overheated() ? "hud.tycooncraft.proto.overheat" : "hud.tycooncraft.proto.heat"), font);
        String rk = m.rocketCd() > 0 ? String.format("%.1fs", m.rocketCd() / 20f) : "READY";
        String sl = m.slamCd() > 0 ? String.format("%.1fs", m.slamCd() / 20f) : "READY";
        g.drawString(font, Component.translatable("hud.tycooncraft.proto.rocket", rk), x + 96, y + 9, m.rocketCd() > 0 ? 0xFF909090 : 0xFF80FF80);
        g.drawString(font, Component.translatable("hud.tycooncraft.proto.slam", sl), x + 96, y + 19, m.slamCd() > 0 ? 0xFF909090 : 0xFF80FF80);
    }

    private static void bar(GuiGraphics g, int x, int y, float f, int col, Component label, net.minecraft.client.gui.Font font) {
        g.fill(x, y, x + 90, y + 8, 0xFF202020);
        g.fill(x + 1, y + 1, x + 1 + Math.round(88 * Mth.clamp(f, 0, 1)), y + 7, col);
        g.drawString(font, label, x + 3, y, 0xFFFFFFFF, true);
    }

    private ProtoClient() {}
}
