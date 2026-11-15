package com.tycooncraft.network;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.registry.ModSounds;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.LocomotiveEntity;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 경적/기적: 운전 중 스페이스 → 서버가 주변 모두에게 소리를 들려준다 */
public record HornPayload() implements CustomPacketPayload {
    public static final HornPayload INSTANCE = new HornPayload();
    public static final Type<HornPayload> TYPE = new Type<>(TycoonCraft.id("horn"));
    public static final StreamCodec<ByteBuf, HornPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, HornPayload::handle);
    }

    private static void handle(HornPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Entity v = ctx.player().getVehicle();
            if (v instanceof CarEntity) {
                v.level().playSound(null, v.getX(), v.getY(), v.getZ(), ModSounds.CAR_HORN.get(), SoundSource.NEUTRAL, 3.0f, 1.0f);
            } else if (v instanceof LocomotiveEntity) {
                v.level().playSound(null, v.getX(), v.getY(), v.getZ(), ModSounds.TRAIN_HORN.get(), SoundSource.NEUTRAL, 6.0f, 1.0f);
            }
        });
    }
}
