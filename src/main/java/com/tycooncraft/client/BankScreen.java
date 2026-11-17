package com.tycooncraft.client;

import com.tycooncraft.economy.Bank;
import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.BankPayloads;
import com.tycooncraft.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 타이쿤 은행 창: [통장] 입출금·거래 내역, [송금], [경매] 둘러보기·입찰·판매 등록·수령함 */
public class BankScreen extends Screen {
    public static final int W = 300, H = 206;
    private static final int BG = 0xF01C2026, PANEL = 0xFF262C34, LINE = 0xFF3E4652, GOLD = 0xFFF2C94C,
            TEXT = 0xFFE6E6E6, DIM = 0xFF9AA3AE, PLUS = 0xFF6FE38F, MINUS = 0xFFFF7A6E, SEL = 0x6049A0FF, HOVER = 0x30FFFFFF;
    private static final int ROWS = 4, ROW_H = 24;

    public static int tab;          // 0 통장, 1 송금, 2 경매
    public static int auctionMode;  // 0 둘러보기, 1 판매 등록
    private static int dur = 1;
    private final Screen parent;
    private int left, top, page, selectedId = -1, selectedSlot = -1, refreshTimer;
    private Component status = Component.empty();
    private boolean statusOk = true;

    private final List<AbstractWidget> accountW = new ArrayList<>(), transferW = new ArrayList<>(),
            browseW = new ArrayList<>(), sellW = new ArrayList<>(), auctionW = new ArrayList<>();
    private final List<Button> tabs = new ArrayList<>(), playerButtons = new ArrayList<>();
    private EditBox amountBox, nameBox, sendBox, bidBox, startBox, buyoutBox;
    private Button claimButton, durButton, browseButton, sellButton, bidButton, buyButton, cancelButton;

    public BankScreen(Screen parent) {
        super(Component.translatable("gui.tycooncraft.bank.title"));
        this.parent = parent;
    }

