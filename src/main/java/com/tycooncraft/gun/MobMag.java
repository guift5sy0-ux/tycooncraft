package com.tycooncraft.gun;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;

/** v0.35.1: 총 든 몹의 탄창 - 다 쏘면 플레이어처럼 재장전 동작(보는 사람에게 손동작·소리)을 하고 그동안은 못 쏨 */
public final class MobMag {
    private int shots;
    private int reloadUntil = -1;
    public static int reloads, testMag;

    /** 재장전 중이면 true (쏘지 않음) */
    public boolean busy(Mob m) {
        if (reloadUntil < 0) return false;
        if (m.tickCount >= reloadUntil) {
            reloadUntil = -1;
            return false;
        }
        return true;
    }

    /** 한 발 쐈음 - 탄창이 비면 재장전 시작 */
    public void fired(Mob m) {
        ItemStack st = m.getMainHandItem();
        if (!(st.getItem() instanceof GunItem g)) return;
        int mag = testMag > 0 ? testMag : Math.max(1, g.magazine(st));
        if (++shots < mag) return;
        shots = 0;
        int rt = Math.max(20, g.reloadTicks(st));
        reloadUntil = m.tickCount + rt;
        reloads++;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(m,
                new com.tycooncraft.network.ReloadAnimPayload(m.getId(), rt, mag, true));
    }
}
