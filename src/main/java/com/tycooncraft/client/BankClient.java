package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.economy.BankPayloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 인벤토리 옆 [은행] 버튼 + 서버에서 온 은행 상태 보관 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class BankClient {
    private static final ResourceLocation ICON = TycoonCraft.id("textures/gui/bank_icon.png");
    public static BankPayloads.Sync last;
    public static long lastAt;
    private static Button button;

    public static void onSync(BankPayloads.Sync sync) {
        last = sync;
        lastAt = System.currentTimeMillis();
        if (Minecraft.getInstance().screen instanceof BankScreen s) s.onSync(sync);
    }

    public static void send(BankPayloads.Action a) {
        PacketDistributor.sendToServer(a);
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new BankScreen(isInventory(mc.screen) ? mc.screen : null));
        send(BankPayloads.Action.of(BankPayloads.OP_REFRESH));
    }

    static final class BankButton extends Button {
        BankButton(int x, int y) {
            super(x, y, 20, 20, Component.empty(), b -> open(), DEFAULT_NARRATION);
            setTooltip(Tooltip.create(Component.translatable("gui.tycooncraft.bank.button")));
        }

        @Override
        protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float pt) {
            super.renderWidget(g, mouseX, mouseY, pt);
            g.blit(ICON, getX() + 2, getY() + 2, 0, 0, 16, 16, 16, 16);
        }
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!isInventory(event.getScreen())) return;
        var inv = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) event.getScreen();
        button = new BankButton(inv.getGuiLeft() + inv.getXSize() + 2, inv.getGuiTop() + 4);
        event.addListener(button);
    }

    /** 레시피북을 열면 인벤토리가 옆으로 밀리므로 매 프레임 위치를 맞춤 */
    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Pre event) {
        if (button != null && isInventory(event.getScreen())) {
            var inv = (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>) event.getScreen();
            button.setX(inv.getGuiLeft() + inv.getXSize() + 2);
            button.setY(inv.getGuiTop() + (event.getScreen() instanceof InventoryScreen ? 4 : 32));
        }
    }

    static boolean isInventory(net.minecraft.client.gui.screens.Screen s) {
        return s instanceof InventoryScreen || s instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
    }

    private BankClient() {}
}
