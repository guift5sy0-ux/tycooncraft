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
        boolean mine = mc.player != null && mc.player.getId() == p.shooter();
        boolean fp = mine && mc.options.getCameraType().isFirstPerson() && mc.player.getVehicle() == null;
        if (fp) {
            // 1인칭: 손에 든 총 모델은 파티클 위에 덧그려지므로, 화면상 총구 끝 바로 앞(총에 가리지 않는 곳)에서 섬광·연기·총알이 나오게
            var cam = mc.gameRenderer.getMainCamera();
            Vec3 look = new Vec3(cam.getLookVector()), up = new Vec3(cam.getUpVector()), right = look.cross(up).normalize();
            boolean left = mc.player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT;
            if (mc.player.getMainHandItem().getItem() instanceof com.tycooncraft.gun.GunItem g && g.isDual()
                    && mc.player.getPersistentData().getBoolean("tycooncraft_dual_left")) left = !left;
            boolean scoped = com.tycooncraft.client.GunClient.isScoping();
            Vec3 fpm = scoped ? cam.getPosition().add(look.scale(1.6)).subtract(up.scale(0.12))
                    : cam.getPosition().add(look.scale(1.9)).add(right.scale(left ? -0.27 : 0.27)).subtract(up.scale(0.19));
            p = new TracerPayload(fpm, p.to(), p.kind(), p.impact(), p.shooter(), p.speed(), p.flash());
        }
        mc.particleEngine.add(new Bullet(mc.level, p));
        Vec3 d = p.to().subtract(p.from());
        Vec3 dir = d.lengthSqr() > 1e-6 ? d.normalize() : new Vec3(0, 0, 1);
        // 총기 크기 대략: 탄속(저격 30 > 소총 22 > 권총·기관단총 16), 기관포(kind 3)는 크게
        float size = p.kind() == 3 ? 1.6f : 0.75f + (p.speed() - 16f) * 0.04f;
        if (p.flash()) {
            mc.particleEngine.add(new Flash(mc.level, p.from(), dir, p.kind(), size * (fp ? 0.55f : 1f)));
            // 총구 연기: 앞으로 뿜어졌다가 느려지며 퍼짐
            int puffs = 2 + (int) (size * 2);
            for (int i = 0; i < puffs; i++)
                mc.particleEngine.add(new Smoke(mc.level, p.from().add(dir.scale(0.1 + i * 0.12)), dir.scale(0.05 + 0.09 * mc.level.random.nextFloat())
                        .add(gauss(mc.level, 0.012)), 0.06f * size, 0.35f + 0.25f * size, 0.22f, 16 + mc.level.random.nextInt(18), 0.78f));
        } else {   // 소음기: 옅은 김만
            mc.particleEngine.add(new Smoke(mc.level, p.from(), dir.scale(0.03), 0.04f, 0.2f, 0.12f, 12, 0.8f));
        }
        // 탄피 배출: 사람이 든 총만 (포탑·차량·전투기 제외)
        var shooter = mc.level.getEntity(p.shooter());
        if (shooter instanceof net.minecraft.world.entity.LivingEntity && !(shooter.getVehicle() != null && !(shooter.getVehicle() instanceof net.minecraft.world.entity.LivingEntity))
                && p.kind() != 3 && !(shooter instanceof com.tycooncraft.mech.ProtoDestroyer) && !(shooter instanceof com.tycooncraft.mob.DestroyerGolem)) {
            Vec3 right = dir.cross(new Vec3(0, 1, 0));
            right = right.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : right.normalize();
            Vec3 at = fp ? p.from().subtract(dir.scale(1.0)).add(right.scale(0.12)).add(0, -0.05, 0) : p.from().subtract(dir.scale(0.6)).add(right.scale(0.05));
            mc.particleEngine.add(new Shell(mc.level, at, right.scale(0.09 + 0.04 * mc.level.random.nextFloat()).add(0, 0.11, 0).add(dir.scale(-0.02))));
        }
        spawned++;
    }

    static Vec3 gauss(ClientLevel l, double s) {
        return new Vec3(l.random.nextGaussian() * s, l.random.nextGaussian() * s, l.random.nextGaussian() * s);
    }

    private static final java.util.Map<String, TextureAtlasSprite> SPRITES = new java.util.HashMap<>();

    static TextureAtlasSprite fx(String name) {
        return SPRITES.computeIfAbsent(name, n -> Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES) instanceof TextureAtlas atlas
                ? atlas.getSprite(com.tycooncraft.TycoonCraft.id(n)) : sprite());
    }

    /** 카메라를 향한 사각형 (회전·크기·색·빛) */
    static void billboard(VertexConsumer vc, Camera cam, Vec3 c, float size, float rot, TextureAtlasSprite sp, float r, float g, float b, float a, int light) {
        org.joml.Quaternionf q = new org.joml.Quaternionf(cam.rotation()).rotateZ(rot);
        Vec3 o = c.subtract(cam.getPosition());
        org.joml.Vector3f[] v = {new org.joml.Vector3f(-1, -1, 0), new org.joml.Vector3f(-1, 1, 0), new org.joml.Vector3f(1, 1, 0), new org.joml.Vector3f(1, -1, 0)};
        float[][] uv = {{sp.getU1(), sp.getV1()}, {sp.getU1(), sp.getV0()}, {sp.getU0(), sp.getV0()}, {sp.getU0(), sp.getV1()}};
        for (int i = 0; i < 4; i++) {
            v[i].rotate(q).mul(size).add((float) o.x, (float) o.y, (float) o.z);
            vc.addVertex(v[i].x(), v[i].y(), v[i].z()).setUv(uv[i][0], uv[i][1]).setColor(r, g, b, a).setLight(light);
        }
    }

    /** 총구·탄착 연기: 처음엔 빠르게 퍼지고 점점 느려지며 커지고, 살짝 떠오르며 사라짐 (조명 영향 받음) */
    static final class Smoke extends Particle {
        final TextureAtlasSprite sp;
        final float s0, s1, a0, spin, rot0, tint;
        float r = 1, g = 1, b = 1;

        Smoke(ClientLevel level, Vec3 at, Vec3 vel, float s0, float s1, float a0, int life, float tint) {
            super(level, at.x, at.y, at.z);
            this.sp = fx("tc_smoke_" + random.nextInt(4));
            this.s0 = s0;
            this.s1 = s1;
            this.a0 = a0;
            this.tint = tint * (0.92f + random.nextFloat() * 0.16f);
            lifetime = life;
            hasPhysics = false;
            gravity = 0;
            xd = vel.x;
            yd = vel.y;
            zd = vel.z;
            spin = (random.nextFloat() - 0.5f) * 0.06f;
            rot0 = random.nextFloat() * Mth.TWO_PI;
        }

        Smoke color(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            return this;
        }

        @Override
        public void tick() {
            xo = x;
            yo = y;
            zo = z;
            if (age++ >= lifetime) {
                remove();
                return;
            }
            x += xd;
            y += yd;
            z += zd;
            xd *= 0.82;
            yd = yd * 0.82 + 0.0025;
            zd *= 0.82;
        }

        @Override
        public void render(VertexConsumer vc, Camera cam, float pt) {
            float t = Mth.clamp((age + pt) / lifetime, 0, 1);
            float size = Mth.lerp(1 - (1 - t) * (1 - t), s0, s1);
            float alpha = a0 * (t < 0.1f ? t / 0.1f : (1 - t) * (1 - t) / 0.81f);
            Vec3 c = new Vec3(Mth.lerp(pt, xo, x), Mth.lerp(pt, yo, y), Mth.lerp(pt, zo, z));
            int light = getLightColor(pt);
            billboard(vc, cam, c, size, rot0 + spin * (age + pt), sp, tint * r, tint * g, tint * b, alpha, light);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** 불꽃: 짧고 가는 빛줄기로 튀었다가 중력으로 떨어지며 식음 */
    static final class Spark extends Particle {
        final TextureAtlasSprite sp = fx("tc_glow");

        Spark(ClientLevel level, Vec3 at, Vec3 vel) {
            super(level, at.x, at.y, at.z);
            xd = vel.x;
            yd = vel.y;
            zd = vel.z;
            lifetime = 5 + random.nextInt(7);
            gravity = 0;
            hasPhysics = true;
        }

        @Override
        public void tick() {
            super.tick();
            yd -= 0.05;
            xd *= 0.9;
            zd *= 0.9;
        }

        @Override
        public void render(VertexConsumer vc, Camera cam, float pt) {
            Vec3 p = new Vec3(Mth.lerp(pt, xo, x), Mth.lerp(pt, yo, y), Mth.lerp(pt, zo, z));
            Vec3 tail = p.subtract(xd * 1.4, yd * 1.4, zd * 1.4);
            float t = (age + pt) / lifetime;
            quad(vc, tail, p, cam.getPosition(), 0.018f, 1f, 0.85f - 0.4f * t, 0.45f - 0.4f * t, 1f - t * t, sp, 0.3f, 0.7f);
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** 탄피: 오른쪽 위로 튀어 나가 빙글빙글 돌며 떨어지고, 바닥에 '팅' 소리 */
    static final class Shell extends Particle {
        final TextureAtlasSprite sp = fx("tc_shell");
        float rot, spinV;
        boolean clinked;

        Shell(ClientLevel level, Vec3 at, Vec3 vel) {
            super(level, at.x, at.y, at.z);
            xd = vel.x;
            yd = vel.y;
            zd = vel.z;
            gravity = 0.9f;
            lifetime = 45;
            hasPhysics = true;
            setSize(0.05f, 0.05f);
            spinV = 0.6f + random.nextFloat() * 0.5f;
        }

        @Override
        public void tick() {
            super.tick();
            rot += spinV;
            if (onGround) {
                spinV *= 0.6f;
                if (!clinked) {
                    clinked = true;
                    level.playLocalSound(x, y, z, net.minecraft.sounds.SoundEvents.CHAIN_FALL, SoundSource.PLAYERS, 0.25f, 1.9f + random.nextFloat() * 0.2f, false);
                    yd = 0.06;
                }
            }
        }

        @Override
        public void render(VertexConsumer vc, Camera cam, float pt) {
            Vec3 c = new Vec3(Mth.lerp(pt, xo, x), Mth.lerp(pt, yo, y), Mth.lerp(pt, zo, z));
            float a = age > lifetime - 10 ? (lifetime - age - pt) / 10f : 1f;
            billboard(vc, cam, c, 0.035f, rot + spinV * pt, sp, 1, 1, 1, Math.max(0, a), getLightColor(pt));
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private static TextureAtlasSprite cached;

    static TextureAtlasSprite sprite() {
        // 파티클 아틀라스는 모델 아틀라스 목록(getTextureAtlas)에 없다 → 텍스처 매니저에서 직접
        if (cached == null && Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES) instanceof TextureAtlas atlas)
            cached = atlas.getSprite(ResourceLocation.withDefaultNamespace("flash"));
        return cached;
    }

    /** kind 별 색: {심 r,g,b, 테두리 r,g,b} */
    static final float[][] COLORS = {
            {1f, 0.95f, 0.75f, 1f, 0.7f, 0.25f},
            {1f, 0.85f, 0.5f, 1f, 0.4f, 0.1f},
            {0.95f, 0.8f, 1f, 0.6f, 0.2f, 1f},
            {1f, 0.8f, 0.6f, 1f, 0.25f, 0.15f},
    };

    static void quad(VertexConsumer vc, Vec3 a, Vec3 b, Vec3 cam, float w, float r, float g, float bl, float al, TextureAtlasSprite s) {
        quad(vc, a, b, cam, w, r, g, bl, al, s, 0.4f, 0.6f);
    }

    /** uvLo~uvHi: 스프라이트에서 쓸 구간 (0.4~0.6 = 가운데만, 0~1 = 전체) */
    static void quad(VertexConsumer vc, Vec3 a, Vec3 b, Vec3 cam, float w, float r, float g, float bl, float al, TextureAtlasSprite s, float uvLo, float uvHi) {
        Vec3 dir = b.subtract(a);
        Vec3 view = a.add(b).scale(0.5).subtract(cam);
        Vec3 side = dir.cross(view);
        if (side.lengthSqr() < 1e-8) side = new Vec3(0, 1, 0);
        side = side.normalize().scale(w);
        float u0 = Mth.lerp(uvLo, s.getU0(), s.getU1()), u1 = Mth.lerp(uvHi, s.getU0(), s.getU1());
        float v0 = Mth.lerp(uvLo, s.getV0(), s.getV1()), v1 = Mth.lerp(uvHi, s.getV0(), s.getV1());
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
            len = p.kind() == 0 ? Math.min(1.6, speed * 0.1) : Math.min(3.2, speed * 0.22);
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
            if (impact == 2) {
                Vec3 back = dir.scale(-1);
                var hit = level.getEntities((net.minecraft.world.entity.Entity) null, new AABB(to, to).inflate(0.8), e -> e.isPickable());
                boolean machine = hit.stream().anyMatch(e -> !(e instanceof net.minecraft.world.entity.LivingEntity) || e instanceof com.tycooncraft.mob.DestroyerGolem
                        || e instanceof com.tycooncraft.mech.ProtoDestroyer || e instanceof net.minecraft.world.entity.animal.IronGolem);
                if (machine) {
                    for (int i = 0; i < 6; i++) Minecraft.getInstance().particleEngine.add(new Spark(level, to, back.scale(0.2).add(gauss(level, 0.12)).add(0, 0.08, 0)));
                    level.playLocalSound(to.x, to.y, to.z, ModSounds.BULLET_IMPACT_METAL.get(), SoundSource.PLAYERS, 0.6f, 1.0f + random.nextFloat() * 0.2f, false);
                } else {
                    for (int i = 0; i < 3; i++)
                        Minecraft.getInstance().particleEngine.add(new Smoke(level, to, back.scale(0.04).add(gauss(level, 0.02)), 0.05f, 0.28f, 0.32f, 10 + random.nextInt(6), 1f)
                                .color(0.55f, 0.08f, 0.07f));
                }
                return;
            }
            if (impact != 1) return;
            BlockPos bp = BlockPos.containing(to.add(dir.scale(0.05)));
            BlockState st = level.getBlockState(bp);
            if (st.isAir()) return;
            Vec3 back = dir.scale(-1);
            // 작은 파편 몇 개 + 블록 색 흙먼지가 튀어나와 퍼짐
            for (int i = 0; i < 4; i++)
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, st), to.x, to.y, to.z,
                        back.x * 0.12 + random.nextGaussian() * 0.06, back.y * 0.12 + 0.08 + random.nextGaussian() * 0.05, back.z * 0.12 + random.nextGaussian() * 0.06);
            int col = st.getMapColor(level, bp).col;
            float cr = ((col >> 16) & 255) / 255f, cg = ((col >> 8) & 255) / 255f, cb = (col & 255) / 255f;
            for (int i = 0; i < 4; i++)
                Minecraft.getInstance().particleEngine.add(new Smoke(level, to.add(back.scale(0.05)), back.scale(0.05 + 0.05 * i).add(gauss(level, 0.02)), 0.06f, 0.35f + 0.08f * i,
                        0.45f, 14 + random.nextInt(14), 1f).color(0.5f + cr * 0.5f, 0.5f + cg * 0.5f, 0.5f + cb * 0.5f));
            boolean metal = st.getSoundType() == SoundType.METAL || st.getSoundType() == SoundType.ANVIL || st.getSoundType() == SoundType.NETHERITE_BLOCK
                    || st.getSoundType() == SoundType.COPPER || st.getSoundType() == SoundType.CHAIN;
            boolean soft = st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(BlockTags.LEAVES) || st.is(BlockTags.WOOL) || st.is(BlockTags.LOGS) || st.is(BlockTags.PLANKS);
            if (metal || !soft)
                for (int i = 0; i < (metal ? 6 : 2); i++)
                    Minecraft.getInstance().particleEngine.add(new Spark(level, to, back.scale(0.18).add(gauss(level, 0.1)).add(0, 0.06, 0)));
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
            if (kind == 0) {   // 일반탄: 실제 총알처럼 거의 안 보이게, 옅고 가는 줄만
                quad(vc, tail, head, cam, 0.012f, 1f, 0.95f, 0.85f, 0.28f * fade, sprite);
                return;
            }
            float w = kind == 3 ? 0.05f : kind == 2 ? 0.045f : 0.03f;
            quad(vc, tail, head, cam, w * 2.2f, c[3], c[4], c[5], 0.22f * fade, sprite);   // 바깥 빛
            quad(vc, tail, head, cam, w, c[0], c[1], c[2], 0.85f * fade, sprite);          // 밝은 심
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    /** 총구 화염: 아주 짧게(1~2틱) 번쩍 — 정면에선 꽃잎 모양 섬광, 옆에선 앞으로 뻗는 화염 혀 */
    static final class Flash extends Particle {
        final TextureAtlasSprite front = fx("tc_flash_" + random.nextInt(2)), side = fx("tc_flash_side"), glow = fx("tc_glow");
        final Vec3 dir;
        final float size;
        final int kind;

        Flash(ClientLevel level, Vec3 at, Vec3 dir, int kind, float size) {
            super(level, at.x, at.y, at.z);
            this.dir = dir;
            this.kind = Mth.clamp(kind, 0, 3);
            this.size = size * (0.8f + random.nextFloat() * 0.4f);
            lifetime = 1 + random.nextInt(2);
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
            Vec3 c = new Vec3(x, y, z);
            float k = 1 - (age + pt) / (lifetime + 1);
            float s = size * (0.6f + 0.4f * k);
            boolean fire = kind == 1;
            float g = fire ? 0.6f : 0.85f, b = fire ? 0.35f : 0.6f;
            billboard(vc, camera, c.add(dir.scale(0.08 * s)), 0.5f * s, roll, glow, 1f, g, b, 0.35f * k, 0xF000F0);       // 주변 빛 번짐
            billboard(vc, camera, c.add(dir.scale(0.1 * s)), 0.22f * s, roll, front, 1f, 1f, 1f, k, 0xF000F0);          // 정면 섬광
            quad(vc, c, c.add(dir.scale(0.55 * s)), camera.getPosition(), 0.09f * s, 1f, g, b, 0.9f * k, side, 0f, 1f);  // 앞으로 뻗는 화염 혀
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
        }
    }

    private BulletFx() {}
}
