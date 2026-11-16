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
    public static int SPAWNED;

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
                // 청크 로딩 도중엔 엔티티를 넣지 않고 다음 틱에
                level.getServer().execute(() -> {
                    if (data.done.contains(origin.asLong()) || !level.isLoaded(origin.offset(16, 0, 16))) return;
                    AABB box = new AABB(origin).expandTowards(32, 8, 32).inflate(2);
                    if (level.getEntitiesOfClass(Villager.class, box, Villager::hasCustomName).isEmpty()) {
                        StructureBuilder.spawnVillageTraders(level, origin);
                        SPAWNED++;
                    }
                    data.done.add(origin.asLong());
                    data.setDirty();
                });
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
