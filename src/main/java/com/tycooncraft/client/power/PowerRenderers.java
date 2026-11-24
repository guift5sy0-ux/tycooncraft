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

    /**
     * v0.23.0 로봇 팔: 평소엔 집는 쪽(뒤)을 보고 있다가, 옮길 때 앞으로 휙 돌아 내려놓고 돌아온다 (0.5초 + 0.5초).
     * 머리 모델은 +Z(남) 쪽으로 팔을 뻗은 모양.
     */
    public static class Arm implements BlockEntityRenderer<com.tycooncraft.automation.RobotArmBlockEntity> {
        public Arm(BlockEntityRendererProvider.Context c) {
        }

        @Override
        public void render(com.tycooncraft.automation.RobotArmBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (be.getLevel() == null) return;
            var f = be.getBlockState().getValue(com.tycooncraft.automation.RobotArmBlock.FACING);
            float front = -f.toYRot(), back = front + 180f;
            float t = (be.getLevel().getGameTime() - be.swingAt + pt) / 10f;
            float yaw, lift = 0;
            boolean carry = false;
            if (t >= 0 && t < 1) {
                float e = t * t * (3 - 2 * t);
                yaw = back + 180f * e;
                lift = Mth.sin(t * Mth.PI) * 2.5f;
                carry = true;
            } else if (t >= 1 && t < 2) {
                float u = t - 1, e = u * u * (3 - 2 * u);
                yaw = front + 180f * e;
                lift = Mth.sin(u * Mth.PI) * 1.5f;
            } else yaw = back;
            ps.pushPose();
            ps.translate(0.5, (6 + lift) / 16.0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            VehicleModels.part(ps, VehicleModels.cutout(buf), VehicleModels.ROBOT_ARM_HEAD, 1f, light);
            if (carry && be.carried != null) {
                ps.translate(0, 6 / 16.0, 8.5 / 16.0);
                ps.scale(0.38f, 0.38f, 0.38f);
                net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(new net.minecraft.world.item.ItemStack(be.carried),
                        net.minecraft.world.item.ItemDisplayContext.FIXED, light, overlay, ps, buf, be.getLevel(), 0);
            }
            ps.popPose();
            // 필터 아이템: 받침 앞면 작은 표시
            if (!be.filter().isEmpty()) renderFilter(be.filter(), f, ps, buf, light, overlay, be.getLevel(), 0.2);
        }
    }

    /** 분류기: 윗면 왼쪽(갈라지는 쪽)에 필터 아이템 표시 */
    public static class Sorter implements BlockEntityRenderer<com.tycooncraft.automation.SorterBlockEntity> {
        public Sorter(BlockEntityRendererProvider.Context c) {
        }

        @Override
        public void render(com.tycooncraft.automation.SorterBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (be.filter().isEmpty() || be.getLevel() == null) return;
            var f = be.getBlockState().getValue(com.tycooncraft.automation.SorterBlock.FACING);
            var left = f.getCounterClockWise();
            ps.pushPose();
            ps.translate(0.5 + left.getStepX() * 0.47, 0.32, 0.5 + left.getStepZ() * 0.47);
            ps.mulPose(Axis.YP.rotationDegrees(-left.toYRot()));
            ps.scale(0.4f, 0.4f, 0.4f);
            net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(be.filter(),
                    net.minecraft.world.item.ItemDisplayContext.FIXED, light, overlay, ps, buf, be.getLevel(), 0);
            ps.popPose();
        }
    }

    private static void renderFilter(net.minecraft.world.item.ItemStack st, net.minecraft.core.Direction f, PoseStack ps, MultiBufferSource buf,
                                     int light, int overlay, net.minecraft.world.level.Level level, double y) {
        var side = f.getClockWise();
        ps.pushPose();
        ps.translate(0.5 + side.getStepX() * 0.445, y, 0.5 + side.getStepZ() * 0.445);
        ps.mulPose(Axis.YP.rotationDegrees(-side.toYRot()));
        ps.scale(0.25f, 0.25f, 0.25f);
        net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(st, net.minecraft.world.item.ItemDisplayContext.FIXED, light, overlay, ps, buf, level, 0);
        ps.popPose();
    }

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent e) {
        var st = e.getItemStack();
        String key = st.is(ModItems.BOILER.get()) ? "boiler" : st.is(ModItems.STEAM_ENGINE.get()) ? "steam_engine"
                : st.is(ModItems.SHAFT.get()) ? "shaft" : st.is(ModItems.POWER_GEARBOX.get()) ? "power_gearbox"
                : st.is(ModItems.GENERATOR.get()) ? "generator" : st.is(ModItems.POWER_MOTOR.get()) ? "power_motor"
                : st.is(ModItems.POWER_CABLE.get()) ? "power_cable" : st.is(ModItems.SOLAR_GENERATOR.get()) ? "solar_generator"
                : st.is(ModItems.ACCUMULATOR.get()) ? "accumulator" : st.is(ModItems.ROBOT_ARM.get()) ? "robot_arm"
                : st.is(ModItems.SORTER.get()) ? "sorter" : st.is(ModItems.WAREHOUSE.get()) ? "warehouse" : null;
        if (key != null) e.getToolTip().add(Component.translatable("tooltip.tycooncraft." + key).withStyle(ChatFormatting.GRAY));
        if (st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof com.tycooncraft.block.MachineBlock mb)
            e.getToolTip().add(mb.type().electricOnly()
                    ? Component.translatable("tooltip.tycooncraft.machine_elec_only", mb.type().power()).withStyle(ChatFormatting.AQUA)
                    : Component.translatable("tooltip.tycooncraft.machine_power", mb.type().power(),
                    Math.round(mb.type().power() * com.tycooncraft.blockentity.MachineBlockEntity.ELEC_SPEED)).withStyle(ChatFormatting.GOLD));
    }

    private PowerRenderers() {}
}
