package com.tycooncraft.client.gear;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.client.vehicle.VehicleModels;
import com.tycooncraft.gear.Parachute;
import com.tycooncraft.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;

/** 낙하산 배낭·캐노피, 쌍권총의 왼손 권총 그리기 */
@EventBusSubscriber(modid = TycoonCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GearClient {

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model m : event.getSkins()) {
            if (event.getSkin(m) instanceof PlayerRenderer r) {
                r.addLayer(new ParachuteLayer(r));
                r.addLayer(new DualPistolLayer(r));
            }
        }
    }


    /** 원격 플레이어: 땅에서 떨어져 천천히 내려오면 펼친 것으로 보고 그림 */
    public static boolean chuteOpen(Player p) {
        if (!Parachute.wearing(p) || p.onGround() || p.isPassenger()) return false;
        if (p == Minecraft.getInstance().player) return Parachute.open(p);
        double dy = p.getY() - p.yo;
        return dy < -0.03 && dy > -0.3;
    }

    static class ParachuteLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        ParachuteLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                           float pt, float age, float headYaw, float headPitch) {
            if (!Parachute.wearing(p)) return;
            ps.pushPose();
            getParentModel().body.translateAndRotate(ps);
            ps.scale(-1, -1, 1);    // 모델 공간(위아래 뒤집힘) → 월드 방향
            ps.pushPose();
            ps.translate(0, -0.70, 0.13);
            VehicleModels.body(ps, VehicleModels.cutout(buffers), VehicleModels.CHUTE_PACK, 1f, light);
            ps.popPose();
            if (chuteOpen(p)) {
                ps.translate(0, 0.15 + 1.25, 0.1);
                VehicleModels.part(ps, VehicleModels.cutout(buffers), VehicleModels.CHUTE_CANOPY, 4f, light);
            }
            ps.popPose();
        }
    }

    /** 3인칭: 쌍권총을 들면 왼손에도 권총 */
    static class DualPistolLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
        DualPistolLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
            super(parent);
        }

        @Override
        public void render(PoseStack ps, MultiBufferSource buffers, int light, AbstractClientPlayer p, float limbSwing, float limbSwingAmount,
                           float pt, float age, float headYaw, float headPitch) {
            if (!p.getMainHandItem().is(ModItems.DUAL_PISTOLS.get()) || !p.getOffhandItem().isEmpty()) return;
            HumanoidArm arm = p.getMainArm().getOpposite();
            ps.pushPose();
            getParentModel().translateToHand(arm, ps);
            ps.mulPose(Axis.XP.rotationDegrees(-90f));
            ps.mulPose(Axis.YP.rotationDegrees(180f));
            boolean left = arm == HumanoidArm.LEFT;
            ps.translate((left ? -1 : 1) / 16f, 0.125f, -0.625f);
            Minecraft.getInstance().getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, pistol(p),
                    left ? ItemDisplayContext.THIRD_PERSON_LEFT_HAND : ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, left, ps, buffers, light);
            ps.popPose();
        }
    }

    static ItemStack pistol(Player p) {
        return new ItemStack(ModItems.PISTOL.get());
    }

    // v0.32: 1인칭 쌍권총은 GunViewModel 이 두 자루 모두 그림

    private GearClient() {}
}
