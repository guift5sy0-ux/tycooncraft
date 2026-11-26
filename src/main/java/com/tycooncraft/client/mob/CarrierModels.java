package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.client.vehicle.VehicleModels;
import com.tycooncraft.mob.carrier.AerialDestroyer;
import com.tycooncraft.mob.carrier.MiniDrone;
import com.tycooncraft.mob.carrier.ScoutDestroyer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** 정찰 디스트로이어(사족 기계) · 플라잉 디스트로이어(모체) · 미니 드론 — v0.28 고퀄 모델 */
public final class CarrierModels {
    public static final ModelLayerLocation SCOUT = new ModelLayerLocation(TycoonCraft.id("scout_destroyer"), "main");
    public static final ModelLayerLocation CARRIER = new ModelLayerLocation(TycoonCraft.id("aerial_destroyer"), "main");

    // ═════════ 정찰 디스트로이어 ═════════
    public static LayerDefinition scoutLayer() {
        return ScoutDestroyerGeo.createBodyLayer();   // v0.28 고퀄 리메이크 (scripts/gen_destroyers.py)
    }

    public static class ScoutModel<T extends net.minecraft.world.entity.LivingEntity> extends HierarchicalModel<T> {
        private final ModelPart root, body, head;
        private final ModelPart[] legs = new ModelPart[4];
        private final ModelPart bl, br;

        private final ModelPart abdomen, mandL, mandR;

        public ScoutModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
            abdomen = body.getChild("abdomen");
            mandL = head.getChild("mandible_l");
            mandR = head.getChild("mandible_r");
            legs[0] = body.getChild("leg_l0");
            legs[1] = body.getChild("leg_r0");
            legs[2] = body.getChild("leg_l1");
            legs[3] = body.getChild("leg_r1");
            bl = body.getChild("blade_l");
            br = body.getChild("blade_r");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(T e, float limb, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            float amt = Math.min(1f, amount * 1.3f);
            // 빠른 종종걸음 (대각선 다리끼리) + 아랫다리 굽힘
            float ph = limb * 1.6f;
            for (int i = 0; i < 4; i++) {
                float p = ph + ((i == 0 || i == 3) ? 0 : Mth.PI);
                legs[i].xRot += Mth.cos(p) * 0.75f * amt;
                ModelPart low = legs[i].getChild("low");
                low.zRot += (i % 2 == 0 ? -1 : 1) * Math.max(0f, Mth.sin(p)) * 0.5f * amt;
            }
            body.y += Mth.abs(Mth.sin(ph)) * -0.9f * amt;
            body.zRot = Mth.sin(ph * 0.5f) * 0.07f * amt;
            body.xRot = 0.08f * amt;                               // 달릴 때 앞으로 숙임
            abdomen.xRot += Mth.sin(age * 0.12f) * 0.05f + Mth.sin(ph) * 0.06f * amt;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.7f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f;
            float chatter = Mth.sin(age * 0.9f) * 0.12f;           // 집게턱 딸깍임
            mandL.yRot += chatter;
            mandR.yRot -= chatter;
            float t = (e instanceof ScoutDestroyer sd ? e.level().getGameTime() - sd.slashAt
                    : e instanceof com.tycooncraft.mech.ProtoScout ps ? e.tickCount - ps.slashAt() : 99) + (age - (int) age);
            if (t >= 0 && t < 8) {   // 베기: 칼날을 치켜들었다가 내려침 (팔뚝·칼날도 펴짐)
                float u = t / 8f;
                float sw = u < 0.35f ? -u / 0.35f * 0.9f : -0.9f + (u - 0.35f) / 0.65f * 2.0f;
                bl.xRot += sw;
                br.xRot += sw;
                float ext = Mth.sin(u * Mth.PI) * 0.6f;
                bl.getChild("fore").xRot -= ext;
                br.getChild("fore").xRot -= ext;
                body.xRot += 0.18f * Mth.sin(u * Mth.PI);
                mandL.yRot += 0.4f * Mth.sin(u * Mth.PI);
                mandR.yRot -= 0.4f * Mth.sin(u * Mth.PI);
            } else {
                bl.xRot += Mth.sin(age * 0.15f) * 0.05f;
                br.xRot -= Mth.sin(age * 0.15f) * 0.05f;
            }
        }
    }

