package com.tycooncraft.blackmarket;

import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.Coins;
import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * v0.28 의뢰 게시판 (암살 의뢰 없음):
 *  - 배달: 밀수 화물을 지상의 접선 지점까지 (도착 근처에서 갱단이 덮칠 수 있음)
 *  - 회수: 지상 어딘가에 숨겨진 도난 화물 통을 찾아 게시판으로 가져오기 (갱단이 지킴)
 *  - 납품: 정해진 물건을 게시판에 바로 넘기기
 * 하루(게임) 3건씩 새로 올라오고, 한 번에 하나만 맡을 수 있음. 제한 시간 2일. 실패·포기 시 평판 -15.
 */
public final class Missions extends SavedData {
    public static final int DELIVERY = 0, RETRIEVAL = 1, SUPPLY = 2;
    public static final long LIMIT = 48_000;
    public static final String TAG = "tycooncraft_mission";
    public static int completed, failed, ambushes;

    public static final class M {
        public int type, count, rep;
        public long reward, deadline;
        public BlockPos target = BlockPos.ZERO;
        public String item = "";
        public String dim = "minecraft:overworld";
        public long id;
        public boolean placed, ambushed;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("type", type);
            t.putInt("count", count);
            t.putInt("rep", rep);
            t.putLong("reward", reward);
            t.putLong("deadline", deadline);
            t.putLong("target", target.asLong());
            t.putString("item", item);
            t.putString("dim", dim);
            t.putLong("id", id);
            t.putBoolean("placed", placed);
            t.putBoolean("ambushed", ambushed);
            return t;
        }

        static M load(CompoundTag t) {
            M m = new M();
            m.type = t.getInt("type");
            m.count = t.getInt("count");
            m.rep = t.getInt("rep");
            m.reward = t.getLong("reward");
            m.deadline = t.getLong("deadline");
            m.target = BlockPos.of(t.getLong("target"));
            m.item = t.getString("item");
            m.dim = t.getString("dim");
            m.id = t.getLong("id");
            m.placed = t.getBoolean("placed");
            m.ambushed = t.getBoolean("ambushed");
            return m;
        }

