package com.tycooncraft.gun;

import com.tycooncraft.TycoonCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 총기 제작대: 제작법을 눌러 재료 채우기 */
public record GunWorkbenchPayload(int recipe, boolean max) implements CustomPacketPayload {
    public static final Type<GunWorkbenchPayload> TYPE = new Type<>(TycoonCraft.id("gun_workbench_fill"));
    public static final StreamCodec<ByteBuf, GunWorkbenchPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GunWorkbenchPayload::recipe, ByteBufCodecs.BOOL, GunWorkbenchPayload::max, GunWorkbenchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(PayloadRegistrar r) {
        r.playToServer(TYPE, CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player().containerMenu instanceof GunWorkbenchMenu m) m.fill(ctx.player(), p.recipe(), p.max());
        }));
    }
}
