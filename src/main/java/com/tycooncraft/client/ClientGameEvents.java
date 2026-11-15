package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.GunItem;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/** 클라이언트 전용 게임 이벤트: 저격총 조준 줌, 스코프 중 손 숨기기 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ClientGameEvents {

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        if (player.isUsingItem() && player.getUseItem().getItem() instanceof GunItem gun && gun.isScoped(player.getUseItem())) {
            event.setNewFovModifier(player.getUseItem().is(com.tycooncraft.registry.ModItems.VSS.get()) ? 0.28f : 0.2f);
        }
    }

    /** 스코프로 볼 때는 손에 든 총을 숨김 */
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (GunClient.isScoping()) event.setCanceled(true);
    }

    /**
     * 스코프 조준 중 마우스 감도를 줌 배율만큼 낮춤.
     * 마크 실제 감도 = (설정값*0.6+0.2)^3 → 원하는 배율이 되도록 설정값을 역산
     */
    @SubscribeEvent
    public static void onTurn(CalculatePlayerTurnEvent event) {
        if (!GunClient.isScoping()) return;
        double s = event.getMouseSensitivity();
        double eff = Math.pow(s * 0.6 + 0.2, 3) * SCOPE_SENS;
        double scaled = (Math.cbrt(eff) - 0.2) / 0.6;
        event.setMouseSensitivity(Math.max(-0.33, scaled));
        event.setCinematicCameraEnabled(false);
    }

    private static final double SCOPE_SENS = 0.22;

    private ClientGameEvents() {}
}
