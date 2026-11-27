package com.tycooncraft.network;

import com.tycooncraft.TycoonCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** v0.32: 누가 재장전을 시작함 → 보는 사람들이 그 손동작과 소리를 맞춰 재생 (ticks: 걸리는 시간, shells: 넣은 탄 수) */
public record ReloadAnimPayload(int entity, int ticks, int shells, boolean wasEmpty) implements CustomPacketPayload {
    public static final Type<ReloadAnimPayload> TYPE = new Type<>(TycoonCraft.id("reload_anim"));
    public static final StreamCodec<FriendlyByteBuf, ReloadAnimPayload> CODEC = StreamCodec.of((buf, p) -> {
        buf.writeVarInt(p.entity);
        buf.writeVarInt(p.ticks);
        buf.writeVarInt(p.shells);
        buf.writeBoolean(p.wasEmpty);
    }, buf -> new ReloadAnimPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
