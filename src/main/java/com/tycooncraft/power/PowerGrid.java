package com.tycooncraft.power;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;

/**
 * 레벨마다 동력 네트워크를 관리. 블록이 놓이거나 사라지면 다시 묶고(flood fill),
 * 매 틱 끝에 네트워크마다 공급/수요를 합쳐 '충족 비율'을 정한다 → 기계는 다음 틱에 그 비율만큼 빠르게 일함.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID)
public final class PowerGrid {
    public static final class Net {
        public final List<BlockPos> nodes = new ArrayList<>();
        public int supply, demand;
        public float ratio;
        boolean running;
    }

    private static final class LevelGrid {
        final Set<BlockPos> nodes = new HashSet<>();
        final Map<BlockPos, Net> netOf = new HashMap<>();
        final List<Net> nets = new ArrayList<>();
        boolean dirty = true;
    }

    private static final Map<ResourceKey<Level>, LevelGrid> GRIDS = new HashMap<>();
    public static int rebuilds;

    private static LevelGrid grid(Level level) {
        return GRIDS.computeIfAbsent(level.dimension(), k -> new LevelGrid());
    }

    public static void add(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        LevelGrid g = grid(level);
        g.nodes.add(pos.immutable());
        g.dirty = true;
    }

    public static void remove(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        LevelGrid g = grid(level);
        g.nodes.remove(pos);
        g.dirty = true;
    }

    /** 축 방향 바뀜 등 연결 변화 */
    public static void changed(Level level) {
        if (!level.isClientSide) grid(level).dirty = true;
    }

    /** 이 위치 블록이 받는 동력 비율 (0 = 동력 없음, 1 = 충분) */
    public static float ratio(Level level, BlockPos pos) {
        Net n = grid(level).netOf.get(pos);
        return n == null ? 0f : n.ratio;
    }

    public static Net net(Level level, BlockPos pos) {
        return grid(level).netOf.get(pos);
    }

    private static PowerNode node(Level level, BlockPos p) {
        if (!level.isLoaded(p)) return null;
        BlockEntity be = level.getBlockEntity(p);
        return be instanceof PowerNode n ? n : null;
    }

    private static void rebuild(Level level, LevelGrid g) {
        rebuilds++;
        g.netOf.clear();
        g.nets.clear();
        g.nodes.removeIf(p -> node(level, p) == null);
        for (BlockPos start : g.nodes) {
            if (g.netOf.containsKey(start)) continue;
            PowerNode sn = node(level, start);
            if (sn == null || sn.consumer()) continue;   // 네트워크는 전달체에서 시작
            Net net = new Net();
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            q.add(start);
            g.netOf.put(start, net);
            while (!q.isEmpty() && net.nodes.size() < 8192) {
                BlockPos p = q.poll();
                net.nodes.add(p);
                PowerNode a = node(level, p);
                if (a == null || a.consumer()) continue;
                for (Direction d : Direction.values()) {
                    BlockPos np = p.relative(d);
                    if (g.netOf.containsKey(np)) continue;
                    PowerNode b = node(level, np);
                    if (b == null) continue;
                    boolean link = b.consumer() ? a.drives(d) : a.conducts(d) && b.conducts(d.getOpposite());
                    if (!link) continue;
                    g.netOf.put(np.immutable(), net);
                    q.add(np.immutable());
                }
            }
            g.nets.add(net);
        }
        g.dirty = false;
    }

    @SubscribeEvent
    public static void onTick(LevelTickEvent.Post e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        LevelGrid g = GRIDS.get(level.dimension());
        if (g == null) return;
        if (g.dirty) rebuild(level, g);
        for (Net n : g.nets) {
            int s = 0, d = 0;
            for (BlockPos p : n.nodes) {
                PowerNode pn = node(level, p);
                if (pn == null) continue;
                s += pn.supply();
                d += pn.demand();
            }
            n.supply = s;
            n.demand = d;
            n.ratio = d <= 0 ? (s > 0 ? 1f : 0f) : Math.min(1f, s / (float) d);
            boolean running = s > 0;
            if (running != n.running || level.getGameTime() % 100 == 0) {
                n.running = running;
                for (BlockPos p : n.nodes) {
                    PowerNode pn = node(level, p);
                    if (pn != null) pn.onNetState(running, n.ratio);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onStop(net.neoforged.neoforge.event.server.ServerStoppedEvent e) {
        clear();
    }

    /** 서버 종료·월드 바뀜 */
    public static void clear() {
        GRIDS.clear();
    }

    private PowerGrid() {}
}
