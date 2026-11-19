package com.tycooncraft.cctv;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** CCTV 를 보는 사람 → 서버: 카메라 머리 방향 (다른 사람에게도 머리가 돌아가는 게 보임) */
public record CctvAimPayload(BlockPos pos, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<CctvAimPayload> TYPE = new Type<>(TycoonCraft.id("cctv_aim"));
    public static final StreamCodec<FriendlyByteBuf, CctvAimPayload> CODEC = CustomPacketPayload.codec(CctvAimPayload::write, CctvAimPayload::new);

    private CctvAimPayload(FriendlyByteBuf b) {
        this(b.readBlockPos(), b.readFloat(), b.readFloat());
    }

    private void write(FriendlyByteBuf b) {
        b.writeBlockPos(pos);
        b.writeFloat(yaw);
        b.writeFloat(pitch);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CctvAimPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            if (!sp.level().isLoaded(p.pos()) || sp.blockPosition().distSqr(p.pos()) > (ScreenBlock.MAX_RANGE + 16) * (ScreenBlock.MAX_RANGE + 16)) return;
            if (sp.level().getBlockEntity(p.pos()) instanceof CctvBlockEntity be) be.aimAndSync(p.yaw(), p.pitch());
        });
    }
}
