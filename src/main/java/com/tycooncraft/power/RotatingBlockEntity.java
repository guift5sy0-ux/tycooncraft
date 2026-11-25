package com.tycooncraft.power;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** 동력 네트워크에 붙고, 돌아가는지(spin)를 클라이언트에 알려 부드럽게 가속·감속하며 도는 블록의 공통 부분 */
public abstract class RotatingBlockEntity extends BlockEntity implements PowerNode {
    protected boolean spinning;
    /** 클라이언트: 회전 각(도)과 속도 */
    public float angle, angleO, speed;

    protected RotatingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) PowerGrid.add(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) PowerGrid.remove(level, worldPosition);
    }

    // ── v0.27.0 축 시스템 삭제: 옛 회전축·분배기도 전선처럼 전기를 잇는다 ──
    @Override
    public boolean in(Layer l) {
        return l == Layer.ELEC;
    }

    @Override
    public boolean conducts(Layer l, net.minecraft.core.Direction d) {
        return l == Layer.ELEC && conducts(d);
    }

    @Override
    public boolean drives(Layer l, net.minecraft.core.Direction d) {
        return l == Layer.ELEC && drives(d);
    }

    @Override
    public int supply(Layer l) {
        return l == Layer.ELEC ? supply() : 0;
    }

    @Override
    public int demand(Layer l) {
        return 0;
    }

    @Override
    public void onNetState(Layer l, boolean running, float ratio) {
        if (l == Layer.ELEC) onNetState(running, ratio);
    }

    @Override
    public void onNetState(boolean running, float ratio) {
        if (running != spinning) {
            spinning = running;
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public boolean spinning() {
        return spinning;
    }

    /** 최고 회전 속도 (도/틱) */
    protected float maxSpeed() {
        return 18f;
    }

    public void clientTick() {
        angleO = angle;
        speed += ((spinning ? maxSpeed() : 0f) - speed) * 0.08f;   // 서서히 돌기 시작하고 서서히 멈춤
        if (speed < 0.01f) speed = 0;
        angle = (angle + speed) % 360f;
        if (angle < angleO) angleO -= 360f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putBoolean("Spin", spinning);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        spinning = tag.getBoolean("Spin");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag t = super.getUpdateTag(reg);
        t.putBoolean("Spin", spinning);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
