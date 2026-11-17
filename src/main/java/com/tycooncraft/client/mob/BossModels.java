package com.tycooncraft.client.mob;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mob.DestroyerGolem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

import java.util.Map;

/** 보스 3종 모델 + 애니메이션 */
public final class BossModels {
    public static final ModelLayerLocation GOLEM = new ModelLayerLocation(TycoonCraft.id("destroyer_golem"), "main");

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
    // ═════════ 폭풍 와이번 ═════════
    private BossModels() {}
}
