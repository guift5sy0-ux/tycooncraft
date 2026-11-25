package com.tycooncraft.blackmarket;

import com.tycooncraft.TycoonCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

/** 현상금 게시판 화면 ↔ 서버 */
public final class BountyPayloads {
    /** 서버 → 화면: 게시판 목록 + 내 은행 잔고 + 결과 메시지 */
    public record Open(List<String> names, List<Long> amounts, long balance, String msg) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(TycoonCraft.id("bounty_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = CustomPacketPayload.codec(Open::write, Open::read);

        static Open read(FriendlyByteBuf b) {
            int n = b.readVarInt();
            List<String> ns = new ArrayList<>();
            List<Long> as = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                ns.add(b.readUtf());
                as.add(b.readVarLong());
            }
            return new Open(ns, as, b.readVarLong(), b.readUtf());
        }

        void write(FriendlyByteBuf b) {
            b.writeVarInt(names.size());
            for (int i = 0; i < names.size(); i++) {
                b.writeUtf(names.get(i));
                b.writeVarLong(amounts.get(i));
            }
            b.writeVarLong(balance);
            b.writeUtf(msg);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 화면 → 서버: 현상금 걸기 */
    public record Post(String name, long amount) implements CustomPacketPayload {
        public static final Type<Post> TYPE = new Type<>(TycoonCraft.id("bounty_post"));
        public static final StreamCodec<FriendlyByteBuf, Post> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeUtf(p.name, 32);
            b.writeVarLong(p.amount);
        }, b -> new Post(b.readUtf(32), b.readVarLong()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void send(ServerPlayer p, Component msg) {
        BountyData d = BountyData.get(p.server);
        List<String> ns = new ArrayList<>();
        List<Long> as = new ArrayList<>();
        for (var e : d.board()) {
            if (ns.size() >= 12) break;
            ns.add(e.getKey());
            as.add(e.getValue());
        }
        long bal = com.tycooncraft.economy.BankData.get(p.server).account(p.getUUID()).balance;
        PacketDistributor.sendToPlayer(p, new Open(ns, as, bal, msg == null ? "" : msg.getString()));
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Open.TYPE, Open.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.tycooncraft.client.BountyScreen.open(p)));
        r.playToServer(Post.TYPE, Post.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp) || !BlackMarket.isBM(sp.level())) return;
            if (sp.blockPosition().distSqr(BlackMarketHub.BOUNTY) > 64) return;
            send(sp, BountyData.post(sp, p.name().trim(), p.amount()));
        }));
    }

    private BountyPayloads() {}
}
