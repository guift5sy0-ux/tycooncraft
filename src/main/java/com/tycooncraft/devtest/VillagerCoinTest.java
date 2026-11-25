package com.tycooncraft.devtest;

import com.tycooncraft.economy.VillagerCoins;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

/** v0.17.1: 주민·떠돌이 상인 코인 거래, 상인 마을 상인 목록 복구 */
public final class VillagerCoinTest {
    static String describe(MerchantOffer o) {
        return o.getItemCostA().count() + "x" + o.getItemCostA().item().unwrapKey().get().location().getPath()
                + o.getItemCostB().map(b -> "+" + b.count() + "x" + b.item().unwrapKey().get().location().getPath()).orElse("")
                + "->" + o.getResult().getCount() + "x" + o.getResult().getItem().builtInRegistryHolder().key().location().getPath();
    }

    static int emeraldTrades(AbstractVillager v) {
        int n = 0;
        for (MerchantOffer o : v.getOffers())
            if (o.getItemCostA().item().value() == Items.EMERALD || o.getResult().is(Items.EMERALD)
                    || o.getItemCostB().map(b -> b.item().value() == Items.EMERALD).orElse(false)) n++;
        return n;
    }

    static void tick(ServerLevel level, int t, int gy) {
        if (t != 2320) return;
        MerchantOffer buy = VillagerCoins.convert(new MerchantOffer(new ItemCost(Items.EMERALD, 5), Optional.of(new ItemCost(Items.BOOK, 1)), new ItemStack(Items.ENCHANTED_BOOK), 12, 0, 0.2f));
        MerchantOffer sell = VillagerCoins.convert(new MerchantOffer(new ItemCost(Items.WHEAT, 20), Optional.empty(), new ItemStack(Items.EMERALD), 16, 2, 0.05f));
        MerchantOffer big = VillagerCoins.convert(new MerchantOffer(new ItemCost(Items.EMERALD, 64), Optional.empty(), new ItemStack(Items.DIAMOND), 1, 0, 0f));
        System.out.println("[AUTOTEST] v17 coins buy=" + describe(buy) + " sell=" + describe(sell) + " big=" + describe(big));
        int emer = 0, total = 0;
        for (VillagerProfession prof : new VillagerProfession[]{VillagerProfession.FARMER, VillagerProfession.LIBRARIAN, VillagerProfession.ARMORER, VillagerProfession.CLERIC}) {
            Villager v = EntityType.VILLAGER.create(level);
            v.moveTo(130, gy, 98);
            v.setVillagerData(v.getVillagerData().setProfession(prof).setLevel(3));
            emer += emeraldTrades(v);
            total += v.getOffers().size();
            v.discard();
        }
        var w = EntityType.WANDERING_TRADER.create(level);
        int wEmer = emeraldTrades(w), wTotal = w.getOffers().size();
        // 상인 마을 상인: 불러오다 바닐라 거래로 바뀐 상황 흉내 → 원래 코인 목록으로
        Villager tr = EntityType.VILLAGER.create(level);
        tr.setVillagerData(tr.getVillagerData().setProfession(VillagerProfession.FARMER).setLevel(5));
        tr.setCustomName(Component.translatable("trader.tycooncraft.food"));
        MerchantOffers bad = new MerchantOffers();
        bad.add(new MerchantOffer(new ItemCost(Items.WHEAT, 20), Optional.empty(), new ItemStack(Items.EMERALD), 16, 2, 0.05f));
        VillagerCoins.setOffers(tr, bad);
        VillagerCoins.convertAll(tr);
        System.out.println("[AUTOTEST] v17 villagers emeraldTrades=" + emer + "/" + total + " wanderer=" + wEmer + "/" + wTotal
                + " traderRestored=" + (tr.getOffers().size() == 12 && VillagerCoins.fromPool(tr.getOffers(), com.tycooncraft.world.StructureBuilder.traderOffers("trader.tycooncraft.food"))
                        && emeraldTrades(tr) == 0) + " first=" + describe(tr.getOffers().get(0)));
        // 진열 무작위: 같은 상인을 5번 뽑아 서로 다른 상품 구성 수
        java.util.Set<String> sets = new java.util.HashSet<>();
        for (int i = 0; i < 5; i++) {
            StringBuilder b = new StringBuilder();
            for (MerchantOffer o : com.tycooncraft.world.StructureBuilder.traderStock("trader.tycooncraft.gear", level.random)) b.append(describe(o)).append(',');
            sets.add(b.toString());
        }
        int pool = 0;
        for (String k : new String[]{"food", "builder", "gear", "machines", "buyer", "gun", "engineer", "scrap_buyer", "miner", "chemist", "vehicle", "pharmacist"}) pool += com.tycooncraft.world.StructureBuilder.traderOffers("trader.tycooncraft." + k).size();
        System.out.println("[AUTOTEST] v18 traderStock distinct=" + sets.size() + "/5 poolTotal=" + pool + " sample=" + sets.iterator().next());
    }

    private VillagerCoinTest() {}
}
