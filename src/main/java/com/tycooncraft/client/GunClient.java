package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/** 총: 3인칭 조준 자세, 저격총 스코프 화면 */
@EventBusSubscriber(modid = TycoonCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GunClient {
    private static final ResourceLocation SCOPE = TycoonCraft.id("textures/gui/scope.png");

    /** 총을 들면 두 팔을 앞으로 들어 조준하는 자세 */
    @SubscribeEvent
    public static void registerExtensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions aim = new IClientItemExtensions() {
            @Override
            public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                return GunPoses.poseFor(stack);
            }
        };
        event.registerItem(aim, ModItems.PISTOL.get(), ModItems.SMG.get(), ModItems.RIFLE.get(),
                ModItems.SHOTGUN.get(), ModItems.SNIPER.get(), ModItems.VSS.get(), ModItems.DUAL_PISTOLS.get(), ModItems.RPG.get(), ModItems.JUDGE.get());
        event.registerItem(new IClientItemExtensions() {
            private net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer r;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (r == null) r = new com.tycooncraft.client.gear.PoliceShieldRenderer();
                return r;
            }
        }, ModItems.POLICE_SHIELD.get());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, TycoonCraft.id("sniper_scope"), (g, delta) -> renderScope(g));
    }

    public static boolean isScoping() {
        Player player = Minecraft.getInstance().player;
        return player != null && player.isUsingItem()
                && player.getUseItem().getItem() instanceof GunItem gun && gun.isScoped(player.getUseItem())
                && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    private static void renderScope(GuiGraphics g) {
        if (!isScoping()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int size = h;
        int x = (w - size) / 2;
        RenderSystem.enableBlend();
        g.blit(SCOPE, x, 0, size, size, 0, 0, 512, 512, 512, 512);
        g.fill(0, 0, x, h, 0xFF000000);
        g.fill(x + size, 0, w, h, 0xFF000000);
    }

    private GunClient() {}
}
