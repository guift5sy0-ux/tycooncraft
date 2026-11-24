package com.tycooncraft.power.elec;

import com.tycooncraft.power.Layer;
import com.tycooncraft.power.PowerGrid;
import com.tycooncraft.power.PowerNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** v0.22.0: 전력망 블록 공통 — 놓이면 망에 등록, 전선 쪽은 여섯 면 모두로 이어짐 */
public abstract class GridBlockEntity extends BlockEntity implements PowerNode {
    protected GridBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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

    @Override
    public boolean in(Layer l) {
        return l == Layer.ELEC;
    }

    @Override
    public boolean conducts(Layer l, Direction d) {
        return l == Layer.ELEC;
    }

    @Override
    public boolean consumer(Layer l) {
        return false;
    }

    public void serverTick() {}

    /** 우클릭 상태 표시: 이 블록이 속한 전력망 */
    public Component netStatus() {
        var n = level == null ? null : PowerGrid.net(level, worldPosition, Layer.ELEC);
        if (n == null) return Component.translatable("message.tycooncraft.elec.none");
        return Component.translatable("message.tycooncraft.elec.status", n.supply, n.demand, n.stored, n.charged);
    }
}
