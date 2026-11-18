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
            float ph = limb * 0.32f;                       // 느리고 큰 보폭
            float amt = Math.min(1f, amount * 1.6f);
            for (int i = 0; i < 2; i++) {
                String side = i == 0 ? "left" : "right";
                float p = ph + (i == 0 ? 0 : Mth.PI);
                float swing = Mth.sin(p) * 0.42f * amt;                  // 앞뒤로 내딛기
                float lift = Math.max(0f, Mth.cos(p)) * 0.55f * amt;     // 들어 올릴 때 무릎 접힘
                ModelPart th = b.get(side + "_thigh"), sh = b.get(side + "_shin"), ft = b.get(side + "_foot");
                th.xRot -= swing + lift * 0.5f;
                sh.xRot += lift;
                ft.xRot += swing + lift * 0.5f - lift;                   // 발은 늘 수평 유지
            }
            ModelPart hull = b.get("hull");
            float stomp = 1f - Math.abs(Mth.sin(ph));                  // 발이 땅을 디딜 때 1
            hull.y += (float) Math.pow(stomp, 3) * 1.6f * amt;           // 쿵 하고 내려앉음
            hull.zRot += Mth.sin(ph) * 0.035f * amt;                     // 좌우로 묵직하게 흔들림
            hull.xRot += Mth.sin(age * 0.05f) * 0.008f;                  // 대기 중 미세한 떨림
            float aimPitch = Mth.clamp(e.getXRot(), -35, 35) * Mth.DEG_TO_RAD;
            b.get("arm_left").xRot += aimPitch;
            b.get("arm_right").xRot += aimPitch;
            float t = e.animT + pt();
            switch (e.attack()) {
                case ProtoDestroyer.FIRING -> {
                    float spin = t < 6 ? t * t * 0.06f : 2.2f + (t - 6) * 1.3f;   // 회전 올라감
                    b.get("barrels_left").zRot = spin;
                    b.get("barrels_right").zRot = -spin;
                    b.get("arm_left").z += (age % 2f < 1f) ? 0.5f : 0f;
                    b.get("arm_right").z += (age % 2f >= 1f) ? 0.5f : 0f;
                }
                case ProtoDestroyer.ROCKETS -> b.get("pod").xRot -= 0.3f * Math.min(1f, t / 4f);
                case ProtoDestroyer.SLAM -> {
                    float k = Math.max(0f, 1f - t / 10f);
                    hull.y += 3.5f * k;                                        // 착지 충격으로 주저앉음
                    for (String side : new String[]{"left", "right"}) {
                        b.get(side + "_thigh").xRot -= 0.4f * k;
                        b.get(side + "_shin").xRot += 0.8f * k;
                        b.get(side + "_foot").xRot -= 0.4f * k;
                    }
                }
                default -> {}
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

        /** 1인칭으로 조종 중이면 기체를 그리지 않음 (바닐라는 탑승 중인 탈것을 shouldRender 와 무관하게 그리므로 여기서 막음) */
        @Override
        public void render(ProtoDestroyer e, float yaw, float pt, com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf, int light) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.options.getCameraType().isFirstPerson() && mc.player != null && mc.player.getVehicle() == e) return;
            super.render(e, yaw, pt, ps, buf, light);
        }
    }

    /** 조종석 안의 탑승자는 보이지 않게 (3인칭에서 기체 위로 튀어나오지 않음) */
    @SubscribeEvent
    public static void hideRider(net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<?, ?> e) {
        if (e.getEntity() instanceof net.minecraft.world.entity.player.Player p && p.getVehicle() instanceof ProtoDestroyer) e.setCanceled(true);
    }

    /** 걸을 때·착지할 때 조종석 화면이 묵직하게 흔들림 */
    @SubscribeEvent
    public static void shake(net.neoforged.neoforge.client.event.ViewportEvent.ComputeCameraAngles e) {
        ProtoDestroyer m = piloting();
        if (m == null) return;
        float pt = (float) e.getPartialTick();
        float limb = m.walkAnimation.position(pt) * 0.32f, amt = Math.min(1f, m.walkAnimation.speed(pt) * 1.6f);
        float stomp = (float) Math.pow(1f - Math.abs(Mth.sin(limb)), 6) * amt;
        float land = m.attack() == ProtoDestroyer.SLAM ? Math.max(0f, 1f - (m.animT + pt) / 10f) : 0f;
        e.setPitch(e.getPitch() + stomp * 1.4f + land * 4f);
        e.setRoll(e.getRoll() + Mth.sin(limb) * 0.8f * amt + (land > 0 ? Mth.sin((m.animT + pt) * 2.5f) * land * 2f : 0));
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
        if (j && !jumpWas && m.onGround()) {
            var dm = m.getDeltaMovement();
            m.setDeltaMovement(dm.x, ProtoDestroyer.JUMP_VEL, dm.z);   // 조종하는 쪽(클라이언트)이 움직임을 담당
            send(m, ProtoDestroyer.ACT_JUMP);
        }
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
        int x = 8, y = 10;   // 왼쪽 위 (체력 하트·핫바와 겹치지 않게)
        var font = mc.font;
        g.fill(x - 4, y - 4, x + 200, y + 40, 0x90000000);
        g.drawString(font, Component.translatable("entity.tycooncraft.proto_destroyer"), x, y - 1, 0xFFE0A050);
        bar(g, x, y + 9, m.getHealth() / m.getMaxHealth(), 0xFFD03A2A, Component.translatable("hud.tycooncraft.proto.hp", Math.round(m.getHealth())), font);
        int hc = m.overheated() ? ((m.tickCount / 4) % 2 == 0 ? 0xFFFF3020 : 0xFF802010) : 0xFFFF9A30;
        bar(g, x, y + 19, m.heat() / (float) ProtoDestroyer.MAX_HEAT, hc,
                Component.translatable(m.overheated() ? "hud.tycooncraft.proto.overheat" : "hud.tycooncraft.proto.heat"), font);
        boolean cr = mc.player.getAbilities().instabuild;
        int ammo = ProtoDestroyer.count(mc.player, com.tycooncraft.registry.ModItems.HEAVY_AMMO.get());
        g.drawString(font, Component.translatable("hud.tycooncraft.proto.ammo", cr ? "∞" : String.valueOf(ammo)), x + 96, y + 9, cr || ammo > 0 ? 0xFFFFFFFF : 0xFFFF5040);
        int rockets = ProtoDestroyer.count(mc.player, com.tycooncraft.registry.ModItems.ROCKET.get());
        String rk = m.rocketCd() > 0 ? String.format("%.1fs", m.rocketCd() / 20f) : "READY";
        g.drawString(font, Component.translatable("hud.tycooncraft.proto.rocket", cr ? "∞" : String.valueOf(rockets), rk), x + 96, y + 19,
                m.rocketCd() > 0 || (!cr && rockets == 0) ? 0xFF909090 : 0xFF80FF80);
        g.drawString(font, Component.translatable(m.slamCd() > 0 ? "hud.tycooncraft.proto.stomp_wait" : "hud.tycooncraft.proto.stomp"), x, y + 29,
                m.slamCd() > 0 ? 0xFF909090 : 0xFF80FF80);
    }

    private static void bar(GuiGraphics g, int x, int y, float f, int col, Component label, net.minecraft.client.gui.Font font) {
        g.fill(x, y, x + 90, y + 8, 0xFF202020);
        g.fill(x + 1, y + 1, x + 1 + Math.round(88 * Mth.clamp(f, 0, 1)), y + 7, col);
        g.drawString(font, label, x + 3, y, 0xFFFFFFFF, true);
    }

    private ProtoClient() {}
}
