package com.tycooncraft.client.gear;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gear.PoliceShieldItem;
import com.tycooncraft.gear.ShieldBashPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** 오른손에 진압용 방패: Q(버리기 키)를 누르면 버리지 않고 돌진 */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class ShieldBashClient {
    @SubscribeEvent
    public static void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !(mc.player.getMainHandItem().getItem() instanceof PoliceShieldItem)) return;
        boolean pressed = false;
        while (mc.options.keyDrop.consumeClick()) pressed = true;
        if (pressed && !mc.player.getCooldowns().isOnCooldown(mc.player.getMainHandItem().getItem()))
            PacketDistributor.sendToServer(new ShieldBashPayload());
    }

    private ShieldBashClient() {}
}
