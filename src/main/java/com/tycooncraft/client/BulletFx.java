package com.tycooncraft.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tycooncraft.network.TracerPayload;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 눈에 보이는 총알: 빛나는 탄 줄기가 총구에서 탄착점까지 날아가고, 총구 화염 · 스치는 소리 · 탄착 파편 */
public final class BulletFx {
    public static int spawned;

    public static void spawn(TracerPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        mc.particleEngine.add(new Bullet(mc.level, p));
        if (p.flash()) mc.particleEngine.add(new Flash(mc.level, p.from(), p.kind()));
        spawned++;
    }

    static TextureAtlasSprite sprite() {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_PARTICLES).apply(ResourceLocation.withDefaultNamespace("flash"));
    }

    /** kind 별 색: {심 r,g,b, 테두리 r,g,b} */
    static final float[][] COLORS = {
            {1f, 0.95f, 0.75f, 1f, 0.7f, 0.25f},
            {1f, 0.85f, 0.5f, 1f, 0.4f, 0.1f},
            {0.95f, 0.8f, 1f, 0.6f, 0.2f, 1f},
            {1f, 0.8f, 0.6f, 1f, 0.25f, 0.15f},
    };

    static void quad(VertexConsumer vc, Vec3 a, Vec3 b, Vec3 cam, float w, float r, float g, float bl, float al, TextureAtlasSprite s) {
        Vec3 dir = b.subtract(a);
        Vec3 view = a.add(b).scale(0.5).subtract(cam);
        Vec3 side = dir.cross(view);
        if (side.lengthSqr() < 1e-8) side = new Vec3(0, 1, 0);
        side = side.normalize().scale(w);
        float u0 = Mth.lerp(0.4f, s.getU0(), s.getU1()), u1 = Mth.lerp(0.6f, s.getU0(), s.getU1());
        float v0 = Mth.lerp(0.4f, s.getV0(), s.getV1()), v1 = Mth.lerp(0.6f, s.getV0(), s.getV1());
        int light = 0xF000F0;
        Vec3 p1 = a.add(side).subtract(cam), p2 = a.subtract(side).subtract(cam), p3 = b.subtract(side).subtract(cam), p4 = b.add(side).subtract(cam);
        // 양면
        vc.addVertex((float) p1.x, (float) p1.y, (float) p1.z).setUv(u0, v0).setColor(r, g, bl, al * 0.6f).setLight(light);
        vc.addVertex((float) p2.x, (float) p2.y, (float) p2.z).setUv(u0, v1).setColor(r, g, bl, al * 0.6f).setLight(light);
        vc.addVertex((float) p3.x, (float) p3.y, (float) p3.z).setUv(u1, v1).setColor(r, g, bl, al).setLight(light);
        vc.addVertex((float) p4.x, (float) p4.y, (float) p4.z).setUv(u1, v0).setColor(r, g, bl, al).setLight(light);
        vc.addVertex((float) p4.x, (float) p4.y, (float) p4.z).setUv(u1, v0).setColor(r, g, bl, al).setLight(light);
        vc.addVertex((float) p3.x, (float) p3.y, (float) p3.z).setUv(u1, v1).setColor(r, g, bl, al).setLight(light);
        vc.addVertex((float) p2.x, (float) p2.y, (float) p2.z).setUv(u0, v1).setColor(r, g, bl, al * 0.6f).setLight(light);
        vc.addVertex((float) p1.x, (float) p1.y, (float) p1.z).setUv(u0, v0).setColor(r, g, bl, al * 0.6f).setLight(light);
    }

    static final class Bullet extends Particle {
        final Vec3 from, to, dir;
        final double dist, len, speed;
        final int kind, impact, shooter;
        final TextureAtlasSprite sprite = sprite();
        boolean whizzed, landed;

        Bullet(ClientLevel level, TracerPayload p) {
            super(level, p.from().x, p.from().y, p.from().z);
            from = p.from();
            to = p.to();
            Vec3 d = to.subtract(from);
            dist = d.length();
            dir = dist > 1e-6 ? d.scale(1 / dist) : new Vec3(0, 0, 1);
            speed = p.speed();
            len = Math.min(3.2, speed * 0.22);
            kind = Mth.clamp(p.kind(), 0, 3);
            impact = p.impact();
            shooter = p.shooter();
            hasPhysics = false;
            gravity = 0;
            lifetime = (int) Math.ceil((dist + len) / speed) + 1;
            setBoundingBox(new AABB(from, to).inflate(0.5));
            Minecraft mc = Minecraft.getInstance();
            whizzed = mc.player != null && mc.player.getId() == shooter;
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            double prevH = Math.min(dist, age * speed);
            if (age++ >= lifetime) {
                remove();
                return;
            }
            double h = Math.min(dist, age * speed);
            Vec3 head = from.add(dir.scale(h));
            setPos(head.x, head.y, head.z);
            setBoundingBox(new AABB(from, to).inflate(0.5));
            // 내 머리 근처를 스치면 '휙' (쏜 사람 본인은 제외)
            if (!whizzed) {
                Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
                Vec3 a = from.add(dir.scale(prevH));
                double tt = Mth.clamp(cam.subtract(a).dot(dir), 0, h - prevH);
                Vec3 close = a.add(dir.scale(tt));
                if (close.distanceTo(cam) < 3.0 && to.distanceTo(cam) > 1.5) {
                    whizzed = true;
                    level.playLocalSound(close.x, close.y, close.z, ModSounds.BULLET_WHIZ.get(), SoundSource.PLAYERS, 0.9f, 0.9f + random.nextFloat() * 0.25f, false);
                }
            }
            if (!landed && h >= dist) {
                landed = true;
                land();
            }
        }

        private void land() {
            if (impact != 1) return;
            BlockPos bp = BlockPos.containing(to.add(dir.scale(0.05)));
            BlockState st = level.getBlockState(bp);
            if (st.isAir()) return;
            Vec3 back = dir.scale(-1);
            for (int i = 0; i < 8; i++)
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, st), to.x, to.y, to.z,
                        back.x * 0.15 + random.nextGaussian() * 0.08, back.y * 0.15 + 0.1 + random.nextGaussian() * 0.06, back.z * 0.15 + random.nextGaussian() * 0.08);
            level.addParticle(ParticleTypes.SMOKE, to.x, to.y, to.z, back.x * 0.02, 0.02, back.z * 0.02);
            boolean metal = st.getSoundType() == SoundType.METAL || st.getSoundType() == SoundType.ANVIL || st.getSoundType() == SoundType.NETHERITE_BLOCK
                    || st.getSoundType() == SoundType.COPPER || st.getSoundType() == SoundType.CHAIN;
            boolean soft = st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(BlockTags.LEAVES) || st.is(BlockTags.WOOL) || st.is(BlockTags.LOGS) || st.is(BlockTags.PLANKS);
            if (metal || !soft)
                for (int i = 0; i < 4; i++)
                    level.addParticle(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, back.x * 0.2 + random.nextGaussian() * 0.1, 0.12 + random.nextGaussian() * 0.08, back.z * 0.2 + random.nextGaussian() * 0.1);
            SoundEvent s = metal ? ModSounds.BULLET_IMPACT_METAL.get() : soft ? ModSounds.BULLET_IMPACT_DIRT.get() : ModSounds.BULLET_IMPACT_STONE.get();
            level.playLocalSound(to.x, to.y, to.z, s, SoundSource.PLAYERS, 0.7f, 0.9f + random.nextFloat() * 0.2f, false);
        }

        @Override
        public void render(VertexConsumer vc, Camera camera, float pt) {
            double trav = (age + pt) * speed;
            double h = Math.min(trav, dist), tl = Math.max(0, trav - len);
            if (tl >= h - 1e-3) return;
            Vec3 head = from.add(dir.scale(h)), tail = from.add(dir.scale(tl));
            Vec3 cam = camera.getPosition();
            float[] c = COLORS[kind];
            float fade = trav > dist ? (float) Math.max(0, 1 - (trav - dist) / len) : 1f;
            float w = kind == 3 ? 0.07f : kind == 2 ? 0.055f : 0.04f;
            quad(vc, tail, head, cam, w * 2.6f, c[3], c[4], c[5], 0.35f * fade, sprite);   // 바깥 빛
            quad(vc, tail, head, cam, w, c[0], c[1], c[2], 0.95f * fade, sprite);          // 밝은 심
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** 총구 화염: 2틱 동안 십자 모양 섬광 */
    static final class Flash extends Particle {
        final TextureAtlasSprite sprite = sprite();
        final int kind;

        Flash(ClientLevel level, Vec3 at, int kind) {
            super(level, at.x, at.y, at.z);
            this.kind = Mth.clamp(kind, 0, 3);
            lifetime = 2;
            hasPhysics = false;
            gravity = 0;
            roll = random.nextFloat() * Mth.TWO_PI;
        }

        @Override
        public void tick() {
            if (age++ >= lifetime) remove();
        }

        @Override
        public void render(VertexConsumer vc, Camera camera, float pt) {
            Vec3 cam = camera.getPosition();
            Vec3 c = new Vec3(x, y, z);
            Vec3 view = c.subtract(cam).normalize();
            Vec3 up = Math.abs(view.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
            Vec3 a = view.cross(up).normalize(), b = view.cross(a).normalize();
            float s = (1 - (age + pt) / (lifetime + 1)) * 0.32f;
            float[] col = COLORS[kind];
            for (int i = 0; i < 3; i++) {
                double ang = roll + i * Math.PI / 3;
                Vec3 d = a.scale(Math.cos(ang)).add(b.scale(Math.sin(ang))).scale(s);
                quad(vc, c.subtract(d), c.add(d), cam, s * 0.35f, col[3], col[4], col[5], 0.8f, sprite);
            }
            quad(vc, c.subtract(a.scale(s * 0.3)), c.add(a.scale(s * 0.3)), cam, s * 0.3f, col[0], col[1], col[2], 1f, sprite);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private BulletFx() {}
}
