package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.GunItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;

/** 클라이언트 전용 게임 이벤트: 저격총 조준 줌 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof GunItem gun && gun.stats().scoped()) {
            event.setNewFovModifier(0.2f);
        }
    }

    private ClientGameEvents() {}
}
