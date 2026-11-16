package com.tycooncraft.network;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.vehicle.DroneEntity;
import com.tycooncraft.vehicle.VehicleHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** 드론 조종 통신: 서버→클라 "조종 시작", 클라→서버 "위치·방향·행동" (매 틱) */
public final class DronePayloads {

    /** 서버 → 조종자: 이 드론으로 카메라 전환 */
    public record Pilot(int entityId) implements CustomPacketPayload {
        public static final Type<Pilot> TYPE = new Type<>(TycoonCraft.id("drone_pilot"));
        public static final StreamCodec<FriendlyByteBuf, Pilot> CODEC = CustomPacketPayload.codec(Pilot::write, Pilot::new);

        private Pilot(FriendlyByteBuf buf) {
            this(buf.readVarInt());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** 조종자 → 서버. action: 0 비행, 1 폭파, 2 조종 끝 */
    public record Control(int entityId, double x, double y, double z, float yaw, float pitch, byte action) implements CustomPacketPayload {
        public static final Type<Control> TYPE = new Type<>(TycoonCraft.id("drone_control"));
        public static final StreamCodec<FriendlyByteBuf, Control> CODEC = CustomPacketPayload.codec(Control::write, Control::new);

        private Control(FriendlyByteBuf buf) {
            this(buf.readVarInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readByte());
        }

        private void write(FriendlyByteBuf buf) {
            buf.writeVarInt(entityId);
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeFloat(yaw);
            buf.writeFloat(pitch);
            buf.writeByte(action);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Pilot.TYPE, Pilot.CODEC, (p, ctx) -> ctx.enqueueWork(() -> VehicleHooks.client.startPilot(p.entityId())));
        registrar.playToServer(Control.TYPE, Control.CODEC, DronePayloads::handleControl);
    }

    private static void handleControl(Control c, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            Entity e = sp.level().getEntity(c.entityId());
            if (!(e instanceof DroneEntity drone) || !sp.getUUID().equals(drone.ownerId())) return;
            drone.applyPilotUpdate(sp, c.x(), c.y(), c.z(), c.yaw(), c.pitch());
            if (c.action() == 1 || c.action() == 2) drone.detonate();   // 자폭 드론은 취소 없음
        });
    }

    private DronePayloads() {}
}
