package com.tycooncraft.blackmarket;

import com.tycooncraft.economy.CoinItem;
import com.tycooncraft.economy.Coins;
import com.tycooncraft.economy.PriceTable;
import com.tycooncraft.economy.VillagerCoins;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.world.StructureBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 암상인 진열 (매일 바뀜).
 * 0 무기 밀수꾼: 총·탄약 30% 싸게 (총은 '밀수품' — 5% 확률로 탄 걸림)
 * 1 희귀품 브로커: 정규 상점에 없는 것, 비쌈, 하루 수량 제한
 * 2 장물아비: 한정 특가 4개 + 시세 무시 매입(기준가 70%) + 보스 전리품 1.2배 매입
 */
public final class DealerTrades {
    public static final double SMUGGLE = 0.7, FENCE_PAY = 0.7, BOSS_PAY = 1.2, BROKER_MARKUP = 4.5, FENCE_SALE = 1.8;
    public static final String SMUGGLED = "tycooncraft_smuggled";

    record E(Item item, int count, long won) {
        E(Item item, int count) {
            this(item, count, 0);
        }
    }

    static List<E> armsPool() {
        return List.of(new E(ModItems.PISTOL.get(), 1), new E(ModItems.DUAL_PISTOLS.get(), 1), new E(ModItems.SMG.get(), 1), new E(ModItems.SHOTGUN.get(), 1),
                new E(ModItems.RIFLE.get(), 1), new E(ModItems.VSS.get(), 1), new E(ModItems.SNIPER.get(), 1), new E(ModItems.RPG.get(), 1),
                new E(ModItems.LIGHT_AMMO.get(), 64), new E(ModItems.HEAVY_AMMO.get(), 32), new E(ModItems.SHOTGUN_SHELL.get(), 32),
                new E(ModItems.ROCKET.get(), 4), new E(ModItems.HISPANO_AMMO.get(), 8), new E(ModItems.FRAG_GRENADE.get(), 4),
                new E(ModItems.FLASH_GRENADE.get(), 4), new E(ModItems.SMOKE_GRENADE.get(), 4), new E(ModItems.SILENCER_ADVANCED.get(), 1, 6_000));
    }

    static List<E> brokerPool() {
        return List.of(new E(Items.NETHER_STAR, 1, 60_000), new E(ModItems.DESTROYER_CORE.get(), 1), new E(Items.WITHER_SKELETON_SKULL, 1, 15_000),
                new E(ModItems.VEST_4.get(), 1, 45_000), new E(ModItems.HELMET_4.get(), 1, 30_000), new E(Items.NETHERITE_INGOT, 1, 25_000),
                new E(Items.ENCHANTED_GOLDEN_APPLE, 1, 40_000), new E(Items.TOTEM_OF_UNDYING, 1, 35_000), new E(Items.ELYTRA, 1, 100_000),
                new E(Items.HEART_OF_THE_SEA, 1, 20_000), new E(Items.SHULKER_SHELL, 2, 12_000), new E(ModItems.RT20.get(), 1, 80_000),
                new E(ModItems.MANHOLE.get(), 1, 50_000), new E(Items.NETHERITE_SCRAP, 2, 18_000), new E(Items.DRAGON_BREATH, 4, 6_000),
                new E(Items.TRIDENT, 1, 30_000));
    }

    static List<E> fenceSalePool() {
        return List.of(new E(Items.DIAMOND, 8), new E(Items.ENDER_PEARL, 16, 4_000), new E(Items.GOLDEN_APPLE, 4, 6_000), new E(ModItems.STEEL_INGOT.get(), 64),
                new E(ModItems.ADVANCED_CIRCUIT.get(), 8), new E(ModItems.ENGINE.get(), 2), new E(Items.EXPERIENCE_BOTTLE, 32, 4_000), new E(ModItems.GUN_RECEIVER.get(), 2, 3_000),
                new E(Items.NETHERITE_SCRAP, 1, 7_000), new E(ModItems.MICROCHIP.get(), 16), new E(Items.OBSIDIAN, 32, 2_500), new E(ModItems.BATTERY_CELL.get(), 8));
    }

    static List<E> fenceBuyPool() {
        return List.of(new E(ModItems.IRON_PLATE.get(), 32), new E(ModItems.COPPER_PLATE.get(), 32), new E(ModItems.IRON_GEAR.get(), 16), new E(ModItems.STEEL_INGOT.get(), 16),
                new E(ModItems.STEEL_PLATE.get(), 16), new E(ModItems.CIRCUIT.get(), 8), new E(ModItems.MECHANISM.get(), 4), new E(ModItems.ENGINE.get(), 1),
                new E(ModItems.COPPER_WIRE.get(), 64), new E(ModItems.STEEL_ROD.get(), 32), new E(ModItems.IRON_DUST.get(), 64), new E(Items.IRON_INGOT, 32),
                new E(Items.GOLD_INGOT, 16), new E(ModItems.PLASTIC_PELLET.get(), 32), new E(ModItems.ALUMINUM_INGOT.get(), 16), new E(ModItems.CEMENT.get(), 32),
                new E(ModItems.ADVANCED_CIRCUIT.get(), 4), new E(ModItems.BALL_BEARING.get(), 16), new E(ModItems.REBAR.get(), 32), new E(Items.BREAD, 32));
    }

