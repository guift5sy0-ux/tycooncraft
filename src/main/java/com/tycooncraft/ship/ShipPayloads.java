package com.tycooncraft.ship;

import com.tycooncraft.TycoonCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 배 조종 입력(W/S, A/D)과 조종기 사용(우클릭/웅크리고 우클릭) */
public final class ShipPayloads {
    public record Input(float forward, float turn, float vertical) implements CustomPacketPayload {
        public static final Type<Input> TYPE = new Type<>(TycoonCraft.id("ship_input"));
        public static final StreamCodec<ByteBuf, Input> CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Input::forward, ByteBufCodecs.FLOAT, Input::turn, ByteBufCodecs.FLOAT, Input::vertical, Input::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Use(int ship, boolean sneak) implements CustomPacketPayload {
        public static final Type<Use> TYPE = new Type<>(TycoonCraft.id("ship_use"));
        public static final StreamCodec<ByteBuf, Use> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Use::ship, ByteBufCodecs.BOOL, Use::sneak, Use::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(Input.TYPE, Input.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player().getVehicle() instanceof ShipEntity s) s.input(ctx.player(), p.forward(), p.turn(), p.vertical());
        }));
        r.playToServer(Use.TYPE, Use.CODEC, (p, ctx) -> ctx.enqueueWork(() -> use(p, ctx)));
    }

    private static void use(Use p, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sp && sp.level().getEntity(p.ship()) instanceof ShipEntity s) s.useHelm(sp, p.sneak());
    }

    private ShipPayloads() {}
}
