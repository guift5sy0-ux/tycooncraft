package com.tycooncraft.client.gun;

import com.tycooncraft.client.GunAnim;
import com.tycooncraft.network.ReloadAnimPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;

/** v0.32: 서버에서 온 총 동작 알림 처리 */
public final class GunNet {
    public static void onReload(ReloadAnimPayload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        if (mc.player != null && p.entity() == mc.player.getId()) {
            GunViewModel.syncReload(mc.player.getMainHandItem(), p.ticks(), p.shells(), p.wasEmpty());
            return;
        }
        if (!(mc.level.getEntity(p.entity()) instanceof LivingEntity e)) return;
        GunKind k = GunKind.of(e.getMainHandItem());
        if (k == null) return;
        GunAnim.startReload(p.entity(), p.ticks());
        GunCues.cancel(p.entity());
        GunCues.schedule(p.entity(), GunCues.now(), GunCues.reload(k, p.ticks() / 20f, p.wasEmpty(), p.shells()));
    }

    private GunNet() {}
}