    // ── 위젯 ──
    private EditBox numberBox(int x, int y, int w, List<AbstractWidget> group, String hint) {
        EditBox b = new EditBox(font, x, y, w, 16, Component.literal(hint));
        b.setMaxLength(13);
        b.setFilter(s -> s.matches("\\d{0,13}"));
        b.setHint(Component.translatable(hint).withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(b);
        group.add(b);
        return b;
    }

    private Button button(String key, int x, int y, int w, List<AbstractWidget> group, Button.OnPress press) {
        Button b = Button.builder(Component.translatable(key), press).bounds(x, y, w, 18).build();
        addRenderableWidget(b);
        if (group != null) group.add(b);
        return b;
    }

    private static long num(EditBox b) {
        try {
            return b.getValue().isEmpty() ? 0 : Long.parseLong(b.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static void add(EditBox b, long v) {
        b.setValue(String.valueOf(Math.min(Bank.MAX_AMOUNT, num(b) + v)));
    }

    private static void send(int op, int id, int x, long a, long b, String s) {
        BankClient.send(new BankPayloads.Action(op, id, x, a, b, s));
    }

    @Override
    protected void init() {
        String keepAmount = amountBox == null ? "" : amountBox.getValue(), keepName = nameBox == null ? "" : nameBox.getValue(),
                keepSend = sendBox == null ? "" : sendBox.getValue(), keepStart = startBox == null ? "" : startBox.getValue(),
                keepBuy = buyoutBox == null ? "" : buyoutBox.getValue();
        for (List<AbstractWidget> l : List.of(accountW, transferW, browseW, sellW, auctionW)) l.clear();
        tabs.clear();
        playerButtons.clear();
        left = (width - W) / 2;
        top = (height - H) / 2;
        int x = left, y = top;

        String[] tabKeys = {"gui.tycooncraft.bank.tab.account", "gui.tycooncraft.bank.tab.transfer", "gui.tycooncraft.bank.tab.auction"};
        for (int i = 0; i < 3; i++) {
            int t = i;
            tabs.add(button(tabKeys[i], x + 112 + i * 62, y + 4, 60, null, b -> {
                tab = t;
                layout();
            }));
        }

        // ── 통장 ──
        amountBox = numberBox(x + 10, y + 52, 132, accountW, "gui.tycooncraft.bank.amount_hint");
        amountBox.setValue(keepAmount);
        button("gui.tycooncraft.bank.p10k", x + 10, y + 72, 32, accountW, b -> add(amountBox, 10_000));
        button("gui.tycooncraft.bank.p100k", x + 44, y + 72, 34, accountW, b -> add(amountBox, 100_000));
        button("gui.tycooncraft.bank.p1m", x + 80, y + 72, 38, accountW, b -> add(amountBox, 1_000_000));
        button("gui.tycooncraft.bank.clear", x + 120, y + 72, 22, accountW, b -> amountBox.setValue(""));
        button("gui.tycooncraft.bank.deposit", x + 10, y + 96, 65, accountW, b -> {
            if (num(amountBox) > 0) send(BankPayloads.OP_DEPOSIT, 0, 0, num(amountBox), 0, "");
        });
        button("gui.tycooncraft.bank.withdraw", x + 77, y + 96, 65, accountW, b -> {
            if (num(amountBox) > 0) send(BankPayloads.OP_WITHDRAW, 0, 0, num(amountBox), 0, "");
        });
        button("gui.tycooncraft.bank.deposit_all", x + 10, y + 118, 65, accountW, b -> send(BankPayloads.OP_DEPOSIT, 0, 0, 0, 0, ""));
        button("gui.tycooncraft.bank.withdraw_all", x + 77, y + 118, 65, accountW, b -> send(BankPayloads.OP_WITHDRAW, 0, 0, 0, 0, ""));

        // ── 송금 ──
        nameBox = new EditBox(font, x + 10, y + 50, 140, 16, Component.literal("name"));
        nameBox.setMaxLength(16);
        nameBox.setHint(Component.translatable("gui.tycooncraft.bank.name_hint").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(keepName);
        addRenderableWidget(nameBox);
        transferW.add(nameBox);
        sendBox = numberBox(x + 10, y + 84, 140, transferW, "gui.tycooncraft.bank.amount_hint");
        sendBox.setValue(keepSend);
        button("gui.tycooncraft.bank.p10k", x + 10, y + 104, 34, transferW, b -> add(sendBox, 10_000));
        button("gui.tycooncraft.bank.p100k", x + 46, y + 104, 34, transferW, b -> add(sendBox, 100_000));
        button("gui.tycooncraft.bank.p1m", x + 82, y + 104, 42, transferW, b -> add(sendBox, 1_000_000));
        button("gui.tycooncraft.bank.clear", x + 126, y + 104, 24, transferW, b -> sendBox.setValue(""));
        button("gui.tycooncraft.bank.send", x + 10, y + 146, 140, transferW, b -> {
            if (num(sendBox) > 0 && !nameBox.getValue().isBlank())
                send(BankPayloads.OP_TRANSFER, 0, 0, num(sendBox), 0, nameBox.getValue().trim());
        });
        for (int i = 0; i < 7; i++) {
            Button pb = Button.builder(Component.empty(), b -> nameBox.setValue(b.getMessage().getString()))
                    .bounds(x + 166, y + 50 + i * 19, 124, 17).build();
            addRenderableWidget(pb);
            playerButtons.add(pb);
        }

        // ── 경매 공통 ──
        browseButton = button("gui.tycooncraft.bank.browse", x + 6, y + 26, 70, auctionW, b -> {
            auctionMode = 0;
            layout();
        });
        sellButton = button("gui.tycooncraft.bank.sell", x + 78, y + 26, 70, auctionW, b -> {
            auctionMode = 1;
            layout();
        });
        claimButton = button("gui.tycooncraft.bank.claim", x + 214, y + 26, 80, auctionW, b -> send(BankPayloads.OP_CLAIM, 0, 0, 0, 0, ""));

        // ── 경매: 둘러보기 ──
        bidBox = numberBox(x + 6, y + 172, 76, browseW, "gui.tycooncraft.bank.bid_hint");
        bidButton = button("gui.tycooncraft.bank.bid", x + 84, y + 171, 44, browseW, b -> {
            if (selectedId >= 0 && num(bidBox) > 0) send(BankPayloads.OP_BID, selectedId, 0, num(bidBox), 0, "");
        });
        buyButton = button("gui.tycooncraft.bank.buyout", x + 130, y + 171, 58, browseW, b -> {
            if (selectedId >= 0) send(BankPayloads.OP_BUYOUT, selectedId, 0, 0, 0, "");
        });
        cancelButton = button("gui.tycooncraft.bank.cancel", x + 190, y + 171, 58, browseW, b -> {
            if (selectedId >= 0) send(BankPayloads.OP_CANCEL, selectedId, 0, 0, 0, "");
        });
        button("gui.tycooncraft.bank.prev", x + 252, y + 171, 20, browseW, b -> page = Math.max(0, page - 1));
        button("gui.tycooncraft.bank.next", x + 274, y + 171, 20, browseW, b -> page = Math.min(pages() - 1, page + 1));

        // ── 경매: 판매 등록 ──
        startBox = numberBox(x + 178, y + 62, 116, sellW, "gui.tycooncraft.bank.start_hint");
        startBox.setValue(keepStart);
        buyoutBox = numberBox(x + 178, y + 92, 116, sellW, "gui.tycooncraft.bank.buyout_hint");
        buyoutBox.setValue(keepBuy);
        durButton = button("gui.tycooncraft.bank.dur" + dur, x + 178, y + 112, 116, sellW, b -> {
            dur = (dur + 1) % Bank.DURATIONS.length;
            b.setMessage(Component.translatable("gui.tycooncraft.bank.dur_label", Component.translatable("gui.tycooncraft.bank.dur" + dur)));
        });
        durButton.setMessage(Component.translatable("gui.tycooncraft.bank.dur_label", Component.translatable("gui.tycooncraft.bank.dur" + dur)));
        button("gui.tycooncraft.bank.list", x + 178, y + 150, 116, sellW, b -> {
            if (selectedSlot >= 0 && num(startBox) > 0) {
                send(BankPayloads.OP_LIST, selectedSlot, dur, num(startBox), num(buyoutBox), "");
                selectedSlot = -1;
            }
        });

        if (BankClient.last != null) onSync(BankClient.last);
        layout();
    }

    private void layout() {
        for (AbstractWidget w : accountW) w.visible = tab == 0;
        for (AbstractWidget w : transferW) w.visible = tab == 1;
        for (AbstractWidget w : auctionW) w.visible = tab == 2;
        for (AbstractWidget w : browseW) w.visible = tab == 2 && auctionMode == 0;
        for (AbstractWidget w : sellW) w.visible = tab == 2 && auctionMode == 1;
        for (int i = 0; i < tabs.size(); i++) tabs.get(i).active = tab != i;
        browseButton.active = auctionMode != 0;
        sellButton.active = auctionMode != 1;
        updatePlayers();
        updateAuctionButtons();
    }

    private void updatePlayers() {
        List<String> names = new ArrayList<>();
        if (minecraft != null && minecraft.getConnection() != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().getName();
                if (minecraft.player == null || !n.equals(minecraft.player.getGameProfile().getName())) names.add(n);
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        for (int i = 0; i < playerButtons.size(); i++) {
            Button b = playerButtons.get(i);
            b.visible = tab == 1 && i < names.size();
            if (i < names.size()) b.setMessage(Component.literal(names.get(i)));
        }
    }

    private List<BankPayloads.View> views() {
        return BankClient.last == null ? List.of() : BankClient.last.auctions();
    }

    private int pages() {
        return Math.max(1, (views().size() + ROWS - 1) / ROWS);
    }

    private BankPayloads.View selected() {
        for (BankPayloads.View v : views()) if (v.id() == selectedId) return v;
        return null;
    }

    private void updateAuctionButtons() {
        BankPayloads.View v = selected();
        if (v == null) selectedId = -1;
        bidButton.active = v != null && !v.mine();
        buyButton.active = v != null && !v.mine() && v.buyout() > 0;
        cancelButton.active = v != null && v.mine() && v.bid() <= 0;
        int claims = BankClient.last == null ? 0 : BankClient.last.claims();
        claimButton.setMessage(Component.translatable("gui.tycooncraft.bank.claim_n", claims));
        claimButton.active = claims > 0;
    }

    public void onSync(BankPayloads.Sync s) {
        if (!s.msg().getString().isEmpty()) {
            status = s.msg();
            statusOk = s.ok();
        }
        page = Math.min(page, pages() - 1);
        if (claimButton != null) updateAuctionButtons();
    }

    @Override
    public void tick() {
        super.tick();
        if (++refreshTimer >= 40) {
            refreshTimer = 0;
            BankClient.send(BankPayloads.Action.of(BankPayloads.OP_REFRESH));
        }
        if (tab == 1 && refreshTimer % 20 == 0) updatePlayers();
    }

    // ── 그리기 ──
    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        int x = left, y = top;
        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF0B0D10);
        g.fill(x, y, x + W, y + H, BG);
        g.fill(x, y, x + W, y + 24, PANEL);
        g.fill(x, y + 24, x + W, y + 25, GOLD);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);
        int x = left, y = top;
        g.renderItem(new ItemStack(com.tycooncraft.registry.ModItems.GOLD_BAR_1M.get()), x + 5, y + 4);
        Button at = tabs.get(tab);
        g.fill(at.getX() + 2, at.getY() + at.getHeight(), at.getX() + at.getWidth() - 2, at.getY() + at.getHeight() + 2, GOLD);
        if (tab == 2) {
            Button am = auctionMode == 0 ? browseButton : sellButton;
            g.fill(am.getX() + 2, am.getY() + am.getHeight(), am.getX() + am.getWidth() - 2, am.getY() + am.getHeight() + 1, GOLD);
        }
        g.drawString(font, title, x + 24, y + 8, GOLD, false);
        long balance = BankClient.last == null ? 0 : BankClient.last.balance();
        long wallet = minecraft != null && minecraft.player != null ? Coins.wallet(minecraft.player) : 0;
        ItemStack hover = ItemStack.EMPTY;
        switch (tab) {
            case 0 -> drawAccount(g, x, y, balance, wallet);
            case 1 -> drawTransfer(g, x, y, balance);
            default -> hover = auctionMode == 0 ? drawBrowse(g, x, y, mx, my, balance) : drawSell(g, x, y, mx, my, balance);
        }
        if (!status.getString().isEmpty()) {
            g.fill(x, y + H - 14, x + W, y + H - 13, LINE);
            g.drawCenteredString(font, status, x + W / 2, y + H - 10, statusOk ? PLUS : MINUS);
        }
        if (!hover.isEmpty()) g.renderTooltip(font, hover, mx, my);
    }

    private void money(GuiGraphics g, Component label, long v, int x, int y, int w, int color) {
        g.drawString(font, label, x, y, DIM, false);
        String s = "₩" + Coins.format(v);
        g.drawString(font, s, x + w - font.width(s), y, color, false);
    }

    private void drawAccount(GuiGraphics g, int x, int y, long balance, long wallet) {
        money(g, Component.translatable("gui.tycooncraft.bank.balance"), balance, x + 10, y + 30, 132, GOLD);
        money(g, Component.translatable("gui.tycooncraft.bank.wallet"), wallet, x + 10, y + 40, 132, TEXT);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.interest"), x + 10, y + 140, DIM, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.interest_cap", Coins.format(Bank.INTEREST_CAP)), x + 10, y + 150, DIM, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.denoms"), x + 10, y + 160, DIM, false);
        // 화폐 단위 아이콘
        int i = 0;
        for (var c : Coins.denominations()) {
            g.renderItem(new ItemStack(c), x + 8 + (Coins.denominations().size() - 1 - i) * 13, y + 170);
            i++;
        }
        // 거래 내역
        int lx = x + 152;
        g.fill(lx - 4, y + 28, lx - 3, y + 186, LINE);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.history"), lx, y + 30, GOLD, false);
        List<BankData.LogEntry> log = BankClient.last == null ? List.of() : BankClient.last.log();
        if (log.isEmpty()) g.drawString(font, Component.translatable("gui.tycooncraft.bank.no_history"), lx, y + 44, DIM, false);
        for (int k = 0; k < Math.min(12, log.size()); k++) {
            BankData.LogEntry e = log.get(k);
            boolean in = switch (e.type()) {
                case "deposit", "recv", "interest", "refund", "sold" -> true;
                default -> false;
            };
            Component what = Component.translatable("gui.tycooncraft.bank.log." + e.type(), e.other());
            String name = font.plainSubstrByWidth(what.getString(), 76);
            int ly = y + 43 + k * 12;
            g.drawString(font, name, lx, ly, TEXT, false);
            String amt = (in ? "+" : "-") + Coins.format(e.amount());
            g.drawString(font, amt, x + W - 6 - font.width(amt), ly, in ? PLUS : MINUS, false);
        }
    }

    private void drawTransfer(GuiGraphics g, int x, int y, long balance) {
        money(g, Component.translatable("gui.tycooncraft.bank.balance"), balance, x + 10, y + 30, 140, GOLD);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.to"), x + 10, y + 41, DIM, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.send_amount"), x + 10, y + 74, DIM, false);
        long a = num(sendBox);
        long fee = a > 0 ? Bank.transferFee(a) : 0;
        money(g, Component.translatable("gui.tycooncraft.bank.fee"), fee, x + 10, y + 126, 140, MINUS);
        money(g, Component.translatable("gui.tycooncraft.bank.total"), a + fee, x + 10, y + 136, 140, a + fee > balance ? MINUS : TEXT);
        g.fill(x + 158, y + 28, x + 159, y + 186, LINE);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.online"), x + 166, y + 36, GOLD, false);
        if (playerButtons.stream().noneMatch(b -> b.visible))
            g.drawString(font, Component.translatable("gui.tycooncraft.bank.nobody"), x + 166, y + 52, DIM, false);
    }

