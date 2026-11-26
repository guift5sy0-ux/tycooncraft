package com.tycooncraft.mech;

import com.tycooncraft.TycoonCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** v0.28.1 프로토-정찰 원격 조종 통신: 서버→조종자 "시작/끝", 조종자→서버 매 틱 입력 */
public final class ScoutRemote {
    /** 서버 → 조종자. entityId < 0 이면 조종 끝 */
    public record Link(int entityId) implements CustomPacketPayload {
        public static final Type<Link> TYPE = new Type<>(TycoonCraft.id("scout_link"));
        public static final StreamCodec<FriendlyByteBuf, Link> CODEC = CustomPacketPayload.codec((p, b) -> b.writeInt(p.entityId), b -> new Link(b.readInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 조종자 → 서버. act: 0 없음, ProtoScout.ACT_* , STOP */
    public record Input(int entityId, float fwd, float strafe, boolean jump, float yaw, float pitch, boolean slash, byte act) implements CustomPacketPayload {
        public static final byte STOP = 9;
        public static final Type<Input> TYPE = new Type<>(TycoonCraft.id("scout_input"));
        public static final StreamCodec<FriendlyByteBuf, Input> CODEC = CustomPacketPayload.codec((p, b) -> {
            b.writeVarInt(p.entityId);
            b.writeFloat(p.fwd);
            b.writeFloat(p.strafe);
            b.writeBoolean(p.jump);
            b.writeFloat(p.yaw);
            b.writeFloat(p.pitch);
            b.writeBoolean(p.slash);
            b.writeByte(p.act);
        }, b -> new Input(b.readVarInt(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readFloat(), b.readFloat(), b.readBoolean(), b.readByte()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void link(ServerPlayer sp, int id) {
        PacketDistributor.sendToPlayer(sp, new Link(id));
    }

    public static void register(PayloadRegistrar r) {
        r.playToClient(Link.TYPE, Link.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.tycooncraft.client.mech.ScoutRemoteClient.link(p.entityId())));
        r.playToServer(Input.TYPE, Input.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.level().getEntity(p.entityId()) instanceof ProtoScout m) m.remoteInput(sp, p);
        }));
    }

    private ScoutRemote() {}
}
