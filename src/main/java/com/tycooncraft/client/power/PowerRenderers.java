package com.tycooncraft.client.power;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.tycooncraft.TycoonCraft;
import com.tycooncraft.client.vehicle.VehicleModels;
import com.tycooncraft.power.*;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** 회전축·증기기관이 실제로 돌아가는 모습 + 동력 관련 아이템 설명 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class PowerRenderers {
    /** 축: 자기 축 방향으로 돎 */
    public static class Shaft implements BlockEntityRenderer<ShaftBlockEntity> {
        public Shaft(BlockEntityRendererProvider.Context c) {
        }

        @Override
        public void render(ShaftBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            var st = be.getBlockState();
            if (!st.hasProperty(ShaftBlock.AXIS)) return;
            float a = Mth.lerp(pt, be.angleO, be.angle);
            ps.pushPose();
            ps.translate(0.5, 0.5, 0.5);
            switch (st.getValue(ShaftBlock.AXIS)) {
                case X -> ps.mulPose(Axis.ZP.rotationDegrees(90));
                case Z -> ps.mulPose(Axis.XP.rotationDegrees(90));
                default -> {}
            }
            ps.mulPose(Axis.YP.rotationDegrees(a));
            VehicleModels.part(ps, VehicleModels.cutout(buf), VehicleModels.SHAFT, 1f, light);
            ps.popPose();
        }
    }

    /** 증기기관: 동쪽(정면 기준 오른쪽) 플라이휠이 돌고, 피스톤 막대가 앞뒤로 왕복 */
    public static class Engine implements BlockEntityRenderer<SteamEngineBlockEntity> {
        public Engine(BlockEntityRendererProvider.Context c) {
        }

        @Override
        public void render(SteamEngineBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            var st = be.getBlockState();
            if (!st.hasProperty(SteamEngineBlock.FACING)) return;
            float face = switch (st.getValue(SteamEngineBlock.FACING)) {
                case EAST -> 90f;
                case SOUTH -> 180f;
                case WEST -> 270f;
                default -> 0f;
            };
            float a = Mth.lerp(pt, be.angleO, be.angle);
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-face));
            ps.translate(-0.5, 0, -0.5);
            // 플라이휠: 중심 (14.75, 9.5, 10)
            ps.pushPose();
            ps.translate(14.75 / 16, 9.5 / 16, 10 / 16.0);
            ps.mulPose(Axis.XP.rotationDegrees(-a));
            VehicleModels.part(ps, VehicleModels.cutout(buf), VehicleModels.ENGINE_FLYWHEEL, 1f, light);
            ps.popPose();
            // 피스톤 막대: 실린더 안(z≈4)에서 크로스헤드(z≈12) 사이를 왕복
            float stroke = Mth.sin(a * Mth.DEG_TO_RAD) * 2.2f;
            ps.pushPose();
            ps.translate(5.5 / 16, 6.5 / 16, (7 + stroke) / 16.0);
            VehicleModels.part(ps, VehicleModels.cutout(buf), VehicleModels.ENGINE_ROD, 1f, light);
            ps.popPose();
            ps.popPose();
        }
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent e) {
        var st = e.getItemStack();
        String key = st.is(ModItems.BOILER.get()) ? "boiler" : st.is(ModItems.STEAM_ENGINE.get()) ? "steam_engine"
                : st.is(ModItems.SHAFT.get()) ? "shaft" : st.is(ModItems.POWER_GEARBOX.get()) ? "power_gearbox" : null;
        if (key != null) e.getToolTip().add(Component.translatable("tooltip.tycooncraft." + key).withStyle(ChatFormatting.GRAY));
        if (st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof com.tycooncraft.block.MachineBlock mb)
            e.getToolTip().add(Component.translatable("tooltip.tycooncraft.machine_power", mb.type().power()).withStyle(ChatFormatting.GOLD));
    }

    private PowerRenderers() {}
}
