package com.tycooncraft.economy;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * 타이쿤 은행: 입출금(저축), 송금, 경매.
 * 돈이 빠져나가는 곳: 송금 수수료 1%, 경매 등록비 1%(최소 10원), 낙찰 수수료 5%.
 * 이자: 하루(20분)마다 잔액의 0.1%, 한 번에 최대 5,000원.
 */
public final class Bank {
    public static final long MAX_AMOUNT = 1_000_000_000_000L;
    public static final int MAX_LISTINGS = 8, MAX_AUCTIONS = 400;
    public static final long[] DURATIONS = {10 * 60_000L, 60 * 60_000L, 6 * 60 * 60_000L, 24 * 60 * 60_000L};
    public static final long INTEREST_PERIOD = 24_000, INTEREST_CAP = 300;
    public static final long SNIPE_GUARD = 30_000;

    public static long transferFee(long amount) {
        return Math.max(1, (amount + 99) / 100);
    }

    public static long listFee(long start) {
        return Math.max(10, (start + 99) / 100);
    }

    public static long saleCut(long price) {
        return price * 5 / 100;
    }

    public static long minBid(BankData.Auction a) {
        return a.bid <= 0 ? a.start : a.bid + Math.max(1, a.bid * 5 / 100);
    }

    /** 결과 메시지 */
    public record Result(boolean ok, Component msg) {
        static Result ok(String key, Object... args) {
            return new Result(true, Component.translatable("bank.tycooncraft." + key, args));
        }

        static Result fail(String key, Object... args) {
            return new Result(false, Component.translatable("bank.tycooncraft." + key, args));
        }
    }

    private static String won(long v) {
        return Coins.format(v);
    }

