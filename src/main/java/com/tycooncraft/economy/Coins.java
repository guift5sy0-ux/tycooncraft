package com.tycooncraft.economy;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** 금액(₩) ↔ 동전 변환 */
public final class Coins {

    /** 큰 단위부터 */
    public static List<CoinItem> denominations() {
        return List.of(ModItems.GOLD_BAR_1M.get(), ModItems.CHECK_100K.get(), ModItems.BILL_50K.get(), ModItems.COIN_DIAMOND.get(),
                ModItems.BILL_5K.get(), ModItems.COIN_EMERALD.get(), ModItems.COIN_SILVER.get(), ModItems.COIN_GOLD.get(),
                ModItems.COIN_IRON.get(), ModItems.COIN_COPPER.get());
    }

    public static boolean isCoin(ItemStack stack) {
        return stack.getItem() instanceof CoinItem;
    }

    public static long valueOf(ItemStack stack) {
        return stack.getItem() instanceof CoinItem coin ? coin.getValue() * stack.getCount() : 0;
    }

    public static List<ItemStack> toStacks(long amount) {
        List<ItemStack> result = new ArrayList<>();
        for (CoinItem coin : denominations()) {
            long count = amount / coin.getValue();
            amount -= count * coin.getValue();
            while (count > 0) {
                int n = (int) Math.min(count, coin.getDefaultMaxStackSize());
                result.add(new ItemStack(coin, n));
                count -= n;
            }
        }
        return result;
    }

    /** 동전 지급. 인벤토리가 차면 바닥에 떨어뜨린다. */
    public static void give(Player player, long amount) {
        for (ItemStack stack : toStacks(amount)) giveStack(player, stack);
    }

    public static void giveStack(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    public static Item coinFor(long value) {
        for (CoinItem coin : denominations()) if (coin.getValue() == value) return coin;
        return ModItems.COIN_COPPER.get();
    }

    /** 인벤토리(가방+왼손)의 현금 합계 */
    public static long wallet(Player player) {
        var inv = player.getInventory();
        long total = 0;
        for (int i = 0; i < inv.items.size(); i++) total += valueOf(inv.items.get(i));
        for (ItemStack s : inv.offhand) total += valueOf(s);
        return total;
    }

    /** 현금을 전부 걷어서 합계를 돌려준다 */
    public static long takeAll(Player player) {
        var inv = player.getInventory();
        long total = 0;
        for (int i = 0; i < inv.items.size(); i++)
            if (isCoin(inv.items.get(i))) { total += valueOf(inv.items.get(i)); inv.items.set(i, ItemStack.EMPTY); }
        for (int i = 0; i < inv.offhand.size(); i++)
            if (isCoin(inv.offhand.get(i))) { total += valueOf(inv.offhand.get(i)); inv.offhand.set(i, ItemStack.EMPTY); }
        return total;
    }

    public static String format(long amount) {
        return String.format("%,d", amount);
    }

    public static String format(double amount) {
        return amount >= 10 ? String.format("%,.0f", amount) : String.format("%.1f", amount);
    }

    private Coins() {}
}
