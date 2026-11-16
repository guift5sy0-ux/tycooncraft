package com.tycooncraft.client.vehicle;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.gun.GunItem;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.world.item.Item;
import com.tycooncraft.vehicle.FueledVehicle;
import com.tycooncraft.vehicle.VehicleHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** 탈것 클라이언트 등록: 렌더러, 부품 모델, 계기판, 입력·소리 연결 */
@EventBusSubscriber(modid = TycoonCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class VehicleClient {

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        VehicleHooks.client = new VehicleClientHooks();
        // 소음기 단 총은 소음기 모델로
        event.enqueueWork(() -> {
            for (Item gun : new Item[]{ModItems.PISTOL.get(), ModItems.SMG.get(), ModItems.RIFLE.get(), ModItems.SHOTGUN.get(), ModItems.SNIPER.get()}) {
                ItemProperties.register(gun, TycoonCraft.id("silenced"), (stack, level, entity, seed) -> GunItem.silencerTier(stack) / 2f);
            }
        });
    }

    @SubscribeEvent
    public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(VehicleKeyBinds.DESCEND);
    }

    @SubscribeEvent
    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(com.tycooncraft.client.mob.BossModels.GOLEM, com.tycooncraft.client.mob.DestroyerGolemGeo::createBodyLayer);
        event.registerLayerDefinition(com.tycooncraft.client.mob.BossModels.BEHEMOTH, com.tycooncraft.client.mob.BehemothGeo::createBodyLayer);
        event.registerLayerDefinition(com.tycooncraft.client.mob.BossModels.WYVERN, com.tycooncraft.client.mob.StormWyvernGeo::createBodyLayer);
    }

    @SubscribeEvent
    public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CAR.get(), CarRenderer::new);
        event.registerEntityRenderer(ModEntities.HELICOPTER.get(), HelicopterRenderer::new);
        event.registerEntityRenderer(ModEntities.LOCOMOTIVE.get(), LocomotiveRenderer::new);
        event.registerEntityRenderer(ModEntities.DRONE.get(), DroneRenderer::new);
        event.registerEntityRenderer(ModEntities.TANK.get(), TankRenderer::new);
        event.registerEntityRenderer(ModEntities.FIGHTER_JET.get(), JetRenderer::new);
        event.registerEntityRenderer(ModEntities.ROCKET.get(), RocketRenderer::new);
        event.registerEntityRenderer(ModEntities.AUTO_MINER.get(), AutoMinerRenderer::new);
        event.registerEntityRenderer(ModEntities.DESTROYER_GOLEM.get(), com.tycooncraft.client.mob.BossRenderers.Golem::new);
        event.registerEntityRenderer(ModEntities.BEHEMOTH.get(), com.tycooncraft.client.mob.BossRenderers.Beast::new);
        event.registerEntityRenderer(ModEntities.STORM_WYVERN.get(), com.tycooncraft.client.mob.BossRenderers.Wyvern::new);
        event.registerEntityRenderer(ModEntities.BOSS_ORB.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.6f, true));
        event.registerEntityRenderer(ModEntities.GRENADE.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 0.75f, false));
        event.registerEntityRenderer(ModEntities.SPIDER_SHOT.get(), ctx -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(ctx, 1.3f, true));
        event.registerEntityRenderer(ModEntities.GANGSTER.get(), com.tycooncraft.client.mob.GunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.MERCENARY.get(), com.tycooncraft.client.mob.GunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.OUTLAW.get(), com.tycooncraft.client.mob.GunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.CARTEL_BOSS.get(), com.tycooncraft.client.mob.GunnerRenderer::new);
        event.registerEntityRenderer(ModEntities.MUTANT_SPIDER.get(), com.tycooncraft.client.mob.MutantSpiderRenderer::new);
        event.registerBlockEntityRenderer(com.tycooncraft.registry.ModBlockEntities.TURRET.get(), TurretRenderer::new);
    }

    @SubscribeEvent
    public static void models(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation id : VehicleModels.ALL) event.register(id);
    }

    @SubscribeEvent
    public static void layers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, TycoonCraft.id("vehicle_hud"), (g, delta) -> renderHud(g));
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, TycoonCraft.id("drone_hud"), (g, delta) -> DroneClient.renderHud(g));
        event.registerAboveAll(TycoonCraft.id("flash_overlay"), (g, delta) -> renderFlash(g));
    }

    /** 계기판: 핫바 오른쪽 — 속도, 연료, 내구도 (헬기는 고도) */
    private static void renderHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        Entity v = mc.player.getVehicle();
        if (!(v instanceof FueledVehicle fv)) return;
        Font font = mc.font;
        int w = 124, h = fv.showAltitude() ? 50 : 40;
        int x = g.guiWidth() / 2 + 96, y = g.guiHeight() - h - 3;
        if (x + w > g.guiWidth() - 2) {
            x = g.guiWidth() - w - 2;
            y = g.guiHeight() - h - 26;
        }
        g.fill(x, y, x + w, y + h, 0xB0101418);
        g.fill(x, y, x + w, y + 1, 0xFFD4A63A);
        g.fill(x, y + h - 1, x + w, y + h, 0x80D4A63A);

        g.drawString(font, Component.translatable(fv.hudName()), x + 4, y + 4, 0xFFD4A63A, false);
        int kmh = (int) Math.round(VehicleClientHooks.speedKmh(v));
        String sp = kmh + " km/h";
        g.drawString(font, sp, x + w - 4 - font.width(sp), y + 4, 0xFFFFFFFF, false);

        float fuel = fv.maxFuel() > 0 ? (float) fv.getFuel() / fv.maxFuel() : 0;
        bar(g, font, x + 4, y + 16, w - 8, Component.translatable("hud.tycooncraft.fuel"), fuel,
                fuel < 0.15f ? 0xFFE04030 : 0xFFE0B030);
        float hp = fv.healthFraction();
        bar(g, font, x + 4, y + 27, w - 8, Component.translatable("hud.tycooncraft.hull"), hp,
                hp < 0.3f ? 0xFFE04030 : 0xFF50C878);
        if (fv.showAltitude()) {
            String alt = Component.translatable("hud.tycooncraft.altitude", (int) Math.floor(v.getY())).getString();
            g.drawString(font, alt, x + 4, y + 39, 0xFF9FD8FF, false);
            if (v instanceof com.tycooncraft.vehicle.HelicopterEntity heli && !heli.hasLift()) {
                String sp2 = Component.translatable("hud.tycooncraft.spool", (int) (heli.rotor * 100)).getString();
                g.drawString(font, sp2, x + w - 4 - font.width(sp2), y + 39, 0xFFFFC060, false);
            }
        }
        if (fv.getFuel() <= 0) {
            Component warn = Component.translatable("hud.tycooncraft.no_fuel");
            g.drawString(font, warn, x + (w - font.width(warn)) / 2, y - 11, 0xFFFF5040, true);
        }
    }

    private static void bar(GuiGraphics g, Font font, int x, int y, int w, Component label, float frac, int color) {
        int lw = 30;
        g.drawString(font, label, x, y, 0xFFB0B4BC, false);
        int bx = x + lw, bw = w - lw;
        g.fill(bx, y + 1, bx + bw, y + 7, 0xFF2A2E36);
        g.fill(bx, y + 1, bx + Math.max(0, Math.round(bw * frac)), y + 7, color);
        g.fill(bx, y + 1, bx + Math.max(0, Math.round(bw * frac)), y + 2, 0x40FFFFFF);
    }

    /** 섬광탄: 화면 전체가 하얗게, 마지막 2초 동안 서서히 걷힘 */
    private static void renderFlash(net.minecraft.client.gui.GuiGraphics g) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) return;
        var eff = mc.player.getEffect(com.tycooncraft.registry.ModEffects.FLASHED);
        if (eff == null) return;
        float a = Math.min(1f, eff.getDuration() / 40f);
        int alpha = (int) (255 * a);
        if (alpha <= 2) return;
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), (alpha << 24) | 0xFFFFFF);
    }

    private VehicleClient() {}
}
