package com.tycooncraft.vehicle;

import com.tycooncraft.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.function.IntConsumer;

/** 연료 (단위: 틱). 연료통 5분, 석탄 2분, 석탄 블록 18분, 용암 양동이 10분 */
public final class Fuel {

    public static int value(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (stack.is(ModItems.FUEL_CANISTER.get())) return 6000;
        if (stack.is(Items.COAL) || stack.is(Items.CHARCOAL)) return 2400;
        if (stack.is(Items.COAL_BLOCK)) return 21600;
        if (stack.is(Items.LAVA_BUCKET)) return 12000;
        return 0;
    }

    /** 서버: 손에 든 연료를 넣는다. 가득 차 있으면 안내만 */
    public static void refuel(Entity vehicle, Player player, InteractionHand hand, int current, int max, IntConsumer set) {
        ItemStack stack = player.getItemInHand(hand);
        int add = value(stack);
        if (add <= 0) return;
        if (current >= max - add / 4) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.vehicle.fuel_full"), true);
            return;
        }
        int next = Math.min(max, current + add);
        set.accept(next);
        if (!player.getAbilities().instabuild) {
            if (stack.is(Items.LAVA_BUCKET)) player.setItemInHand(hand, new ItemStack(Items.BUCKET));
            else stack.shrink(1);
        }
        vehicle.level().playSound(null, vehicle.getX(), vehicle.getY(), vehicle.getZ(), SoundEvents.BUCKET_EMPTY, SoundSource.NEUTRAL, 0.8f, 0.8f);
        player.displayClientMessage(Component.translatable("message.tycooncraft.vehicle.fuel", next * 100 / max), true);
    }

    private Fuel() {}
}
