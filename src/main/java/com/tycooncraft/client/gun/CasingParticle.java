package com.tycooncraft.client.gun;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * v0.32: 입체 탄피. 놋쇠 원통(샷건은 빨간 플라스틱 몸통 + 놋쇠 바닥)이 빙글빙글 돌며 날아가
 * 바닥에 튕길 때마다 '팅'(샷건은 '툭') 소리를 내고, 몇 번 튀다가 옆으로 누워 굴러가 멈춘다.
 */
public final class CasingParticle extends Particle {
    private static int alive;
    private final GunKind.Casing type;
    private final Quaternionf rot = new Quaternionf(), rotO = new Quaternionf();
    private final Vector3f spin;
    private int bounces;
    private boolean resting;
    private final TextureAtlasSprite sprite;

    public CasingParticle(ClientLevel level, Vec3 pos, Vector3f vel, GunKind.Casing type, Vector3f bore, Vector3f up) {
        super(level, pos.x, pos.y, pos.z);
        this.type = type;
        xd = vel.x;
        yd = vel.y;
        zd = vel.z;
        gravity = 0f;
        friction = 1f;
        hasPhysics = true;
        lifetime = 160 + random.nextInt(40);
        float s = type.rad * 1.6f;
        setSize(s, s);
        // 탄피 축 = 총열 방향 (모델의 +z 가 축)
        Vector3f axis = new Vector3f(bore).normalize();
        rot.rotationTo(new Vector3f(0, 0, 1), axis);
        rotO.set(rot);
        // 주로 위 축을 중심으로 앞뒤로 뒤집히며 돎
        spin = new Vector3f(up).mul(0.55f + random.nextFloat() * 0.45f).add((random.nextFloat() - 0.5f) * 0.3f, (random.nextFloat() - 0.5f) * 0.3f, (random.nextFloat() - 0.5f) * 0.3f);
        sprite = sprite(type.shotshell ? "tc_shotshell" : "tc_brass");
        alive++;
        if (alive > 160) lifetime = 40;
    }

