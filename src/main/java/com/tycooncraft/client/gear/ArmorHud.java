package com.tycooncraft.client.gear;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tycooncraft.TycoonCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** v0.31: 방어력 21~40은 같은 줄 위에 금빛 갑옷 칸을 겹쳐 그림 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ArmorHud {
    private static final ResourceLocation FULL = ResourceLocation.withDefaultNamespace("hud/armor_full");
    private static final ResourceLocation HALF = ResourceLocation.withDefaultNamespace("hud/armor_half");

    @SubscribeEvent
    public static void onLayer(RenderGuiLayerEvent.Post e) {
        if (!e.getName().equals(VanillaGuiLayers.ARMOR_LEVEL)) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.gameMode == null || !mc.gameMode.canHurtPlayer()) return;
        int extra = mc.player.getArmorValue() - 20;
        if (extra <= 0) return;
        var g = e.getGuiGraphics();
        int x0 = g.guiWidth() / 2 - 91, y = g.guiHeight() - mc.gui.leftHeight + 10;
        RenderSystem.enableBlend();
        g.setColor(1f, 0.78f, 0.25f, 1f);
        for (int k = 0; k < 10; k++) {
            if (k * 2 + 1 < extra) g.blitSprite(FULL, x0 + k * 8, y, 9, 9);
            else if (k * 2 + 1 == extra) g.blitSprite(HALF, x0 + k * 8, y, 9, 9);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();
    }

    private ArmorHud() {}
}