    // ── 창에서 온 요청 ──
    public static void handle(ServerPlayer p, BankPayloads.Action act) {
        Result r = switch (act.op()) {
            case BankPayloads.OP_DEPOSIT -> deposit(p, act.a());
            case BankPayloads.OP_WITHDRAW -> withdraw(p, act.a());
            case BankPayloads.OP_TRANSFER -> transfer(p, act.s(), act.a());
            case BankPayloads.OP_LIST -> list(p, act.id(), act.a(), act.b(), act.x());
            case BankPayloads.OP_BID -> bid(p.server, p.getUUID(), p.getGameProfile().getName(), act.id(), act.a());
            case BankPayloads.OP_BUYOUT -> buyout(p.server, p.getUUID(), p.getGameProfile().getName(), act.id());
            case BankPayloads.OP_CANCEL -> cancel(p, act.id());
            case BankPayloads.OP_CLAIM -> claim(p);
            case BankPayloads.OP_ALLY_CREATE -> ally(com.tycooncraft.alliance.Alliances.create(p, act.s()));
            case BankPayloads.OP_ALLY_INVITE -> ally(com.tycooncraft.alliance.Alliances.invite(p, act.s()));
            case BankPayloads.OP_ALLY_ACCEPT -> ally(com.tycooncraft.alliance.Alliances.accept(p, act.s()));
            case BankPayloads.OP_ALLY_DECLINE -> ally(com.tycooncraft.alliance.Alliances.decline(p, act.s()));
            case BankPayloads.OP_ALLY_LEAVE -> ally(com.tycooncraft.alliance.Alliances.leave(p));
            case BankPayloads.OP_ALLY_KICK -> ally(com.tycooncraft.alliance.Alliances.kick(p, act.s()));
            case BankPayloads.OP_ALLY_DISBAND -> ally(com.tycooncraft.alliance.Alliances.disband(p));
            default -> null;
        };
        if (r != null && r.ok()) p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4f, 1.6f);
        sync(p, r);
    }

    private static Result ally(com.tycooncraft.alliance.Alliances.Result r) {
        return new Result(r.ok(), r.msg());
    }

    static BankPayloads.Ally allyView(ServerPlayer p) {
        var d = com.tycooncraft.alliance.AllianceData.get(p.server);
        var a = d.of(p.getUUID());
        List<String> inv = new ArrayList<>(d.invites.getOrDefault(p.getUUID(), java.util.Set.of()));
        if (a == null) return new BankPayloads.Ally("", "", false, List.of(), List.of(), inv);
        List<String> mem = new ArrayList<>();
        List<Boolean> on = new ArrayList<>();
        for (UUID m : a.members) {
            mem.add(d.nameOf(m));
            on.add(p.server.getPlayerList().getPlayer(m) != null);
        }
        return new BankPayloads.Ally(a.name, d.nameOf(a.leader), a.leader.equals(p.getUUID()), mem, on, inv);
    }

    public static void sync(ServerPlayer p, Result r) {
        BankData d = BankData.get(p.server);
        BankData.Account acc = touch(d, p);
        List<BankData.LogEntry> log = new ArrayList<>(acc.log).subList(0, Math.min(12, acc.log.size()));
        long now = System.currentTimeMillis();
        List<BankPayloads.View> views = new ArrayList<>();
        d.auctions.stream().sorted(Comparator.comparingLong(a -> a.end)).limit(300).forEach(a -> views.add(new BankPayloads.View(
                a.id, a.item, a.sellerName, a.start, a.bid, a.buyout, Math.max(0, a.end - now), a.bidderName,
                p.getUUID().equals(a.seller), p.getUUID().equals(a.bidder))));
        PacketDistributor.sendToPlayer(p, new BankPayloads.Sync(acc.balance, d.claimCount(p.getUUID()), List.copyOf(log), views,
                r == null ? Component.empty() : r.msg(), r == null || r.ok(), allyView(p)));
    }

    static BankData.Account touch(BankData d, ServerPlayer p) {
        BankData.Account acc = d.account(p.getUUID());
        String name = p.getGameProfile().getName();
        if (!name.equals(acc.name)) {
            acc.name = name;
            d.setDirty();
        }
        return acc;
    }

    // ── 입출금 ──
    public static Result deposit(ServerPlayer p, long amount) {
        BankData d = BankData.get(p.server);
        BankData.Account acc = touch(d, p);
        long wallet = Coins.wallet(p);
        if (wallet <= 0) return Result.fail("no_cash");
        if (amount <= 0) amount = wallet;
        if (amount > wallet) return Result.fail("not_enough_cash", won(wallet));
        long taken = Coins.takeAll(p);
        acc.balance += amount;
        if (taken > amount) Coins.give(p, taken - amount);
        d.log(p.getUUID(), "deposit", amount, "");
        return Result.ok("deposited", won(amount));
    }

    public static Result withdraw(ServerPlayer p, long amount) {
        BankData d = BankData.get(p.server);
        BankData.Account acc = touch(d, p);
        if (acc.balance <= 0) return Result.fail("empty_account");
        if (amount <= 0) amount = acc.balance;
        if (amount > acc.balance) return Result.fail("not_enough_balance", won(acc.balance));
        acc.balance -= amount;
        Coins.give(p, amount);
        d.log(p.getUUID(), "withdraw", amount, "");
        return Result.ok("withdrew", won(amount));
    }

    // ── 송금 ──
    public static Result transfer(ServerPlayer p, String to, long amount) {
        BankData d = BankData.get(p.server);
        touch(d, p);
        return transfer(p.server, p.getUUID(), to, amount);
    }

    public static Result transfer(MinecraftServer server, UUID from, String to, long amount) {
        BankData d = BankData.get(server);
        to = to == null ? "" : to.trim();
        if (to.isEmpty()) return Result.fail("no_target");
        if (amount <= 0 || amount > MAX_AMOUNT) return Result.fail("bad_amount");
        UUID target = d.findByName(to);
        if (target == null) {
            ServerPlayer online = server.getPlayerList().getPlayerByName(to);
            if (online != null) target = touch(d, online) == null ? null : online.getUUID();
        }
        if (target == null) return Result.fail("unknown_player", to);
        if (target.equals(from)) return Result.fail("self_transfer");
        BankData.Account src = d.account(from), dst = d.account(target);
        long fee = transferFee(amount);
        if (src.balance < amount + fee) return Result.fail("not_enough_balance_fee", won(amount + fee), won(src.balance));
        src.balance -= amount + fee;
        dst.balance += amount;
        d.log(from, "send", amount, dst.name);
        d.log(from, "fee", fee, "");
        d.log(target, "recv", amount, src.name);
        ServerPlayer rp = server.getPlayerList().getPlayer(target);
        if (rp != null) rp.sendSystemMessage(Component.translatable("bank.tycooncraft.received", src.name, won(amount)).withStyle(ChatFormatting.GREEN));
        return Result.ok("sent", dst.name, won(amount), won(fee));
    }

    // ── 경매 ──
    public static Result list(ServerPlayer p, int slot, long start, long buyout, int dur) {
        BankData d = BankData.get(p.server);
        BankData.Account acc = touch(d, p);
        var inv = p.getInventory();
        if (slot < 0 || slot >= inv.items.size()) return Result.fail("no_item");
        ItemStack stack = inv.items.get(slot);
        if (stack.isEmpty()) return Result.fail("no_item");
        if (Coins.isCoin(stack)) return Result.fail("no_coins");
        if (start <= 0 || start > MAX_AMOUNT) return Result.fail("bad_amount");
        if (buyout != 0 && (buyout < start || buyout > MAX_AMOUNT)) return Result.fail("bad_buyout");
        if (d.auctions.stream().filter(a -> a.seller.equals(p.getUUID())).count() >= MAX_LISTINGS)
            return Result.fail("too_many", MAX_LISTINGS);
        if (d.auctions.size() >= MAX_AUCTIONS) return Result.fail("house_full");
        long fee = listFee(start);
        if (acc.balance < fee) return Result.fail("no_list_fee", won(fee));
        acc.balance -= fee;
        d.log(p.getUUID(), "list_fee", fee, stack.getHoverName().getString());
        BankData.Auction a = new BankData.Auction();
        a.id = d.nextId++;
        a.seller = p.getUUID();
        a.sellerName = acc.name;
        a.item = stack.copy();
        a.start = start;
        a.buyout = buyout;
        a.end = System.currentTimeMillis() + DURATIONS[Math.floorMod(dur, DURATIONS.length)];
        inv.items.set(slot, ItemStack.EMPTY);
        d.auctions.add(a);
        d.setDirty();
        return Result.ok("listed", a.item.getHoverName(), a.item.getCount(), won(start), won(fee));
    }

    /** 앞선 입찰자에게 돈을 돌려줌 */
    private static void refund(MinecraftServer server, BankData d, BankData.Auction a, UUID except) {
        if (a.bidder == null || a.bid <= 0) return;
        d.account(a.bidder).balance += a.bid;
        d.log(a.bidder, "refund", a.bid, a.item.getHoverName().getString());
        if (!a.bidder.equals(except)) {
            ServerPlayer op = server.getPlayerList().getPlayer(a.bidder);
            if (op != null) op.sendSystemMessage(Component.translatable("bank.tycooncraft.outbid", a.item.getHoverName(), won(a.bid)).withStyle(ChatFormatting.GOLD));
        }
    }

    public static Result bid(MinecraftServer server, UUID who, String name, int id, long amount) {
        BankData d = BankData.get(server);
        BankData.Auction a = d.auction(id);
        long now = System.currentTimeMillis();
        if (a == null || a.end <= now) return Result.fail("gone");
        if (a.seller.equals(who)) return Result.fail("own_item");
        if (a.buyout > 0 && amount >= a.buyout) return buyout(server, who, name, id);
        long min = minBid(a);
        if (amount < min) return Result.fail("bid_low", won(min));
        BankData.Account acc = d.account(who);
        if (acc.name.isEmpty()) acc.name = name;
        long avail = acc.balance + (who.equals(a.bidder) ? a.bid : 0);
        if (avail < amount) return Result.fail("not_enough_balance", won(acc.balance));
        refund(server, d, a, who);
        acc.balance -= amount;
        d.log(who, "bid", amount, a.item.getHoverName().getString());
        a.bid = amount;
        a.bidder = who;
        a.bidderName = acc.name;
        if (a.end - now < SNIPE_GUARD) a.end = now + SNIPE_GUARD;
        d.setDirty();
        return Result.ok("bid_ok", a.item.getHoverName(), won(amount));
    }

    public static Result buyout(MinecraftServer server, UUID who, String name, int id) {
        BankData d = BankData.get(server);
        BankData.Auction a = d.auction(id);
        if (a == null || a.end <= System.currentTimeMillis()) return Result.fail("gone");
        if (a.seller.equals(who)) return Result.fail("own_item");
        if (a.buyout <= 0) return Result.fail("no_buyout");
        BankData.Account acc = d.account(who);
        if (acc.name.isEmpty()) acc.name = name;
        long avail = acc.balance + (who.equals(a.bidder) ? a.bid : 0);
        if (avail < a.buyout) return Result.fail("not_enough_balance", won(acc.balance));
        refund(server, d, a, who);
        acc.balance -= a.buyout;
        d.log(who, "buy", a.buyout, a.item.getHoverName().getString());
        a.bid = a.buyout;
        a.bidder = who;
        a.bidderName = acc.name;
        settle(server, d, a);
        d.auctions.remove(a);
        d.setDirty();
        return Result.ok("bought", a.item.getHoverName(), won(a.buyout));
    }

    /** 낙찰: 물건은 낙찰자 수령함, 돈(수수료 5% 뺀)은 판매자 계좌. 유찰이면 판매자 수령함으로. */
    static void settle(MinecraftServer server, BankData d, BankData.Auction a) {
        if (a.bidder == null || a.bid <= 0) {
            d.addClaim(a.seller, a.item);
            ServerPlayer sp = server.getPlayerList().getPlayer(a.seller);
            if (sp != null) sp.sendSystemMessage(Component.translatable("bank.tycooncraft.unsold", a.item.getHoverName()).withStyle(ChatFormatting.GRAY));
            return;
        }
        long cut = saleCut(a.bid);
        d.account(a.seller).balance += a.bid - cut;
        d.log(a.seller, "sold", a.bid - cut, a.item.getHoverName().getString());
        d.addClaim(a.bidder, a.item);
        ServerPlayer sp = server.getPlayerList().getPlayer(a.seller);
        if (sp != null) sp.sendSystemMessage(Component.translatable("bank.tycooncraft.sold_msg", a.item.getHoverName(), a.bidderName, won(a.bid - cut)).withStyle(ChatFormatting.GREEN));
        ServerPlayer bp = server.getPlayerList().getPlayer(a.bidder);
        if (bp != null) bp.sendSystemMessage(Component.translatable("bank.tycooncraft.won_msg", a.item.getHoverName(), won(a.bid)).withStyle(ChatFormatting.GREEN));
    }

    public static Result cancel(ServerPlayer p, int id) {
        BankData d = BankData.get(p.server);
        BankData.Auction a = d.auction(id);
        if (a == null) return Result.fail("gone");
        if (!a.seller.equals(p.getUUID())) return Result.fail("not_yours");
        if (a.bidder != null) return Result.fail("has_bids");
        d.auctions.remove(a);
        d.setDirty();
        Coins.giveStack(p, a.item.copy());
        return Result.ok("cancelled", a.item.getHoverName());
    }

    public static Result claim(ServerPlayer p) {
        BankData d = BankData.get(p.server);
        List<ItemStack> list = d.claims.remove(p.getUUID());
        if (list == null || list.isEmpty()) return Result.fail("nothing_to_claim");
        for (ItemStack s : list) Coins.giveStack(p, s);
        d.setDirty();
        return Result.ok("claimed", list.size());
    }

    /** 마감된 경매 정리 */
    public static int expire(MinecraftServer server) {
        BankData d = BankData.get(server);
        long now = System.currentTimeMillis();
        int n = 0;
        for (Iterator<BankData.Auction> it = d.auctions.iterator(); it.hasNext(); ) {
            BankData.Auction a = it.next();
            if (a.end > now) continue;
            it.remove();
            settle(server, d, a);
            n++;
        }
        if (n > 0) d.setDirty();
        return n;
    }

    public static void payInterest(MinecraftServer server) {
        BankData d = BankData.get(server);
        d.accounts.forEach((id, acc) -> {
            long i = Math.min(INTEREST_CAP, acc.balance / 1000);
            if (i <= 0) return;
            acc.balance += i;
            d.log(id, "interest", i, "");
        });
        d.setDirty();
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        expire(server);
        BankData d = BankData.get(server);
        d.interestTicks += 20;
        if (d.interestTicks >= INTEREST_PERIOD) {
            d.interestTicks = 0;
            payInterest(server);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        BankData d = BankData.get(p.server);
        touch(d, p);
        int c = d.claimCount(p.getUUID());
        if (c > 0) p.sendSystemMessage(Component.translatable("bank.tycooncraft.claims_waiting", c).withStyle(ChatFormatting.YELLOW));
    }

    private Bank() {}
}
