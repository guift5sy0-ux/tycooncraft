package com.tycooncraft.cctv;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** CCTV 머리 방향: yaw = 정면 기준 좌우(±60°), pitch = 아래로 숙인 각도(기본 20°, -10~50°) */
public class CctvBlockEntity extends BlockEntity {
    public static final float BASE_PITCH = 20f;
    public float yaw, pitch = BASE_PITCH;
    /** 클라이언트 렌더용 (부드럽게 따라감) */
    public float shownYaw, shownPitch = BASE_PITCH, shownYawO, shownPitchO = BASE_PITCH;

    public CctvBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CCTV.get(), pos, state);
    }

    public void aim(float y, float p) {
        yaw = Mth.clamp(y, -60, 60);
        pitch = Mth.clamp(p, BASE_PITCH - 30, BASE_PITCH + 30);
    }

    /** 서버: 값 바꾸고 주변에 알림 */
    public void aimAndSync(float y, float p) {
        aim(y, p);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, CctvBlockEntity be) {
        be.shownYawO = be.shownYaw;
        be.shownPitchO = be.shownPitch;
        be.shownYaw += (be.yaw - be.shownYaw) * 0.35f;
        be.shownPitch += (be.pitch - be.shownPitch) * 0.35f;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putFloat("Yaw", yaw);
        tag.putFloat("Pitch", pitch);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        if (tag.contains("Yaw")) aim(tag.getFloat("Yaw"), tag.getFloat("Pitch"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag t = super.getUpdateTag(reg);
        saveAdditional(t, reg);
        return t;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