    /** 보스 전리품 (늘 매입) */
    static List<E> bossLoot() {
        return List.of(new E(ModItems.DESTROYER_CORE.get(), 1), new E(Items.NETHER_STAR, 1, 20_000), new E(Items.WITHER_SKELETON_SKULL, 1, 4_000),
                new E(ModItems.VENOM_SPIT.get(), 4, 1_500), new E(Items.DRAGON_BREATH, 4, 1_600), new E(Items.ELYTRA, 1, 30_000));
    }

    static long base(E e) {
        double p = PriceTable.basePrice(e.item());
        return p > 0 ? Math.round(p * e.count()) : e.won();
    }

    public static List<MerchantOffer> stock(int kind, RandomSource r) {
        List<MerchantOffer> out = new ArrayList<>();
        switch (kind) {
            case 0 -> {
                for (E e : pick(armsPool(), 12, r)) {
                    double h = StructureBuilder.hours(e.item());
                    long won = h > 0 ? Math.round(h * PriceTable.MANUAL_HOUR * SMUGGLE)
                            : e.won() > 0 ? e.won() : Math.round(PriceTable.basePrice(e.item()) * e.count() * StructureBuilder.TRADER_MARKUP * SMUGGLE);
                    won = Math.round(won * StructureBuilder.usableMult(e.item()));   // 밀수품도 정규가(3배)의 70%
                    ItemStack res = new ItemStack(e.item(), e.count());
                    if (e.item() instanceof GunItem) smuggle(res);
                    out.add(sell(won, res, 8, r));
                }
            }
            case 1 -> {
                for (E e : pick(brokerPool(), 10, r)) {
                    long won = e.won() > 0 ? e.won() : Math.round(base(e) * BROKER_MARKUP);
                    out.add(sell(won, new ItemStack(e.item(), e.count()), 1 + r.nextInt(2), r));
                }
            }
            default -> {
                for (E e : pick(fenceSalePool(), 4, r)) {   // 한정 특가
                    long won = e.won() > 0 ? e.won() : Math.round(base(e) * FENCE_SALE);
                    ItemStack res = new ItemStack(e.item(), e.count());
                    res.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("lore.tycooncraft.bm_limited"))));
                    out.add(sell(won, res, 1, r));
                }
                for (E e : pick(fenceBuyPool(), 8, r)) out.add(buy(e, FENCE_PAY, 5, r));
                for (E e : bossLoot()) out.add(buy(e, BOSS_PAY, 4, r));
            }
        }
        return out;
    }

    static <T> List<T> pick(List<T> pool, int n, RandomSource r) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < pool.size(); i++) idx.add(i);
        net.minecraft.Util.shuffle(idx, r);
        idx = new ArrayList<>(idx.subList(0, Math.min(n, idx.size())));
        java.util.Collections.sort(idx);
        List<T> out = new ArrayList<>();
        for (int i : idx) out.add(pool.get(i));
        return out;
    }

    public static void smuggle(ItemStack s) {
        CustomData.update(DataComponents.CUSTOM_DATA, s, c -> c.putBoolean(SMUGGLED, true));
        s.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("lore.tycooncraft.smuggled"))));
    }

    public static boolean smuggled(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d != null && d.copyTag().getBoolean(SMUGGLED);
    }

    /** v0.27.0 물가 조정: 암상인 판매가 30% 인하 */
    public static final double PRICE_CUT = 0.7;

    static MerchantOffer sell(long won, ItemStack res, int uses, RandomSource r) {
        won = Math.max(1, Math.round(won * PRICE_CUT * (0.9 + r.nextFloat() * 0.25)));
        ItemStack[] c = VillagerCoins.coinsForTest((int) Math.min(Integer.MAX_VALUE, won));
        return new MerchantOffer(new ItemCost(c[0].getItem(), c[0].getCount()),
                c.length > 1 ? Optional.of(new ItemCost(c[1].getItem(), c[1].getCount())) : Optional.empty(), res, uses, 0, 0f);
    }

    static MerchantOffer buy(E e, double rate, int uses, RandomSource r) {
        long won = Math.max(1, Math.round(base(e) * rate * (0.95 + r.nextFloat() * 0.1)));
        return new MerchantOffer(new ItemCost(e.item(), e.count()), Optional.empty(), oneStack(won), uses, 0, 0f);
    }

    /** 금액을 한 칸(64개 이하)의 동전으로 */
    static ItemStack oneStack(long won) {
        var ds = new ArrayList<>(Coins.denominations());
        java.util.Collections.reverse(ds);
        for (Item c : ds) {
            long v = ((CoinItem) c).getValue();
            if (won >= v && won / v <= 64) return new ItemStack(c, (int) Math.max(1, Math.round((double) won / v)));
        }
        return new ItemStack(ModItems.COIN_COPPER.get(), (int) Math.min(64, won));
    }

    private DealerTrades() {}
}
