package com.tycooncraft.blackmarket;

import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/**
 * v0.28 블랙마켓 카지노.
 *  슬롯머신: 릴 3개 (체리·레몬·종·다이아·7·해골). 해골이 하나라도 나오면 꽝. 환급률 약 92%
 *    3개 일치: 체리 ×5 · 레몬 ×8 · 종 ×15 · 다이아 ×40 · 7 ×100 / 체리 2개 ×2 / 체리 1개 ×0.5
 *  룰렛: 0~36 (빨강 18·검정 18·초록 0). 빨강/검정 적중 ×2, 초록 적중 ×36
 * 베팅 한도는 평판 단계에 따라 (₩1만 → ₩50만). 돈은 지갑 → 은행 순으로 걷고, 당첨금은 지갑(현금)으로.
 */
public final class Casino {
    public static final int SLOTS = 0, ROULETTE = 1;
    public static final int[] WEIGHTS = {6, 5, 4, 2, 1, 2};
    public static final int[] TRIPLE = {5, 8, 15, 40, 100};
    public static final int RED = 0, BLACK = 1, GREEN = 2;
    private static final java.util.Set<Integer> REDS = java.util.Set.of(1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);
    public static long wagered, paidOut;
    public static int spins;

    public static int symbol(RandomSource r) {
        int t = 0;
        for (int w : WEIGHTS) t += w;
        int k = r.nextInt(t);
        for (int i = 0; i < WEIGHTS.length; i++) {
            if (k < WEIGHTS[i]) return i;
            k -= WEIGHTS[i];
        }
        return 0;
    }

    /** 배당 (걸은 돈의 몇 배를 돌려주나) */
    public static double slotPay(int a, int b, int c) {
        if (a == 5 || b == 5 || c == 5) return 0;
        if (a == b && b == c) return TRIPLE[a];
        int ch = (a == 0 ? 1 : 0) + (b == 0 ? 1 : 0) + (c == 0 ? 1 : 0);
        return ch == 2 ? 2 : ch == 1 ? 0.5 : 0;
    }

    public static int color(int n) {
        return n == 0 ? GREEN : REDS.contains(n) ? RED : BLACK;
    }

    public static double roulettePay(int n, int choice) {
        int c = color(n);
        if (c != choice) return 0;
        return c == GREEN ? 36 : 2;
    }

    public static long maxBet(ServerPlayer sp) {
        return Reputation.MAX_BET[Reputation.tier(sp)];
    }

    public static long funds(ServerPlayer sp) {
        return Coins.wallet(sp) + BankData.get(sp.server).account(sp.getUUID()).balance;
    }

    /** 결과: {r0, r1, r2, payout} — 룰렛은 r0 = 숫자. 실패하면 null + 메시지 */
    public static long[] play(ServerPlayer sp, int game, long bet, int choice, Component[] msg) {
        if (bet <= 0) return null;
        if (bet > maxBet(sp)) {
            msg[0] = Component.translatable("message.tycooncraft.casino.limit", Coins.format(maxBet(sp)), Reputation.tierName(Reputation.tier(sp))).withStyle(ChatFormatting.RED);
            return null;
        }
        if (!VaultBlock.pay(sp, bet)) {
            msg[0] = Component.translatable("message.tycooncraft.casino.no_money").withStyle(ChatFormatting.RED);
            return null;
        }
        RandomSource r = sp.getRandom();
        long[] res = new long[4];
        double mult;
        if (game == SLOTS) {
            int a = symbol(r), b = symbol(r), c = symbol(r);
            res[0] = a;
            res[1] = b;
            res[2] = c;
            mult = slotPay(a, b, c);
        } else {
            int n = r.nextInt(37);
            res[0] = n;
            mult = roulettePay(n, choice);
        }
        long win = Math.round(bet * mult);
        res[3] = win;
        if (win > 0) Coins.give(sp, win);
        wagered += bet;
        paidOut += win;
        spins++;
        // 카지노 단골: ₩20,000 걸 때마다 평판 1
        var pd = sp.getPersistentData();
        long acc = pd.getLong("tycooncraft_casino_acc") + bet;
        int rep = (int) (acc / 20_000);
        pd.putLong("tycooncraft_casino_acc", acc % 20_000);
        if (rep > 0) Reputation.add(sp, rep);
        if (win >= bet * 15) sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1f, 1f);
        msg[0] = win > 0 ? Component.translatable("message.tycooncraft.casino.win", Coins.format(win)).withStyle(ChatFormatting.GOLD)
                : Component.translatable("message.tycooncraft.casino.lose").withStyle(ChatFormatting.GRAY);
        return res;
    }

    private Casino() {}
}
