package com.tycooncraft.economy;

import com.tycooncraft.registry.ModItems;
import com.tycooncraft.world.StructureBuilder;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.util.List;
import java.util.Optional;

/**
 * 모든 주민·떠돌이 상인이 에메랄드 대신 코인을 쓴다.
 * 플레이어가 낼 때: 에메랄드 1개 = 100원(금화 1), 주민이 줄 때: 에메랄드 1개 = 20원(철화 2) → 돈이 계속 빠져나가는 구조.
 * 상인 마을의 상인은 원래 코인 목록으로 되돌린다(불러올 때 바닐라 거래로 바뀌는 문제 방지).
 */
public final class VillagerCoins {
    public static final int PAY_PER_EMERALD = 100, GET_PER_EMERALD = 20;
    public static int CONVERTED, RESTORED, RESTOCKED;

    // ── 금액 → 코인 (한 칸 64개 이하가 되는 가장 큰 단위) ──
    static Item[] coinItems() {
        return Coins.denominations().toArray(new Item[0]);
    }
    static final int[] VALUE = {1_000_000, 100_000, 50_000, 10_000, 5_000, 1_000, 500, 100, 10, 1};

    public static ItemStack[] coinsForTest(int won) {
        return coins(won);
    }

    /** 금액을 (주 동전, 보조 동전) 두 칸 이하로 */
    static ItemStack[] coins(int won) {
        Item[] COINS = coinItems();
        for (int i = 0; i < VALUE.length; i++) {
            int n = won / VALUE[i];
            if (n <= 0) continue;
            if (n > 64) continue;
            int rest = won - n * VALUE[i];
            if (rest == 0) return new ItemStack[]{new ItemStack(COINS[i], n)};
            for (int j = i + 1; j < VALUE.length; j++) {
                if (rest % VALUE[j] == 0 && rest / VALUE[j] <= 64)
                    return new ItemStack[]{new ItemStack(COINS[i], n), new ItemStack(COINS[j], rest / VALUE[j])};
            }
            return new ItemStack[]{new ItemStack(COINS[i], n)};
        }
        return new ItemStack[]{new ItemStack(COINS[VALUE.length - 1], Math.max(1, Math.min(64, won)))};
    }

    static boolean emerald(ItemCost c) {
        return c.item().value() == Items.EMERALD;
    }

    /** 에메랄드가 들어간 거래 하나를 코인 거래로 (없으면 그대로) */
    public static MerchantOffer convert(MerchantOffer o) {
        if (o == null) return null;
        ItemCost a = o.getItemCostA();
        Optional<ItemCost> b = o.getItemCostB();
        ItemStack res = o.getResult();
        boolean ea = emerald(a), eb = b.isPresent() && emerald(b.get()), er = res.is(Items.EMERALD);
        if (!ea && !eb && !er) return o;
        ItemCost na = a;
        Optional<ItemCost> nb = b;
        if (ea && eb) {                      // 둘 다 에메랄드: 합쳐서
            ItemStack[] c = coins((a.count() + b.get().count()) * PAY_PER_EMERALD);
            na = new ItemCost(c[0].getItem(), c[0].getCount());
            nb = c.length > 1 ? Optional.of(new ItemCost(c[1].getItem(), c[1].getCount())) : Optional.empty();
        } else if (ea) {
            ItemStack[] c = coins(a.count() * PAY_PER_EMERALD);
            na = new ItemCost(c[0].getItem(), c[0].getCount());
            if (c.length > 1 && b.isEmpty()) nb = Optional.of(new ItemCost(c[1].getItem(), c[1].getCount()));
        } else if (eb) {
            ItemStack[] c = coins(b.get().count() * PAY_PER_EMERALD);
            nb = Optional.of(new ItemCost(c[0].getItem(), c[0].getCount()));
        }
        ItemStack nr = res;
        if (er) {
            ItemStack[] c = coins(res.getCount() * GET_PER_EMERALD);
            nr = c[0];
        }
        CONVERTED++;
        return new MerchantOffer(na, nb, nr, o.getUses(), o.getMaxUses(), o.getXp(), o.getPriceMultiplier(), o.getDemand());
    }

