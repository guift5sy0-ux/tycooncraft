package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.HellBikeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 헬파이어 바이크 렌더러.
 * - 몸체 / 조향되는 앞부분(45도 레이크 축으로 회전) / 앞뒤 바퀴(굴러감)
 * - 달군 림·디스크·엔진 불빛·헤드램프는 어둠 속에서도 빛남 (풀브라이트 덧그림)
 * - 바퀴를 감싸는 진짜 불길(바닐라 불 텍스처 애니메이션), 배기구 불기둥, 땅에 남는 불자국, 밤에는 헤드라이트 빔
 * - 코너에서 안쪽으로 눕고, 세게 출발하면 윌리 (탑승자도 같이 기울어짐 → HellBikePose)
 * 모델 단위: 1/16 블록, 모델 원점 (8,0,8) = 엔티티 바닥 중심, 앞 = -Z
 */
public class HellBikeRenderer extends EntityRenderer<HellBikeEntity> {
    public static final float FRONT_Y = 8f, FRONT_Z = -7f, FRONT_R = 8f;
    public static final float REAR_Y = 7.5f, REAR_Z = 21f, REAR_R = 7.5f;
    /** 조향축: 헤드튜브 (y, z), 방향 (0, cos45, sin45) */
    public static final float HEAD_Y = 21.79f, HEAD_Z = 6.79f;
    private static final ResourceLocation BEAM = TycoonCraft.id("textures/entity/hell_bike_beam.png");

