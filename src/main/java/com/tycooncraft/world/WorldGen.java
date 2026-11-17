package com.tycooncraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 상인 마을·폐공장 자연 생성.
 * 플레이어가 탐험하면 시야 밖(100~140블록) 평평한 자연 지형에 확률적으로 생긴다.
 * 스폰 근처, 다른 구조물 근처, 플레이어가 기계를 설치한 기지 근처에는 생기지 않는다.
 */
public final class WorldGen {
    private static final int CHECK_INTERVAL = 600;
    private static final double VILLAGE_CHANCE = 0.10;
    private static final double RUIN_CHANCE = 0.14;
    private static final int MIN_DIST = 100, MAX_DIST = 140;
    private static final int VILLAGE_SPACING = 700, RUIN_SPACING = 450, ANY_SPACING = 220;
    private static final int SPAWN_PROTECT = 300, BASE_PROTECT = 160;
    private static final int MAX_SLOPE = 4;

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % CHECK_INTERVAL != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level().dimension() != Level.OVERWORLD) continue;
            double r = player.getRandom().nextDouble();
            if (r < VILLAGE_CHANCE) tryBuild(player, "village");
            else if (r < VILLAGE_CHANCE + RUIN_CHANCE) tryBuild(player, "ruin");
        }
    }

    private static void tryBuild(ServerPlayer player, String type) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();
        int sx = type.equals("village") ? 32 : 22;
        int sz = type.equals("village") ? 32 : 14;

        double angle = random.nextDouble() * Math.PI * 2;
        double dist = MIN_DIST + random.nextDouble() * (MAX_DIST - MIN_DIST);
        int cx = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
        int cz = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
        int ox = cx - sx / 2, oz = cz - sz / 2;

        for (ServerPlayer other : level.players()) {
            if (Math.hypot(other.getX() - cx, other.getZ() - cz) < 60) return;
        }
        Data data = Data.get(level);
        if (!data.farEnough(level, type, cx, cz)) return;
        BlockPos nat = level.findNearestMapStructure(tag(type), new BlockPos(cx, 64, cz), (type.equals("village") ? VILLAGE_SPACING : RUIN_SPACING) / 16, false);
        if (nat != null && Math.hypot(nat.getX() - cx, nat.getZ() - cz) < (type.equals("village") ? VILLAGE_SPACING : RUIN_SPACING)) return;
        Integer y = groundY(level, ox, oz, sx, sz);
        if (y == null) return;

        BlockPos origin = new BlockPos(ox, y, oz);
        if (type.equals("village")) StructureBuilder.village(level, origin);
        else StructureBuilder.ruin(level, origin);
        data.add(type, cx, cz);
    }

    /** 평평한 자연 지형이면 지을 높이, 아니면 null */
    private static Integer groundY(ServerLevel level, int ox, int oz, int sx, int sz) {
        int[][] pts = {{0, 0}, {sx, 0}, {0, sz}, {sx, sz}, {sx / 2, sz / 2}, {sx / 2, 0}, {sx / 2, sz}, {0, sz / 2}, {sx, sz / 2}};
        int[] hs = new int[pts.length];
        for (int i = 0; i < pts.length; i++) {
            int x = ox + pts[i][0], z = oz + pts[i][1];
            if (!level.hasChunk(x >> 4, z >> 4)) return null;
            int h = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockState ground = level.getBlockState(new BlockPos(x, h - 1, z));
            if (!isNaturalGround(ground)) return null;
            hs[i] = h;
        }
        Arrays.sort(hs);
        if (hs[hs.length - 1] - hs[0] > MAX_SLOPE) return null;
        return hs[hs.length / 2];
    }

    private static boolean isNaturalGround(BlockState s) {
        return s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.STONE) || s.is(Blocks.ANDESITE)
                || s.is(Blocks.DIORITE) || s.is(Blocks.GRANITE) || s.is(Blocks.GRAVEL) || s.is(Blocks.SNOW_BLOCK)
                || s.is(Blocks.CLAY) || s.is(Blocks.TERRACOTTA);
    }

    public static void markBase(ServerLevel level, BlockPos pos) {
        if (level.dimension() == Level.OVERWORLD) Data.get(level).addBase(new ChunkPos(pos));
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> structureTag(String type) {
        return tag(type);
    }

    /** 월드 생성 구조물 태그 */
    static net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> tag(String type) {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.STRUCTURE,
                com.tycooncraft.TycoonCraft.id(type.equals("village") ? "trader_village" : type.equals("hideout") ? "gang_hideout" : "abandoned_factory"));
    }

    public static BlockPos nearest(ServerLevel level, String type, BlockPos from) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        if (level.dimension() == Level.OVERWORLD) {
            BlockPos nat = level.findNearestMapStructure(tag(type), from, 64, false);
            if (nat != null) {
                best = nat;
                bestDist = Math.hypot(nat.getX() - from.getX(), nat.getZ() - from.getZ());
            }
        }
        for (Data.Entry e : Data.get(level).structures) {
            if (!e.type.equals(type)) continue;
            double d = Math.hypot(e.x - from.getX(), e.z - from.getZ());
            if (d < bestDist) {
                bestDist = d;
                best = new BlockPos(e.x, from.getY(), e.z);
            }
        }
        return best;
    }

    /** 생성된 구조물과 플레이어 기지(청크) 기록 */
    public static class Data extends SavedData {
        record Entry(String type, int x, int z) {}

        final List<Entry> structures = new ArrayList<>();
        final Set<Long> baseChunks = new HashSet<>();

        static Data get(ServerLevel level) {
            ServerLevel overworld = level.getServer().overworld();
            return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), "tycooncraft_worldgen");
        }

        boolean farEnough(ServerLevel level, String type, int cx, int cz) {
            BlockPos spawn = level.getSharedSpawnPos();
            if (Math.hypot(cx - spawn.getX(), cz - spawn.getZ()) < SPAWN_PROTECT) return false;
            for (Entry e : structures) {
                double d = Math.hypot(cx - e.x, cz - e.z);
                if (d < ANY_SPACING) return false;
                if (e.type.equals(type) && d < (type.equals("village") ? VILLAGE_SPACING : RUIN_SPACING)) return false;
            }
            for (long packed : baseChunks) {
                ChunkPos c = new ChunkPos(packed);
                if (Math.hypot(cx - c.getMiddleBlockX(), cz - c.getMiddleBlockZ()) < BASE_PROTECT) return false;
            }
            return true;
        }

        void add(String type, int x, int z) {
            structures.add(new Entry(type, x, z));
            setDirty();
        }

        void addBase(ChunkPos chunk) {
            if (baseChunks.add(chunk.toLong())) setDirty();
        }

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data d = new Data();
            ListTag list = tag.getList("structures", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                d.structures.add(new Entry(e.getString("type"), e.getInt("x"), e.getInt("z")));
            }
            for (long l : tag.getLongArray("bases")) d.baseChunks.add(l);
            return d;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            for (Entry e : structures) {
                CompoundTag c = new CompoundTag();
                c.putString("type", e.type);
                c.putInt("x", e.x);
                c.putInt("z", e.z);
                list.add(c);
            }
            tag.put("structures", list);
            tag.putLongArray("bases", baseChunks.stream().mapToLong(Long::longValue).toArray());
            return tag;
        }
    }

    private WorldGen() {}
}
