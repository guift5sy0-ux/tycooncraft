package com.tycooncraft.mech;

import com.tycooncraft.TycoonCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 탑승자 → 서버: 프로토 디스트로이어 무기 (1 개틀링, 2 로켓, 3 내려찍기) */
public record MechActionPayload(int entityId, byte action) implements CustomPacketPayload {
    public static final Type<MechActionPayload> TYPE = new Type<>(TycoonCraft.id("mech_action"));
    public static final StreamCodec<FriendlyByteBuf, MechActionPayload> CODEC = CustomPacketPayload.codec(MechActionPayload::write, MechActionPayload::new);

    private MechActionPayload(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readByte());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(entityId);
        buf.writeByte(action);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MechActionPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp && sp.level().getEntity(p.entityId()) instanceof ProtoDestroyer m) m.action(sp, p.action());
        });
    }
}
