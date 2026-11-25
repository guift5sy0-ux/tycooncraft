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
     * v0.23.3 로봇 팔 — 몸통(좌우)·어깨·팔꿈치·손목·집게 따로 움직임. 한 동작 24틱:
     * 0~3 뒤로 숙여 내려가기(집게 벌림) · 3~5 집기 · 5~8 들어 올리기 · 8~14 앞으로 돌기 ·
     * 14~17 내려가기 · 17~19 놓기(18틱에 서버가 실제로 넣음) · 19~24 올리며 뒤로 돌아오기
     */
    public static class Arm implements BlockEntityRenderer<com.tycooncraft.automation.RobotArmBlockEntity> {
        public Arm(BlockEntityRendererProvider.Context c) {
        }

        private static float smooth(float a, float b, float t, float t0, float t1) {
            float u = Mth.clamp((t - t0) / (t1 - t0), 0f, 1f);
            u = u * u * (3 - 2 * u);
            return a + (b - a) * u;
        }

        /** [몸통 돌림(0=뒤,1=앞), 숙임(0=듦,1=내림), 집게 벌림(0~1), 들고 있음] */
        static float[] pose(float t) {
            if (t < 0 || t >= 24) return new float[]{0, 0, 0.55f, 0};
            float turn = t < 14 ? smooth(0, 1, t, 8, 14) : smooth(1, 0, t, 19, 24);
            float reach;
            if (t < 5) reach = smooth(0, 1, t, 0, 3);
            else if (t < 14) reach = smooth(1, 0, t, 5, 8);
            else if (t < 19) reach = smooth(0, 1, t, 14, 17);
            else reach = smooth(1, 0, t, 19, 22);
            float open;
            if (t < 3) open = smooth(0.55f, 1, t, 0, 3);
            else if (t < 17) open = smooth(1, 0, t, 3, 5);
            else open = smooth(0, 1, t, 17, 19) - smooth(0, 0.45f, t, 20, 24);
            boolean carry = t >= 4.5f && t < 18f;
            return new float[]{turn, reach, open, carry ? 1 : 0};
        }

        @Override
        public net.minecraft.world.phys.AABB getRenderBoundingBox(com.tycooncraft.automation.RobotArmBlockEntity be) {
            return new net.minecraft.world.phys.AABB(be.getBlockPos()).inflate(1.2, 0, 1.2).expandTowards(0, 1.2, 0);
        }

        @Override
        public void render(com.tycooncraft.automation.RobotArmBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (be.getLevel() == null) return;
            var f = be.getBlockState().getValue(com.tycooncraft.automation.RobotArmBlock.FACING);
            float back = -f.toYRot() + 180f;
            float t = be.getLevel().getGameTime() - be.swingAt + pt;
            float[] p = pose(t);
            float yaw = back - 180f * p[0];
            float shoulder = Mth.lerp(p[1], -10f, 25f), elbow = Mth.lerp(p[1], -40f, -55f);
            // 쉬는 동안 살짝 숨쉬듯
            if (p[3] == 0 && (t < 0 || t >= 24)) shoulder += Mth.sin((be.getLevel().getGameTime() + pt) * 0.08f) * 1.2f;
            var vc = VehicleModels.cutout(buf);
            ps.pushPose();
            ps.translate(0.5, 6 / 16.0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(yaw));
            VehicleModels.part(ps, vc, VehicleModels.ROBOT_ARM_TURRET, 1f, light);
            ps.translate(0, 4 / 16.0, 0);
            ps.mulPose(Axis.XP.rotationDegrees(shoulder));
            VehicleModels.part(ps, vc, VehicleModels.ROBOT_ARM_UPPER, 1f, light);
            ps.translate(0, 10 / 16.0, 0);
            ps.mulPose(Axis.XP.rotationDegrees(elbow));
            VehicleModels.part(ps, vc, VehicleModels.ROBOT_ARM_FORE, 1f, light);
            ps.translate(0, 0, 11 / 16.0);
            ps.mulPose(Axis.XP.rotationDegrees(-(shoulder + elbow)));   // 집게는 늘 수직
            VehicleModels.part(ps, vc, VehicleModels.ROBOT_ARM_WRIST, 1f, light);
            float gap = (0.9f + p[2] * 1.6f) / 16f;
            for (int side = -1; side <= 1; side += 2) {
                ps.pushPose();
                ps.translate(side * gap, 0, 0);
                VehicleModels.part(ps, vc, VehicleModels.ROBOT_ARM_FINGER, 1f, light);
                ps.popPose();
            }
            if (p[3] > 0 && be.carried != null) {
                ps.translate(0, -7.0 / 16.0, 0);
                ps.mulPose(Axis.YP.rotationDegrees(90));   // 팔 옆에서 보면 넓은 면이 보이게
                ps.scale(0.38f, 0.38f, 0.38f);
                net.minecraft.client.Minecraft.getInstance().getItemRenderer().renderStatic(new net.minecraft.world.item.ItemStack(be.carried),
                        net.minecraft.world.item.ItemDisplayContext.FIXED, light, overlay, ps, buf, be.getLevel(), 0);
            }
            ps.popPose();
            if (!be.filter().isEmpty()) renderFilter(be.filter(), f, ps, buf, light, overlay, be.getLevel(), 0.2);
        }
    }

    /** 분류기: 윗면 왼쪽(갈라지는 쪽)에 필터 아이템 표시 */
    public static class Sorter implements BlockEntityRenderer<com.tycooncraft.automation.SorterBlockEntity> {
        public Sorter(BlockEntityRendererProvider.Context c) {
        }

        @Override
        public void render(com.tycooncraft.automation.SorterBlockEntity be, float pt, PoseStack ps, MultiBufferSource buf, int light, int overlay) {
            if (be.getLevel() == null) return;
            var f = be.getBlockState().getValue(com.tycooncraft.automation.SorterBlock.FACING);
            // 가림판: 평소엔 흐름 방향과 나란히, 왼쪽으로 보낼 때 45° 꺾였다가 돌아옴
            float t = be.getLevel().getGameTime() - be.flapAt + pt;
            float swing = t < 0 || t > 12 ? 0 : t < 2 ? t / 2 : t < 8 ? 1 : 1 - (t - 8) / 4;
            if (be.flapLeft) swing = swing * swing * (3 - 2 * swing);
            else swing = 0;
            ps.pushPose();
            ps.translate(0.5, 8 / 16.0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(-f.toYRot() + 180f + 40f * swing));
            VehicleModels.part(ps, VehicleModels.cutout(buf), VehicleModels.SORTER_FLAP, 1f, light);
            ps.popPose();
            if (be.filter().isEmpty()) return;
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
                : st.is(ModItems.SORTER.get()) ? "sorter" : st.is(ModItems.WAREHOUSE.get()) ? "warehouse"
                : st.is(ModItems.BLACK_MARKET_PASS.get()) ? "black_market_pass" : st.is(ModItems.MANHOLE.get()) ? "manhole" : null;
        if (key != null) e.getToolTip().add(Component.translatable("tooltip.tycooncraft." + key).withStyle(ChatFormatting.GRAY));
        if (st.getItem() instanceof BlockItem bi && bi.getBlock() instanceof com.tycooncraft.block.MachineBlock mb)
            e.getToolTip().add(mb.type().electricOnly()
                    ? Component.translatable("tooltip.tycooncraft.machine_elec_only", mb.type().power()).withStyle(ChatFormatting.AQUA)
                    : Component.translatable("tooltip.tycooncraft.machine_power", mb.type().power(),
                    Math.round(mb.type().power() * com.tycooncraft.blockentity.MachineBlockEntity.ELEC_SPEED)).withStyle(ChatFormatting.GOLD));
    }

    private PowerRenderers() {}
}
