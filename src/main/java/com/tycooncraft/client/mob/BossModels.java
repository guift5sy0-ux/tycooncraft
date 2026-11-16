package com.tycooncraft.client.mob;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mob.Behemoth;
import com.tycooncraft.mob.DestroyerGolem;
import com.tycooncraft.mob.StormWyvern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.Map;

/** 보스 3종 모델 + 애니메이션 */
public final class BossModels {
    public static final ModelLayerLocation GOLEM = new ModelLayerLocation(TycoonCraft.id("destroyer_golem"), "main");
    public static final ModelLayerLocation BEHEMOTH = new ModelLayerLocation(TycoonCraft.id("behemoth"), "main");
    public static final ModelLayerLocation WYVERN = new ModelLayerLocation(TycoonCraft.id("storm_wyvern"), "main");

    private static float pt() {
        return Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
    }

    // ═════════ 파괴수 골렘 ═════════
    public static class Golem extends HierarchicalModel<DestroyerGolem> {
        private final ModelPart root;
        private final Map<String, ModelPart> b;

        public Golem(ModelPart root) {
            this.root = root;
            this.b = DestroyerGolemGeo.bones(root);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(DestroyerGolem e, float limb, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            float w = Mth.cos(limb * 0.45f) * 0.9f * amount;
            b.get("left_leg").xRot = w;
            b.get("right_leg").xRot = -w;
            b.get("body").yRot = Mth.cos(limb * 0.45f) * 0.06f * amount;
            b.get("head").yRot = yaw * Mth.DEG_TO_RAD;
            b.get("head").xRot = pitch * Mth.DEG_TO_RAD;
            ModelPart ra = b.get("right_arm"), la = b.get("left_arm");
            ra.xRot = -w * 0.6f;
            la.xRot = w * 0.6f;
            float t = e.animT + pt();
            switch (e.attack()) {
                case DestroyerGolem.MINIGUN -> {
                    float raise = Math.min(1f, t / 8f);
                    ra.xRot = Mth.lerp(raise, ra.xRot, -Mth.HALF_PI + pitch * Mth.DEG_TO_RAD);
                    float spin = t < 16 ? t * t * 0.02f : (5.1f + (t - 16) * 1.3f);
                    b.get("barrels").yRot = spin;
                    ra.y += Mth.sin(t * 2.5f) * (t > 16 ? 0.25f : 0f);   // 반동
                }
                case DestroyerGolem.ROCKETS -> {
                    b.get("pod").xRot = -0.45f * Math.min(1f, t / 6f);
                    la.xRot = -0.3f;
                }
                case DestroyerGolem.SLAM -> {
                    float up = t < 12 ? t / 12f : Math.max(0f, 1f - (t - 12) / 3f);
                    float slam = t >= 12 ? Math.min(1f, (t - 12) / 3f) : 0f;
                    ra.xRot = -2.8f * up - 0.6f * slam;
                    la.xRot = -2.8f * up - 0.6f * slam;
                    b.get("body").xRot = 0.35f * slam - 0.15f * up;
                }
                default -> {
                    ra.zRot = 0.08f + Mth.sin(age * 0.06f) * 0.03f;
                    la.zRot = -0.08f - Mth.sin(age * 0.06f) * 0.03f;
                }
            }
        }
    }

    // ═════════ 베헤모스 ═════════
    public static class Beast extends HierarchicalModel<Behemoth> {
        private final ModelPart root;
        private final Map<String, ModelPart> b;

