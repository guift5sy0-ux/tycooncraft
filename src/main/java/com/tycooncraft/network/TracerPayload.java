package com.tycooncraft.network;

import com.tycooncraft.TycoonCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/**
 * 서버 → 클라이언트: 총알 한 발의 궤적 (눈에 보이는 총알·총구 화염·스치는 소리·탄착 효과는 클라이언트가 그린다)
 * kind: 0 일반(노랑) 1 화염(주황) 2 심판자(보라) 3 기관포(빨강)  impact: 0 허공 1 블록 2 생물
 */
public record TracerPayload(Vec3 from, Vec3 to, byte kind, byte impact, int shooter, float speed, boolean flash) implements CustomPacketPayload {
    public static final Type<TracerPayload> TYPE = new Type<>(TycoonCraft.id("tracer"));
    public static final StreamCodec<FriendlyByteBuf, TracerPayload> CODEC = StreamCodec.of((buf, p) -> {
        buf.writeDouble(p.from.x);
        buf.writeDouble(p.from.y);
        buf.writeDouble(p.from.z);
        buf.writeDouble(p.to.x);
        buf.writeDouble(p.to.y);
        buf.writeDouble(p.to.z);
        buf.writeByte(p.kind);
        buf.writeByte(p.impact);
        buf.writeVarInt(p.shooter);
        buf.writeFloat(p.speed);
        buf.writeBoolean(p.flash);
    }, buf -> new TracerPayload(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()),
            new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()), buf.readByte(), buf.readByte(), buf.readVarInt(), buf.readFloat(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
