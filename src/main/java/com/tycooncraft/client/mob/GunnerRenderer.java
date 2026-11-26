package com.tycooncraft.client.mob;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.mob.GunnerEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

/** 갱단원·용병·무법자·카르텔 두목 (사람 모델 + 방탄장비 표시 + 손에 든 총) */
public class GunnerRenderer extends HumanoidMobRenderer<GunnerEntity, GunnerModel<GunnerEntity>> {

    public GunnerRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GunnerModel<>(ctx.bakeLayer(ModelLayers.PLAYER)), 0.5f);
        addLayer(new HumanoidArmorLayer<>(this, new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)), ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(GunnerEntity e) {
        String name = switch (e.kind()) {
            case GANGSTER -> "gangster";
            case MERCENARY -> "mercenary";
            case OUTLAW -> "outlaw";
            case BOSS -> "cartel_boss";
            case ENFORCER -> "gang_enforcer";
            case BRUTE -> "gang_brute";
            case MARKSMAN -> "gang_marksman";
        };
        return TycoonCraft.id("textures/entity/" + name + ".png");
    }

    // ── v0.29.1 저격수 조준선 ──
    private static final java.util.Map<Integer, Integer> AIM_START = new java.util.HashMap<>();
    public static int beamsDrawn;

    @Override
    public void render(GunnerEntity e, float yaw, float pt, PoseStack ps, net.minecraft.client.renderer.MultiBufferSource buf, int light) {
        super.render(e, yaw, pt, ps, buf, light);
        if (e.kind() != GunnerEntity.Kind.MARKSMAN) return;
        int tid = e.aimTarget();
        if (tid < 0) {
            AIM_START.remove(e.getId());
            return;
        }
        var t = e.level().getEntity(tid);
        if (t == null) return;
        int start = AIM_START.computeIfAbsent(e.getId(), k -> e.tickCount);
        float prog = Math.min(1f, (e.tickCount - start + pt) / GunnerEntity.AIM_TICKS);
        var from = new net.minecraft.world.phys.Vec3(0, e.getEyeHeight() - 0.15, 0);
        double tx = net.minecraft.util.Mth.lerp(pt, t.xo, t.getX()) - net.minecraft.util.Mth.lerp(pt, e.xo, e.getX());
        double ty = net.minecraft.util.Mth.lerp(pt, t.yo, t.getY()) + t.getBbHeight() * 0.6 - net.minecraft.util.Mth.lerp(pt, e.yo, e.getY());
        double tz = net.minecraft.util.Mth.lerp(pt, t.zo, t.getZ()) - net.minecraft.util.Mth.lerp(pt, e.zo, e.getZ());
        var to = new net.minecraft.world.phys.Vec3(tx, ty, tz);
        // 시작은 흐리게 → 쏘기 직전엔 진하게 깜빡임
        float a = prog < 0.75f ? 0.25f + prog * 0.5f : ((e.tickCount / 2) % 2 == 0 ? 0.95f : 0.6f);
        float w = 0.025f + prog * 0.02f;
        var vc = buf.getBuffer(net.minecraft.client.renderer.RenderType.lightning());
        var m = ps.last().pose();
        var dir = to.subtract(from).normalize();
        var side = dir.cross(new net.minecraft.world.phys.Vec3(0, 1, 0));
        if (side.lengthSqr() < 1e-4) side = new net.minecraft.world.phys.Vec3(1, 0, 0);
        side = side.normalize().scale(w);
        var up = dir.cross(side).normalize().scale(w);
        for (var off : new net.minecraft.world.phys.Vec3[]{side, up}) {
            quad(vc, m, from.add(off), from.subtract(off), to.subtract(off), to.add(off), a);
            quad(vc, m, from.subtract(off), from.add(off), to.add(off), to.subtract(off), a);   // 뒷면
        }
        beamsDrawn++;
    }

    private static void quad(com.mojang.blaze3d.vertex.VertexConsumer vc, org.joml.Matrix4f m, net.minecraft.world.phys.Vec3 a, net.minecraft.world.phys.Vec3 b,
                             net.minecraft.world.phys.Vec3 c, net.minecraft.world.phys.Vec3 d, float alpha) {
        for (var v : new net.minecraft.world.phys.Vec3[]{a, b, c, d})
            vc.addVertex(m, (float) v.x, (float) v.y, (float) v.z).setColor(1f, 0.08f, 0.05f, alpha);
    }

    @Override
    public boolean shouldRender(GunnerEntity e, net.minecraft.client.renderer.culling.Frustum f, double x, double y, double z) {
        return super.shouldRender(e, f, x, y, z) || (e.kind() == GunnerEntity.Kind.MARKSMAN && e.aimTarget() >= 0);
    }

    @Override
    protected void scale(GunnerEntity e, PoseStack ps, float pt) {
        if (e.kind() == GunnerEntity.Kind.BOSS) ps.scale(1.4f, 1.4f, 1.4f);
        else if (e.kind() == GunnerEntity.Kind.BRUTE) ps.scale(1.1f, 1.1f, 1.1f);
        else if (e.kind() == GunnerEntity.Kind.ENFORCER) ps.scale(1.02f, 1.02f, 1.02f);
        else ps.scale(0.9375f, 0.9375f, 0.9375f);
    }
}
