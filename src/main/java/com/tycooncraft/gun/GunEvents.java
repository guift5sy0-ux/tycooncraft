package com.tycooncraft.gun;

import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.vehicle.JetEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;

/** F 키(양손 바꾸기): VSS 사격 모드 전환 / 전투기 비행 중이면 배럴 롤 */
public final class GunEvents {

    @SubscribeEvent
    public static void onSwap(LivingSwapItemsEvent.Hands event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (p.getVehicle() instanceof JetEntity jet) {
            event.setCanceled(true);
            if (jet.getControllingPassenger() == p) jet.startRoll();
            return;
        }
        ItemStack main = p.getMainHandItem();
        if (main.getItem() instanceof GunItem && !main.is(ModItems.VSS.get())) {   // 총(RPG 포함)은 왼손으로 못 옮김
            event.setCanceled(true);
            p.displayClientMessage(Component.translatable("message.tycooncraft.gun.no_offhand").withStyle(ChatFormatting.RED), true);
            return;
        }
        if (main.is(ModItems.VSS.get())) {
            event.setCanceled(true);
            int mode = GunItem.fireMode(main) == 0 ? 1 : 0;
            main.set(ModComponents.FIRE_MODE.get(), mode);
            p.stopUsingItem();
            p.displayClientMessage(Component.translatable(mode == 1 ? "message.tycooncraft.gun.mode_auto" : "message.tycooncraft.gun.mode_semi")
                    .withStyle(ChatFormatting.YELLOW), true);
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.LEVER_CLICK, SoundSource.PLAYERS, 0.6f, 1.6f);
        }
    }

    public static int OFFHAND_BLOCKED;

    /** 인벤토리에서 끌어다 놓는 경우까지: 왼손에 총이 있으면 가방으로 돌려보냄(가득 차면 떨굼) */
    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide) return;
        ItemStack off = p.getOffhandItem();
        if (!(off.getItem() instanceof GunItem)) return;
        ItemStack moved = off.copy();
        p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND, ItemStack.EMPTY);
        if (!p.getInventory().add(moved)) p.drop(moved, false);
        OFFHAND_BLOCKED++;
        p.displayClientMessage(Component.translatable("message.tycooncraft.gun.no_offhand").withStyle(ChatFormatting.RED), true);
    }

    @SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (!com.tycooncraft.entity.SpiderShot.WEBS.isEmpty()) com.tycooncraft.entity.SpiderShot.cleanup();
    }

    private GunEvents() {}
}
