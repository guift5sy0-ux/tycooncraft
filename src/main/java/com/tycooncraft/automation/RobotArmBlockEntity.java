package com.tycooncraft.automation;

import com.tycooncraft.block.ConveyorBlock;
import com.tycooncraft.blockentity.ItemTransfer;
import com.tycooncraft.power.Layer;
import com.tycooncraft.power.PowerGrid;
import com.tycooncraft.power.PowerNode;
import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * v0.23.0 로봇 팔: 뒤쪽(상자·기계·컨베이어 위)에서 집어 앞쪽(상자·기계·컨베이어)으로 옮긴다.
 * 한 번에 최대 8개, 0.8초마다. 전기 4 ⚡ (전선에 붙여야 움직임). 필터를 정하면 그 아이템만 옮김.
 */
public class RobotArmBlockEntity extends BlockEntity implements PowerNode {
    public static final int CYCLE = 16, BATCH = 8, POWER = 4;
    public static int moves;
    private ItemStack filter = ItemStack.EMPTY;
    private boolean hasWork;
    private float progress;
    /** 클라이언트 애니메이션 */
    public long swingAt = -100;
    public Item carried;

    public RobotArmBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROBOT_ARM.get(), pos, state);
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
    public boolean consumer(Layer l) {
        return true;
    }

    @Override
    public int demand(Layer l) {
        return l == Layer.ELEC && hasWork ? POWER : 0;
    }

    public Direction facing() {
        return getBlockState().getValue(RobotArmBlock.FACING);
    }

    public ItemStack filter() {
        return filter;
    }

    public void setFilter(ItemStack st) {
        filter = st.isEmpty() ? ItemStack.EMPTY : st.copyWithCount(1);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private boolean matches(ItemStack s) {
        return !s.isEmpty() && (filter.isEmpty() || ItemStack.isSameItemSameComponents(s, filter));
    }

    public void serverTick() {
        if (level == null) return;
        if (!hasWork) {
            if ((level.getGameTime() + worldPosition.asLong()) % CYCLE == 0) hasWork = move(true);
            return;
        }
        progress += PowerGrid.ratio(level, worldPosition, Layer.ELEC);
        if (progress < CYCLE) return;
        progress = 0;
        hasWork = move(false);
    }

    /** 실제로 옮기거나(simulate=false) 옮길 수 있는지만 본다 */
    private boolean move(boolean simulate) {
        Direction f = facing();
        BlockPos src = worldPosition.relative(f.getOpposite()), dst = worldPosition.relative(f);
        IItemHandler from = level.getCapability(Capabilities.ItemHandler.BLOCK, src, f);
        IItemHandler to = level.getCapability(Capabilities.ItemHandler.BLOCK, dst, f.getOpposite());
        boolean dstBelt = to == null && level.getBlockState(dst).getBlock() instanceof ConveyorBlock;
        if (to == null && !dstBelt) return false;
        if (from != null) {
            for (int i = 0; i < from.getSlots(); i++) {
                ItemStack peek = from.extractItem(i, BATCH, true);
                if (!matches(peek)) continue;
                int can = dstBelt ? peek.getCount() : peek.getCount() - ItemHandlerHelper.insertItemStacked(to, peek, true).getCount();
                if (can <= 0) continue;
                if (simulate) return true;
                ItemStack got = from.extractItem(i, can, false);
                deliver(got, to, dst, f);
                return true;
            }
            return false;
        }
        // 상자가 아니면: 그 칸(컨베이어 위 등)에 떨어진 아이템을 집음
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(src).expandTowards(0, 0.5, 0))) {
            ItemStack s = e.getItem();
            if (!matches(s)) continue;
            ItemStack take = s.copyWithCount(Math.min(BATCH, s.getCount()));
            int can = dstBelt ? take.getCount() : take.getCount() - ItemHandlerHelper.insertItemStacked(to, take, true).getCount();
            if (can <= 0) continue;
            if (simulate) return true;
            ItemStack got = s.split(can);
            if (s.isEmpty()) e.discard();
            else e.setItem(s);
            deliver(got, to, dst, f);
            return true;
        }
        return false;
    }

    private void deliver(ItemStack got, IItemHandler to, BlockPos dst, Direction f) {
        ItemStack rest = to != null ? ItemHandlerHelper.insertItemStacked(to, got, false) : ItemTransfer.output(level, worldPosition, f, got, false);
        if (!rest.isEmpty()) ItemTransfer.spawnAt(level, worldPosition, f, rest);
        moves++;
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, Item.getId(got.getItem()));
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.2f, 1.6f + level.random.nextFloat() * 0.2f);
    }

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id == 1) {
            if (level != null && level.isClientSide) {
                swingAt = level.getGameTime();
                carried = Item.byId(param);
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        if (!filter.isEmpty()) tag.put("Filter", filter.save(reg));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        filter = tag.contains("Filter") ? ItemStack.parseOptional(reg, tag.getCompound("Filter")) : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag t = new CompoundTag();
        saveAdditional(t, reg);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
