package com.tycooncraft.world.gen;

import com.tycooncraft.world.StructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * 월드 생성 단계에서 넣은 주민은 저장 과정에서 사라질 수 있어서,
 * 마을의 시작 청크가 실제로 로딩될 때 상인 5명을 한 번만 배치한다.
 */
public final class VillageTraders {
    public static int SPAWNED, EXISTING;

    record Pending(ServerLevel level, BlockPos origin, int since) {}

    private static final java.util.List<Pending> PENDING = new java.util.ArrayList<>();

    /** 엔티티까지 다 로딩된 뒤에만 판단 (블록과 엔티티는 따로 로딩된다 → 너무 일찍 보면 있는 상인도 없어 보임) */
    @SubscribeEvent
    public static void onTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        if (PENDING.isEmpty()) return;
        int now = e.getServer().getTickCount();
        for (var it = PENDING.iterator(); it.hasNext(); ) {
            Pending p = it.next();
            Data data = Data.get(p.level());
            if (data.done.contains(p.origin().asLong()) || now - p.since() > 6000) {
                it.remove();
                continue;
            }
            int half = com.tycooncraft.world.StructureBuilder.VILLAGE_SIZE / 2;
            BlockPos center = p.origin().offset(half, 0, half);
            if (!p.level().isLoaded(center) || !p.level().areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(center).toLong())) continue;
            AABB box = new AABB(p.origin()).expandTowards(half * 2, 8, half * 2).inflate(2);
            if (p.level().getEntitiesOfClass(Villager.class, box, Villager::hasCustomName).isEmpty()) {
                StructureBuilder.spawnVillageTraders(p.level(), p.origin());
                SPAWNED++;
            } else EXISTING++;
            data.done.add(p.origin().asLong());
            data.setDirty();
            it.remove();
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level) || !(e.getChunk() instanceof LevelChunk chunk)) return;
        for (StructureStart start : chunk.getAllStarts().values()) {
            if (!(start.getStructure() instanceof TycoonStructure ts) || !ts.kind().equals("village") || !start.isValid()) continue;
            for (StructurePiece piece : start.getPieces()) {
                if (!(piece instanceof TycoonPiece tp)) continue;
                BlockPos origin = tp.origin();
                Data data = Data.get(level);
                if (data.done.contains(origin.asLong())) continue;
                PENDING.add(new Pending(level, origin, level.getServer().getTickCount()));
            }
        }
    }

    public static class Data extends SavedData {
        final Set<Long> done = new HashSet<>();

        static Data get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(new Factory<>(Data::new, Data::load), "tycooncraft_village_traders");
        }

        static Data load(CompoundTag tag, HolderLookup.Provider reg) {
            Data d = new Data();
            for (long l : tag.getLongArray("done")) d.done.add(l);
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
            tag.putLongArray("done", done.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }
    }

    private VillageTraders() {}
}