    public static class ScoutRenderer extends MobRenderer<ScoutDestroyer, ScoutModel<ScoutDestroyer>> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/scout_destroyer.png");

        public ScoutRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ScoutModel<>(ctx.bakeLayer(SCOUT)), 0.5f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/scout_destroyer_glow.png")));
        }

        @Override
        public ResourceLocation getTextureLocation(ScoutDestroyer e) {
            return TEX;
        }
    }

    /** 프로토 디스트로이어-정찰: 정찰 디스트로이어와 같은 모델·크기, 강철·금색 도장 (v0.28.1 원본 크기) */
    public static class ProtoScoutRenderer extends MobRenderer<com.tycooncraft.mech.ProtoScout, ScoutModel<com.tycooncraft.mech.ProtoScout>> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/proto_scout.png");

        public ProtoScoutRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ScoutModel<>(ctx.bakeLayer(SCOUT)), 0.5f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/proto_scout_glow.png")));
        }


        @Override
        public ResourceLocation getTextureLocation(com.tycooncraft.mech.ProtoScout e) {
            return TEX;
        }

    }

    // ═════════ 공중 디스트로이어 (모체) ═════════
    public static LayerDefinition carrierLayer() {
        return FlyingDestroyerGeo.createBodyLayer();   // v0.28 고퀄 리메이크
    }

    public static class CarrierModel extends HierarchicalModel<AerialDestroyer> {
        private final ModelPart root, hull, dl, dr;
        private final ModelPart[] pods = new ModelPart[3];

        public CarrierModel(ModelPart root) {
            this.root = root;
            hull = root.getChild("hull");
            for (int i = 0; i < 3; i++) pods[i] = hull.getChild("pod" + i);
            dl = hull.getChild("door_l");
            dr = hull.getChild("door_r");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(AerialDestroyer e, float limb, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            hull.y += Mth.sin(age * 0.05f) * 0.6f;   // 둥실둥실
            hull.zRot += Mth.sin(age * 0.031f) * 0.02f;
            // 주포탑이 천천히 좌우를 훑음
            hull.getChild("turret0").yRot = Mth.sin(age * 0.021f) * 0.35f;
            hull.getChild("turret1").yRot = Mth.sin(age * 0.021f + 1.3f) * 0.35f;
            for (int i = 0; i < 3; i++) if (e.core(i) > 0) pods[i].xRot += Mth.sin(age * 0.07f + i) * 0.04f;   // 엔진 미세 조향
            for (int i = 0; i < 3; i++) {
                boolean alive = e.core(i) > 0;
                ModelPart rotor = pods[i].getChild("rotor");
                rotor.zRot = alive ? age * 1.4f * (i == 1 ? -1 : 1) : 0.4f * i;   // v0.28.1 터빈: 앞뒤 축으로 회전
                pods[i].getChild("glow").visible = alive;
                if (!alive) pods[i].xRot = 0.35f;   // 부서진 동력원은 처짐
            }
            // 격납고 문
            int h = e.hatch();
            float open = h <= 0 ? 0 : h > 32 ? (40 - h) / 8f : h < 8 ? h / 8f : 1f;
            dl.zRot = open * 1.3f;
            dr.zRot = -open * 1.3f;
            // 추락: 기울며 회전
            if (e.crashing()) {
                float c = Math.min(1f, e.crashTime() / 40f);
                hull.zRot = c * 0.6f;
                hull.xRot = c * 0.35f;
            } else if (e.coresAlive() < 3) {
                for (int i = 0; i < 3; i++) if (e.core(i) <= 0) hull.zRot += i == 0 ? -0.12f : i == 1 ? 0.12f : 0;
                if (e.core(2) <= 0) hull.xRot -= 0.1f;
            }
        }
    }

    public static class CarrierRenderer extends MobRenderer<AerialDestroyer, CarrierModel> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/aerial_destroyer.png");

        public CarrierRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new CarrierModel(ctx.bakeLayer(CARRIER)), 2.5f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/aerial_destroyer_glow.png")));
        }

        @Override
        protected void scale(AerialDestroyer e, PoseStack ps, float pt) {
            ps.scale(2.6f, 2.6f, 2.6f);
        }

        @Override
        protected float getFlipDegrees(AerialDestroyer e) {
            return 0f;   // 죽어도 뒤집히지 않음 (폭발로 사라짐)
        }

        @Override
        public ResourceLocation getTextureLocation(AerialDestroyer e) {
            return TEX;
        }

        @Override
        public boolean shouldRender(AerialDestroyer e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
            return f.isVisible(e.getBoundingBox().inflate(4));
        }
    }

    // ═════════ 미니 드론 (v0.28 전용 모델) ═════════
    public static final ModelLayerLocation DRONE = new ModelLayerLocation(TycoonCraft.id("mini_drone"), "main");

    public static LayerDefinition droneLayer() {
        return MiniDroneGeo.createBodyLayer();
    }

    public static class DroneModel extends HierarchicalModel<MiniDrone> {
        private final ModelPart root, body, gun;
        private final ModelPart[] rotors = new ModelPart[4];

        public DroneModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            gun = body.getChild("gun");
            for (int i = 0; i < 4; i++) rotors[i] = body.getChild("arm" + i).getChild("rotor");
        }

        @Override
        public ModelPart root() {
            return root;
        }

        @Override
        public void setupAnim(MiniDrone e, float limb, float amount, float age, float yaw, float pitch) {
            root.getAllParts().forEach(ModelPart::resetPose);
            for (int i = 0; i < 4; i++) rotors[i].yRot = age * 1.9f * (i % 2 == 0 ? 1 : -1);
            body.y += Mth.sin(age * 0.2f + e.getId()) * 0.5f;
            body.xRot = Math.min(0.35f, amount * 0.6f);       // 앞으로 기울며 비행
            gun.xRot = pitch * Mth.DEG_TO_RAD * 0.6f;
        }
    }

    public static class DroneRenderer extends MobRenderer<MiniDrone, DroneModel> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/mini_drone.png");
        private static final ResourceLocation ALLY = TycoonCraft.id("textures/entity/mini_drone_ally.png");

        public DroneRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new DroneModel(ctx.bakeLayer(DRONE)), 0.25f);
            addLayer(new net.minecraft.client.renderer.entity.layers.RenderLayer<>(this) {
                private final ResourceLocation g = TycoonCraft.id("textures/entity/mini_drone_glow.png"), ga = TycoonCraft.id("textures/entity/mini_drone_ally_glow.png");

                @Override
                public void render(PoseStack ps, MultiBufferSource buf, int light, MiniDrone d, float limb, float amt, float pt, float age, float yaw, float pitch) {
                    VertexConsumer vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.eyes(d.allyClient() ? ga : g));
                    getParentModel().renderToBuffer(ps, vc, 0xF000F0, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
                }
            });
        }

        @Override
        protected void scale(MiniDrone e, PoseStack ps, float pt) {
            ps.scale(0.9f, 0.9f, 0.9f);
        }

        @Override
        protected float getFlipDegrees(MiniDrone e) {
            return 0f;
        }

        @Override
        public ResourceLocation getTextureLocation(MiniDrone e) {
            return e.allyClient() ? ALLY : TEX;
        }
    }

    private CarrierModels() {}
}
