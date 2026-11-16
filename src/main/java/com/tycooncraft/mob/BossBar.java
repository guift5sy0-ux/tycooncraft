package com.tycooncraft.mob;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.LivingEntity;

/** 보스 체력바: 가까운 플레이어에게만 표시 */
public final class BossBar {
    private final ServerBossEvent event;
    private final double range;

    public BossBar(Component name, BossEvent.BossBarColor color, BossEvent.BossBarOverlay overlay, double range) {
        this.event = new ServerBossEvent(name, color, overlay);
        this.range = range;
    }

    public void tick(LivingEntity owner) {
        event.setProgress(owner.getHealth() / owner.getMaxHealth());
        if (owner.tickCount % 10 != 0 || !(owner.level() instanceof net.minecraft.server.level.ServerLevel sl)) return;
        for (ServerPlayer p : sl.players()) {
            boolean near = p.distanceToSqr(owner) < range * range && owner.isAlive();
            if (near && !event.getPlayers().contains(p)) event.addPlayer(p);
            else if (!near && event.getPlayers().contains(p)) event.removePlayer(p);
        }
    }

    public void setDarken(boolean b) {
        event.setDarkenScreen(b);
    }

    public void clear() {
        event.removeAllPlayers();
    }
}
