package com.tycooncraft.devtest;

import com.tycooncraft.economy.Bank;
import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.Coins;
import com.tycooncraft.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.UUID;

/** v0.18: 새 화폐(최대 100만원), 은행 입출금·송금·경매 */
public final class BankTest {
    static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000b0b0");
    static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-0000000ca201");

    static String stacks(ServerPlayer sp) {
        StringBuilder b = new StringBuilder();
        for (ItemStack s : sp.getInventory().items)
            if (Coins.isCoin(s)) b.append(s.getCount()).append("x").append(s.getItem().builtInRegistryHolder().key().location().getPath()).append(' ');
        return b.toString().trim();
    }

    static long bal(BankData d, UUID id) {
        return d.account(id).balance;
    }

    static void run(ServerPlayer sp) {
        var server = sp.server;
        BankData d = BankData.get(server);
        sp.getInventory().clearContent();
        Coins.give(sp, 1_234_567);
        System.out.println("[AUTOTEST] v18 coins wallet=" + Coins.wallet(sp) + " stacks=" + stacks(sp)
                + " villager6400=" + java.util.Arrays.toString(villager(6400)));

        long before = bal(d, sp.getUUID());
        var r1 = Bank.deposit(sp, 1_000_000);
        var r2 = Bank.withdraw(sp, 50_000);
        var r3 = Bank.deposit(sp, 99_999_999);
        System.out.println("[AUTOTEST] v18 bank deposit=" + r1.ok() + " withdraw=" + r2.ok() + " overdeposit=" + r3.ok()
                + " balance=" + (bal(d, sp.getUUID()) - before) + " wallet=" + Coins.wallet(sp));

        d.account(BOB).name = "Bob";
        d.account(CAROL).name = "Carol";
        d.account(CAROL).balance = 500_000;
        long b0 = bal(d, sp.getUUID());
        var t1 = Bank.transfer(sp, "bob", 100_000);
        var t2 = Bank.transfer(sp, sp.getGameProfile().getName(), 10);
        var t3 = Bank.transfer(sp, "nobody_here", 10);
        System.out.println("[AUTOTEST] v18 transfer ok=" + t1.ok() + " self=" + t2.ok() + " unknown=" + t3.ok()
                + " paid=" + (b0 - bal(d, sp.getUUID())) + " bob=" + bal(d, BOB));

        // 경매: 다이아 5개 등록 → Bob 입찰 → Carol 더 높게 → 마감
        sp.getInventory().items.set(30, new ItemStack(Items.DIAMOND, 5));
        long b1 = bal(d, sp.getUUID());
        var l1 = Bank.list(sp, 30, 1000, 0, 0);
        int id = d.nextId - 1;
        var own = Bank.bid(server, sp.getUUID(), "me", id, 2000);
        var bb = Bank.bid(server, BOB, "Bob", id, 1200);
        var low = Bank.bid(server, CAROL, "Carol", id, 1210);
        var cb = Bank.bid(server, CAROL, "Carol", id, 1300);
        long bobAfter = bal(d, BOB);
        d.auction(id).end = System.currentTimeMillis() - 1;
        int done = Bank.expire(server);
        System.out.println("[AUTOTEST] v18 auction listed=" + l1.ok() + " slotEmpty=" + sp.getInventory().items.get(30).isEmpty()
                + " ownBid=" + own.ok() + " bob=" + bb.ok() + " lowBid=" + low.ok() + " carol=" + cb.ok()
                + " bobRefunded=" + (bobAfter == 100_000) + " expired=" + done + " sellerGain=" + (bal(d, sp.getUUID()) - b1)
                + " carolClaims=" + d.claimCount(CAROL) + " carolBal=" + bal(d, CAROL));

        // 즉시 구매
        sp.getInventory().items.set(31, new ItemStack(ModItems.RIFLE.get()));
        long b2 = bal(d, sp.getUUID());
        Bank.list(sp, 31, 3000, 8000, 1);
        int id2 = d.nextId - 1;
        var buy = Bank.buyout(server, BOB, "Bob", id2);
        System.out.println("[AUTOTEST] v18 buyout ok=" + buy.ok() + " gone=" + (d.auction(id2) == null) + " sellerGain=" + (bal(d, sp.getUUID()) - b2)
                + " bobClaims=" + d.claimCount(BOB) + " bobBal=" + bal(d, BOB));

        // 유찰 → 수령함 → 받기
        sp.getInventory().items.set(32, new ItemStack(Items.IRON_INGOT, 10));
        Bank.list(sp, 32, 50, 0, 0);
        d.auction(d.nextId - 1).end = 0;
        Bank.expire(server);
        int claims = d.claimCount(sp.getUUID());
        var cl = Bank.claim(sp);
        // 등록 취소
        sp.getInventory().items.set(33, new ItemStack(Items.GOLD_INGOT, 3));
        Bank.list(sp, 33, 100, 0, 2);
        var cancel = Bank.cancel(sp, d.nextId - 1);
        System.out.println("[AUTOTEST] v18 unsold claims=" + claims + " claimed=" + cl.ok() + " ironBack=" + sp.getInventory().contains(new ItemStack(Items.IRON_INGOT))
                + " cancel=" + cancel.ok() + " goldBack=" + sp.getInventory().contains(new ItemStack(Items.GOLD_INGOT)) + " coinList=" + Bank.list(sp, 0, 10, 0, 0).ok());

        long b3 = bal(d, sp.getUUID());
        Bank.payInterest(server);
        System.out.println("[AUTOTEST] v18 interest=" + (bal(d, sp.getUUID()) - b3) + " on=" + b3);

        // 화면용 경매 몇 개
        demo(d, CAROL, "Carol", new ItemStack(ModItems.SNIPER.get()), 250_000, 600_000, 0, 3_000_000);
        demo(d, BOB, "Bob", new ItemStack(Items.NETHERITE_INGOT, 4), 80_000, 0, 95_000, 40_000);
        demo(d, CAROL, "Carol", new ItemStack(Items.ELYTRA), 1_500_000, 3_000_000, 0, 20_000_000);
        sp.getInventory().items.set(18, new ItemStack(ModItems.RIFLE.get()));
        sp.getInventory().items.set(19, new ItemStack(Items.DIAMOND, 32));
        sp.getInventory().items.set(20, new ItemStack(Items.IRON_INGOT, 64));
        sp.getInventory().items.set(21, new ItemStack(ModItems.STEEL_PLATE.get(), 48));
        Bank.list(sp, 20, 9_000, 15_000, 1);
        Bank.bid(server, BOB, "Bob", d.nextId - 1, 9_500);
        d.setDirty();
    }

    static void demo(BankData d, UUID seller, String name, ItemStack item, long start, long buyout, long bid, long ms) {
        BankData.Auction a = new BankData.Auction();
        a.id = d.nextId++;
        a.seller = seller;
        a.sellerName = name;
        a.item = item;
        a.start = start;
        a.buyout = buyout;
        a.bid = bid;
        if (bid > 0) {
            a.bidder = seller.equals(BOB) ? CAROL : BOB;
            a.bidderName = seller.equals(BOB) ? "Carol" : "Bob";
        }
        a.end = System.currentTimeMillis() + ms;
        d.auctions.add(a);
    }

    static String[] villager(int won) {
        ItemStack[] s = com.tycooncraft.economy.VillagerCoins.coinsForTest(won);
        String[] out = new String[s.length];
        for (int i = 0; i < s.length; i++) out[i] = s[i].getCount() + "x" + s[i].getItem().builtInRegistryHolder().key().location().getPath();
        return out;
    }

    private BankTest() {}
}
