package com.tycooncraft.gear;

import com.tycooncraft.TycoonCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 진압용 방패를 든 채 Q → 돌진 */
public record ShieldBashPayload() implements CustomPacketPayload {
    public static final Type<ShieldBashPayload> TYPE = new Type<>(TycoonCraft.id("shield_bash"));
    public static final StreamCodec<ByteBuf, ShieldBashPayload> CODEC = StreamCodec.unit(new ShieldBashPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) PoliceShieldItem.bash(sp);
        }));
    }
}
