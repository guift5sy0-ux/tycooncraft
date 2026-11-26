package com.tycooncraft.mech;

import com.tycooncraft.TycoonCraft;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** v0.28.2 원격 조종 중 나가기/다시 들어오기: 몸 자리로 되돌리고 무적·투명 해제 */
@EventBusSubscriber(modid = TycoonCraft.MODID)
public final class PilotEvents {
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        for (var lvl : sp.server.getAllLevels())
            for (var m : lvl.getEntities(com.tycooncraft.registry.ModEntities.PROTO_SCOUT.get(), x -> x.pilot() == sp)) m.stopRemote();
        ProtoScout.recover(sp);
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) ProtoScout.recover(sp);
    }

    private PilotEvents() {}
}
