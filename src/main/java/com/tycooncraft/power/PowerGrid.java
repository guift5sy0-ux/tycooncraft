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
        public final Layer layer;
        public int supply, demand;
        /** 전력망: 수요 + 축전지가 더 받을 수 있는 양 (발전기가 일할지 정함) */
        public int want;
        public long stored;
        public int charged, discharged;
        public float ratio;
        boolean running;

        Net(Layer layer) {
            this.layer = layer;
        }
    }

    private static final class LevelGrid {
        final Layer layer;

        LevelGrid(Layer layer) {
            this.layer = layer;
        }

        final Set<BlockPos> nodes = new HashSet<>();
        final Map<BlockPos, Net> netOf = new HashMap<>();
        final List<Net> nets = new ArrayList<>();
        boolean dirty = true;
    }

    private static final Map<ResourceKey<Level>, LevelGrid[]> GRIDS = new HashMap<>();
    public static int rebuilds;

    private static LevelGrid grid(Level level, Layer l) {
        return GRIDS.computeIfAbsent(level.dimension(), k -> new LevelGrid[]{new LevelGrid(Layer.MECH), new LevelGrid(Layer.ELEC)})[l.ordinal()];
    }

    public static void add(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Layer l : Layer.values()) {
            LevelGrid g = grid(level, l);
            g.nodes.add(pos.immutable());
            g.dirty = true;
        }
    }

    public static void remove(Level level, BlockPos pos) {
        if (level.isClientSide) return;
        for (Layer l : Layer.values()) {
            LevelGrid g = grid(level, l);
            g.nodes.remove(pos);
            g.dirty = true;
        }
    }

    /** 축 방향 바뀜 등 연결 변화 */
    public static void changed(Level level) {
        if (!level.isClientSide) for (Layer l : Layer.values()) grid(level, l).dirty = true;
    }

    /** 이 위치 블록이 받는 (축) 동력 비율 (0 = 동력 없음, 1 = 충분) */
    public static float ratio(Level level, BlockPos pos) {
        return ratio(level, pos, Layer.MECH);
    }

    public static float ratio(Level level, BlockPos pos, Layer l) {
        Net n = grid(level, l).netOf.get(pos);
        return n == null ? 0f : n.ratio;
    }

    public static Net net(Level level, BlockPos pos) {
        return net(level, pos, Layer.MECH);
    }

    public static Net net(Level level, BlockPos pos, Layer l) {
        return grid(level, l).netOf.get(pos);
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
        Layer L = g.layer;
        g.nodes.removeIf(p -> node(level, p) == null);
        for (BlockPos start : g.nodes) {
            if (g.netOf.containsKey(start)) continue;
            PowerNode sn = node(level, start);
            if (sn == null || !sn.in(L) || sn.consumer(L)) continue;   // 네트워크는 전달체에서 시작
            Net net = new Net(L);
            ArrayDeque<BlockPos> q = new ArrayDeque<>();
            q.add(start);
            g.netOf.put(start, net);
            while (!q.isEmpty() && net.nodes.size() < 8192) {
                BlockPos p = q.poll();
                net.nodes.add(p);
                PowerNode a = node(level, p);
                if (a == null || a.consumer(L)) continue;
                for (Direction d : Direction.values()) {
                    BlockPos np = p.relative(d);
                    if (g.netOf.containsKey(np)) continue;
                    PowerNode b = node(level, np);
                    if (b == null || !b.in(L)) continue;
                    boolean link = b.consumer(L) ? a.drives(L, d) : a.conducts(L, d) && b.conducts(L, d.getOpposite());
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
        LevelGrid[] gs = GRIDS.get(level.dimension());
        if (gs == null) return;
        for (LevelGrid g : gs) tickGrid(level, g);
    }

    private static void tickGrid(ServerLevel level, LevelGrid g) {
        if (g.dirty) rebuild(level, g);
        Layer L = g.layer;
        List<PowerStorage> stores = new ArrayList<>();
        for (Net n : g.nets) {
            int s = 0, d = 0, room = 0, avail = 0;
            long st = 0;
            stores.clear();
            for (BlockPos p : n.nodes) {
                PowerNode pn = node(level, p);
                if (pn == null) continue;
                s += pn.supply(L);
                d += pn.demand(L);
                if (L == Layer.ELEC && pn instanceof PowerStorage ps) {
                    stores.add(ps);
                    room += ps.chargeRoom();
                    avail += ps.dischargeable();
                    st += ps.stored();
                }
            }
            n.charged = n.discharged = 0;
            // 축전지: 남으면 담고, 모자라면 꺼내 씀
            if (!stores.isEmpty()) {
                if (s > d) {
                    int left = Math.min(s - d, room);
                    n.charged = left;
                    for (PowerStorage ps : stores) {
                        if (left <= 0) break;
                        int c = Math.min(left, ps.chargeRoom());
                        ps.charge(c);
                        left -= c;
                    }
                } else if (d > s) {
                    int need = Math.min(d - s, avail);
                    n.discharged = need;
                    s += need;
                    for (PowerStorage ps : stores) {
                        if (need <= 0) break;
                        int c = Math.min(need, ps.dischargeable());
                        ps.discharge(c);
                        need -= c;
                    }
                }
            }
            n.supply = s;
            n.demand = d;
            n.stored = st + n.charged - n.discharged;
            n.want = d + room;
            n.ratio = d <= 0 ? (s > 0 ? 1f : 0f) : Math.min(1f, s / (float) d);
            boolean running = s > 0;
            if (running != n.running || level.getGameTime() % 100 == 0) {
                n.running = running;
                for (BlockPos p : n.nodes) {
                    PowerNode pn = node(level, p);
                    if (pn != null) pn.onNetState(L, running, n.ratio);
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
