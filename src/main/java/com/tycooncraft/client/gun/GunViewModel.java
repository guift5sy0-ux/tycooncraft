package com.tycooncraft.client.gun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.client.GunAnim;
import com.tycooncraft.client.gun.GunCues.Plan;
import com.tycooncraft.client.gun.GunRig.Part;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;

/**
 * v0.32: 1인칭 총 동작 전체.
 * <ul>
 * <li>스프링 물리: 반동(뒤로·들림·좌우·기울기, 살짝 넘쳤다 돌아옴), 시선 돌릴 때 늦게 따라오는 흔들림, 걷기·달리기·착지·웅크리기, 숨쉬기</li>
 * <li>꺼내기: 아래에서 총구를 숙인 채 올라와 자리 잡음</li>
 * <li>움직이는 부품: 권총 슬라이드 후퇴(빈 탄창이면 뒤로 멈춤), 펌프, 볼트액션 노리쇠, 장전손잡이, AR 먼지덮개, RPG 탄두</li>
 * <li>재장전: 총을 기울이고 → 탄창이 빠져 떨어지고 → 왼손이 파우치에서 새 탄창을 꺼내 꽂고 → (비었으면) 슬라이드·장전손잡이·노리쇠</li>
 * <li>손은 늘 실제 부품 위치(탄창 바닥, 노리쇠 손잡이, 펌프)에 닿고, 팔은 어깨에서 그 손까지 뻗음</li>
 * <li>탄피: 배출구의 실제 위치에서, 슬라이드가 물러나는(펌프·노리쇠를 당기는) 순간 튀어나옴</li>
 * </ul>
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class GunViewModel {
    // ── 스프링 ──
    static final class Spring {
        float x, v;

        void step(float target, float w, float zeta, float dt) {
            float a = -w * w * (x - target) - 2f * zeta * w * v;
            v += a * dt;
            x += v * dt;
        }

        void reset() {
            x = v = 0;
        }
    }

    /** 총 한 자루(쌍권총은 좌우 따로)의 반동·동작 상태 */
    static final class Side {
        final Spring z = new Spring(), pitch = new Spring(), yaw = new Spring(), roll = new Spring(), x = new Spring();
        double shotT = -99, dustT = -99;
        final List<Double> ejects = new ArrayList<>();
        /** 지난 프레임: 모델 공간 → 시야 공간 */
        @Nullable Matrix4f modelView;
        double modelViewT = -99;

        void reset() {
            z.reset();
            pitch.reset();
            yaw.reset();
            roll.reset();
            x.reset();
            shotT = -99;
            dustT = -99;
            ejects.clear();
        }
    }

    static final Side R = new Side(), L = new Side();
    static final Spring swayX = new Spring(), swayY = new Spring(), swayRoll = new Spring(), swayPitch = new Spring(),
            moveX = new Spring(), moveZ = new Spring(), moveRoll = new Spring(), land = new Spring(), sprint = new Spring(), crouch = new Spring();
    static double now, lastNow = -1;
    static float lastYaw, lastPitch, lastVy;
    static boolean lastGround = true;
    static int lastSlot = -1;
    @Nullable static Item lastItem;
    static double equipT = -99;
    static boolean dualLeftNext;

    static final class Reload {
        GunKind kind;
        double start;
        float dur;
        boolean wasEmpty;
        int shells, slot;
        boolean released, seated, ejected;
    }

    @Nullable static Reload reload;

    /** 자동 테스트: 재장전 진행도 / 쏜 뒤 시각을 고정해 프레임 확인 */
    public static float DEBUG_RELOAD_U = -1f;
    public static float DEBUG_SHOT_AGO = -1f;
    public static boolean DEBUG_EMPTY;
    /** 자동 테스트: 추가 움직임 {tx,ty,tz,pitch,yaw,roll} */
    public static final float[] DEBUG_EXTRA = new float[6];
    public static int rendered, ejected;
    public static boolean DEBUG_LOG;

    // ── 서버·아이템 훅 ──

    /** 이 클라이언트에서 총이 한 발 나감 (GunItem.shoot 클라이언트 쪽) */
    public static void onLocalShot(ItemStack stack) {
        GunKind k = GunKind.of(stack);
        Minecraft mc = Minecraft.getInstance();
        if (k == null || mc.player == null) return;
        double t = GunCues.now();
        Side s = R;
        if (k == GunKind.DUAL) {
            s = dualLeftNext ? L : R;
            dualLeftNext = !dualLeftNext;
        }
        s.shotT = t;
        RandomJitter r = new RandomJitter(mc.level.random.nextFloat(), mc.level.random.nextFloat());
        float w = 1.6f;
        s.z.v += k.back * 15f * w;
        s.pitch.v += k.up * 19f * w;
        s.yaw.v += (r.a - 0.5f) * 2f * k.side * 16f * w;
        s.roll.v += (r.b - 0.5f) * 2f * k.roll * 16f * w;
        s.x.v += (r.a - 0.5f) * 0.02f * 16f;
        if (k == GunKind.RIFLE && s.dustT < 0) s.dustT = t;
        if (k.casing != null && k.ejectDelay() >= 0 && mc.options.getCameraType().isFirstPerson()) s.ejects.add(t + k.ejectDelay());
        GunCues.schedule(mc.player.getId(), t, GunCues.afterShot(k));
    }

    /** 재장전 시작 (클라이언트 예측) */
    public static void onLocalReload(ItemStack stack, int ticks, int shells, boolean wasEmpty) {
        Minecraft mc = Minecraft.getInstance();
        GunKind k = GunKind.of(stack);
        if (k == null || mc.player == null) return;
        if (reload != null && GunCues.now() - reload.start < reload.dur) return;
        Reload rl = new Reload();
        rl.kind = k;
        rl.start = GunCues.now();
        rl.dur = ticks / 20f;
        rl.wasEmpty = wasEmpty;
        rl.shells = shells;
        rl.slot = mc.player.getInventory().selected;
        reload = rl;
        GunCues.cancel(mc.player.getId());
        GunCues.schedule(mc.player.getId(), rl.start, GunCues.reload(k, rl.dur, wasEmpty, shells));
        GunAnim.startReload(mc.player.getId(), ticks);
    }

    /** 서버가 확정한 재장전 (블랙마켓 특성으로 시간이 다를 수 있음) */
    public static void syncReload(ItemStack stack, int ticks, int shells, boolean wasEmpty) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Reload rl = reload;
        if (rl == null || GunCues.now() - rl.start > rl.dur) {
            onLocalReload(stack, ticks, shells, wasEmpty);
            return;
        }
        float dur = ticks / 20f;
        if (Math.abs(dur - rl.dur) < 0.01f && rl.shells == shells && rl.wasEmpty == wasEmpty) return;
        double el = GunCues.now() - rl.start;
        rl.dur = dur;
        rl.shells = shells;
        rl.wasEmpty = wasEmpty;
        GunCues.cancel(mc.player.getId());
        List<GunCues.Cue> rest = new ArrayList<>();
        for (GunCues.Cue c : GunCues.reload(rl.kind, dur, wasEmpty, shells)) if (c.at() > el) rest.add(c);
        GunCues.schedule(mc.player.getId(), rl.start, rest);
        GunAnim.startReload(mc.player.getId(), ticks);
    }

    private record RandomJitter(float a, float b) {}

    // ── 매 프레임 상태 갱신 ──

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) {
            lastNow = -1;
            return;
        }
        now = GunCues.now();
        float dt = lastNow < 0 ? 0 : (float) Mth.clamp(now - lastNow, 0, 0.1);
        lastNow = now;
        ItemStack stack = p.getMainHandItem();
        GunKind k = GunKind.of(stack);
        int slot = p.getInventory().selected;
        if (slot != lastSlot || stack.getItem() != lastItem) {
            if (k != null) {
                equipT = now;
                R.reset();
                L.reset();
                GunCues.play(p.getId(), ModSounds.GUN_EQUIP, k == GunKind.RT20 || k == GunKind.RPG ? 0.55f : 0.35f, k.pistolGrip() ? 1.15f : 1f);
            }
            if (reload != null) {
                GunCues.cancel(p.getId());
                GunAnim.stopReload(p.getId());
            }
            reload = null;
            lastSlot = slot;
            lastItem = stack.getItem();
        }
        if (reload != null && now - reload.start > reload.dur + 0.05) reload = null;
        // 화면에 안 그려진 동안 남은 탄피 (스코프·3인칭): 카메라 기준 대략 위치에서
        for (Side s : new Side[]{R, L}) {
            for (Iterator<Double> it = s.ejects.iterator(); it.hasNext(); ) {
                double at = it.next();
                if (at + 0.06 < now) {
                    it.remove();
                    if (k != null && k.casing != null) spawnCasing(k, s == L, null);
                }
            }
        }
        if (dt <= 0 || k == null) {
            lastYaw = p.getViewYRot(1f);
            lastPitch = p.getViewXRot(1f);
            return;
        }
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
        float yaw = p.getViewYRot(pt), pitch = p.getViewXRot(pt);
        float yawVel = Mth.wrapDegrees(yaw - lastYaw) / dt, pitchVel = (pitch - lastPitch) / dt;
        lastYaw = yaw;
        lastPitch = pitch;
        float heavy = k.heavy;
        // 움직임 (몸 기준)
        Vec3 vel = p.getDeltaMovement();
        float yr = yaw * Mth.DEG_TO_RAD;
        float fwd = (float) (-Mth.sin(yr) * vel.x + Mth.cos(yr) * vel.z), right = (float) (-Mth.cos(yr) * vel.x - Mth.sin(yr) * vel.z);
        boolean ground = p.onGround();
        if (ground && !lastGround) land.v -= Mth.clamp(-lastVy * 20f, 0f, 1.6f) * 0.55f;
        if (!ground && lastGround && vel.y > 0.1) land.v += 0.35f;
        lastGround = ground;
        lastVy = (float) vel.y;
        boolean reloading = reload != null;
        boolean sprinting = p.isSprinting() && !reloading && now - Math.max(R.shotT, L.shotT) > 0.35 && !p.isUsingItem();
        int n = Math.max(1, (int) Math.ceil(dt / 0.006f));
        float h = dt / n;
        float sw = 13f / (1f + heavy * 0.7f);
        for (int i = 0; i < n; i++) {
            for (Side s : new Side[]{R, L}) {
                float wk = 21f / (1f + heavy * 0.35f);
                s.z.step(0, wk, 0.42f, h);
                s.pitch.step(0, wk * 0.85f, 0.4f, h);
                s.yaw.step(0, wk * 0.8f, 0.45f, h);
                s.roll.step(0, wk * 0.75f, 0.42f, h);
                s.x.step(0, wk, 0.5f, h);
            }
            swayX.step(Mth.clamp(-yawVel * 0.00011f, -0.045f, 0.045f), sw, 0.62f, h);
            swayRoll.step(Mth.clamp(yawVel * 0.0065f, -6f, 6f), sw, 0.6f, h);
            swayY.step(Mth.clamp(pitchVel * 0.00009f, -0.035f, 0.035f), sw, 0.62f, h);
            swayPitch.step(Mth.clamp(-pitchVel * 0.0045f, -3.5f, 3.5f), sw, 0.6f, h);
            moveX.step(Mth.clamp(-right * 0.22f, -0.035f, 0.035f), 9f, 0.75f, h);
            moveRoll.step(Mth.clamp(-right * 26f, -4.5f, 4.5f), 9f, 0.75f, h);
            moveZ.step(Mth.clamp(fwd * 0.1f, -0.02f, 0.03f), 8f, 0.8f, h);
            land.step(0, 11f, 0.42f, h);
            sprint.step(sprinting ? 1f : 0f, 8.5f, 0.82f, h);
            crouch.step(p.isCrouching() ? 1f : 0f, 10f, 0.9f, h);
        }
    }

    // ── 그리기 ──

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHand(RenderHandEvent event) {
        if (event.isCanceled()) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.getMainArm() != HumanoidArm.RIGHT) return;
        ItemStack stack = p.getMainHandItem();
        GunKind k = GunKind.of(stack);
        if (k == null) return;
        if (event.getHand() == InteractionHand.OFF_HAND) {
            if (k == GunKind.DUAL && p.getOffhandItem().isEmpty()) event.setCanceled(true);   // 왼손 권총도 여기서 같이 그림
            return;
        }
        event.setCanceled(true);
        rendered++;
        PoseStack ps = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        int light = event.getPackedLight();
        if (k == GunKind.DUAL) {
            ItemStack left = new ItemStack(ModItems.PISTOL.get());
            Vector3f rh = drawGun(ps, buf, light, p, stack, k, R, false);
            Vector3f lh = drawGun(ps, buf, light, p, left, k, L, true);
            drawArm(ps, buf, light, p, true, rh, shoulder(true, false));
            drawArm(ps, buf, light, p, false, lh, shoulder(false, false));
        } else {
            drawGun(ps, buf, light, p, stack, k, R, false);
        }
    }

    private static Vector3f shoulder(boolean right, boolean longGun) {
        return right ? new Vector3f(0.5f, -0.95f, 0.2f) : longGun ? new Vector3f(-0.15f, -1.15f, 0.05f) : new Vector3f(-0.5f, -0.95f, 0.2f);
    }

    /** 모델 공간 → 손 기준 시야 공간 (총을 움직이기 전) */
    private static Matrix4f modelToLocal(BakedModel base, boolean left) {
        PoseStack q = new PoseStack();
        q.translate(left ? -0.56f : 0.56f, -0.52f, -0.72f);
        ClientHooks.handleCameraTransforms(q, base, left ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, left);
        q.translate(-0.5f, -0.5f, -0.5f);
        return new Matrix4f(q.last().pose());
    }

    /** 부품 자세 (모델 공간) */
    static final class PartPose {
        final Vector3f off = new Vector3f();
        float angle;
        final Vector3f axis = new Vector3f(0, 0, 1), pivot = new Vector3f();
        boolean hidden;

        Vector3f apply(Vector3f p) {
            Vector3f r = new Vector3f(p);
            if (angle != 0) r.sub(pivot).rotate(new Quaternionf().rotateAxis(angle * Mth.DEG_TO_RAD, axis)).add(pivot);
            return r.add(off);
        }
    }

    /** 한 프레임의 동작 결과 */
    static final class Frame {
        float tx, ty, tz, pitch, yaw, roll;
        final EnumMap<Part, PartPose> parts = new EnumMap<>(Part.class);
        Vector3f rightHand, leftHand;
        @Nullable ItemStack held;
        @Nullable Vector3f heldAt;
        boolean leftArmFree = true;

        PartPose part(Part p) {
            return parts.computeIfAbsent(p, x -> new PartPose());
        }
    }

    static float seg(float u, float a, float b) {
        return b <= a ? (u >= b ? 1 : 0) : Mth.clamp((u - a) / (b - a), 0f, 1f);
    }

    static float smooth(float f) {
        return f * f * (3 - 2 * f);
    }

    static float easeOut(float f) {
        float g = 1 - f;
        return 1 - g * g * g;
    }

    static float easeIn(float f) {
        return f * f * f;
    }

    static float easeOutBack(float f) {
        float c1 = 1.4f, c3 = c1 + 1;
        float g = f - 1;
        return 1 + c3 * g * g * g + c1 * g * g;
    }

    static Vector3f lerp(Vector3f a, Vector3f b, float f) {
        return new Vector3f(a).lerp(b, f);
    }

    /** a → b 를 바깥으로 휘는 곡선으로 */
    static Vector3f arc(Vector3f a, Vector3f b, float f, Vector3f bulge) {
        float s = 4 * f * (1 - f);
        return lerp(a, b, f).add(new Vector3f(bulge).mul(s));
    }

    /** 총 한 자루를 그리고 오른손(쌍권총 왼쪽 총이면 왼손) 위치를 돌려줌 (팔은 단일 총이면 여기서 그림) */
    private static Vector3f drawGun(PoseStack ps, MultiBufferSource buf, int light, LocalPlayer p, ItemStack stack, GunKind k, Side s, boolean leftGun) {
        Minecraft mc = Minecraft.getInstance();
        ItemRenderer ir = mc.getItemRenderer();
        BakedModel base = ir.getModel(stack, mc.level, p, p.getId());
        Matrix4f M = modelToLocal(base, leftGun);
        GunRig.Def def = GunRig.def(k.rig);
        float sg = leftGun ? -1f : 1f;
        float[] hd = k.hands;
        Vector3f grip = new Vector3f(hd[0] * sg, hd[1], hd[2]);
        Vector3f support = k == GunKind.DUAL ? grip : new Vector3f(hd[3], hd[4], hd[5]);
        Frame f = new Frame();
        f.rightHand = grip;
        f.leftHand = support;
        int ammo = GunItem.ammo(stack);
        boolean empty = ammo <= 0 || DEBUG_EMPTY;
        float aim = GunAnim.aim(p);
        if (reload != null || DEBUG_RELOAD_U >= 0 || DEBUG_SHOT_AGO >= 0 && DEBUG_SHOT_AGO < 2) aim = 1f;
        float relax = 1f - aim;
        float pt = mc.getTimer().getGameTimeDeltaPartialTick(false);
        float sp = sprint.x;
        Reload rl = reload;
        float u = -1f;
        if (DEBUG_RELOAD_U >= 0) u = DEBUG_RELOAD_U;
        else if (rl != null) u = (float) Mth.clamp((now - rl.start) / rl.dur, 0, 1);
        boolean rlEmpty = rl != null ? rl.wasEmpty : DEBUG_EMPTY;
        int rlShells = rl != null ? rl.shells : 4;
        float rlDur = rl != null ? rl.dur : (k == GunKind.RT20 ? 14f : 2.5f);
        float ts = DEBUG_SHOT_AGO >= 0 ? DEBUG_SHOT_AGO : (float) (now - s.shotT);

        // ── 1. 총 전체 움직임 ──
        float tx = 0, ty = 0, tz = 0, pitch = 0, yaw = 0, roll = 0;
        if (k == GunKind.PISTOL || k == GunKind.JUDGE) {
            tx -= 0.08f;
            ty += 0.075f;
            tz += 0.02f;
        }
        float e = Mth.clamp((float) (now - equipT) / k.equip, 0f, 1f), eo = 1f - easeOutBack(e);
        ty -= 0.42f * eo;
        tz += 0.08f * eo;
        pitch -= 55f * eo;
        roll += 18f * eo * sg;
        yaw += 8f * eo * sg;
        // 들고 다니는 자세 (로우 레디)
        tx -= 0.03f * relax * sg;
        ty -= 0.07f * relax;
        tz += 0.06f * relax;
        pitch -= 14f * relax;
        yaw += 9f * relax * sg;
        roll -= 6f * relax * sg;
        // 달리기
        float spr = sp * (u >= 0 ? 0 : 1);
        if (k.pistolGrip()) {
            ty -= 0.12f * spr;
            tz += 0.03f * spr;
            pitch -= 38f * spr;
            yaw += 6f * spr * sg;
            roll += 6f * spr * sg;
        } else {
            tx += 0.04f * spr;
            ty -= 0.075f * spr;
            tz += 0.05f * spr;
            pitch -= 20f * spr;
            yaw += 36f * spr;
            roll += 16f * spr;
        }
        // 걷기
        float bob = Mth.lerp(pt, p.oBob, p.bob);
        float phase = -(p.walkDist + (p.walkDist - p.walkDistO) * pt) * Mth.PI;
        float amp = (1f - 0.55f * aim) * (1f + 0.8f * sp) / (1f + k.heavy * 0.3f);
        tx += Mth.sin(phase) * bob * 0.11f * amp;
        ty -= Math.abs(Mth.cos(phase)) * bob * 0.085f * amp;
        roll += Mth.sin(phase) * bob * 9f * amp;
        pitch += (Math.abs(Mth.cos(phase)) - 0.5f) * bob * 4f * amp;
        yaw += Mth.sin(phase) * bob * 3f * amp;
        // 시선 흔들림·움직임·착지·웅크리기
        tx += swayX.x + moveX.x;
        ty += swayY.x + land.x * 0.09f;
        tz += moveZ.x;
        roll += swayRoll.x + moveRoll.x;
        pitch += swayPitch.x + land.x * 3f;
        float cr = crouch.x;
        ty -= 0.02f * cr;
        tz += 0.025f * cr;
        roll += 2f * cr * sg;
        // 숨쉬기
        float br = (1f - 0.5f * aim) * (1f - sp);
        float tt = (float) now;
        ty += Mth.sin(tt * 1.6f) * 0.0045f * br;
        pitch += Mth.sin(tt * 1.6f + 0.6f) * 0.45f * br;
        tx += Mth.sin(tt * 0.8f) * 0.002f * br;
        roll += Mth.sin(tt * 0.8f + 1f) * 0.35f * br;
        // 반동
        tz += s.z.x;
        tx += s.x.x;
        pitch += s.pitch.x;
        yaw += s.yaw.x * sg;
        roll += s.roll.x;

        // 재장전·노리쇠 동작이 총에 주는 움직임
        float[] mo = new float[6];
        actionMotion(k, s, u, ts, rlDur, rlEmpty, rlShells, leftGun, mo);
        tx += mo[0] * sg;
        ty += mo[1];
        tz += mo[2];
        pitch += mo[3];
        yaw += mo[4] * sg;
        roll += mo[5] * sg;

        tx += DEBUG_EXTRA[0];
        ty += DEBUG_EXTRA[1];
        tz += DEBUG_EXTRA[2];
        pitch += DEBUG_EXTRA[3];
        yaw += DEBUG_EXTRA[4];
        roll += DEBUG_EXTRA[5];
        PoseStack g = new PoseStack();
        g.translate(tx, ty, tz);
        g.translate(grip.x, grip.y, grip.z);
        g.mulPose(Axis.YP.rotationDegrees(yaw));
        g.mulPose(Axis.XP.rotationDegrees(pitch));
        g.mulPose(Axis.ZP.rotationDegrees(roll));
        g.translate(-grip.x, -grip.y, -grip.z);
        Matrix4f G = new Matrix4f(g.last().pose());
        Matrix4f Gi = new Matrix4f(G).invert();

        // ── 2. 부품·손 ──
        parts(k, def, s, f, M, Gi, u, ts, rlDur, rlEmpty, rlShells, empty, leftGun);

        // ── 3. 그리기 ──
        ps.pushPose();
        ps.mulPose(G);
        ps.pushPose();
        ps.translate(leftGun ? -0.56f : 0.56f, -0.52f, -0.72f);
        BakedModel m = ClientHooks.handleCameraTransforms(ps, base, leftGun ? ItemDisplayContext.FIRST_PERSON_LEFT_HAND : ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, leftGun);
        ps.translate(-0.5f, -0.5f, -0.5f);
        Matrix4f modelView = new Matrix4f(ps.last().pose());
        for (BakedModel pass : m.getRenderPasses(stack, true)) {
            GunRig.Split split = GunRig.split(k.rig, pass);
            for (RenderType rt : pass.getRenderTypes(stack, true)) {
                VertexConsumer vc = ItemRenderer.getFoilBufferDirect(buf, rt, true, stack.hasFoil());
                ir.renderModelLists(split.body, stack, light, OverlayTexture.NO_OVERLAY, ps, vc);
                for (var en : split.parts.entrySet()) {
                    Part part = en.getKey();
                    PartPose pp = f.parts.get(part);
                    PartPose parent = part.parent != null ? f.parts.get(part.parent) : null;
                    if ((pp != null && pp.hidden) || (parent != null && parent.hidden)) continue;
                    ps.pushPose();
                    if (parent != null) applyPose(ps, parent);
                    if (pp != null) applyPose(ps, pp);
                    ir.renderModelLists(en.getValue(), stack, light, OverlayTexture.NO_OVERLAY, ps, vc);
                    ps.popPose();
                }
            }
        }
        ps.popPose();
        // 손에 든 탄 (샷건 탄·대물탄)
        if (f.held != null && f.heldAt != null) {
            ps.pushPose();
            ps.translate(f.heldAt.x, f.heldAt.y, f.heldAt.z);
            ps.mulPose(Axis.YP.rotationDegrees(90f));
            ps.mulPose(Axis.ZP.rotationDegrees(-20f));
            float sc = f.held.is(ModItems.HISPANO_AMMO.get()) ? 0.32f : 0.26f;
            ps.scale(sc, sc, sc);
            ir.renderStatic(f.held, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
            ps.popPose();
        }
        ps.popPose();

        // 배출구 위치 기록 + 탄피
        s.modelView = new Matrix4f(ps.last().pose()).mul(G).mul(M);
        s.modelViewT = now;
        GunRig.Box portBox = def.elements.get(k.port);
        for (Iterator<Double> it = s.ejects.iterator(); it.hasNext(); ) {
            double at = it.next();
            if (at > now) continue;
            it.remove();
            if (portBox != null && k.casing != null) {
                Vector3f pm = portBox.at(k.portX, k.portY, k.portZ);
                PartPose sl = f.parts.get(Part.SLIDE);
                if (sl != null) pm.add(sl.off);
                Vector3f vp = s.modelView.transformPosition(pm);
                if (DEBUG_LOG) System.out.println("[AUTOTEST] v320 eject port=" + vp);
                spawnCasing(k, leftGun, vp);
            }
        }

        // 팔: 손 위치는 총을 따라가고, 어깨는 몸에 붙어 있음 (조금만 따라감)
        Vector3f rh = G.transformPosition(new Vector3f(f.rightHand));
        if (k == GunKind.DUAL) return rh;
        Vector3f lh = G.transformPosition(new Vector3f(f.leftHand));
        Vector3f follow = G.transformPosition(new Vector3f(grip)).sub(grip).mul(0.9f);
        drawArm(ps, buf, light, p, true, rh, shoulder(true, false).add(follow));
        drawArm(ps, buf, light, p, false, lh, shoulder(false, !k.pistolGrip()).add(follow));
        return rh;
    }

    private static void applyPose(PoseStack ps, PartPose pp) {
        ps.translate(pp.off.x, pp.off.y, pp.off.z);
        if (pp.angle != 0) ps.rotateAround(new Quaternionf().rotateAxis(pp.angle * Mth.DEG_TO_RAD, pp.axis), pp.pivot.x, pp.pivot.y, pp.pivot.z);
    }

    // ── 동작: 총 전체 (재장전 기울이기·노리쇠 조작 시 자세) ──
    private static void actionMotion(GunKind k, Side s, float u, float ts, float dur, boolean wasEmpty, int shells, boolean leftGun, float[] o) {
        if (u >= 0) {
            switch (k) {
                case PISTOL, JUDGE, SMG, RIFLE, VSS, SNIPER -> {
                    // 총을 화면 가운데 위로 끌어올려 탄창이 보이게 기울임
                    float c = smooth(seg(u, 0, Plan.CANT_IN)) * (1 - smooth(seg(u, Plan.CANT_OUT, 1)));
                    boolean pg = k.pistolGrip();
                    o[0] -= (pg ? 0.12f : 0.08f) * c;
                    o[1] += (pg ? 0.16f : 0.09f) * c;
                    o[2] -= (pg ? 0.05f : 0.0f) * c;
                    o[3] += 12f * c;
                    o[4] += (pg ? 12f : 18f) * c;
                    o[5] -= (pg ? 24f : 32f) * c;
                    // 새 탄창 꽂는 순간 총이 위로 툭
                    float seat = seg(u, Plan.SEAT - 0.02f, Plan.SEAT) * (1 - smooth(seg(u, Plan.SEAT, Plan.SEAT + 0.08f)));
                    o[1] += 0.014f * seat;
                    o[3] += 3f * seat;
                    // 탄창을 향해 총을 조금 돌려 줌
                    float meet = smooth(seg(u, Plan.NEW_MAG, Plan.ENTRY)) * (1 - smooth(seg(u, Plan.SEAT, Plan.SEAT + 0.1f)));
                    o[5] -= 6f * meet;
                    o[3] += 3f * meet;
                    // 탄창 놓을 때 살짝 흔들
                    float rel = seg(u, Plan.MAG_RELEASE, Plan.MAG_RELEASE + 0.02f) * (1 - smooth(seg(u, Plan.MAG_RELEASE + 0.02f, Plan.MAG_RELEASE + 0.08f)));
                    o[1] += 0.008f * rel;
                    if (wasEmpty) {
                        // 슬라이드 멈치·노리쇠 멈치를 누르거나 장전손잡이를 당길 때 총이 툭
                        float snap = seg(u, Plan.CH_SNAP - 0.01f, Plan.CH_SNAP) * (1 - smooth(seg(u, Plan.CH_SNAP, Plan.CH_SNAP + 0.06f)));
                        o[3] += 2.5f * snap;
                        o[2] += 0.01f * snap;
                        if (k == GunKind.SNIPER) {
                            float pull = smooth(seg(u, Plan.SEAT, Plan.CH_GRAB)) * (1 - smooth(seg(u, Plan.CH_DONE, Plan.HAND_BACK)));
                            o[5] += 30f * pull;
                            o[1] -= 0.04f * pull;
                        }
                        if (k == GunKind.VSS || k == GunKind.SMG) {
                            float pull = smooth(seg(u, Plan.SEAT + 0.02f, Plan.CH_GRAB)) * (1 - smooth(seg(u, Plan.CH_SNAP, Plan.HAND_BACK)));
                            o[5] -= 8f * pull;
                        }
                    }
                }
                case DUAL -> {
                    float c = smooth(seg(u, 0, 0.08f)) * (1 - smooth(seg(u, 0.82f, 1f)));
                    float dip = smooth(seg(u, 0.22f, 0.38f)) * (1 - smooth(seg(u, 0.5f, 0.72f)));
                    o[0] -= 0.06f * c;
                    o[1] += 0.1f * c - 0.75f * dip;
                    o[2] -= 0.03f * c - 0.06f * dip;
                    o[5] -= 26f * c;
                    o[3] += 18f * c - 62f * dip;
                    o[4] += 10f * c;
                    float seat = seg(u, 0.72f, 0.74f) * (1 - smooth(seg(u, 0.74f, 0.8f)));
                    o[3] += 3f * seat;
                }
                case SHOTGUN -> {
                    float end = wasEmpty ? Plan.SG_END_EMPTY : Plan.SG_END;
                    float c = smooth(seg(u, 0, Plan.SG_START)) * (1 - smooth(seg(u, end + 0.02f, Math.min(1f, end + 0.1f))));
                    o[0] -= 0.08f * c;
                    o[1] += 0.1f * c;
                    o[3] += 12f * c;
                    o[4] += 16f * c;
                    o[5] -= 42f * c;
                    int n = Math.max(1, shells);
                    for (int i = 0; i < n; i++) {
                        float a = Plan.SG_START + (end - Plan.SG_START) * i / n, b = Plan.SG_START + (end - Plan.SG_START) * (i + 1) / n;
                        float push = seg(u, a + (b - a) * 0.72f, a + (b - a) * Plan.SG_PUSH) * (1 - smooth(seg(u, a + (b - a) * Plan.SG_PUSH, a + (b - a) * 0.95f)));
                        o[3] += 2f * push;
                        o[2] -= 0.01f * push;
                    }
                    if (wasEmpty) {
                        float pump = smooth(seg(u, 0.86f, 0.885f)) * (1 - smooth(seg(u, 0.92f, 0.95f)));
                        o[2] += 0.015f * pump;
                    }
                }
                case RT20 -> {
                    float ta = u * dur / GunCues.Plan.rt20Scale(dur);
                    float c = smooth(seg(ta, 0, 0.6f)) * (1 - smooth(seg(ta, 4.4f, 5.0f)));
                    o[0] -= 0.1f * c;
                    o[1] += 0.12f * c;
                    o[3] += 6f * c;
                    o[4] += 16f * c;
                    o[5] += 18f * c;
                    float push = seg(ta, 3.0f, 3.4f) * (1 - smooth(seg(ta, 3.4f, 3.7f)));
                    o[3] += 2f * push;
                }
                case RPG -> {
                    float c = smooth(seg(u, 0, 0.15f)) * (1 - smooth(seg(u, 0.85f, 1f)));
                    o[0] -= 0.06f * c;
                    o[1] += 0.06f * c;
                    o[3] += 28f * c;
                    o[4] += 10f * c;
                    o[5] -= 8f * c;
                    float push = seg(u, 0.6f, 0.68f) * (1 - smooth(seg(u, 0.68f, 0.75f)));
                    o[2] += 0.02f * push;
                }
            }
            return;
        }
        // 쏜 뒤 손 동작
        if (k == GunKind.SNIPER) {
            float w = smooth(seg(ts, 0.16f, 0.3f)) * (1 - smooth(seg(ts, 0.92f, 1.1f)));
            o[0] -= 0.05f * w;
            o[1] += 0.04f * w;
            o[3] -= 2f * w;
            o[5] += 22f * w;
        } else if (k == GunKind.SHOTGUN) {
            float pump = smooth(seg(ts, 0.15f, 0.27f)) * (1 - smooth(seg(ts, 0.31f, 0.42f)));
            o[2] += 0.012f * pump;
            o[3] -= 1.5f * pump;
        }
    }

    // ── 동작: 부품과 손 ──
    private static void parts(GunKind k, GunRig.Def def, Side s, Frame f, Matrix4f M, Matrix4f Gi, float u, float ts, float dur,
                              boolean wasEmpty, int shells, boolean empty, boolean leftGun) {
        Vector3f support = f.leftHand, grip = f.rightHand;
        // 몸에 붙은 점(파우치)을 총 기준 좌표로
        java.util.function.Function<Vector3f, Vector3f> body = v -> Gi.transformPosition(new Vector3f(v));
        java.util.function.Function<Vector3f, Vector3f> L = v -> M.transformPosition(new Vector3f(v));
        Matrix4f Mi = new Matrix4f(M).invert();
        java.util.function.Function<Vector3f, Vector3f> toModel = v -> Mi.transformPosition(new Vector3f(v));
        GunRig.Box mag = def.bounds.get(Part.MAG);
        boolean rel = u >= 0;

        // 슬라이드 (권총류)
        if (def.bounds.containsKey(Part.SLIDE)) {
            GunRig.Box sb = def.bounds.get(Part.SLIDE);
            float travel = (sb.max().z - sb.min().z) * 0.24f;
            float b = ts < 0.022f ? easeOut(ts / 0.022f) : ts < 0.09f ? 1 - easeIn((ts - 0.022f) / 0.068f) : 0f;
            float lock = 0f;
            if (rel) {
                boolean snapT = k == GunKind.DUAL ? true : false;
                float snapAt = k == GunKind.DUAL ? (leftGun ? 0.81f : 0.78f) : Plan.CH_SNAP;
                if (wasEmpty) {
                    lock = u < snapAt ? 0.9f : 1f - easeIn(seg(u, snapAt, snapAt + 0.025f));
                }
            } else if (empty && ts > 0.022f) lock = 0.9f;
            f.part(Part.SLIDE).off.z = travel * Math.max(b, lock);
            // (v0.32: 슬라이드 멈치를 엄지로 눌러 놓음 - 왼손은 그대로)
            if (false) {
                Vector3f rear = sb.at(0.5f, 1.05f, 0.88f);
                rear.z += f.part(Part.SLIDE).off.z;
                Vector3f c = L.apply(rear);
                Vector3f seatPt = mag != null ? L.apply(new Vector3f((mag.min().x + mag.max().x) / 2, mag.min().y, (mag.min().z + mag.max().z) / 2)) : support;
                if (u >= Plan.SEAT + 0.01f && u < Plan.CH_GRAB) f.leftHand = arc(seatPt, c, smooth(seg(u, Plan.SEAT + 0.01f, Plan.CH_GRAB)), new Vector3f(-0.06f, 0.05f, 0.04f));
                else if (u >= Plan.CH_GRAB && u < Plan.CH_SNAP) f.leftHand = c;
                else if (u >= Plan.CH_SNAP && u < Plan.HAND_BACK) {
                    Vector3f flick = new Vector3f(c).add(-0.05f, 0.06f, 0.1f);
                    float q = seg(u, Plan.CH_SNAP, Plan.HAND_BACK);
                    f.leftHand = q < 0.3f ? lerp(c, flick, smooth(q / 0.3f)) : arc(flick, support, smooth((q - 0.3f) / 0.7f), new Vector3f(-0.04f, -0.04f, 0));
                }
            }
        }
        // 기관단총 장전손잡이 (빈 탄창이면 뒤로 걸려 있음 → HK 슬랩)
        if (k == GunKind.SMG) {
            float lock = 0f;
            if (rel) {
                if (wasEmpty) lock = u < Plan.CH_SNAP ? 1f : 1f - easeIn(seg(u, Plan.CH_SNAP, Plan.CH_SNAP + 0.02f));
            } else if (empty) lock = 1f;
            f.part(Part.CHARGE).off.z = 0.11f * lock;
        }
        // AR 먼지덮개: 처음 쏘면 열림
        if (k == GunKind.RIFLE && s.dustT > 0 && def.bounds.containsKey(Part.DUSTCOVER)) {
            GunRig.Box db = def.bounds.get(Part.DUSTCOVER);
            PartPose pp = f.part(Part.DUSTCOVER);
            float o = smooth(Mth.clamp((float) (now - s.dustT) / 0.05f, 0, 1));
            pp.pivot.set((db.min().x + db.max().x) / 2, db.min().y, 0);
            pp.axis.set(0, 0, 1);
            pp.angle = -100f * o;
        }
        // 샷건 펌프 (쏜 뒤 / 재장전 끝)
        if (k == GunKind.SHOTGUN) {
            float pump = 0f;
            if (rel) {
                if (wasEmpty) pump = smooth(seg(u, 0.875f, 0.9f)) * (1 - smooth(seg(u, 0.925f, 0.95f)));
            } else pump = smooth(seg(ts, 0.15f, 0.27f)) * (1 - smooth(seg(ts, 0.31f, 0.42f)));
            float travel = 0.17f * pump;
            f.part(Part.PUMP).off.z = travel;
            Vector3f d = L.apply(new Vector3f(0, 0, travel)).sub(L.apply(new Vector3f()));
            f.leftHand = new Vector3f(support).add(d);
        }
        // 볼트액션 (저격총: 쏜 뒤 / RT-20: 재장전)
        if (def.bounds.containsKey(Part.BOLT)) {
            GunRig.Box bb = def.bounds.get(Part.BOLT);
            float up = 0, back = 0, reach = 0;
            if (k == GunKind.SNIPER) {
                if (rel) {
                    if (wasEmpty) {
                        up = smooth(seg(u, Plan.CH_GRAB, Plan.CH_GRAB + 0.025f)) * (1 - smooth(seg(u, Plan.CH_SNAP, Plan.CH_DONE)));
                        back = smooth(seg(u, Plan.CH_GRAB + 0.025f, Plan.CH_PULL)) * (1 - smooth(seg(u, Plan.CH_PULL + 0.01f, Plan.CH_SNAP)));
                        reach = smooth(seg(u, Plan.SEAT, Plan.CH_GRAB)) * (1 - smooth(seg(u, Plan.CH_DONE, Plan.HAND_BACK)));
                    }
                } else {
                    up = smooth(seg(ts, 0.30f, 0.42f)) * (1 - smooth(seg(ts, 0.80f, 0.90f)));
                    back = smooth(seg(ts, 0.42f, 0.58f)) * (1 - smooth(seg(ts, 0.66f, 0.80f)));
                    reach = smooth(seg(ts, 0.16f, 0.30f)) * (1 - smooth(seg(ts, 0.92f, 1.1f)));
                }
            } else if (k == GunKind.RT20 && rel) {
                float ta = u * dur / Plan.rt20Scale(dur);
                up = smooth(seg(ta, 0.8f, 1.05f)) * (1 - smooth(seg(ta, 4.0f, 4.2f)));
                back = smooth(seg(ta, 1.1f, 1.45f)) * (1 - smooth(seg(ta, 3.75f, 4.0f)));
                reach = smooth(seg(ta, 0.4f, 0.8f)) * (1 - smooth(seg(ta, 4.2f, 4.6f)));
            }
            float travel = (k == GunKind.RT20 ? 0.32f : 0.2f) * back;
            PartPose bolt = f.part(Part.BOLT);
            bolt.off.z = travel;
            PartPose hp = f.part(Part.HANDLE);
            hp.pivot.set((bb.min().x + bb.max().x) / 2, (bb.min().y + bb.max().y) / 2, 0);
            hp.axis.set(0, 0, 1);
            hp.angle = (k == GunKind.RT20 ? 70f : 78f) * up;
            String knobName = k == GunKind.RT20 ? "bolt_knob" : "bolt_knob";
            GunRig.Box kb = def.elements.get(knobName);
            if (kb != null && reach > 0) {
                Vector3f knob = hp.apply(kb.at(0.5f, 0.5f, 0.5f));
                knob.add(bolt.off);
                f.rightHand = lerp(grip, L.apply(knob).add(0.01f, -0.015f, 0.03f), reach);
            }
        }
        // RPG 탄두
        if (k == GunKind.RPG) {
            if (!rel && empty) f.part(Part.ROCKET).hidden = true;
        }

        if (!rel) return;

        // ── 재장전 ──
        switch (k) {
            case PISTOL, JUDGE, SMG, RIFLE, VSS, SNIPER -> {
                if (mag == null) break;
                Vector3f bottom = new Vector3f((mag.min().x + mag.max().x) / 2, mag.min().y, (mag.min().z + mag.max().z) / 2);
                Vector3f top = new Vector3f(bottom.x, mag.max().y, bottom.z);
                PartPose mp = f.part(Part.MAG);
                Vector3f pouch = body.apply(new Vector3f(0.02f, -1.0f, 0.12f));
                Vector3f entry = L.apply(new Vector3f(bottom).add(0, -0.34f, 0.03f));
                Vector3f seat = L.apply(bottom).add(0, -0.025f, 0);
                if (u >= Plan.MAG_RELEASE && u < Plan.NEW_MAG) {
                    float q = seg(u, Plan.MAG_RELEASE, Plan.DROP_END);
                    mp.off.set(0, -q * q * 1.1f, 0.06f * q);
                    mp.pivot.set(top);
                    mp.axis.set(1, 0, 0);
                    mp.angle = -14f * q;
                    if (q > 0.97f || u >= Plan.DROP_END) mp.hidden = true;
                } else if (u >= Plan.NEW_MAG && u < Plan.SEAT) {
                    Vector3f hand = u < Plan.ENTRY ? arc(pouch, entry, smooth(seg(u, Plan.NEW_MAG, Plan.ENTRY)), new Vector3f(-0.08f, 0.02f, 0.02f))
                            : lerp(entry, seat, smooth(seg(u, Plan.ENTRY, Plan.SEAT)));
                    f.leftHand = hand;
                    Vector3f hm = toModel.apply(new Vector3f(hand).add(0, 0.025f, 0));
                    mp.off.set(hm.sub(bottom));
                    mp.pivot.set(top);
                    mp.axis.set(1, 0, 0);
                    mp.angle = 16f * (1 - smooth(seg(u, Plan.ENTRY - 0.06f, Plan.SEAT - 0.02f)));
                }
                // 손 경로
                if (u >= Plan.HAND_OUT && u < Plan.NEW_MAG) {
                    float q = smooth(seg(u, Plan.HAND_OUT, Plan.POUCH));
                    f.leftHand = arc(support, pouch, q, new Vector3f(-0.12f, 0.02f, 0.05f));
                } else if (u >= Plan.SEAT) {
                    boolean charge = wasEmpty && (k == GunKind.SMG || k == GunKind.VSS);
                    if (wasEmpty && k == GunKind.RIFLE) {
                        // 노리쇠 멈치: 왼손 손바닥으로 탄창 위 왼쪽을 툭
                        Vector3f catchPt = L.apply(new Vector3f(mag.min().x, mag.max().y + 0.03f, (mag.min().z + mag.max().z) / 2 + 0.05f)).add(-0.03f, 0, 0);
                        float hit = seg(u, Plan.CH_SNAP - 0.015f, Plan.CH_SNAP);
                        Vector3f hp2 = new Vector3f(catchPt).add(-0.04f * (1 - hit), 0.01f, 0);
                        if (u < Plan.CH_SNAP) f.leftHand = arc(seat, hp2, smooth(seg(u, Plan.SEAT + 0.02f, Plan.CH_SNAP - 0.015f)), new Vector3f(-0.05f, 0.02f, 0));
                        else f.leftHand = arc(catchPt, support, smooth(seg(u, Plan.CH_SNAP + 0.01f, Plan.HAND_BACK)), new Vector3f(-0.06f, -0.03f, 0.02f));
                        break;
                    }
                    if (charge) {
                        Vector3f c;
                        if (k == GunKind.SMG) {
                            GunRig.Box kb = def.elements.get("cocking_knob");
                            Vector3f kn = kb != null ? kb.at(0.5f, 0.5f, 0.5f) : top;
                            kn.add(f.part(Part.CHARGE).off);
                            c = L.apply(kn).add(-0.02f, 0.02f, 0.02f);
                            float slap = smooth(seg(u, Plan.CH_SNAP - 0.015f, Plan.CH_SNAP + 0.01f));
                            c.add(0.0f, -0.05f * slap, -0.03f * slap);
                        } else {
                            GunRig.Box hb = def.bounds.get(Part.CHARGE);
                            float pull = smooth(seg(u, Plan.CH_GRAB, Plan.CH_PULL)) * (1 - easeIn(seg(u, Plan.CH_SNAP - 0.005f, Plan.CH_SNAP + 0.012f)));
                            float hand = smooth(seg(u, Plan.CH_GRAB, Plan.CH_PULL));
                            f.part(Part.CHARGE).off.z = 0.13f * pull;
                            Vector3f hp = hb != null ? hb.at(k == GunKind.VSS ? 0f : 0.5f, 1f, 1f) : top;
                            hp.z += 0.13f * hand;
                            c = L.apply(hp).add(k == GunKind.VSS ? 0.0f : -0.01f, 0.02f, 0.02f);
                        }
                        if (u < Plan.CH_GRAB) f.leftHand = arc(seat, c, smooth(seg(u, Plan.SEAT + 0.01f, Plan.CH_GRAB)), new Vector3f(-0.05f, 0.04f, 0.03f));
                        else if (u < Plan.CH_SNAP + 0.02f) f.leftHand = c;
                        else f.leftHand = arc(c, support, smooth(seg(u, Plan.CH_SNAP + 0.02f, Plan.HAND_BACK)), new Vector3f(-0.06f, -0.03f, 0.02f));
                    } else if (!(wasEmpty && (k == GunKind.PISTOL || k == GunKind.JUDGE))) {
                        float tap = seg(u, Plan.SEAT, Plan.SEAT + 0.03f) * (1 - seg(u, Plan.SEAT + 0.03f, Plan.SEAT + 0.06f));
                        Vector3f tapPt = new Vector3f(seat).add(0, 0.012f * tap, 0);
                        f.leftHand = arc(tapPt, support, smooth(seg(u, Plan.SEAT + 0.05f, Plan.SEAT + 0.24f)), new Vector3f(-0.06f, -0.04f, 0.02f));
                    }
                }
            }
            case DUAL -> {
                if (mag == null) break;
                Vector3f top = new Vector3f((mag.min().x + mag.max().x) / 2, mag.max().y, (mag.min().z + mag.max().z) / 2);
                PartPose mp = f.part(Part.MAG);
                float a = leftGun ? 0.1f : 0.08f;
                if (u >= a && u < 0.42f) {
                    float q = seg(u, a, a + 0.16f);
                    mp.off.set(0, -q * q * 1.0f, 0.05f * q);
                    mp.pivot.set(top);
                    mp.axis.set(1, 0, 0);
                    mp.angle = -12f * q * (leftGun ? -1 : 1);
                    if (q > 0.97f) mp.hidden = true;
                }
            }
            case SHOTGUN -> {
                GunRig.Box port = def.elements.get("loading_port");
                Vector3f pp = port != null ? L.apply(port.at(0.5f, 0f, 0.5f)) : support;
                Vector3f under = new Vector3f(pp).add(0.0f, -0.09f, 0.05f);
                Vector3f in = new Vector3f(pp).add(0, 0.008f, -0.05f);
                Vector3f pouch = body.apply(new Vector3f(0.16f, -0.86f, 0.02f));
                float end = wasEmpty ? Plan.SG_END_EMPTY : Plan.SG_END;
                int n = Math.max(1, shells);
                if (u >= Plan.SG_START && u < end) {
                    int i = Math.min(n - 1, (int) ((u - Plan.SG_START) / (end - Plan.SG_START) * n));
                    float a = Plan.SG_START + (end - Plan.SG_START) * i / n, b = Plan.SG_START + (end - Plan.SG_START) * (i + 1) / n;
                    float q = (u - a) / (b - a);
                    Vector3f from = i == 0 ? support : in;
                    Vector3f hand;
                    if (q < 0.35f) hand = arc(from, pouch, smooth(q / 0.35f), new Vector3f(0.05f, 0.03f, 0.04f));
                    else if (q < 0.42f) hand = pouch;
                    else if (q < 0.72f) hand = arc(pouch, under, smooth((q - 0.42f) / 0.3f), new Vector3f(0.04f, 0, 0.03f));
                    else if (q < Plan.SG_PUSH + 0.06f) hand = lerp(under, in, smooth(seg(q, 0.72f, Plan.SG_PUSH + 0.04f)));
                    else hand = in;
                    f.leftHand = hand;
                    if (q > 0.38f && q < Plan.SG_PUSH + 0.02f) {
                        f.held = new ItemStack(ModItems.SHOTGUN_SHELL.get());
                        f.heldAt = new Vector3f(hand).add(0.0f, 0.03f, -0.01f);
                    }
                } else if (u < Plan.SG_START) {
                    f.leftHand = arc(support, under, smooth(seg(u, 0.03f, Plan.SG_START)), new Vector3f(0, -0.03f, 0));
                } else {
                    float back = wasEmpty ? 0.87f : 1f;
                    Vector3f pumpHand = f.leftHand;
                    f.leftHand = arc(in, pumpHand, smooth(seg(u, end, back)), new Vector3f(-0.03f, -0.04f, 0));
                }
            }
            case RT20 -> {
                float ta = u * dur / Plan.rt20Scale(dur);
                GunRig.Box port = def.elements.get("loading_port");
                Vector3f pp = port != null ? L.apply(port.at(0.5f, 0f, 0.5f)) : support;
                Vector3f under = new Vector3f(pp).add(0, -0.12f, 0.02f);
                Vector3f in = new Vector3f(pp).add(0, -0.01f, 0);
                Vector3f pouch = body.apply(new Vector3f(0.0f, -1.0f, 0.1f));
                if (ta >= 1.0f && ta < 2.3f) f.leftHand = arc(support, pouch, smooth(seg(ta, 1.0f, 2.0f)), new Vector3f(-0.1f, 0, 0.05f));
                else if (ta >= 2.3f && ta < 3.0f) f.leftHand = arc(pouch, under, smooth(seg(ta, 2.3f, 3.0f)), new Vector3f(-0.06f, 0.02f, 0));
                else if (ta >= 3.0f && ta < 3.5f) f.leftHand = lerp(under, in, smooth(seg(ta, 3.0f, 3.4f)));
                else if (ta >= 3.5f && ta < 4.4f) f.leftHand = arc(in, support, smooth(seg(ta, 3.5f, 4.4f)), new Vector3f(-0.05f, -0.04f, 0));
                if (ta > 2.1f && ta < 3.42f) {
                    f.held = new ItemStack(ModItems.HISPANO_AMMO.get());
                    f.heldAt = new Vector3f(f.leftHand).add(0, 0.035f, 0);
                }
                Reload rl = reload;
                if (rl != null && !rl.ejected && ta >= 1.45f) {
                    rl.ejected = true;
                    if (Minecraft.getInstance().options.getCameraType().isFirstPerson()) s.ejects.add(now);
                }
            }
            case RPG -> {
                GunRig.Box rb = def.bounds.get(Part.ROCKET);
                if (rb == null) break;
                PartPose rp = f.part(Part.ROCKET);
                Vector3f grab = rb.at(0.5f, 0f, 0.6f);
                Vector3f pouch = body.apply(new Vector3f(-0.05f, -0.95f, 0.1f));
                Vector3f front = L.apply(new Vector3f(grab).add(0, 0, -0.45f));
                Vector3f seated = L.apply(grab);
                if (u < 0.36f && wasEmpty) rp.hidden = true;
                if (u >= 0.08f && u < 0.36f) f.leftHand = arc(support, pouch, smooth(seg(u, 0.08f, 0.3f)), new Vector3f(-0.1f, 0, 0.04f));
                else if (u >= 0.36f && u < 0.78f) {
                    Vector3f hand = u < 0.58f ? arc(pouch, front, smooth(seg(u, 0.36f, 0.58f)), new Vector3f(-0.1f, 0.06f, 0))
                            : lerp(front, seated, smooth(seg(u, 0.6f, 0.7f)));
                    f.leftHand = hand;
                    Vector3f hm = toModel.apply(new Vector3f(hand).add(0, 0.02f, 0));
                    rp.off.set(hm.sub(grab));
                    float twist = seg(u, 0.72f, 0.76f) * (1 - seg(u, 0.76f, 0.78f));
                    rp.pivot.set(rb.at(0.5f, 0.5f, 0.5f));
                    rp.axis.set(0, 0, 1);
                    rp.angle = 18f * twist;
                } else if (u >= 0.78f) f.leftHand = arc(seated, support, smooth(seg(u, 0.78f, 0.9f)), new Vector3f(-0.05f, -0.04f, 0));
            }
        }
    }

    // ── 탄피 ──
    /** at: 손 렌더 포즈(카메라 기준 월드 방향, 마크가 시야 회전의 역행렬을 앞에 곱해 둠)의 배출구 위치 (null 이면 카메라 기준 대략) */
    public static void spawnCasing(GunKind k, boolean leftGun, @Nullable Vector3f at) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || k.casing == null) return;
        var cam = mc.gameRenderer.getMainCamera();
        Quaternionf q = new Quaternionf(cam.rotation());
        Vector3f v = at != null ? new Vector3f(at) : q.transform(new Vector3f(leftGun ? -0.22f : 0.24f, -0.16f, -0.5f));
        Vec3 pos = cam.getPosition().add(v.x, v.y, v.z);
        Vector3f right = q.transform(new Vector3f(1, 0, 0)), up = q.transform(new Vector3f(0, 1, 0)), fwd = q.transform(new Vector3f(0, 0, -1));
        var r = mc.level.random;
        Vector3f vel;
        if (k == GunKind.RT20) vel = new Vector3f(up).mul(-0.06f).add(new Vector3f(right).mul(0.02f));
        else {
            // 오른쪽 위로, 살짝 뒤(얼굴 쪽)로 튀어 잠깐 화면에 보였다가 떨어짐
            float side = 0.055f + r.nextFloat() * 0.03f;
            vel = new Vector3f(right).mul(side).add(new Vector3f(up).mul(0.13f + r.nextFloat() * 0.05f)).add(new Vector3f(fwd).mul(-0.012f + r.nextFloat() * 0.02f));
            if (k == GunKind.SHOTGUN) vel.mul(0.85f);
        }
        Vec3 pv = mc.player.getDeltaMovement();
        vel.add((float) pv.x, (float) Math.max(0, pv.y) * 0.5f, (float) pv.z);
        mc.particleEngine.add(new CasingParticle(mc.level, pos, vel, k.casing, fwd, up));
        ejected++;
    }

    // ── 팔 ──
    /** 손끝(손바닥 중심)이 hand 에 오도록, shoulder 쪽에서 뻗은 팔을 그림 */
    static void drawArm(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, boolean right, Vector3f hand, Vector3f shoulder) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> model = r.getModel();
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        boolean slim = p.getSkin().model() == PlayerSkin.Model.SLIM;
        float cx = (slim ? 0.5f : 1f) * (right ? -1f : 1f);
        Vector3f dir = new Vector3f(hand).sub(shoulder).normalize();
        Vector3f x = new Vector3f(-1, 0, 0);
        x.sub(new Vector3f(dir).mul(x.dot(dir))).normalize();
        Vector3f z = new Vector3f(x).cross(dir).normalize();
        Matrix3f basis = new Matrix3f().setColumn(0, x).setColumn(1, dir).setColumn(2, z);
        Quaternionf q = new Quaternionf().setFromNormalized(basis);
        ps.pushPose();
        ps.translate(hand.x, hand.y, hand.z);
        ps.mulPose(q);
        ps.scale(0.82f, 1f, 0.82f);
        ps.translate(-cx / 16f, -9.5f / 16f, 0);
        float ax = arm.x, ay = arm.y, az = arm.z, rx = arm.xRot, ry = arm.yRot, rz = arm.zRot;
        arm.setPos(0, 0, 0);
        arm.setRotation(0, 0, 0);
        sleeve.setPos(0, 0, 0);
        sleeve.setRotation(0, 0, 0);
        var skin = p.getSkin().texture();
        arm.render(ps, buf.getBuffer(RenderType.entitySolid(skin)), light, OverlayTexture.NO_OVERLAY);
        if (p.isModelPartShown(right ? net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE : net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE))
            sleeve.render(ps, buf.getBuffer(RenderType.entityTranslucent(skin)), light, OverlayTexture.NO_OVERLAY);
        arm.setPos(ax, ay, az);
        arm.setRotation(rx, ry, rz);
        sleeve.setPos(ax, ay, az);
        sleeve.setRotation(rx, ry, rz);
        ps.popPose();
    }

    private GunViewModel() {}
}