    /** 이미 있는 주민의 거래 목록을 바꿔 둠 */
    public static void convertAll(AbstractVillager v) {
        if (v.level().isClientSide) return;
        String key = traderKey(v);
        if (key != null) {
            List<MerchantOffer> pool = StructureBuilder.traderOffers(key);
            if (pool == null) return;
            // 하루(마크 하루)마다 진열 상품을 새로 뽑음 + 바닐라 거래로 바뀌었으면 되돌림
            long day = v.level().getDayTime() / 24000L;
            var tag = v.getPersistentData();
            boolean stale = !tag.contains("tycooncraft_stock_day") || tag.getLong("tycooncraft_stock_day") != day;
            if (stale || !fromPool(v.getOffers(), pool)) {
                setOffers(v, StructureBuilder.traderStock(key, v.getRandom()));
                tag.putLong("tycooncraft_stock_day", day);
                if (!stale) RESTORED++;
                RESTOCKED++;
            }
            return;
        }
        MerchantOffers offers = v.getOffers();
        for (int i = 0; i < offers.size(); i++) offers.set(i, convert(offers.get(i)));
    }

    /** 서버에서 거래 목록 교체 (바닐라 overrideOffers 는 서버에선 무시됨) */
    public static void setOffers(AbstractVillager v, List<MerchantOffer> offers) {
        MerchantOffers o = v.getOffers();
        o.clear();
        for (MerchantOffer m : offers) o.add(m.copy());
    }

    /** 상인 마을 상인: 이름표의 번역 키 (trader.tycooncraft.*) */
    static String traderKey(AbstractVillager v) {
        String tag = v.getPersistentData().getString("tycooncraft_trader");
        if (!tag.isEmpty()) return tag;
        if (v.getCustomName() != null && v.getCustomName().getContents() instanceof TranslatableContents tc && tc.getKey().startsWith("trader.tycooncraft."))
            return tc.getKey();
        return null;
    }

    /** 모든 거래가 이 상인의 후보 목록에 있는 것인지 */
    public static boolean fromPool(MerchantOffers a, List<MerchantOffer> pool) {
        if (a.isEmpty()) return false;
        outer:
        for (MerchantOffer o : a) {
            for (MerchantOffer p : pool)
                if (ItemStack.isSameItem(o.getResult(), p.getResult()) && o.getItemCostA().item() == p.getItemCostA().item()) continue outer;
            return false;
        }
        return true;
    }

    static boolean sameItems(MerchantOffers a, List<MerchantOffer> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++)
            if (!ItemStack.isSameItem(a.get(i).getResult(), b.get(i).getResult()) || a.get(i).getItemCostA().item() != b.get(i).getItemCostA().item()) return false;
        return true;
    }

    /** 새로 생기는 거래(새 주민·레벨업)도 코인으로 */
    @SubscribeEvent
    public static void onTrades(VillagerTradesEvent e) {
        e.getTrades().replaceAll((lvl, list) -> {
            List<VillagerTrades.ItemListing> out = new java.util.ArrayList<>();
            for (VillagerTrades.ItemListing l : list) out.add((ent, rnd) -> convert(l.getOffer(ent, rnd)));
            return out;
        });
    }

    @SubscribeEvent
    public static void onWanderer(WandererTradesEvent e) {
        wrap(e.getGenericTrades());
        wrap(e.getRareTrades());
    }

    private static void wrap(List<VillagerTrades.ItemListing> list) {
        for (int i = 0; i < list.size(); i++) {
            VillagerTrades.ItemListing l = list.get(i);
            list.set(i, (ent, rnd) -> convert(l.getOffer(ent, rnd)));
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (!e.getLevel().isClientSide && e.getEntity() instanceof AbstractVillager v && !e.loadedFromDisk()) return;   // 새로 태어난 주민은 거래 생성 때 변환됨
        if (!e.getLevel().isClientSide && e.getEntity() instanceof AbstractVillager v) convertAll(v);
    }

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract e) {
        if (e.getTarget() instanceof AbstractVillager v && !e.getLevel().isClientSide) convertAll(v);
    }

    private VillagerCoins() {}
}
