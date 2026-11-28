package com.tycooncraft.blackmarket;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * v0.28 불법 개조점: 손에 든 총에 개조를 영구로 새김 (총마다 한 종류씩 한 번).
 *  과압탄 개조   피해 +20%            단골~   ₩20,000
 *  확장 드럼     탄창 +50%            단골~   ₩15,000
 *  해머 개조     연사 +20%            동업자~ ₩30,000
 *  반동 억제 킷  반동 -35%            동업자~ ₩25,000
 *  총열 재가공   탄퍼짐 -30%          간부~   ₩40,000
 *  일련번호 세탁 마을 경비병이 총성에 반응 안 함  대부   ₩80,000
 * 평판 할인 적용. 개조된 총은 툴팁에 붉게 표시됨.
 */
public final class IllegalMods {
    public static final String TAG = "tycooncraft_illegal_mods";
    public static final String[] IDS = {"overpressure", "drum", "hammer", "recoil", "bore", "scrub"};
    public static final int[] TIER = {1, 1, 2, 2, 3, 4};
    public static final long[] PRICE = {160_000, 120_000, 240_000, 200_000, 320_000, 640_000};
    public static final int OVERPRESSURE = 0, DRUM = 1, HAMMER = 2, RECOIL = 3, BORE = 4, SCRUB = 5;
    public static int installs;

    public static int mask(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null ? 0 : cd.copyTag().getInt(TAG);
    }

    public static boolean has(ItemStack s, int mod) {
        return (mask(s) & (1 << mod)) != 0;
    }

    public static float damageMult(ItemStack s) {
        return has(s, OVERPRESSURE) ? 1.2f : 1f;
    }

    public static float magMult(ItemStack s) {
        return has(s, DRUM) ? 1.5f : 1f;
    }

    public static float delayMult(ItemStack s) {
        return has(s, HAMMER) ? 0.8f : 1f;
    }

    public static float recoilMult(ItemStack s) {
        return has(s, RECOIL) ? 0.65f : 1f;
    }

    public static float spreadMult(ItemStack s) {
        return has(s, BORE) ? 0.7f : 1f;
    }

    public static long price(ServerPlayer sp, int mod) {
        return Math.round(PRICE[mod] * (1 - Reputation.DISCOUNT[Reputation.tier(sp)]) / 100) * 100;
    }

    public static Component install(ServerPlayer sp, int mod) {
        if (mod < 0 || mod >= IDS.length) return Component.empty();
        ItemStack gun = sp.getMainHandItem();
        if (!(gun.getItem() instanceof com.tycooncraft.gun.GunItem))
            return Component.translatable("message.tycooncraft.mods.no_gun").withStyle(ChatFormatting.RED);
        if (has(gun, mod)) return Component.translatable("message.tycooncraft.mods.already").withStyle(ChatFormatting.RED);
        if (Reputation.tier(sp) < TIER[mod])
            return Component.translatable("message.tycooncraft.mods.tier", Reputation.tierName(TIER[mod])).withStyle(ChatFormatting.RED);
        long cost = price(sp, mod);
        if (!VaultBlock.pay(sp, cost)) return Component.translatable("message.tycooncraft.casino.no_money").withStyle(ChatFormatting.RED);
        CustomData.update(DataComponents.CUSTOM_DATA, gun, t -> t.putInt(TAG, t.getInt(TAG) | (1 << mod)));
        installs++;
        Reputation.add(sp, (int) (cost / 5000));
        sp.serverLevel().playSound(null, sp.blockPosition(), SoundEvents.SMITHING_TABLE_USE, SoundSource.PLAYERS, 1f, 0.8f);
        return Component.translatable("message.tycooncraft.mods.done", Component.translatable("mod.tycooncraft." + IDS[mod])).withStyle(ChatFormatting.GREEN);
    }

    /** 툴팁 */
    public static void tooltip(ItemStack s, java.util.List<Component> tip) {
        int m = mask(s);
        if (m == 0) return;
        for (int i = 0; i < IDS.length; i++)
            if ((m & (1 << i)) != 0) tip.add(Component.translatable("tooltip.tycooncraft.mod_installed", Component.translatable("mod.tycooncraft." + IDS[i])).withStyle(ChatFormatting.DARK_RED));
    }

    private IllegalMods() {}
}
