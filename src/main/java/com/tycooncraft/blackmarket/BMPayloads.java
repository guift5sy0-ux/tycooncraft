package com.tycooncraft.blackmarket;

import com.tycooncraft.TycoonCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/** v0.28 블랙마켓 화면들 (의뢰 게시판 · 카지노 · 불법 개조점) ↔ 서버 */
public final class BMPayloads {
    /** 화면 종류 */
    public static final int MISSIONS = 0, SLOTS = 1, ROULETTE = 2, MODS = 3;

    /**
     * 서버 → 화면. lines: 화면별 줄 데이터(문자열), nums: 숫자 데이터, msg: 결과 메시지
     * 의뢰: nums = [평판점, 단계, 다음단계점, 진행중여부, 제안수] , lines = 진행중 설명 + 제안들 설명
     * 카지노: nums = [자금, 최대베팅, 단계, r0, r1, r2, 당첨금, 새결과여부]
     * 개조: nums = [단계, 총여부, 마스크, 가격0..5], lines = [총 이름]
     */
    public record Open(int screen, List<String> lines, List<Long> nums, String msg) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(TycoonCraft.id("bm_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = CustomPacketPayload.codec(Open::write, Open::read);

        static Open read(FriendlyByteBuf b) {
            int s = b.readVarInt();
            int n = b.readVarInt();
            List<String> l = new ArrayList<>();
            for (int i = 0; i < n; i++) l.add(b.readUtf());
            int m = b.readVarInt();
            List<Long> nu = new ArrayList<>();
            for (int i = 0; i < m; i++) nu.add(b.readVarLong());
            return new Open(s, l, nu, b.readUtf());
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(screen);
            b.writeVarInt(lines.size());
            for (String s : lines) b.writeUtf(s);
            b.writeVarInt(nums.size());
            for (long v : nums) b.writeVarLong(v);
            b.writeUtf(msg);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 화면 → 서버. 의뢰: action 0=수락(arg 번호) 1=완료 2=포기 / 카지노: action=게임, arg=베팅, arg2=선택 / 개조: arg=개조 번호 */
    public record Act(int screen, int action, long arg, int arg2) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(TycoonCraft.id("bm_act"));
        public static final StreamCodec<FriendlyByteBuf, Act> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.screen);
            b.writeVarInt(p.action);
            b.writeVarLong(p.arg);
            b.writeVarInt(p.arg2);
        }, b -> new Act(b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static String s(Component c) {
        return c.getString();
    }

    public static void missions(ServerPlayer sp, Component msg) {
        List<String> l = new ArrayList<>();
        List<Long> n = new ArrayList<>();
        int pts = Reputation.points(sp), t = Reputation.tier(pts);
        n.add((long) pts);
        n.add((long) t);
        n.add((long) (t + 1 < Reputation.NEED.length ? Reputation.NEED[t + 1] : -1));
        Missions.M a = Missions.active(sp);
        n.add(a == null ? 0L : 1L);
        if (a != null) l.add(s(describe(sp, a, true)));
        var os = Missions.offers(sp);
        n.add((long) os.size());
        for (Missions.M m : os) l.add(s(describe(sp, m, false)));
        PacketDistributor.sendToPlayer(sp, new Open(MISSIONS, l, n, msg == null ? "" : s(msg)));
    }

    static Component describe(ServerPlayer sp, Missions.M m, boolean active) {
        Component what = switch (m.type) {
            case Missions.DELIVERY -> Component.translatable("gui.tycooncraft.mission.delivery", m.target.getX(), m.target.getZ());
            case Missions.RETRIEVAL -> Component.translatable("gui.tycooncraft.mission.retrieval", m.target.getX(), m.target.getZ());
            default -> Component.translatable("gui.tycooncraft.mission.supply", Component.translatable(m.itemType().getDescriptionId()), m.count);
        };
        Component c = Component.empty().append(what).append(Component.translatable("gui.tycooncraft.mission.reward", com.tycooncraft.economy.Coins.format(m.reward), m.rep));
        if (active) {
            long left = Math.max(0, m.deadline - sp.server.overworld().getGameTime());
            c = c.copy().append(Component.translatable("gui.tycooncraft.mission.left", left / 1200));
        }
        return c;
    }

    public static void casino(ServerPlayer sp, int game, long[] res, Component msg) {
        List<Long> n = new ArrayList<>(List.of(Casino.funds(sp), Casino.maxBet(sp), (long) Reputation.tier(sp)));
        if (res != null) {
            for (long v : res) n.add(v);
            n.add(1L);
        } else {
            n.addAll(List.of(-1L, -1L, -1L, 0L, 0L));
        }
        PacketDistributor.sendToPlayer(sp, new Open(game == Casino.SLOTS ? SLOTS : ROULETTE, List.of(s(Reputation.tierName(Reputation.tier(sp)))), n, msg == null ? "" : s(msg)));
    }

    public static void mods(ServerPlayer sp, Component msg) {
        var gun = sp.getMainHandItem();
        boolean isGun = gun.getItem() instanceof com.tycooncraft.gun.GunItem;
        List<Long> n = new ArrayList<>(List.of((long) Reputation.tier(sp), isGun ? 1L : 0L, (long) (isGun ? IllegalMods.mask(gun) : 0)));
        for (int i = 0; i < IllegalMods.IDS.length; i++) n.add(IllegalMods.price(sp, i));
        PacketDistributor.sendToPlayer(sp, new Open(MODS, List.of(isGun ? gun.getHoverName().getString() : "", s(Reputation.describe(sp))), n, msg == null ? "" : s(msg)));
    }

    static boolean near(ServerPlayer sp) {
        return BlackMarket.isBM(sp.level()) && sp.position().x > -20 && sp.position().x < 40 && Math.abs(sp.position().z) < 12;
    }

    public static void handle(ServerPlayer sp, Act p) {
        if (!near(sp)) return;
        switch (p.screen()) {
            case MISSIONS -> {
                Component r = switch (p.action()) {
                    case 0 -> Missions.accept(sp, (int) p.arg());
                    case 1 -> Missions.turnIn(sp);
                    case 2 -> Missions.abandon(sp);
                    default -> null;
                };
                missions(sp, r);
            }
            case SLOTS, ROULETTE -> {
                int game = p.screen() == SLOTS ? Casino.SLOTS : Casino.ROULETTE;
                Component[] m = new Component[1];
                long[] res = Casino.play(sp, game, p.arg(), p.arg2(), m);
                casino(sp, game, res, m[0]);
            }
            case MODS -> mods(sp, IllegalMods.install(sp, (int) p.arg()));
            default -> {}
        }
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.tycooncraft.client.BMScreens.open(p)));
        r.playToServer(Act.TYPE, Act.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) handle(sp, p);
        }));
    }

    private BMPayloads() {}
}
