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

    private GunEvents() {}
}