    public HellBikeRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.7f;
    }

    /** 모델 좌표(1/16) → 바이크 로컬 렌더 좌표(블록) */
    public static Vector3f u(float x, float y, float z) {
        return new Vector3f((x - 8f) / 16f, y / 16f, (z - 8f) / 16f);
    }

    /** 화면에 보이는 조향각(라디안, + = 왼쪽). 빠를수록 작게 꺾임 */
    public static float steerRad(CarEntity bike, float pt) {
        float s = Mth.lerp(pt, bike.steerO, bike.steer);
        float fs = Math.abs(bike.forwardSpeed);
        return -s * (1f - 0.65f * Math.min(1f, fs * 1.2f)) * 0.9f * Mth.DEG_TO_RAD;
    }

    public static Quaternionf steerQuat(float rad) {
        return new Quaternionf().rotationAxis(rad, 0f, Mth.SQRT_OF_TWO / 2f, Mth.SQRT_OF_TWO / 2f);
    }

    /** 바이크 렌더 프레임에 기울기·윌리 적용 (탑승자 렌더에도 같은 변환을 씀) */
    public static void tilt(PoseStack ps, HellBikeEntity bike, float pt) {
        float lean = Mth.lerp(pt, bike.leanO, bike.lean);
        float wh = Mth.lerp(pt, bike.wheelieO, bike.wheelie);
        ps.mulPose(Axis.ZP.rotationDegrees(lean));
        if (wh > 0.01f) {
            float rz = (REAR_Z - 8f) / 16f;
            ps.translate(0, 0, rz);
            ps.mulPose(Axis.XP.rotationDegrees(wh));
            ps.translate(0, 0, -rz);
        }
    }

    @Override
    public boolean shouldRender(HellBikeEntity bike, Frustum frustum, double x, double y, double z) {
        return super.shouldRender(bike, frustum, x, y, z) || (!bike.trail.isEmpty() && bike.distanceToSqr(x, y, z) < 64 * 64);
    }

    @Override
    public void render(HellBikeEntity bike, float entityYaw, float pt, PoseStack ps, MultiBufferSource buffers, int light) {
        Minecraft mc = Minecraft.getInstance();
        float time = (bike.tickCount + pt);
        boolean on = bike.engineRunning();
        float speed = Math.abs(Mth.lerp(pt, bike.forwardSpeedO, bike.forwardSpeed));
        int full = LightTexture.FULL_BRIGHT;
        FireBuf fire = new FireBuf(buffers);
        var atlas = mc.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlasSprite fire0 = atlas.apply(ResourceLocation.withDefaultNamespace("block/fire_0"));
        TextureAtlasSprite fire1 = atlas.apply(ResourceLocation.withDefaultNamespace("block/fire_1"));
        TextureAtlasSprite soul = atlas.apply(ResourceLocation.withDefaultNamespace("block/soul_fire_0"));

        // ── 땅에 남는 불자국 (월드 축 그대로) ──
        if (!bike.trail.isEmpty()) {
            Vec3 bp = bike.getPosition(pt);
            double now = bike.level().getGameTime() + pt;
            int i = 0;
            for (double[] t : bike.trail) {
                float age = (float) ((now - t[3]) / 26.0);
                if (age >= 1f || age < 0f) continue;
                float h = 0.62f * (1f - age) * (1f - age) * (0.75f + 0.25f * Mth.sin(time * 0.7f + i * 1.9f));
                float w = 0.42f * (1f - age * 0.5f);
                ps.pushPose();
                ps.translate(t[0] - bp.x, t[1] - bp.y + 0.02, t[2] - bp.z);
                flame(ps, fire, i % 2 == 0 ? fire0 : fire1, new Vector3f(), new Vector3f(0, 1, 0), w, h, 0.5f + i * 0.7f);
                ps.popPose();
                i++;
            }
        }

        ps.pushPose();
        float yaw = Mth.rotLerp(pt, bike.yRotO, bike.getYRot());
        ps.mulPose(Axis.YP.rotationDegrees(180f - yaw));
        VehicleModels.hurtWobble(ps, bike.getHurtTime(), pt);
        tilt(ps, bike, pt);
        float wheel = Mth.lerp(pt, bike.wheelAngleO, bike.wheelAngle);

        // 몸체
        VehicleModels.body(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_BODY, 1f, light);
        if (on) VehicleModels.body(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_BODY_GLOW, 1f, full);
        if (bike.getControllingPassenger() == null) VehicleModels.body(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_STAND, 1f, light);

        // 뒷바퀴
        ps.pushPose();
        Vector3f rc = u(8, REAR_Y, REAR_Z);
        ps.translate(rc.x, rc.y, rc.z);
        ps.pushPose();
        ps.mulPose(Axis.XP.rotation(wheel));
        VehicleModels.part(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_WHEEL_R, 1f, light);
        if (on) VehicleModels.part(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_GLOW_R, 1f, full);
        ps.popPose();
        float burst = 0.85f + 0.9f * (float) Math.exp(-(time - bike.ignitedAt) / 10f);
        if (on) wheelFire(ps, fire, fire0, fire1, soul, REAR_R / 16f, 0.11f, speed, time, 0f, burst);
        ps.popPose();

        // 앞부분 (조향축으로 회전)
        float steer = steerRad(bike, pt);
        ps.pushPose();
        Vector3f head = u(8, HEAD_Y, HEAD_Z);
        ps.translate(head.x, head.y, head.z);
        ps.mulPose(steerQuat(steer));
        ps.translate(-head.x, -head.y, -head.z);
        VehicleModels.body(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_FRONT, 1f, light);
        if (on) VehicleModels.body(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_FRONT_GLOW, 1f, full);
        ps.pushPose();
        Vector3f fc = u(8, FRONT_Y, FRONT_Z);
        ps.translate(fc.x, fc.y, fc.z);
        ps.pushPose();
        ps.mulPose(Axis.XP.rotation(wheel * 0.94f));
        VehicleModels.part(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_WHEEL_F, 1f, light);
        if (on) VehicleModels.part(ps, VehicleModels.buffer(buffers), VehicleModels.HELL_GLOW_F, 1f, full);
        ps.popPose();
        if (on) wheelFire(ps, fire, fire0, fire1, soul, FRONT_R / 16f, 0.07f, speed, time, 3.1f, burst);
        ps.popPose();
        // 헤드라이트 빔 (밤일수록 진하게)
        if (on) beam(ps, buffers, bike, pt);
        ps.popPose();

        // 배기구 불기둥 (가속할수록 길게)
        if (on) {
            float accel = Math.max(0f, bike.forwardSpeed - bike.forwardSpeedO);
            float revK = (float) Math.exp(-Math.max(0f, time - bike.revAt - 4f) / 5f) * (time - bike.revAt < 4f ? (time - bike.revAt) / 4f : 1f);
            if (time - bike.revAt > 40f) revK = 0f;
            float len = 0.22f + accel * 26f + speed * 0.28f + revK * 1.6f;
            Vector3f d = new Vector3f(0, 0.3827f, 0.9239f);
            for (int k = 0; k < 2; k++) {
                float y0 = k == 0 ? 8.2f : 6.2f;
                Vector3f tip = u(11.3f, y0 + 0.6f + 4.8f * 0.3827f, 25.6f + 4.8f * 0.9239f);
                float fl = len * (0.8f + 0.25f * Mth.sin(time * 1.7f + k * 2.1f));
                flame(ps, fire, k == 0 ? fire0 : fire1, tip, d, 0.16f + revK * 0.22f, fl, k * 1.3f);
                flame(ps, fire, soul, tip, d, 0.09f, fl * 0.6f, k * 1.3f + 0.7f);
            }
        }
        ps.popPose();
        super.render(bike, entityYaw, pt, ps, buffers, light);
    }

    /**
     * 바퀴를 감싼 지옥불: 타이어 둘레 위쪽 3/4 에 겹겹이 피어오르는 큰 불길 (위·뒤쪽일수록 크고 길게),
     * 속도만큼 뒤로 쓸려 넘어감. 불길 뿌리엔 푸른 영혼불 심지. 바퀴와 같이 돌지 않음.
     */
    private static void wheelFire(PoseStack ps, FireBuf vc, TextureAtlasSprite f0, TextureAtlasSprite f1, TextureAtlasSprite soul,
                                  float r, float halfW, float speed, float time, float seed, float burst) {
        int n = 18;
        float sweep = 0.15f + speed * 2.6f;
        for (int i = 0; i < n; i++) {
            float a = (i + 0.5f) / n * Mth.TWO_PI;
            float cy = Mth.cos(a), cz = Mth.sin(a);
            if (cy < -0.6f) continue;                                    // 땅에 닿는 아랫부분 제외
            float top = Math.max(0f, cy);
            float backSide = Math.max(0f, cz);
            float flick = 0.78f + 0.22f * Mth.sin(time * 0.9f + i * 2.37f + seed) + 0.1f * Mth.sin(time * 2.3f + i * 1.1f);
            float h = (0.26f + 0.22f * top + 0.12f * backSide) * flick * (1f + speed * 0.7f) * burst;
            Vector3f dir = new Vector3f(cz * 0.35f * 0, 1f, sweep + cz * 0.25f).normalize();
            for (int side = -1; side <= 1; side += 2) {
                Vector3f base = new Vector3f(side * (halfW + 0.02f), cy * r * 0.93f, cz * r * 0.93f);
                flame(ps, vc, (i + side) % 2 == 0 ? f0 : f1, base, dir, 0.34f, h, i * 0.9f + seed + side);
            }
            if (i % 2 == 0) flame(ps, vc, soul, new Vector3f(0, cy * r * 0.88f, cz * r * 0.88f), dir, 0.18f, h * 0.45f, i + seed);
        }
        // 접지부 뒤로 길게 끌리는 불꼬리
        if (speed > 0.12f) {
            Vector3f back = new Vector3f(0, 0.28f, 1f).normalize();
            for (int k = 0; k < 3; k++) {
                float fl = (0.35f + speed * 0.8f) * (0.85f + 0.15f * Mth.sin(time * 1.3f + k * 2f));
                flame(ps, vc, k % 2 == 0 ? f0 : f1, new Vector3f((k - 1) * halfW, -r * 0.8f, r * 0.35f), back, 0.36f, fl, k + seed);
            }
        }
    }

    /** 십자로 교차한 불꽃 면 2장: base 에서 dir 방향으로 높이 h, 폭 w (풀브라이트) */
    private static void flame(PoseStack ps, FireBuf fb, TextureAtlasSprite sp, Vector3f base, Vector3f dir, float w, float h, float twist) {
        Matrix4f m = ps.last().pose();
        VertexConsumer vc = fb.get();
        Vector3f up = new Vector3f(dir).normalize();
        Vector3f s1 = new Vector3f(Mth.cos(twist), 0, Mth.sin(twist));
        s1.sub(new Vector3f(up).mul(s1.dot(up))).normalize();
        Vector3f s2 = new Vector3f(up).cross(s1).normalize();
        for (Vector3f s : new Vector3f[]{s1, s2}) {
            Vector3f a = new Vector3f(base).sub(new Vector3f(s).mul(w / 2));
            Vector3f b = new Vector3f(base).add(new Vector3f(s).mul(w / 2));
            Vector3f c = new Vector3f(b).add(new Vector3f(up).mul(h));
            Vector3f d = new Vector3f(a).add(new Vector3f(up).mul(h));
            vert(vc, m, a, sp.getU0(), sp.getV1());
            vert(vc, m, b, sp.getU1(), sp.getV1());
            vert(vc, m, c, sp.getU1(), sp.getV0());
            vert(vc, m, d, sp.getU0(), sp.getV0());
        }
    }

    /** 버퍼는 쓸 때마다 다시 받음 (다른 렌더 타입을 받으면 앞의 버퍼가 닫히므로) */
    private record FireBuf(MultiBufferSource src) {
        VertexConsumer get() {
            return src.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        }
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Vector3f p, float uu, float vv) {
        vc.addVertex(m, p.x, p.y, p.z).setColor(255, 255, 255, 255).setUv(uu, vv).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
    }

    /** 헤드라이트 빔: 렌즈에서 앞쪽 아래로 퍼지는 반투명 원뿔 */
    private static void beam(PoseStack ps, MultiBufferSource buffers, HellBikeEntity bike, float pt) {
        float day = bike.level() instanceof net.minecraft.client.multiplayer.ClientLevel cl ? cl.getSkyDarken(pt) : 1f;   // 0.2(밤) ~ 1(낮)
        float dark = Mth.clamp((1f - day) / 0.75f, 0f, 1f);
        if (dark < 0.05f) return;
        int alpha = (int) (dark * 150);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentEmissive(BEAM));
        Matrix4f m = ps.last().pose();
        Vector3f o = u(8, 17.6f, -3.3f);
        Vector3f dir = new Vector3f(0, -0.12f, -1f).normalize();
        float len = 8f, r0 = 0.1f, r1 = 1.5f;
        for (int k = 0; k < 4; k++) {
            float a = k * Mth.PI / 4f;
            Vector3f s = new Vector3f(Mth.cos(a), Mth.sin(a), 0);
            Vector3f p0 = new Vector3f(o).sub(new Vector3f(s).mul(r0)), p1 = new Vector3f(o).add(new Vector3f(s).mul(r0));
            Vector3f e = new Vector3f(o).add(new Vector3f(dir).mul(len));
            Vector3f p2 = new Vector3f(e).add(new Vector3f(s).mul(r1)), p3 = new Vector3f(e).sub(new Vector3f(s).mul(r1));
            Vector3f[] q = {p0, p1, p2, p3};
            float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
            for (int side = 0; side < 2; side++)
                for (int j = 0; j < 4; j++) {
                    int idx = side == 0 ? j : 3 - j;
                    vc.addVertex(m, q[idx].x, q[idx].y, q[idx].z).setColor(255, 240, 210, alpha).setUv(uv[idx][0], uv[idx][1])
                            .setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(0, 1, 0);
                }
        }
    }

    /** 클라이언트 틱: 기울기·윌리 계산, 불자국 기록, 불씨·연기 */
    public static void fx(CarEntity car, RandomSource r) {
        if (!(car instanceof HellBikeEntity bike)) return;
        var level = bike.level();
        float fs = bike.forwardSpeed;
        boolean ridden = bike.getControllingPassenger() != null;
        bike.leanO = bike.lean;
        float target = ridden ? Mth.clamp(-bike.yawRate * Math.min(1f, Math.abs(fs) * 1.6f) * 5.0f, -30f, 30f) : 9f;
        bike.lean += (target - bike.lean) * (ridden ? 0.22f : 0.1f);
        bike.wheelieO = bike.wheelie;
        float accel = fs - bike.forwardSpeedO;
        float wt = ridden && bike.onGround() && accel > 0.012f && fs < 0.75f ? Mth.clamp((accel - 0.01f) * 900f, 0f, 16f) : 0f;
        bike.wheelie += (wt - bike.wheelie) * (wt > bike.wheelie ? 0.3f : 0.12f);
        if (HellBikeEntity.demo && !(bike.getFirstPassenger() instanceof net.minecraft.world.entity.player.Player)) {
            if (!Float.isNaN(HellBikeEntity.demoLean)) bike.lean = bike.leanO = HellBikeEntity.demoLean;
            if (!Float.isNaN(HellBikeEntity.demoWheelie)) bike.wheelie = bike.wheelieO = HellBikeEntity.demoWheelie;
        }

        boolean onNow = bike.engineRunning();
        if (onNow && !bike.wasOn) {
            bike.ignitedAt = bike.tickCount;
            // 시동: 바퀴에서 불길이 확 터짐
            for (int k = 0; k < 40; k++) {
                boolean front = k % 2 == 0;
                double a = r.nextDouble() * Math.PI * 2;
                float wr = (front ? FRONT_R : REAR_R) / 16f;
                Vec3 c = new Vec3((r.nextDouble() - 0.5) * 0.3, (front ? FRONT_Y : REAR_Y) / 16.0 + Math.cos(a) * wr, ((front ? FRONT_Z : REAR_Z) - 8) / 16.0 + Math.sin(a) * wr);
                Vec3 p = bike.position().add(bike.toWorld(c));
                level.addParticle(k % 5 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME, p.x, p.y, p.z, (r.nextDouble() - 0.5) * 0.12, 0.015 + r.nextDouble() * 0.04, (r.nextDouble() - 0.5) * 0.12);
            }
        }
        bike.wasOn = onNow;
        long now = level.getGameTime();
        while (!bike.trail.isEmpty() && now - bike.trail.peekFirst()[3] > 28) bike.trail.pollFirst();
        if (!bike.engineRunning()) return;
        float sp = Math.abs(fs);
        Vec3 pos = bike.position();
        if (sp > 0.2f && bike.onGround() && !bike.isInWater()) {
            // 뒷바퀴 접지점: 지난 점에서 0.3블록 간격으로 메워 끊김 없는 불길 자국
            Vec3 p = pos.add(bike.toWorld(new Vec3(0, 0, (REAR_Z - 8) / 16.0 + 0.1)));
            double[] last = bike.trail.peekLast();
            if (last == null || now - last[3] > 3) bike.trail.addLast(new double[]{p.x, p.y, p.z, now});
            else {
                double dx = p.x - last[0], dy = p.y - last[1], dz = p.z - last[2];
                double dist = Math.sqrt(dx * dx + dz * dz);
                int k = (int) Math.min(8, Math.floor(dist / 0.3));
                for (int j = 1; j <= k; j++) {
                    double t = j * 0.3 / dist;
                    bike.trail.addLast(new double[]{last[0] + dx * t, last[1] + dy * t, last[2] + dz * t, now - (1 - (double) j / k)});
                }
            }
        }
        Vec3 back = bike.forward().scale(-0.04 - sp * 0.25);
        // 바퀴 불씨
        if (r.nextInt(2) == 0) {
            boolean front = r.nextBoolean();
            double a = r.nextDouble() * Math.PI;
            float wr = (front ? FRONT_R : REAR_R) / 16f;
            Vec3 c = new Vec3(0, (front ? FRONT_Y : REAR_Y) / 16.0 + Math.cos(a) * wr, ((front ? FRONT_Z : REAR_Z) - 8) / 16.0 + Math.sin(a) * wr);
            Vec3 p = pos.add(bike.toWorld(c));
            level.addParticle(ParticleTypes.SMALL_FLAME, p.x, p.y, p.z, back.x * 0.6, 0.04 + r.nextDouble() * 0.05, back.z * 0.6);
        }
        // 배기 아지랑이 (가끔, 옅게)
        if (r.nextInt(5) == 0) {
            for (int k = 0; k < 2; k++) {
                float y0 = k == 0 ? 8.2f : 6.2f;
                Vector3f t = u(11.3f, y0 + 2.6f, 30.3f);
                Vec3 p = pos.add(bike.toWorld(new Vec3(t.x, t.y, t.z)));
                level.addParticle(ParticleTypes.SMOKE, p.x, p.y, p.z, back.x, 0.02, back.z);
            }
        }
    }

    @Override
    public ResourceLocation getTextureLocation(HellBikeEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
