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
            Direction f = facing();
            if (action == 8) lid(worldPosition.relative(f.getOpposite()), false);    // 집고 나서 뚜껑 닫기
            if (action == 14) lid(worldPosition.relative(f), true);                  // 내려놓기 직전 앞 상자 열기
            if (action == DELIVER && !held.isEmpty()) {
                deliver(held);
                held = ItemStack.EMPTY;
                setChanged();
            }
            if (action == 22) lid(worldPosition.relative(f), false);
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

    /** v0.24.1: 상자·통 뚜껑 여닫기 (사람이 열어 둔 상자는 그대로 열린 채) */
    private void lid(BlockPos p, boolean open) {
        BlockState st = level.getBlockState(p);
        if (level.getBlockEntity(p) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity) {
            int people = net.minecraft.world.level.block.entity.ChestBlockEntity.getOpenCount(level, p);
            java.util.List<BlockPos> halves = new java.util.ArrayList<>(java.util.List.of(p));
            if (st.hasProperty(net.minecraft.world.level.block.ChestBlock.TYPE)
                    && st.getValue(net.minecraft.world.level.block.ChestBlock.TYPE) != net.minecraft.world.level.block.state.properties.ChestType.SINGLE)
                halves.add(p.relative(net.minecraft.world.level.block.ChestBlock.getConnectedDirection(st)));
            for (BlockPos h : halves) level.blockEvent(h, level.getBlockState(h).getBlock(), 1, open ? people + 1 : people);
            if (people == 0) level.playSound(null, p, open ? SoundEvents.CHEST_OPEN : SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.35f, 1.1f);
        } else if (st.getBlock() instanceof net.minecraft.world.level.block.BarrelBlock && st.getValue(net.minecraft.world.level.block.BarrelBlock.OPEN) != open) {
            level.setBlock(p, st.setValue(net.minecraft.world.level.block.BarrelBlock.OPEN, open), 3);
            level.playSound(null, p, open ? SoundEvents.BARREL_OPEN : SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS, 0.35f, 1.1f);
        }
    }

    /**
     * v0.24.1: 면에 따라 칸이 다른 블록(화로·용광로·훈연기·호퍼 등)도 알맞게:
     * 넣을 때 옆면(연료) → 윗면(재료) 순으로, 꺼낼 때 옆면 → 아랫면(결과물) 순으로 시도
     */
    private IItemHandler inserter(Direction f, ItemStack st) {
        BlockPos dst = worldPosition.relative(f);
        for (Direction side : new Direction[]{f.getOpposite(), Direction.UP, Direction.DOWN}) {
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, dst, side);
            if (h == null) return null;
            if (st.isEmpty() || ItemHandlerHelper.insertItemStacked(h, st, true).getCount() < st.getCount()) return h;
        }
        return null;
    }

    private boolean hasTarget(Direction f) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(f), f.getOpposite()) != null || beltAhead(f);
    }

    private boolean beltAhead(Direction f) {
        return level.getBlockState(worldPosition.relative(f)).getBlock() instanceof ConveyorBlock;
    }

    /** 이만큼을 앞에 내려놓을 수 있나 (컨베이어면 전부) */
    private int canPlace(Direction f, ItemStack st) {
        if (beltAhead(f) && level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(f), f.getOpposite()) == null) return st.getCount();
        IItemHandler to = inserter(f, st);
        return to == null ? 0 : st.getCount() - ItemHandlerHelper.insertItemStacked(to, st, true).getCount();
    }

    /** 옮길 것을 찾아 집는다 (simulate=true 면 찾기만) */
    private boolean pick(boolean simulate) {
        Direction f = facing();
        BlockPos src = worldPosition.relative(f.getOpposite());
        if (!hasTarget(f)) return false;
        boolean anyInv = false;
        // 면마다 칸이 다른 블록(화로 등)은 아랫면 = 결과물에서만 꺼냄 (연료를 빼 오지 않게)
        Direction[] faces = level.getBlockEntity(src) instanceof net.minecraft.world.WorldlyContainer ? new Direction[]{Direction.DOWN} : new Direction[]{f};
        for (Direction side : faces) {
            IItemHandler from = level.getCapability(Capabilities.ItemHandler.BLOCK, src, side);
            if (from == null) break;
            anyInv = true;
            for (int i = 0; i < from.getSlots(); i++) {
                ItemStack peek = from.extractItem(i, BATCH, true);
                if (!matches(peek)) continue;
                int can = canPlace(f, peek);
                if (can <= 0) continue;
                if (!simulate) {
                    lid(src, true);
                    begin(from.extractItem(i, can, false));
                }
                return true;
            }
        }
        if (anyInv) return false;
        // 상자가 아니면: 그 칸(컨베이어 위 등)에 떨어진 아이템을 주움
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(src).expandTowards(0, 0.5, 0))) {
            ItemStack s = e.getItem();
            if (!matches(s)) continue;
            int can = canPlace(f, s.copyWithCount(Math.min(BATCH, s.getCount())));
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
        IItemHandler to = inserter(f, got);
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