    private static TextureAtlasSprite sprite(String name) {
        return Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES) instanceof TextureAtlas atlas
                ? atlas.getSprite(com.tycooncraft.TycoonCraft.id(name)) : null;
    }

    @Override
    public void remove() {
        if (!removed) alive--;
        super.remove();
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        rotO.set(rot);
        if (age++ >= lifetime) {
            remove();
            return;
        }
        if (!resting) {
            yd -= 0.045;
            xd *= 0.985;
            zd *= 0.985;
            yd *= 0.985;
        }
        double preY = yd, preX = xd, preZ = zd;
        move(xd, yd, zd);
        if (onGround && preY < -0.02) {
            float speed = (float) -preY;
            if (speed > 0.06 && bounces < 4) {
                yd = speed * (type.shotshell ? 0.22f : 0.36f) * (0.8 + random.nextFloat() * 0.4);
                xd = preX * 0.55 + (random.nextFloat() - 0.5f) * 0.03;
                zd = preZ * 0.55 + (random.nextFloat() - 0.5f) * 0.03;
                spin.mul(0.6f).add((random.nextFloat() - 0.5f) * 0.6f, (random.nextFloat() - 0.5f) * 0.6f, (random.nextFloat() - 0.5f) * 0.6f);
                clink(Math.min(1f, speed * 2.4f));
                bounces++;
            } else {
                if (bounces < 5 && speed > 0.03) clink(Math.min(0.5f, speed * 2f));
                bounces = 5;
                yd = 0;
                xd = preX * 0.6;
                zd = preZ * 0.6;
            }
        }
        if (onGround && bounces >= 4) {
            // 옆으로 눕고 축에 수직으로 굴러가다 멈춤
            xd *= 0.8;
            zd *= 0.8;
            Vector3f axis = rot.transform(new Vector3f(0, 0, 1));
            axis.y = 0;
            if (axis.lengthSquared() < 1e-4f) axis.set(1, 0, 0);
            axis.normalize();
            Quaternionf flat = new Quaternionf().rotationTo(new Vector3f(0, 0, 1), axis);
            float roll = (float) Math.sqrt(xd * xd + zd * zd) / Math.max(0.005f, type.rad) * 0.5f;
            rollAngle += roll;
            flat.rotateZ(rollAngle);
            rot.slerp(flat, 0.35f);
            spin.mul(0.5f);
            if (xd * xd + zd * zd < 1e-6) resting = true;
        } else {
            float a = spin.length();
            if (a > 1e-4f) rot.premul(new Quaternionf().rotateAxis(a, spin.x / a, spin.y / a, spin.z / a));
        }
    }

    private float rollAngle;

    private void clink(float vol) {
        var s = type.shotshell ? ModSounds.CASING_SHOTGUN : (type == GunKind.Casing.BIG || type == GunKind.Casing.RT20) ? ModSounds.CASING_BIG : ModSounds.CASING_BRASS;
        float pitch = switch (type) {
            case PISTOL -> 1.15f;
            case RIFLE -> 1.0f;
            case BIG -> 1.0f;
            case RT20 -> 0.72f;
            case SHOTGUN -> 1.0f;
        };
        level.playLocalSound(x, y, z, s.get(), SoundSource.PLAYERS, 0.2f + 0.45f * vol, pitch * (0.92f + random.nextFloat() * 0.16f), false);
    }

    public static int renders;

    @Override
    public void render(VertexConsumer vc, Camera cam, float pt) {
        renders++;
        if (sprite == null) return;
        Vec3 cp = cam.getPosition();
        float px = (float) (Mth.lerp(pt, xo, x) - cp.x), py = (float) (Mth.lerp(pt, yo, y) - cp.y), pz = (float) (Mth.lerp(pt, zo, z) - cp.z);
        Quaternionf q = new Quaternionf(rotO).slerp(rot, pt);
        float fade = age > lifetime - 15 ? Math.max(0f, (lifetime - age - pt) / 15f) : 1f;
        float r = type.rad * fade, hl = type.len * 0.5f * fade;
        if (r <= 0.0005f) return;
        // 원통 대신 팔각 기둥 (8면) + 양 끝
        int light = getLightColor(pt);
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();
        float uHead = Mth.lerp(0.18f, u0, u1);   // 텍스처 왼쪽 18% = 바닥(뇌관) 쪽
        int sides = 8;
        Vector3f light0 = new Vector3f(0.35f, 1f, 0.45f).normalize();
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides, a1 = Mth.TWO_PI * (i + 1) / sides;
            Vector3f n = new Vector3f(Mth.cos((a0 + a1) / 2), Mth.sin((a0 + a1) / 2), 0);
            q.transform(n);
            float sh = 0.55f + 0.45f * Math.max(0f, n.dot(light0)) + 0.12f * Math.max(0f, -n.y);
            float vv0 = Mth.lerp((float) i / sides, v0, v1), vv1 = Mth.lerp((float) (i + 1) / sides, v0, v1);
            Vector3f a = p(q, Mth.cos(a0) * r, Mth.sin(a0) * r, -hl), b = p(q, Mth.cos(a1) * r, Mth.sin(a1) * r, -hl),
                    c = p(q, Mth.cos(a1) * r, Mth.sin(a1) * r, hl), d = p(q, Mth.cos(a0) * r, Mth.sin(a0) * r, hl);
            vert(vc, a, px, py, pz, u0, vv0, sh, light);
            vert(vc, b, px, py, pz, u0, vv1, sh, light);
            vert(vc, c, px, py, pz, u1, vv1, sh, light);
            vert(vc, d, px, py, pz, u1, vv0, sh, light);
            // 끝면 (부채꼴 대신 사각형 두 장으로 근사)
            Vector3f o1 = p(q, 0, 0, -hl), o2 = p(q, 0, 0, hl);
            float she = 0.75f;
            vert(vc, o1, px, py, pz, u0, v0, she, light);
            vert(vc, o1, px, py, pz, u0, v0, she, light);
            vert(vc, b, px, py, pz, uHead, vv1, she, light);
            vert(vc, a, px, py, pz, uHead, vv0, she, light);
            vert(vc, o2, px, py, pz, u1, v0, she * 0.6f, light);
            vert(vc, d, px, py, pz, u1, vv0, she * 0.6f, light);
            vert(vc, c, px, py, pz, u1, vv1, she * 0.6f, light);
            vert(vc, o2, px, py, pz, u1, v0, she * 0.6f, light);
        }
    }

    private static Vector3f p(Quaternionf q, float x, float y, float z) {
        return q.transform(new Vector3f(x, y, z));
    }

    private static void vert(VertexConsumer vc, Vector3f v, float px, float py, float pz, float u, float vv, float sh, int light) {
        vc.addVertex(v.x + px, v.y + py, v.z + pz).setUv(u, vv).setColor(sh, sh, sh, 1f).setLight(light);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }
}
