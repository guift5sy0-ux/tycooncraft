package com.tycooncraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1인칭: 총을 실제로 쥔 두 팔을 그림.
 * 오른손은 권총 손잡이, 왼손은 총열 덮개·앞 손잡이(권총은 오른손을 감싸 쥠)에 정확히 닿도록
 * 어깨 쪽에서 손 위치를 향해 팔을 뻗음. 쏠 때는 반동으로 총과 팔이 함께 뒤로 튐.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class GunArms {
    /** 시야 공간(블록) 손 위치: {오른손 x,y,z, 왼손 x,y,z} — scripts/gen_guns.py 의 손잡이 좌표에서 계산 */
    private static float[] hands(ItemStack s) {
        if (s.is(ModItems.PISTOL.get())) return new float[]{0.36f, -0.34f, -0.62f, 0.309f, -0.429f, -0.587f};
        if (s.is(ModItems.JUDGE.get())) return new float[]{0.36f, -0.34f, -0.62f, 0.312f, -0.417f, -0.591f};
        if (s.is(ModItems.SMG.get())) return new float[]{0.34f, -0.33f, -0.60f, 0.308f, -0.275f, -1.062f};
        if (s.is(ModItems.RIFLE.get())) return new float[]{0.33f, -0.32f, -0.60f, 0.296f, -0.291f, -1.083f};
        if (s.is(ModItems.SHOTGUN.get())) return new float[]{0.33f, -0.32f, -0.60f, 0.289f, -0.339f, -1.189f};
        if (s.is(ModItems.SNIPER.get())) return new float[]{0.33f, -0.33f, -0.62f, 0.30f, -0.264f, -1.048f};
        if (s.is(ModItems.VSS.get())) return new float[]{0.31f, -0.33f, -0.62f, 0.29f, -0.285f, -0.98f};
        if (s.is(ModItems.RPG.get())) return new float[]{0.34f, -0.28f, -0.58f, 0.331f, -0.314f, -0.752f};
        // RT-20: 오른손은 앞쪽 권총 손잡이(불펍), 왼손은 총열 덮개 아래를 받침 (rt20.json 좌표에서 계산)
        if (s.is(ModItems.RT20.get())) return new float[]{0.34f, -0.33f, -0.64f, 0.326f, -0.285f, -0.846f};
        if (s.is(ModItems.DUAL_PISTOLS.get())) return new float[]{0.36f, -0.34f, -0.62f, -0.36f, -0.34f, -0.62f};
        return null;
    }

    private static float kick, kickO;
    private static int lastAmmo = -1, lastSlot = -1;

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        LocalPlayer p = Minecraft.getInstance().player;
        kickO = kick;
        kick *= 0.55f;
        if (p == null) return;
        ItemStack s = p.getMainHandItem();
        if (!(s.getItem() instanceof GunItem)) {
            lastAmmo = -1;
            return;
        }
        int ammo = GunItem.ammo(s);
        int slot = p.getInventory().selected;
        if (slot == lastSlot && lastAmmo >= 0 && ammo < lastAmmo)
            kick = s.is(ModItems.RT20.get()) ? 3.2f : s.is(ModItems.RPG.get()) || s.is(ModItems.SHOTGUN.get()) || s.is(ModItems.SNIPER.get()) ? 1.6f : 1f;
        lastAmmo = ammo;
        lastSlot = slot;
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onHand(RenderHandEvent event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.getMainArm() != HumanoidArm.RIGHT) return;
        ItemStack stack = event.getItemStack();
        float[] h = hands(stack);
        if (h == null) return;
        event.setCanceled(true);
        PoseStack ps = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        int light = event.getPackedLight();
        float pt = event.getPartialTick();
        float k = Mth.lerp(pt, kickO, kick);
        ps.pushPose();
        // 권총류: 두 손으로 감싸 쥔 모습이 보이도록 조금 가운데·위로
        if (stack.is(ModItems.PISTOL.get()) || stack.is(ModItems.JUDGE.get())) ps.translate(-0.08f, 0.075f, 0.02f);
        // 꺼내기(장착) 애니메이션 + 반동: 손잡이를 축으로 총구가 들리며 뒤로
        ps.translate(0, event.getEquipProgress() * -0.6f, k * 0.06f);
        // v0.20.1 들고 다닐 때: 총을 조금 내리고 총구를 아래·안쪽으로 (쏘거나 조준하면 빠르게 올라옴)
        float relax = 1f - GunAnim.aim(p);
        ps.translate(-0.03f * relax, -0.07f * relax, 0.06f * relax);
        ps.translate(h[0], h[1], h[2]);
        ps.mulPose(Axis.XP.rotationDegrees(k * 7f - 14f * relax));
        ps.mulPose(Axis.YP.rotationDegrees(9f * relax));
        ps.mulPose(Axis.ZP.rotationDegrees(-6f * relax));
        ps.translate(-h[0], -h[1], -h[2]);
        // 총
        ps.pushPose();
        ps.translate(0.56f, -0.52f, -0.72f);
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, stack, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, false, ps, buf, light);
        ps.popPose();
        // 팔
        drawArm(ps, buf, light, p, true, new Vector3f(h[0], h[1], h[2]), new Vector3f(0.5f, -0.95f, 0.2f));
        // 왼팔: 권총류는 옆에서 감싸 쥐고, 긴 총은 아래쪽 가운데에서 올라와 총열 덮개를 받침
        boolean longGun = !(stack.is(ModItems.PISTOL.get()) || stack.is(ModItems.JUDGE.get()) || stack.is(ModItems.DUAL_PISTOLS.get()));
        drawArm(ps, buf, light, p, false, new Vector3f(h[3], h[4], h[5]),
                longGun ? new Vector3f(-0.15f, -1.15f, 0.05f) : new Vector3f(-0.5f, -0.95f, 0.2f));
        ps.popPose();
    }

    /** 손끝(손바닥 중심)이 hand 에 오도록, shoulder 쪽에서 뻗은 팔을 그림 */
    private static void drawArm(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer p, boolean right, Vector3f hand, Vector3f shoulder) {
        if (!(Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(p) instanceof PlayerRenderer r)) return;
        PlayerModel<AbstractClientPlayer> model = r.getModel();
        ModelPart arm = right ? model.rightArm : model.leftArm;
        ModelPart sleeve = right ? model.rightSleeve : model.leftSleeve;
        boolean slim = p.getSkin().model() == PlayerSkin.Model.SLIM;
        float cx = (slim ? 0.5f : 1f) * (right ? -1f : 1f);          // 팔 큐브 가로 중심(px)
        Vector3f dir = new Vector3f(hand).sub(shoulder).normalize();  // 어깨 → 손 (모델의 +y)
        Vector3f x = new Vector3f(-1, 0, 0);                          // 팔 바깥면이 화면 바깥쪽을 보도록
        x.sub(new Vector3f(dir).mul(x.dot(dir))).normalize();
        Vector3f z = new Vector3f(x).cross(dir).normalize();
        Matrix3f basis = new Matrix3f().setColumn(0, x).setColumn(1, dir).setColumn(2, z);
        Quaternionf q = new Quaternionf().setFromNormalized(basis);
        ps.pushPose();
        ps.translate(hand.x, hand.y, hand.z);
        ps.mulPose(q);
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

    private GunArms() {}
}
