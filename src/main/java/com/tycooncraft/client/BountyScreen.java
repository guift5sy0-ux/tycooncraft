package com.tycooncraft.client;

import com.tycooncraft.blackmarket.BountyPayloads;
import com.tycooncraft.economy.Coins;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** v0.26.0 현상금 게시판: 걸린 현상금 목록 + 이름·금액 입력 */
public class BountyScreen extends Screen {
    private final BountyPayloads.Open data;
    private EditBox name, amount;

    public BountyScreen(BountyPayloads.Open data) {
        super(Component.translatable("gui.tycooncraft.bounty.title"));
        this.data = data;
    }

    public static void open(BountyPayloads.Open p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof BountyScreen old) {
            String n = old.name.getValue(), a = old.amount.getValue();
            BountyScreen s = new BountyScreen(p);
            mc.setScreen(s);
            s.name.setValue(n);
            s.amount.setValue(p.msg().isEmpty() ? a : "");
        } else mc.setScreen(new BountyScreen(p));
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 + 40;
        name = new EditBox(font, cx - 120, y, 110, 18, Component.translatable("gui.tycooncraft.bounty.name"));
        name.setMaxLength(16);
        name.setHint(Component.translatable("gui.tycooncraft.bounty.name"));
        amount = new EditBox(font, cx - 4, y, 70, 18, Component.translatable("gui.tycooncraft.bounty.amount"));
        amount.setFilter(s -> s.matches("\\d{0,12}"));
        amount.setHint(Component.literal("₩"));
        addRenderableWidget(name);
        addRenderableWidget(amount);
        addRenderableWidget(Button.builder(Component.translatable("gui.tycooncraft.bounty.post"), b -> {
            long a = amount.getValue().isEmpty() ? 0 : Long.parseLong(amount.getValue());
            if (!name.getValue().isBlank() && a > 0) PacketDistributor.sendToServer(new BountyPayloads.Post(name.getValue(), a));
        }).bounds(cx + 72, y - 1, 52, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int cx = width / 2, top = height / 2 - 100;
        g.fill(cx - 130, top - 8, cx + 130, height / 2 + 72, 0xD0100808);
        g.renderOutline(cx - 130, top - 8, 260, height / 2 + 80 - top, 0xFFAA2222);
        g.drawCenteredString(font, title, cx, top, 0xFFFF5555);
        g.drawCenteredString(font, Component.translatable("gui.tycooncraft.bounty.hint"), cx, top + 12, 0xFFAAAAAA);
        int y = top + 30;
        if (data.names().isEmpty()) g.drawCenteredString(font, Component.translatable("gui.tycooncraft.bounty.empty"), cx, y + 20, 0xFF888888);
        for (int i = 0; i < data.names().size(); i++) {
            g.drawString(font, (i + 1) + ". " + data.names().get(i), cx - 116, y, 0xFFFFFFFF);
            String a = "₩" + Coins.format(data.amounts().get(i));
            g.drawString(font, a, cx + 116 - font.width(a), y, 0xFFFFD040);
            y += 11;
        }
        g.drawString(font, Component.translatable("gui.tycooncraft.bounty.balance", Coins.format(data.balance())), cx - 120, height / 2 + 28, 0xFF88FF88);
        if (!data.msg().isEmpty()) g.drawCenteredString(font, data.msg(), cx, height / 2 + 62, 0xFFFFAA44);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