        public Item itemType() {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(item));
        }
    }

    static final class P {
        long offerDay = -1;
        final List<M> offers = new ArrayList<>();
        M active;
    }

    private final Map<UUID, P> players = new HashMap<>();

    public static Missions get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(Missions::new, Missions::load), "tycooncraft_bm_missions");
    }

    static Missions load(CompoundTag t, HolderLookup.Provider r) {
        Missions d = new Missions();
        CompoundTag ps = t.getCompound("p");
        for (String k : ps.getAllKeys()) {
            CompoundTag c = ps.getCompound(k);
            P p = new P();
            p.offerDay = c.getLong("day");
            for (var e : c.getList("offers", 10)) p.offers.add(M.load((CompoundTag) e));
            if (c.contains("active")) p.active = M.load(c.getCompound("active"));
            d.players.put(UUID.fromString(k), p);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        CompoundTag ps = new CompoundTag();
        players.forEach((k, p) -> {
            CompoundTag c = new CompoundTag();
            c.putLong("day", p.offerDay);
            var l = new net.minecraft.nbt.ListTag();
            for (M m : p.offers) l.add(m.save());
            c.put("offers", l);
            if (p.active != null) c.put("active", p.active.save());
            ps.put(k.toString(), c);
        });
        t.put("p", ps);
        return t;
    }

    P of(UUID u) {
        return players.computeIfAbsent(u, k -> new P());
    }

    public static M active(ServerPlayer sp) {
        return get(sp.server).of(sp.getUUID()).active;
    }

    // ── 납품 품목 (값은 기준가로 계산) ──
    static final Object[][] SUPPLY_POOL = {
            {"minecraft:gunpowder", 32}, {"tycooncraft:steel_ingot", 16}, {"minecraft:iron_ingot", 48}, {"tycooncraft:circuit", 12},
            {"minecraft:diamond", 4}, {"minecraft:redstone", 48}, {"minecraft:coal", 64}, {"tycooncraft:heavy_ammo", 32},
            {"minecraft:gold_ingot", 16}, {"tycooncraft:advanced_circuit", 4}, {"minecraft:emerald", 12}, {"minecraft:blaze_rod", 8}};

    public static List<M> offers(ServerPlayer sp) {
        Missions d = get(sp.server);
        P p = d.of(sp.getUUID());
        long day = sp.server.overworld().getDayTime() / 24000L;
        if (p.offerDay != day) {
            p.offerDay = day;
            p.offers.clear();
            RandomSource r = RandomSource.create(day * 31 + sp.getUUID().getLeastSignificantBits());
            int[] types = {DELIVERY, RETRIEVAL, SUPPLY};
            for (int i = 0; i < 3; i++) p.offers.add(make(sp, types[i], r));
            d.setDirty();
        }
        return p.offers;
    }

    static M make(ServerPlayer sp, int type, RandomSource r) {
        M m = new M();
        m.type = type;
        m.id = r.nextLong();
        double mult = 1 + 0.12 * Reputation.tier(sp);
        switch (type) {
            case DELIVERY -> {
                m.reward = Math.round((40_000 + r.nextInt(40_000)) * mult / 1000) * 1000;
                m.rep = 25 + r.nextInt(16);
                m.target = spot(sp, r);
            }
            case RETRIEVAL -> {
                m.reward = Math.round((60_000 + r.nextInt(60_000)) * mult / 1000) * 1000;
                m.rep = 35 + r.nextInt(26);
                m.target = spot(sp, r);
            }
            default -> {
                Object[] e = SUPPLY_POOL[r.nextInt(SUPPLY_POOL.length)];
                m.item = (String) e[0];
                m.count = (int) e[1];
                double unit = com.tycooncraft.economy.PriceTable.basePrice(m.itemType());
                m.reward = Math.max(15_000, Math.round(unit * m.count * 2.5 * mult / 100) * 100);
                m.rep = 10 + r.nextInt(11);
            }
        }
        return m;
    }

    /** 들어왔던 맨홀(없으면 월드 스폰) 기준 300~700블록 떨어진 지상 좌표 (y 는 도착 때 정함) */
    static BlockPos spot(ServerPlayer sp, RandomSource r) {
        CompoundTag ret = BlackMarket.persisted(sp).getCompound(BlackMarket.RET);
        BlockPos o = ret.contains("x") ? new BlockPos(ret.getInt("x"), 64, ret.getInt("z")) : sp.server.overworld().getSharedSpawnPos();
        double a = r.nextDouble() * Math.PI * 2, dist = 300 + r.nextInt(400);
        return new BlockPos(o.getX() + (int) (Math.cos(a) * dist), 64, o.getZ() + (int) (Math.sin(a) * dist));
    }

    // ── 수락 · 납품 · 포기 ──
    public static Component accept(ServerPlayer sp, int idx) {
        Missions d = get(sp.server);
        P p = d.of(sp.getUUID());
        if (p.active != null) return Component.translatable("message.tycooncraft.mission.busy").withStyle(ChatFormatting.RED);
        List<M> os = offers(sp);
        if (idx < 0 || idx >= os.size()) return Component.empty();
        M m = os.remove(idx);
        m.deadline = sp.server.overworld().getGameTime() + LIMIT;
        p.active = m;
        d.setDirty();
        if (m.type == DELIVERY) {
            ItemStack pkg = new ItemStack(ModItems.SMUGGLE_PACKAGE.get());
            tag(pkg, m);
            if (!sp.getInventory().add(pkg)) sp.drop(pkg, false);
        }
        sp.playNotifySound(SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 0.8f);
        return Component.translatable("message.tycooncraft.mission.accepted." + m.type,
                m.target.getX(), m.target.getZ(), m.count, Component.translatable(m.itemType().getDescriptionId())).withStyle(ChatFormatting.GREEN);
    }

    static void tag(ItemStack s, M m) {
        CompoundTag t = new CompoundTag();
        t.putLong(TAG, m.id);
        t.putInt("tx", m.target.getX());
        t.putInt("tz", m.target.getZ());
        s.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }

    static long idOf(ItemStack s) {
        CustomData cd = s.get(DataComponents.CUSTOM_DATA);
        return cd == null ? 0 : cd.copyTag().getLong(TAG);
    }

    /** 게시판에서 [완료]: 납품(물건) · 회수(화물) */
    public static Component turnIn(ServerPlayer sp) {
        Missions d = get(sp.server);
        P p = d.of(sp.getUUID());
        M m = p.active;
        if (m == null) return Component.empty();
        var inv = sp.getInventory();
        if (m.type == SUPPLY) {
            Item it = m.itemType();
            int have = 0;
            for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(it)) have += inv.getItem(i).getCount();
            if (have < m.count) return Component.translatable("message.tycooncraft.mission.not_enough", have, m.count).withStyle(ChatFormatting.RED);
            int left = m.count;
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.is(it)) continue;
                int k = Math.min(left, s.getCount());
                s.shrink(k);
                left -= k;
            }
        } else if (m.type == RETRIEVAL) {
            int slot = -1;
            for (int i = 0; i < inv.getContainerSize(); i++)
                if (inv.getItem(i).is(ModItems.STOLEN_CARGO.get()) && idOf(inv.getItem(i)) == m.id) slot = i;
            if (slot < 0) return Component.translatable("message.tycooncraft.mission.no_cargo").withStyle(ChatFormatting.RED);
            inv.getItem(slot).shrink(1);
        } else return Component.translatable("message.tycooncraft.mission.go_drop", m.target.getX(), m.target.getZ()).withStyle(ChatFormatting.YELLOW);
        return complete(sp, d, p);
    }

    static Component complete(ServerPlayer sp, Missions d, P p) {
        M m = p.active;
        p.active = null;
        d.setDirty();
        var acc = BankData.get(sp.server).account(sp.getUUID());
        acc.balance += m.reward;
        BankData.get(sp.server).setDirty();
        Reputation.add(sp, m.rep);
        completed++;
        sp.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.2f);
        Component c = Component.translatable("message.tycooncraft.mission.done", Coins.format(m.reward), m.rep).withStyle(ChatFormatting.GOLD);
        sp.sendSystemMessage(c);
        return c;
    }

    public static Component abandon(ServerPlayer sp) {
        Missions d = get(sp.server);
        P p = d.of(sp.getUUID());
        if (p.active == null) return Component.empty();
        fail(sp, d, p, "message.tycooncraft.mission.abandoned");
        return Component.translatable("message.tycooncraft.mission.abandoned").withStyle(ChatFormatting.RED);
    }

    static void fail(ServerPlayer sp, Missions d, P p, String key) {
        M m = p.active;
        p.active = null;
        d.setDirty();
        failed++;
        Reputation.add(sp, Reputation.FAIL);
        // 남은 화물은 회수
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if ((s.is(ModItems.SMUGGLE_PACKAGE.get()) || s.is(ModItems.STOLEN_CARGO.get())) && idOf(s) == m.id) inv.setItem(i, ItemStack.EMPTY);
        }
        sp.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.RED));
    }

    /** 밀수 화물 우클릭: 접선 지점 근처면 배달 완료 */
    public static boolean deliver(ServerPlayer sp, ItemStack pkg) {
        Missions d = get(sp.server);
        P p = d.of(sp.getUUID());
        M m = p.active;
        if (m == null || m.type != DELIVERY || idOf(pkg) != m.id) {
            sp.displayClientMessage(Component.translatable("message.tycooncraft.mission.stale"), true);
            return false;
        }
        if (sp.level().dimension() != Level.OVERWORLD || horiz(sp.blockPosition(), m.target) > 6) {
            sp.displayClientMessage(Component.translatable("message.tycooncraft.mission.far", (int) horiz(sp.blockPosition(), m.target)), true);
            return false;
        }
        pkg.shrink(1);
        complete(sp, d, p);
        return true;
    }

    static double horiz(BlockPos a, BlockPos b) {
        return Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getZ() - b.getZ(), 2));
    }

    // ── 매 초: 기한 · 방향 안내 · 회수 통 배치 · 매복 ──
    public static void tick(MinecraftServer s) {
        if (s.getTickCount() % 20 != 0) return;
        Missions d = get(s);
        long now = s.overworld().getGameTime();
        for (ServerPlayer sp : s.getPlayerList().getPlayers()) {
            P p = d.players.get(sp.getUUID());
            if (p == null || p.active == null) continue;
            M m = p.active;
            if (now > m.deadline) {
                fail(sp, d, p, "message.tycooncraft.mission.timeout");
                continue;
            }
            if (m.type == SUPPLY || sp.level().dimension() != Level.OVERWORLD) continue;
            boolean carrying = m.type == DELIVERY ? holding(sp, ModItems.SMUGGLE_PACKAGE.get(), m.id) : !m.placed;
            double dist = horiz(sp.blockPosition(), m.target);
            if (carrying || (m.type == RETRIEVAL && !holding(sp, ModItems.STOLEN_CARGO.get(), m.id)))
                if (sp.getMainHandItem().is(ModItems.SMUGGLE_PACKAGE.get()) || sp.getMainHandItem().is(Items.COMPASS) || dist < 120)
                    sp.displayClientMessage(Component.translatable("message.tycooncraft.mission.nav", (int) dist, dir(sp, m.target)).withStyle(ChatFormatting.YELLOW), true);
            ServerLevel ow = (ServerLevel) sp.level();
            if (m.type == RETRIEVAL && !m.placed && dist < 48) {
                placeCrate(ow, m);
                d.setDirty();
            }
            if (!m.ambushed && dist < (m.type == RETRIEVAL ? 40 : 60)) {
                m.ambushed = true;
                d.setDirty();
                if (m.type == RETRIEVAL || sp.getRandom().nextFloat() < 0.4f) {
                    ambushes++;
                    GangHunters.squad(ow, sp);
                }
            }
        }
    }

    static boolean holding(ServerPlayer sp, Item it, long id) {
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(it) && idOf(inv.getItem(i)) == id) return true;
        return false;
    }

    static Component dir(ServerPlayer sp, BlockPos t) {
        double ang = Math.toDegrees(Math.atan2(t.getZ() + 0.5 - sp.getZ(), t.getX() + 0.5 - sp.getX())) - 90;
        float rel = Mth.wrapDegrees((float) ang - sp.getYRot());
        String[] arrows = {"⬆", "⬈", "➡", "⬊", "⬇", "⬋", "⬅", "⬉"};
        int k = Math.floorMod(Math.round(rel / 45f), 8);
        return Component.literal(arrows[k]);
    }

    /** 회수 의뢰: 지면에 도난 화물 통 (화물 1 + 잡동사니) */
    static void placeCrate(ServerLevel l, M m) {
        BlockPos top = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, m.target);
        if (!l.getFluidState(top.below()).isEmpty()) top = top.above();
        m.target = top;
        m.placed = true;
        l.setBlockAndUpdate(top, Blocks.BARREL.defaultBlockState());
        l.setBlockAndUpdate(top.above(), Blocks.REDSTONE_TORCH.defaultBlockState());
        if (l.getBlockEntity(top) instanceof BarrelBlockEntity b) {
            ItemStack cargo = new ItemStack(ModItems.STOLEN_CARGO.get());
            tag(cargo, m);
            b.setItem(13, cargo);
            b.setItem(4, new ItemStack(Items.GUNPOWDER, 4 + l.random.nextInt(8)));
            b.setItem(22, new ItemStack(Items.IRON_INGOT, 2 + l.random.nextInt(6)));
        }
        l.playSound(null, top, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 1f, 0.8f);
    }

    /** 테스트용: 지정 위치를 목표로 의뢰를 강제로 맡김 */
    public static M force(ServerPlayer sp, int type, BlockPos target) {
        Missions d = get(sp.server);
        P p = d.of(sp.getUUID());
        M m = make(sp, type, sp.getRandom());
        if (target != null) m.target = target;
        m.deadline = sp.server.overworld().getGameTime() + LIMIT;
        p.active = m;
        d.setDirty();
        if (type == DELIVERY) {
            ItemStack pkg = new ItemStack(ModItems.SMUGGLE_PACKAGE.get());
            tag(pkg, m);
            sp.getInventory().add(pkg);
        }
        return m;
    }

    private Missions() {}
}
