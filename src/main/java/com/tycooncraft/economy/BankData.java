package com.tycooncraft.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 서버 전체 은행: 플레이어 계좌(잔액·거래 내역), 경매 목록, 경매 수령함 */
public class BankData extends SavedData {
    private static final String NAME = "tycooncraft_bank";
    public static final int LOG_KEEP = 30;

    public static final class Account {
        public String name = "";
        public long balance;
        public final ArrayDeque<LogEntry> log = new ArrayDeque<>();
    }

    /** type: deposit, withdraw, send, recv, fee, interest, list_fee, bid, refund, buy, sold */
    public record LogEntry(String type, long amount, String other, long time) {}

    public static final class Auction {
        public int id;
        public UUID seller;
        public String sellerName = "";
        public ItemStack item = ItemStack.EMPTY;
        public long start, buyout, bid, end;
        public UUID bidder;
        public String bidderName = "";
    }

    public final Map<UUID, Account> accounts = new HashMap<>();
    public final List<Auction> auctions = new ArrayList<>();
    public final Map<UUID, List<ItemStack>> claims = new HashMap<>();
    public int nextId = 1;
    public long interestTicks;

    public static BankData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(BankData::new, BankData::load, null), NAME);
    }

    public Account account(UUID id) {
        return accounts.computeIfAbsent(id, k -> new Account());
    }

    public UUID findByName(String name) {
        for (var e : accounts.entrySet()) if (e.getValue().name.equalsIgnoreCase(name)) return e.getKey();
        return null;
    }

    public void log(UUID id, String type, long amount, String other) {
        Account a = account(id);
        a.log.addFirst(new LogEntry(type, amount, other == null ? "" : other, System.currentTimeMillis()));
        while (a.log.size() > LOG_KEEP) a.log.removeLast();
        setDirty();
    }

    public Auction auction(int id) {
        for (Auction a : auctions) if (a.id == id) return a;
        return null;
    }

    public void addClaim(UUID id, ItemStack stack) {
        if (stack.isEmpty()) return;
        claims.computeIfAbsent(id, k -> new ArrayList<>()).add(stack);
        setDirty();
    }

    public int claimCount(UUID id) {
        return claims.getOrDefault(id, List.of()).size();
    }

    // ── 저장 ──
    private static BankData load(CompoundTag tag, HolderLookup.Provider reg) {
        BankData d = new BankData();
        d.nextId = Math.max(1, tag.getInt("nextId"));
        d.interestTicks = tag.getLong("interestTicks");
        CompoundTag acc = tag.getCompound("accounts");
        for (String key : acc.getAllKeys()) {
            CompoundTag a = acc.getCompound(key);
            Account account = new Account();
            account.name = a.getString("name");
            account.balance = a.getLong("balance");
            for (Tag t : a.getList("log", Tag.TAG_COMPOUND)) {
                CompoundTag l = (CompoundTag) t;
                account.log.addLast(new LogEntry(l.getString("t"), l.getLong("a"), l.getString("o"), l.getLong("w")));
            }
            d.accounts.put(UUID.fromString(key), account);
        }
        for (Tag t : tag.getList("auctions", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Auction a = new Auction();
            a.id = c.getInt("id");
            a.seller = c.getUUID("seller");
            a.sellerName = c.getString("sellerName");
            a.item = ItemStack.parseOptional(reg, c.getCompound("item"));
            a.start = c.getLong("start");
            a.buyout = c.getLong("buyout");
            a.bid = c.getLong("bid");
            a.end = c.getLong("end");
            if (c.hasUUID("bidder")) a.bidder = c.getUUID("bidder");
            a.bidderName = c.getString("bidderName");
            if (!a.item.isEmpty()) d.auctions.add(a);
        }
        CompoundTag cl = tag.getCompound("claims");
        for (String key : cl.getAllKeys()) {
            List<ItemStack> list = new ArrayList<>();
            for (Tag t : cl.getList(key, Tag.TAG_COMPOUND)) {
                ItemStack s = ItemStack.parseOptional(reg, (CompoundTag) t);
                if (!s.isEmpty()) list.add(s);
            }
            if (!list.isEmpty()) d.claims.put(UUID.fromString(key), list);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        tag.putInt("nextId", nextId);
        tag.putLong("interestTicks", interestTicks);
        CompoundTag acc = new CompoundTag();
        accounts.forEach((id, a) -> {
            CompoundTag c = new CompoundTag();
            c.putString("name", a.name);
            c.putLong("balance", a.balance);
            ListTag log = new ListTag();
            for (LogEntry l : a.log) {
                CompoundTag e = new CompoundTag();
                e.putString("t", l.type());
                e.putLong("a", l.amount());
                e.putString("o", l.other());
                e.putLong("w", l.time());
                log.add(e);
            }
            c.put("log", log);
            acc.put(id.toString(), c);
        });
        tag.put("accounts", acc);
        ListTag list = new ListTag();
        for (Auction a : auctions) {
            CompoundTag c = new CompoundTag();
            c.putInt("id", a.id);
            c.putUUID("seller", a.seller);
            c.putString("sellerName", a.sellerName);
            c.put("item", a.item.save(reg));
            c.putLong("start", a.start);
            c.putLong("buyout", a.buyout);
            c.putLong("bid", a.bid);
            c.putLong("end", a.end);
            if (a.bidder != null) c.putUUID("bidder", a.bidder);
            c.putString("bidderName", a.bidderName);
            list.add(c);
        }
        tag.put("auctions", list);
        CompoundTag cl = new CompoundTag();
        claims.forEach((id, items) -> {
            ListTag l = new ListTag();
            for (ItemStack s : items) if (!s.isEmpty()) l.add(s.save(reg));
            if (!l.isEmpty()) cl.put(id.toString(), l);
        });
        tag.put("claims", cl);
        return tag;
    }
}
