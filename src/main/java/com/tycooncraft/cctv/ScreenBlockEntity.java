package com.tycooncraft.cctv;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** 스크린에 연결된 CCTV 위치 (최대 5) — 클라이언트에도 동기화 */
public class ScreenBlockEntity extends BlockEntity {
    private final List<BlockPos> links = new ArrayList<>();

    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CCTV_SCREEN.get(), pos, state);
    }

    public List<BlockPos> links() {
        return List.copyOf(links);
    }

    public void add(BlockPos p) {
        links.add(p.immutable());
        changed();
    }

    public void remove(BlockPos p) {
        links.remove(p);
        changed();
    }

    private void changed() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        ListTag l = new ListTag();
        for (BlockPos p : links) l.add(NbtUtils.writeBlockPos(p));
        tag.put("Links", l);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        links.clear();
        ListTag l = tag.getList("Links", Tag.TAG_INT_ARRAY);
        for (Tag t : l) {
            int[] a = ((net.minecraft.nbt.IntArrayTag) t).getAsIntArray();
            if (a.length == 3) links.add(new BlockPos(a[0], a[1], a[2]));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        return saveWithoutMetadata(reg);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
