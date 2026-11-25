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
    /** 한 동작 = 24틱(1.2초): 내려가 집기 → 들어 올려 돌기 → 내려놓기(18틱째 실제로 넣음) → 돌아오기 */
    public static final int CYCLE = 24, DELIVER = 18, BATCH = 12, POWER = 4;
    public static int moves;
    private ItemStack filter = ItemStack.EMPTY;
    private ItemStack held = ItemStack.EMPTY;
    private int action = -1;
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
        return l == Layer.ELEC && (hasWork || action >= 0) ? POWER : 0;
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

    public ItemStack held() {
        return held;
    }

    public void serverTick() {
        if (level == null) return;
        if (action >= 0) {
            action++;
            if (action == DELIVER && !held.isEmpty()) {
                deliver(held);
                held = ItemStack.EMPTY;
                setChanged();
            }
            if (action >= CYCLE) action = -1;
        }
        if (!hasWork) {
            if ((level.getGameTime() + worldPosition.asLong()) % 8 == 0) hasWork = pick(true);
            return;
        }
        // 전기는 동작하는 동안에도 미리 모아 둠 → 전기가 충분하면 쉬지 않고 연달아 움직임
        if (progress < CYCLE) progress += PowerGrid.ratio(level, worldPosition, Layer.ELEC);
        if (action < 0 && progress >= CYCLE) {
            progress -= CYCLE;
            hasWork = pick(false);
        }
    }

    private IItemHandler target(Direction f) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(f), f.getOpposite());
    }

    private boolean beltAhead(Direction f) {
        return level.getBlockState(worldPosition.relative(f)).getBlock() instanceof ConveyorBlock;
    }

    /** 옮길 것을 찾아 집는다 (simulate=true 면 찾기만) */
    private boolean pick(boolean simulate) {
        Direction f = facing();
        BlockPos src = worldPosition.relative(f.getOpposite());
        IItemHandler from = level.getCapability(Capabilities.ItemHandler.BLOCK, src, f);
        IItemHandler to = target(f);
        boolean dstBelt = to == null && beltAhead(f);
        if (to == null && !dstBelt) return false;
        if (from != null) {
            for (int i = 0; i < from.getSlots(); i++) {
                ItemStack peek = from.extractItem(i, BATCH, true);
                if (!matches(peek)) continue;
                int can = dstBelt ? peek.getCount() : peek.getCount() - ItemHandlerHelper.insertItemStacked(to, peek, true).getCount();
                if (can <= 0) continue;
                if (!simulate) begin(from.extractItem(i, can, false));
                return true;
            }
            return false;
        }
        // 상자가 아니면: 그 칸(컨베이어 위 등)에 떨어진 아이템을 주움
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
            begin(got);
            return true;
        }
        return false;
    }

    private void begin(ItemStack got) {
        held = got;
        action = 0;
        setChanged();
        level.blockEvent(worldPosition, getBlockState().getBlock(), 1, Item.getId(got.getItem()));
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.15f, 1.7f);
    }

    private void deliver(ItemStack got) {
        Direction f = facing();
        IItemHandler to = target(f);
        ItemStack rest = to != null ? ItemHandlerHelper.insertItemStacked(to, got, false) : ItemTransfer.output(level, worldPosition, f, got, false);
        if (!rest.isEmpty()) ItemTransfer.spawnAt(level, worldPosition, f, rest);
        moves++;
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.15f, 1.8f);
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
        if (!held.isEmpty()) tag.put("Held", held.save(reg));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        filter = tag.contains("Filter") ? ItemStack.parseOptional(reg, tag.getCompound("Filter")) : ItemStack.EMPTY;
        held = tag.contains("Held") ? ItemStack.parseOptional(reg, tag.getCompound("Held")) : ItemStack.EMPTY;
        if (!held.isEmpty()) action = 0;   // 들고 있던 건 다시 내려놓기부터
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
