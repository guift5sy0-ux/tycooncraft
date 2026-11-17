package com.tycooncraft.turret;

import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.UUID;

/** 방공 요격 포대: 탄약 9칸(중탄 1개 = 4발, 경탄 1개 = 2발). 호퍼·컨베이어로 공급 가능 */
public class CiwsBlockEntity extends BlockEntity implements MenuProvider {
    private final SimpleContainer ammo = new SimpleContainer(9) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(ModItems.HEAVY_AMMO.get()) || stack.is(ModItems.LIGHT_AMMO.get());
        }

        @Override
        public void setChanged() {
            super.setChanged();
            CiwsBlockEntity.this.setChanged();
        }
    };
    private final IItemHandler handler = new InvWrapper(ammo);
    private final Interceptor.State state = new Interceptor.State();
    private UUID owner;
    private int targetId = -1;
    public float yaw, yawO, pitch, pitchO, spin, spinO;

    public CiwsBlockEntity(BlockPos pos, BlockState s) {
        super(ModBlockEntities.CIWS.get(), pos, s);
    }

    public SimpleContainer ammo() {
        return ammo;
    }

    public IItemHandler getItemHandler() {
        return handler;
    }

    public void setOwner(UUID id) {
        owner = id;
        setChanged();
    }

    public Vec3 muzzle() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, 1.15, 0);
    }

    /** 중탄 우선 1개 꺼냄 */
    Boolean takeAmmo() {
        for (boolean heavyPass : new boolean[]{true, false})
            for (int i = 0; i < 9; i++) {
                ItemStack s = ammo.getItem(i);
                if (heavyPass ? s.is(ModItems.HEAVY_AMMO.get()) : s.is(ModItems.LIGHT_AMMO.get())) {
                    s.shrink(1);
                    ammo.setChanged();
                    return heavyPass;
                }
            }
        return null;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState st, CiwsBlockEntity t) {
        if (!(level instanceof ServerLevel sl)) return;
        if (Interceptor.tick(sl, t.muzzle(), t.state, t.owner, t::takeAmmo, null) || t.state.target != t.targetId) {
            t.targetId = t.state.target;
            t.setChanged();
            sl.sendBlockUpdated(pos, st, st, 3);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState st, CiwsBlockEntity t) {
        t.yawO = t.yaw;
        t.pitchO = t.pitch;
        t.spinO = t.spin;
        Entity target = t.targetId >= 0 ? level.getEntity(t.targetId) : null;
        float ty, tp;
        if (target != null) {
            Vec3 d = target.getBoundingBox().getCenter().subtract(t.muzzle());
            ty = (float) Mth.atan2(-d.x, -d.z);
            tp = (float) Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
            t.spin += 1.2f;
        } else {
            ty = t.yaw + 0.015f;
            tp = 0.35f;   // 대기: 하늘을 살핌
        }
        float dy = Mth.wrapDegrees((ty - t.yaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        t.yaw += Mth.clamp(dy, -0.6f, 0.6f);
        t.pitch += Mth.clamp(tp - t.pitch, -0.4f, 0.4f);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Ammo", ammo.createTag(reg));
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Target", targetId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        ammo.fromTag(tag.getList("Ammo", 10), reg);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        targetId = tag.contains("Target") ? tag.getInt("Target") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider reg) {
        CompoundTag tag = super.getUpdateTag(reg);
        tag.putInt("Target", targetId);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tycooncraft.ciws");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inv, ammo, 1);
    }
}
