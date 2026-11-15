package com.tycooncraft.client.vehicle;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.vehicle.JetEntity;
import com.tycooncraft.vehicle.TankEntity;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;

/** 전차·전투기 탑승자는 차체 안에 있으므로 몸을 그리지 않는다 (머리·몸이 삐져나오지 않게) */
@EventBusSubscriber(modid = TycoonCraft.MODID, value = Dist.CLIENT)
public final class HideRiders {

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Pre<?, ?> event) {
        Entity v = event.getEntity().getVehicle();
        if (v instanceof TankEntity || v instanceof JetEntity) event.setCanceled(true);
    }

    private HideRiders() {}
}
