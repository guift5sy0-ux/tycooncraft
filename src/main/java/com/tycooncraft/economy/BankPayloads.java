package com.tycooncraft.economy;

import com.tycooncraft.TycoonCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/** 은행 창 ↔ 서버 통신 */
public final class BankPayloads {
    public static final int OP_REFRESH = 0, OP_DEPOSIT = 1, OP_WITHDRAW = 2, OP_TRANSFER = 3, OP_LIST = 4,
            OP_BID = 5, OP_BUYOUT = 6, OP_CANCEL = 7, OP_CLAIM = 8,
            OP_ALLY_CREATE = 20, OP_ALLY_INVITE = 21, OP_ALLY_ACCEPT = 22, OP_ALLY_DECLINE = 23, OP_ALLY_LEAVE = 24, OP_ALLY_KICK = 25, OP_ALLY_DISBAND = 26;

    /** 동맹 정보 (보는 사람 기준). name 이 비어 있으면 소속 없음 */
    public record Ally(String name, String leader, boolean amLeader, List<String> members, List<Boolean> online, List<String> invites) {
        public static final Ally NONE = new Ally("", "", false, List.of(), List.of(), List.of());
    }

    /** 클라 → 서버: op, id(경매 번호/칸), x(기간), a·b(금액), s(받는 사람) */
    public record Action(int op, int id, int x, long a, long b, String s) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(TycoonCraft.id("bank_action"));
        public static final StreamCodec<ByteBuf, Action> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::op, ByteBufCodecs.VAR_INT, Action::id, ByteBufCodecs.VAR_INT, Action::x,
                ByteBufCodecs.VAR_LONG, Action::a, ByteBufCodecs.VAR_LONG, Action::b, ByteBufCodecs.stringUtf8(64), Action::s, Action::new);

        public static Action of(int op) {
            return new Action(op, 0, 0, 0, 0, "");
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 경매 한 건 (보는 사람 기준) */
    public record View(int id, ItemStack item, String seller, long start, long bid, long buyout, long remainMs, String bidder, boolean mine, boolean leading) {}

    /** 서버 → 클라: 계좌·경매 상태 + 방금 한 일의 결과 */
    public record Sync(long balance, int claims, List<BankData.LogEntry> log, List<View> auctions, Component msg, boolean ok, Ally ally) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(TycoonCraft.id("bank_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> CODEC = StreamCodec.of((buf, p) -> p.write(buf), Sync::read);

        void write(RegistryFriendlyByteBuf buf) {
            buf.writeVarLong(balance);
            buf.writeVarInt(claims);
            buf.writeVarInt(log.size());
            for (BankData.LogEntry l : log) {
                buf.writeUtf(l.type());
                buf.writeVarLong(l.amount());
                buf.writeUtf(l.other());
                buf.writeLong(l.time());
            }
            buf.writeVarInt(auctions.size());
            for (View v : auctions) {
                buf.writeVarInt(v.id());
                ItemStack.STREAM_CODEC.encode(buf, v.item());
                buf.writeUtf(v.seller());
                buf.writeVarLong(v.start());
                buf.writeVarLong(v.bid());
                buf.writeVarLong(v.buyout());
                buf.writeVarLong(v.remainMs());
                buf.writeUtf(v.bidder());
                buf.writeBoolean(v.mine());
                buf.writeBoolean(v.leading());
            }
            ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, msg);
            buf.writeBoolean(ok);
            buf.writeUtf(ally.name());
            buf.writeUtf(ally.leader());
            buf.writeBoolean(ally.amLeader());
            buf.writeVarInt(ally.members().size());
            for (int i = 0; i < ally.members().size(); i++) {
                buf.writeUtf(ally.members().get(i));
                buf.writeBoolean(ally.online().get(i));
            }
            buf.writeVarInt(ally.invites().size());
            for (String s : ally.invites()) buf.writeUtf(s);
        }

        static Sync read(RegistryFriendlyByteBuf buf) {
            long bal = buf.readVarLong();
            int claims = buf.readVarInt();
            int n = buf.readVarInt();
            List<BankData.LogEntry> log = new ArrayList<>();
            for (int i = 0; i < n; i++) log.add(new BankData.LogEntry(buf.readUtf(), buf.readVarLong(), buf.readUtf(), buf.readLong()));
            n = buf.readVarInt();
            List<View> views = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                views.add(new View(buf.readVarInt(), ItemStack.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readVarLong(), buf.readVarLong(),
                        buf.readVarLong(), buf.readVarLong(), buf.readUtf(), buf.readBoolean(), buf.readBoolean()));
            }
            Component msg = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(buf);
            boolean ok = buf.readBoolean();
            String an = buf.readUtf(), al = buf.readUtf();
            boolean lead = buf.readBoolean();
            int m = buf.readVarInt();
            List<String> mem = new ArrayList<>();
            List<Boolean> on = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                mem.add(buf.readUtf());
                on.add(buf.readBoolean());
            }
            int k = buf.readVarInt();
            List<String> inv = new ArrayList<>();
            for (int i = 0; i < k; i++) inv.add(buf.readUtf());
            return new Sync(bal, claims, log, views, msg, ok, new Ally(an, al, lead, mem, on, inv));
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Action.TYPE, Action.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) Bank.handle(sp, p);
        }));
        r.playToClient(Sync.TYPE, Sync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.tycooncraft.client.BankClient.onSync(p)));
    }

    private BankPayloads() {}
}