    private static String time(long ms) {
        long s = ms / 1000;
        if (s >= 3600) return Component.translatable("gui.tycooncraft.bank.time_hm", s / 3600, (s % 3600) / 60).getString();
        return Component.translatable("gui.tycooncraft.bank.time_ms", s / 60, s % 60).getString();
    }

    private ItemStack drawBrowse(GuiGraphics g, int x, int y, int mx, int my, long balance) {
        String bal = "₩" + Coins.format(balance);
        g.drawString(font, bal, x + 210 - font.width(bal), y + 31, GOLD, false);
        List<BankPayloads.View> list = views();
        long elapsed = System.currentTimeMillis() - BankClient.lastAt;
        ItemStack hover = ItemStack.EMPTY;
        int ry0 = y + 58;
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.col_item"), x + 30, ry0 - 10, DIM, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.col_price"), x + 170, ry0 - 10, DIM, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.col_time"), x + 250, ry0 - 10, DIM, false);
        if (list.isEmpty()) g.drawCenteredString(font, Component.translatable("gui.tycooncraft.bank.empty_house"), x + W / 2, ry0 + 40, DIM);
        for (int r = 0; r < ROWS; r++) {
            int idx = page * ROWS + r;
            if (idx >= list.size()) break;
            BankPayloads.View v = list.get(idx);
            int ry = ry0 + r * ROW_H;
            g.fill(x + 6, ry, x + W - 6, ry + ROW_H - 2, PANEL);
            if (v.id() == selectedId) g.fill(x + 6, ry, x + W - 6, ry + ROW_H - 2, SEL);
            else if (mx >= x + 6 && mx < x + W - 6 && my >= ry && my < ry + ROW_H - 2) g.fill(x + 6, ry, x + W - 6, ry + ROW_H - 2, HOVER);
            g.renderItem(v.item(), x + 9, ry + 3);
            g.renderItemDecorations(font, v.item(), x + 9, ry + 3);
            if (mx >= x + 9 && mx < x + 25 && my >= ry + 3 && my < ry + 19) hover = v.item();
            String name = font.plainSubstrByWidth(v.item().getHoverName().getString(), 132);
            g.drawString(font, name, x + 30, ry + 3, v.mine() ? GOLD : TEXT, false);
            Component who = Component.translatable(v.mine() ? "gui.tycooncraft.bank.my_listing" : "gui.tycooncraft.bank.seller", v.seller());
            g.drawString(font, font.plainSubstrByWidth(who.getString(), 132), x + 30, ry + 13, DIM, false);
            String price = "₩" + Coins.format(v.bid() > 0 ? v.bid() : v.start());
            int pc = v.leading() ? PLUS : v.bid() > 0 ? TEXT : DIM;
            g.drawString(font, price, x + 170, ry + 3, pc, false);
            String sub = v.buyout() > 0 ? Component.translatable("gui.tycooncraft.bank.buyout_short", Coins.format(v.buyout())).getString()
                    : Component.translatable(v.bid() > 0 ? "gui.tycooncraft.bank.bids_on" : "gui.tycooncraft.bank.no_bids").getString();
            g.drawString(font, font.plainSubstrByWidth(sub, 78), x + 170, ry + 13, DIM, false);
            g.drawString(font, time(Math.max(0, v.remainMs() - elapsed)), x + 250, ry + 8, v.remainMs() - elapsed < 60_000 ? MINUS : TEXT, false);
        }
        g.drawCenteredString(font, (page + 1) + "/" + pages(), x + 273, y + 162, DIM);
        BankPayloads.View sel = selected();
        if (sel != null && !sel.mine()) {
            String hint = Component.translatable("gui.tycooncraft.bank.min_bid", Coins.format(sel.bid() <= 0 ? sel.start() : sel.bid() + Math.max(1, sel.bid() * 5 / 100))).getString();
            g.drawString(font, hint, x + 6, y + 162, DIM, false);
        }
        return hover;
    }

    private ItemStack drawSell(GuiGraphics g, int x, int y, int mx, int my, long balance) {
        ItemStack hover = ItemStack.EMPTY;
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.pick_item"), x + 8, y + 50, DIM, false);
        if (minecraft == null || minecraft.player == null) return hover;
        var items = minecraft.player.getInventory().items;
        for (int i = 0; i < 36; i++) {
            int[] p = slotPos(i);
            int sx = x + p[0], sy = y + p[1];
            g.fill(sx, sy, sx + 18, sy + 18, 0xFF8B8B8B);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF373737);
            if (i == selectedSlot) g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xFF2F6FBF);
            ItemStack st = items.get(i);
            if (st.isEmpty()) continue;
            g.renderItem(st, sx + 1, sy + 1);
            g.renderItemDecorations(font, st, sx + 1, sy + 1);
            if (Coins.isCoin(st)) g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0x90202020);
            if (mx >= sx && mx < sx + 18 && my >= sy && my < sy + 18) {
                hover = st;
                g.fill(sx + 1, sy + 1, sx + 17, sy + 17, HOVER);
            }
        }
        g.fill(x + 170, y + 48, x + 171, y + 186, LINE);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.start"), x + 178, y + 52, DIM, false);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.buyout_opt"), x + 178, y + 82, DIM, false);
        long start = num(startBox);
        money(g, Component.translatable("gui.tycooncraft.bank.list_fee"), start > 0 ? Bank.listFee(start) : 0, x + 178, y + 134, 116, MINUS);
        g.drawString(font, Component.translatable("gui.tycooncraft.bank.sale_cut"), x + 178, y + 172, DIM, false);
        money(g, Component.translatable("gui.tycooncraft.bank.balance_short"), balance, x + 8, y + 172, 156, GOLD);
        if (selectedSlot >= 0 && selectedSlot < 36 && !items.get(selectedSlot).isEmpty()) {
            ItemStack st = items.get(selectedSlot);
            g.drawString(font, font.plainSubstrByWidth(st.getHoverName().getString() + " x" + st.getCount(), 160), x + 8, y + 160, TEXT, false);
        }
        return hover;
    }

    /** 가방 3줄(9~35) 위, 핫바(0~8) 아래 */
    private static int[] slotPos(int i) {
        int row = i < 9 ? 3 : (i - 9) / 9, col = i % 9;
        return new int[]{8 + col * 18, 62 + row * 18 + (i < 9 ? 4 : 0)};
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (super.mouseClicked(mx, my, button)) return true;
        if (tab != 2) return false;
        if (auctionMode == 0) {
            for (int r = 0; r < ROWS; r++) {
                int idx = page * ROWS + r;
                int ry = top + 58 + r * ROW_H;
                if (idx < views().size() && mx >= left + 6 && mx < left + W - 6 && my >= ry && my < ry + ROW_H - 2) {
                    BankPayloads.View v = views().get(idx);
                    selectedId = v.id();
                    if (!v.mine()) bidBox.setValue(String.valueOf(v.bid() <= 0 ? v.start() : v.bid() + Math.max(1, v.bid() * 5 / 100)));
                    updateAuctionButtons();
                    return true;
                }
            }
        } else if (minecraft != null && minecraft.player != null) {
            for (int i = 0; i < 36; i++) {
                int[] p = slotPos(i);
                if (mx >= left + p[0] && mx < left + p[0] + 18 && my >= top + p[1] && my < top + p[1] + 18) {
                    ItemStack st = minecraft.player.getInventory().items.get(i);
                    selectedSlot = st.isEmpty() || Coins.isCoin(st) ? -1 : i;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (tab == 2 && auctionMode == 0) {
            page = (int) Math.max(0, Math.min(pages() - 1, page - Math.signum(sy)));
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        if (minecraft != null && minecraft.options.keyInventory.matches(key, scan) && !(getFocused() instanceof EditBox)) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
    }

    /** 자동 시험용: 탭 전환 + 예시 입력 */
    public void demo(int t, int mode) {
        tab = t;
        auctionMode = mode;
        if (t == 0) amountBox.setValue("250000");
        if (t == 1) {
            nameBox.setValue("Bob");
            sendBox.setValue("50000");
        }
        if (t == 2 && mode == 0 && !views().isEmpty()) {
            selectedId = views().get(0).id();
            bidBox.setValue(String.valueOf(views().get(0).bid() <= 0 ? views().get(0).start() : views().get(0).bid() + Math.max(1, views().get(0).bid() * 5 / 100)));
        }
        if (t == 2 && mode == 1) {
            selectedSlot = 19;
            startBox.setValue("20000");
            buyoutBox.setValue("45000");
        }
        layout();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
