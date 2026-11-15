package com.tycooncraft.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

/**
 * 서버 전체가 공유하는 시장.
 * 같은 아이템을 많이 팔수록 가격이 떨어지고(최저 25%), 시간이 지나면 회복된다.
 */
public class MarketData extends SavedData {
    private static final String NAME = "tycooncraft_market";
    private static final double HALF_PRICE_SATURATION = 256.0;
    private static final double MIN_MULTIPLIER = 0.25;
    private static final double RECOVERY_RATE = 0.97; // 1분마다

    private final Map<ResourceLocation, Double> saturation = new HashMap<>();

    public static MarketData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(MarketData::new, MarketData::load, null), NAME);
    }

    private static double multiplier(double sat) {
        return Math.max(MIN_MULTIPLIER, 1.0 / (1.0 + sat / HALF_PRICE_SATURATION));
    }

    private double saturationOf(Item item) {
        return saturation.getOrDefault(BuiltInRegistries.ITEM.getKey(item), 0.0);
    }

    public double currentPrice(Item item) {
        return PriceTable.basePrice(item) * multiplier(saturationOf(item));
    }

    public static boolean canSell(ItemStack stack) {
        return !stack.isEmpty() && (Coins.isCoin(stack) || PriceTable.basePrice(stack.getItem()) > 0);
    }

    /** 스택 전체를 판매(동전은 액면가 입금)하고 금액을 돌려준다. 팔 수 없으면 -1 */
    public double sell(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        if (Coins.isCoin(stack)) return Coins.valueOf(stack);
        Item item = stack.getItem();
        double base = PriceTable.basePrice(item);
        if (base <= 0) return -1;
        int count = stack.getCount();
        double before = saturationOf(item);
        double total = base * multiplier(before + count / 2.0) * count;
        saturation.put(BuiltInRegistries.ITEM.getKey(item), before + count);
        setDirty();
        return total;
    }

    public void recover() {
        if (saturation.isEmpty()) return;
        saturation.replaceAll((k, v) -> v * RECOVERY_RATE);
        saturation.values().removeIf(v -> v < 0.5);
        setDirty();
    }

    private static MarketData load(CompoundTag tag, HolderLookup.Provider registries) {
        MarketData data = new MarketData();
        CompoundTag sat = tag.getCompound("saturation");
        for (String key : sat.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) data.saturation.put(id, sat.getDouble(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag sat = new CompoundTag();
        saturation.forEach((id, v) -> sat.putDouble(id.toString(), v));
        tag.put("saturation", sat);
        return tag;
    }
}
