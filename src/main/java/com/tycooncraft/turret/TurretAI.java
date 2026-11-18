package com.tycooncraft.turret;

import com.tycooncraft.alliance.Alliances;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 포탑 공통 규칙 (v0.18.5 밸런스: 총보다 약하게).
 * 연사 6틱(초당 3.3발), 피해 경탄 4 / 중탄 5.5, 탄 퍼짐 큼, 목표를 잡고 0.5초 뒤부터 사격.
 * 대상: 적대 몹 + (침입자 모드일 때) 주인·동맹이 아닌 플레이어. 동맹 플레이어는 맞히지도 않는다.
 */
public final class TurretAI {
    public static final int COOLDOWN = 6, LOCK_DELAY = 10;
    public static final float DMG_LIGHT = 4f, DMG_HEAVY = 5.5f;
    public static final double SPREAD = 0.05;

    public static boolean targetable(ServerLevel sl, Entity e, @Nullable UUID owner, boolean intruders) {
        if (!e.isAlive()) return false;
        if (e instanceof Enemy) return true;
        if (e instanceof Player p) {
            if (!intruders || owner == null || p.isCreative() || p.isSpectator()) return false;
            return !Alliances.friendly(sl.getServer(), owner, p.getUUID());
        }
        return false;
    }

    /** 총알이 맞혀도 되는 대상 (동맹·주인은 관통해 지나감) */
    public static boolean canHit(ServerLevel sl, Entity e, @Nullable UUID owner) {
        if (e instanceof Player p) return owner != null && !Alliances.friendly(sl.getServer(), owner, p.getUUID());
        return true;
    }

    private TurretAI() {}
}
