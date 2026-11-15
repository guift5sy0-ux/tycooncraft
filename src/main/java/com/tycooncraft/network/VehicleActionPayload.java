package com.tycooncraft.network;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.vehicle.VehicleEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** 탑승자 → 서버: 충돌 폭발, 주포·기관포 발사, 미사일 발사 */
public record VehicleActionPayload(int entityId, byte action) implements CustomPacketPayload {
    public static final Type<VehicleActionPayload> TYPE = new Type<>(TycoonCraft.id("vehicle_action"));
    public static final StreamCodec<FriendlyByteBuf, VehicleActionPayload> CODEC = CustomPacketPayload.codec(VehicleActionPayload::write, VehicleActionPayload::new);

    private VehicleActionPayload(FriendlyByteBuf buf) {
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

    public static void handle(VehicleActionPayload p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            Entity e = sp.level().getEntity(p.entityId());
            if (e instanceof VehicleEntity v && v.hasPassenger(sp)) v.handleAction(sp, p.action());
        });
    }
}
