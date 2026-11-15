package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.GunItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** 클라이언트 전용 게임 이벤트: 저격총 조준 줌, 스코프 중 손 숨기기 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof GunItem gun && gun.stats().scoped()) {
            event.setNewFovModifier(0.2f);
        }
    }

    /** 스코프로 볼 때는 손에 든 총을 숨김 */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (GunClient.isScoping()) event.setCanceled(true);
    }

    private ClientGameEvents() {}
}
