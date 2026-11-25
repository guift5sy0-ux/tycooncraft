package com.tycooncraft.mob.carrier;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 공중 디스트로이어 출현: 오버월드, 밤, 월드 3일째부터, 하늘이 보이는 곳에 있는 플레이어 근처.
 * 2분마다 플레이어마다 5% (근처 160칸 안에 이미 있으면 안 나옴).
 */
@EventBusSubscriber(modid = TycoonCraft.MODID)
public final class CarrierSpawner {
    public static final int EVERY = 2400, MIN_DAY = 3;
    public static final float CHANCE = 0.05f;
    public static int spawned;

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % EVERY != 0) return;
        ServerLevel l = e.getServer().overworld();
        if (l.getDifficulty() == Difficulty.PEACEFUL || l.isDay() || l.getDayTime() / 24000L < MIN_DAY) return;
        for (ServerPlayer p : l.players()) {
            if (p.isCreative() || p.isSpectator() || !l.canSeeSky(p.blockPosition())) continue;
            if (l.random.nextFloat() >= CHANCE) continue;
            spawnNear(l, p);
        }
    }

    public static AerialDestroyer spawnNear(ServerLevel l, ServerPlayer p) {
        if (!l.getEntitiesOfClass(AerialDestroyer.class, p.getBoundingBox().inflate(160)).isEmpty()) return null;
        double a = l.random.nextDouble() * Math.PI * 2;
        int x = (int) (p.getX() + Math.cos(a) * 48), z = (int) (p.getZ() + Math.sin(a) * 48);
        BlockPos top = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        AerialDestroyer d = ModEntities.AERIAL_DESTROYER.get().create(l);
        if (d == null) return null;
        d.moveTo(x + 0.5, Math.max(top.getY(), (int) p.getY()) + 20, z + 0.5, l.random.nextFloat() * 360, 0);
        d.finalizeSpawn(l, l.getCurrentDifficultyAt(top), MobSpawnType.EVENT, null);
        l.addFreshEntity(d);
        spawned++;
        p.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.tycooncraft.carrier.incoming")
                .withStyle(net.minecraft.ChatFormatting.RED), true);
        return d;
    }

    private CarrierSpawner() {}
}
