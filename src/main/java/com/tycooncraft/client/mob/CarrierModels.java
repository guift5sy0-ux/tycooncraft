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

/** v0.27.0 정찰 디스트로이어(사족 기계) · 공중 디스트로이어(모체) · 미니 드론 */
public final class CarrierModels {
    public static final ModelLayerLocation SCOUT = new ModelLayerLocation(TycoonCraft.id("scout_destroyer"), "main");
    public static final ModelLayerLocation CARRIER = new ModelLayerLocation(TycoonCraft.id("aerial_destroyer"), "main");

    // ═════════ 정찰 디스트로이어 ═════════
    public static LayerDefinition scoutLayer() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition body = r.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -3, -6, 8, 5, 12)
                .texOffs(40, 0).addBox(-0.5f, -7, 3, 1, 4, 1).texOffs(40, 6).addBox(-2.5f, -4, -1, 5, 1, 6), PartPose.offset(0, 16, 0));
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-3, -2, -5, 6, 4, 5)
                .texOffs(22, 18).addBox(-2.5f, 1.5f, -5.5f, 5, 1, 4), PartPose.offset(0, -0.5f, -6));
        for (int s = -1; s <= 1; s += 2) {
            String n = s < 0 ? "l" : "r";
            // 앞다리 · 뒷다리 (역관절: 윗다리는 바깥·앞뒤로, 아랫다리는 반대로 꺾임)
            for (int f = 0; f < 2; f++) {
                float z = f == 0 ? -4 : 4;
                PartDefinition up = body.addOrReplaceChild("leg_" + n + f, CubeListBuilder.create().texOffs(0, 28).addBox(-1, -0.5f, -1, 2, 5, 2),
                        PartPose.offsetAndRotation(s * 4.2f, 1, z, f == 0 ? -0.7f : 0.7f, 0, s * -0.35f));
                up.addOrReplaceChild("low", CubeListBuilder.create().texOffs(8, 28).addBox(-0.75f, 0, -0.75f, 1.5f, 6, 1.5f),
                        PartPose.offsetAndRotation(0, 4.5f, 0, f == 0 ? 1.25f : -1.25f, 0, s * 0.3f));
            }
            // 낫 같은 칼날 팔
            PartDefinition blade = body.addOrReplaceChild("blade_" + n, CubeListBuilder.create().texOffs(16, 28).addBox(-0.5f, -0.5f, -1, 1, 1, 6)
                    .texOffs(30, 28).addBox(-0.4f, -0.5f, -9, 0.8f, 3, 8), PartPose.offsetAndRotation(s * 3.6f, -2, -4, -0.9f, s * 0.15f, 0));
        }
        return LayerDefinition.create(m, 64, 64);
    }

    public static class ScoutModel<T extends net.minecraft.world.entity.LivingEntity> extends HierarchicalModel<T> {
        private final ModelPart root, body, head;
        private final ModelPart[] legs = new ModelPart[4];
        private final ModelPart bl, br;

        public ScoutModel(ModelPart root) {
            this.root = root;
            body = root.getChild("body");
            head = body.getChild("head");
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
            // 빠른 종종걸음 (대각선 다리끼리)
            float w = Mth.cos(limb * 1.6f) * 0.7f * amount;
            legs[0].xRot += w;
            legs[3].xRot += w;
            legs[1].xRot -= w;
            legs[2].xRot -= w;
            body.y += Mth.abs(Mth.sin(limb * 1.6f)) * -0.8f * amount;
            body.zRot = Mth.sin(limb * 0.8f) * 0.08f * amount;
            head.yRot = yaw * Mth.DEG_TO_RAD * 0.7f;
            head.xRot = pitch * Mth.DEG_TO_RAD * 0.5f;
            float t = (e instanceof ScoutDestroyer sd ? e.level().getGameTime() - sd.slashAt
                    : e instanceof com.tycooncraft.mech.ProtoScout ps ? e.tickCount - ps.slashAt() : 99) + (age - (int) age);
            if (t >= 0 && t < 8) {   // 베기: 칼날을 치켜들었다가 내려침
                float u = t / 8f;
                float sw = u < 0.35f ? -u / 0.35f * 0.9f : -0.9f + (u - 0.35f) / 0.65f * 2.0f;
                bl.xRot += sw;
                br.xRot += sw;
                body.xRot = 0.15f * Mth.sin(u * Mth.PI);
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

    /** v0.27.1 프로토 디스트로이어-정찰: 정찰 디스트로이어 모델을 2.3배로, 강철·금색 도장 */
    public static class ProtoScoutRenderer extends MobRenderer<com.tycooncraft.mech.ProtoScout, ScoutModel<com.tycooncraft.mech.ProtoScout>> {
        private static final ResourceLocation TEX = TycoonCraft.id("textures/entity/proto_scout.png");

        public ProtoScoutRenderer(EntityRendererProvider.Context ctx) {
            super(ctx, new ScoutModel<>(ctx.bakeLayer(SCOUT)), 1.1f);
            addLayer(new GlowLayer<>(this, TycoonCraft.id("textures/entity/scout_destroyer_glow.png")));
        }

        @Override
        protected void scale(com.tycooncraft.mech.ProtoScout e, com.mojang.blaze3d.vertex.PoseStack ps, float pt) {
            ps.scale(2.3f, 2.3f, 2.3f);
        }

        @Override
        public ResourceLocation getTextureLocation(com.tycooncraft.mech.ProtoScout e) {
            return TEX;
        }

        @Override
        public void render(com.tycooncraft.mech.ProtoScout e, float yaw, float pt, com.mojang.blaze3d.vertex.PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf, int light) {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.options.getCameraType().isFirstPerson() && mc.player != null && mc.player.getVehicle() == e) return;
            super.render(e, yaw, pt, ps, buf, light);
        }
    }

    // ═════════ 공중 디스트로이어 (모체) ═════════
    public static LayerDefinition carrierLayer() {
        MeshDefinition m = new MeshDefinition();
        PartDefinition r = m.getRoot();
        PartDefinition hull = r.addOrReplaceChild("hull", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-6, -4, -13, 12, 7, 25)          // 몸통
                .texOffs(0, 32).addBox(-4, -3, -17, 8, 5, 4)           // 기수
                .texOffs(24, 32).addBox(-3, -8, -3, 6, 4, 9)           // 함교
                .texOffs(0, 41).addBox(-1, -11, 2, 2, 3, 1)            // 안테나
                .texOffs(0, 46).addBox(-14, -1, -4, 28, 2, 10)         // 날개판
                .texOffs(56, 32).addBox(-1.5f, 3, -14, 3, 2, 3)         // 기수 포탑
                .texOffs(68, 32).addBox(-0.5f, 3.5f, -19, 1, 1, 5), PartPose.offset(0, 12, 0));
        // 동력원 3개: 왼쪽·오른쪽 날개 끝, 꼬리
        float[][] pods = {{13.5f, 0, 1}, {-13.5f, 0, 1}, {0, -1, 13}};
        for (int i = 0; i < 3; i++) {
            PartDefinition pod = hull.addOrReplaceChild("pod" + i, CubeListBuilder.create().texOffs(0, 60).addBox(-3, -3, -5, 6, 6, 10)
                    .texOffs(32, 60).addBox(-3.5f, -3.5f, 4, 7, 7, 1), PartPose.offset(pods[i][0], pods[i][1], pods[i][2]));
            pod.addOrReplaceChild("glow", CubeListBuilder.create().texOffs(48, 60).addBox(-2, -2, 5, 4, 4, 1, new CubeDeformation(0.1f)), PartPose.ZERO);
            pod.addOrReplaceChild("rotor", CubeListBuilder.create().texOffs(0, 80).addBox(-7, -0.25f, -0.75f, 14, 0.5f, 1.5f)
                    .texOffs(0, 84).addBox(-0.75f, -0.25f, -7, 1.5f, 0.5f, 14), PartPose.offset(0, -3.6f, 0));
        }
        // 격납고 문 (배 밑)
        hull.addOrReplaceChild("door_l", CubeListBuilder.create().texOffs(64, 46).addBox(0, 0, -6, 5, 1, 12), PartPose.offset(-5, 3, -1));
        hull.addOrReplaceChild("door_r", CubeListBuilder.create().texOffs(64, 46).mirror().addBox(-5, 0, -6, 5, 1, 12), PartPose.offset(5, 3, -1));
        return LayerDefinition.create(m, 128, 128);
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
            for (int i = 0; i < 3; i++) {
                boolean alive = e.core(i) > 0;
                ModelPart rotor = pods[i].getChild("rotor");
                rotor.yRot = alive ? age * 1.4f * (i == 1 ? -1 : 1) : 0.4f * i;
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

    // ═════════ 미니 드론 ═════════
    public static class DroneRenderer extends EntityRenderer<MiniDrone> {
        public DroneRenderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            shadowRadius = 0.2f;
        }

        @Override
        public void render(MiniDrone d, float entityYaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
            ps.pushPose();
            float yaw = Mth.rotLerp(pt, d.yRotO, d.getYRot());
            ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
            ps.scale(0.55f, 0.55f, 0.55f);
            VertexConsumer vc = VehicleModels.buffer(buf);
            VehicleModels.body(ps, vc, VehicleModels.DRONE2_BODY, 1f, light);
            float spin = (d.tickCount + pt) * 1.6f;
            for (float[] m : new float[][]{{12, 9.6f, -12, 1}, {-12, 9.6f, -12, -1}, {12, 9.6f, 12, -1}, {-12, 9.6f, 12, 1}}) {
                ps.pushPose();
                VehicleModels.translateUnits(ps, m[0], m[1], m[2]);
                ps.mulPose(Axis.YP.rotation(spin * m[3]));
                VehicleModels.part(ps, vc, VehicleModels.DRONE_PROP2, 1f, light);
                ps.popPose();
            }
            ps.popPose();
            super.render(d, entityYaw, pt, ps, buf, light);
        }

        @Override
        public ResourceLocation getTextureLocation(MiniDrone e) {
            return TextureAtlas.LOCATION_BLOCKS;
        }
    }

    private CarrierModels() {}
}