        public Beast(ModelPart root) {
            this.root = root;
            this.b = BehemothGeo.bones(root);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(Behemoth e, float limb, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            float t = e.animT + pt();
            int a = e.attack();
            float speed = a == Behemoth.CHARGE && t > 22 ? 0.9f : 0.42f;
            float sw = a == Behemoth.CHARGE && t > 22 ? Mth.cos(age * 0.9f) * 0.9f : Mth.cos(limb * speed) * 0.7f * amount;
            b.get("leg_fl").xRot = sw;
            b.get("leg_br").xRot = sw;
            b.get("leg_fr").xRot = -sw;
            b.get("leg_bl").xRot = -sw;
            ModelPart head = b.get("head"), jaw = b.get("jaw"), body = b.get("body");
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.6f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f;
            jaw.xRot = 0.06f + Mth.sin(age * 0.08f) * 0.05f;
            body.y += Mth.sin(age * 0.08f) * 0.3f;
            b.get("tail").yRot = Mth.sin(age * 0.1f) * 0.25f;
            b.get("tail2").yRot = Mth.sin(age * 0.1f - 0.8f) * 0.35f;
            switch (a) {
                case Behemoth.CHARGE -> {
                    if (t < 22) {
                        jaw.xRot = 0.6f;
                        head.xRot = -0.25f;
                        b.get("leg_fl").xRot = Mth.sin(t * 0.8f) * 0.6f;      // 앞발 긁기
                    } else {
                        head.xRot = 0.35f;                                      // 머리 숙여 돌격
                        jaw.xRot = 0.2f;
                    }
                }
                case Behemoth.STOMP -> {
                    float rear = t < 16 ? t / 16f : Math.max(0f, 1f - (t - 16) / 3f);
                    body.xRot = -0.5f * rear;
                    body.y -= 3f * rear;
                    head.xRot -= 0.4f * rear;
                    jaw.xRot = 0.5f * rear;
                    for (String l : new String[]{"leg_fl", "leg_fr"}) {
                        b.get(l).xRot = -1.0f * rear;
                        b.get(l).y -= 7f * rear;
                    }
                }
                case Behemoth.BREATH -> {
                    jaw.xRot = Math.min(0.75f, t / 10f * 0.75f);
                    head.xRot += 0.1f;
                }
                case Behemoth.STUN -> {
                    head.xRot = 0.5f + Mth.sin(t * 0.3f) * 0.1f;
                    head.zRot = Mth.sin(t * 0.25f) * 0.25f;
                    jaw.xRot = 0.3f;
                }
                case Behemoth.METEOR -> {
                    head.xRot = -0.7f;
                    jaw.xRot = 0.8f;
                    body.xRot = -0.15f;
                }
                default -> {
                }
            }
        }
    }

    // ═════════ 폭풍 와이번 ═════════
    public static class Wyvern extends HierarchicalModel<StormWyvern> {
        private final ModelPart root;
        private final Map<String, ModelPart> b;

        public Wyvern(ModelPart root) {
            this.root = root;
            this.b = StormWyvernGeo.bones(root);
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(StormWyvern e, float limb, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            float t = e.animT + pt();
            int a = e.attack();
            float flapSpeed = a == StormWyvern.VOLLEY || a == StormWyvern.THUNDER ? 0.42f : 0.3f;
            float f = Mth.sin(age * flapSpeed);
            float f2 = Mth.sin(age * flapSpeed - 0.9f);
            ModelPart wl = b.get("wing_l"), wr = b.get("wing_r"), tl = b.get("wing_l_tip"), tr = b.get("wing_r_tip");
            wl.zRot = -0.1f - f * 0.6f;
            wr.zRot = 0.1f + f * 0.6f;
            tl.zRot = -f2 * 0.45f;
            tr.zRot = f2 * 0.45f;
            ModelPart body = b.get("body");
            body.xRot = e.getViewXRot(pt()) * Mth.DEG_TO_RAD * 0.7f;
            body.y += f * 1.2f;
            b.get("head").yRot = yaw * Mth.DEG_TO_RAD * 0.5f;
            b.get("neck").yRot = yaw * Mth.DEG_TO_RAD * 0.3f;
            b.get("jaw").xRot = 0.05f;
            b.get("tail").yRot = Mth.sin(age * 0.12f) * 0.18f;
            b.get("tail2").yRot = Mth.sin(age * 0.12f - 0.7f) * 0.25f;
            b.get("tail3").yRot = Mth.sin(age * 0.12f - 1.4f) * 0.3f;
            b.get("tail").xRot = -f * 0.08f;
            b.get("leg_l").xRot = 0.9f;
            b.get("leg_r").xRot = 0.9f;
            switch (a) {
                case StormWyvern.DIVE -> {
                    if (t > 14) {                        // 날개 접고 급강하
                        wl.zRot = -0.25f;
                        wr.zRot = 0.25f;
                        wl.yRot = -0.9f;
                        wr.yRot = 0.9f;
                        tl.zRot = 0.3f;
                        tr.zRot = -0.3f;
                        b.get("jaw").xRot = 0.6f;
                        b.get("leg_l").xRot = -0.4f;      // 발톱을 앞으로
                        b.get("leg_r").xRot = -0.4f;
                    } else {
                        wl.zRot = -0.9f;
                        wr.zRot = 0.9f;
                    }
                }
                case StormWyvern.VOLLEY -> b.get("jaw").xRot = 0.35f + Mth.sin(t * 1.0f) * 0.25f;
                case StormWyvern.THUNDER -> {
                    b.get("jaw").xRot = 0.6f;
                    b.get("head").xRot = -0.5f;
                    b.get("neck").xRot = -0.6f;
                }
                default -> {
                }
            }
        }
    }

    private BossModels() {}
}
