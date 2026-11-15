package com.tycooncraft.turret;

import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.UUID;

/**
 * 자동 포탑: 탄약 9칸. 중탄약 1발 = 피해 7, 경탄약 1발 = 피해 5. 0.15초마다 사격.
 * 몸통을 돌려 겨눈 뒤 쏘고, 탄약이 떨어지면 멈춘다. 컨베이어·호퍼로 탄약 공급 가능.
 */
public class TurretBlockEntity extends BlockEntity implements MenuProvider {
    public static final double RANGE = 24;
    private final SimpleContainer ammo = new SimpleContainer(9) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(ModItems.HEAVY_AMMO.get()) || stack.is(ModItems.LIGHT_AMMO.get());
        }

        @Override
        public void setChanged() {
            super.setChanged();
            TurretBlockEntity.this.setChanged();
        }
    };
    private final IItemHandler handler = new InvWrapper(ammo);
    private UUID owner;
    private int targetId = -1;
    private int cooldown, searchCooldown;
    /** 클라이언트 표시 */
    public float yaw, yawO, pitch, pitchO;

    public TurretBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET.get(), pos, state);
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

    private Vec3 muzzle() {
        return Vec3.atBottomCenterOf(worldPosition).add(0, 0.7, 0);
    }

    // ── 서버 ──
    public static void serverTick(Level level, BlockPos pos, BlockState state, TurretBlockEntity t) {
        if (!(level instanceof ServerLevel sl)) return;
        if (t.cooldown > 0) t.cooldown--;
        Entity target = t.targetId >= 0 ? level.getEntity(t.targetId) : null;
        if (target == null || !t.valid(target)) {
            if (--t.searchCooldown <= 0) {
                t.searchCooldown = 10;
                Entity found = t.findTarget(sl);
                t.setTarget(found == null ? -1 : found.getId());
                target = found;
            }
        }
        if (target == null || t.cooldown > 0) return;
        int slot = -1;
        boolean heavy = false;
        for (int i = 0; i < 9; i++) {
            ItemStack s = t.ammo.getItem(i);
            if (s.is(ModItems.HEAVY_AMMO.get())) {
                slot = i;
                heavy = true;
                break;
            }
            if (slot < 0 && s.is(ModItems.LIGHT_AMMO.get())) slot = i;
        }
        if (slot < 0) return;
        t.ammo.getItem(slot).shrink(1);
        t.ammo.setChanged();
        t.cooldown = 3;
        Vec3 from = t.muzzle();
        Vec3 aim = target.getBoundingBox().getCenter().add(0, target.getBbHeight() * 0.15, 0);
        Vec3 dir = aim.subtract(from).normalize().add(sl.random.nextGaussian() * 0.02, sl.random.nextGaussian() * 0.02, sl.random.nextGaussian() * 0.02).normalize();
        Ballistics.shoot(sl, null, from.add(dir.scale(0.9)), dir, RANGE + 4, heavy ? 7f : 5f, 0, 1.5f, false, true,
                sl.damageSources().generic(), e -> !(e instanceof Player));
        sl.playSound(null, from.x, from.y, from.z, ModSounds.RIFLE_FIRE.get(), SoundSource.BLOCKS, 1.6f, 1.25f + sl.random.nextFloat() * 0.1f);
        Vec3 flash = from.add(dir.scale(1.0));
        sl.sendParticles(ParticleTypes.FLAME, flash.x, flash.y, flash.z, 1, 0.02, 0.02, 0.02, 0.005);
    }

    private boolean valid(Entity e) {
        if (!e.isAlive() || !(e instanceof Enemy) || e.distanceToSqr(muzzle()) > RANGE * RANGE) return false;
        Vec3 eye = e instanceof LivingEntity le ? le.getEyePosition() : e.position();
        return level.clip(new ClipContext(muzzle(), eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e)).getType() == HitResult.Type.MISS;
    }

    private Entity findTarget(ServerLevel level) {
        Entity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : level.getEntities((Entity) null, new AABB(worldPosition).inflate(RANGE), e -> e instanceof Enemy)) {
            double d = e.distanceToSqr(muzzle());
            if (d < bd && valid(e)) {
                bd = d;
                best = e;
            }
        }
        return best;
    }

    private void setTarget(int id) {
        if (id == targetId) return;
        targetId = id;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ── 클라이언트: 목표 쪽으로 몸통·총구 회전 ──
    public static void clientTick(Level level, BlockPos pos, BlockState state, TurretBlockEntity t) {
        t.yawO = t.yaw;
        t.pitchO = t.pitch;
        Entity target = t.targetId >= 0 ? level.getEntity(t.targetId) : null;
        float ty, tp;
        if (target != null) {
            Vec3 d = target.getBoundingBox().getCenter().subtract(t.muzzle());
            ty = (float) Mth.atan2(-d.x, -d.z);
            tp = (float) Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
        } else {
            ty = t.yaw + 0.01f;   // 대기 중: 천천히 둘러봄
            tp = 0;
        }
        float dy = Mth.wrapDegrees((ty - t.yaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
        t.yaw += Mth.clamp(dy, -0.25f, 0.25f);
        t.pitch += Mth.clamp(tp - t.pitch, -0.15f, 0.15f);
    }

    // ── 저장·동기화 ──
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Ammo", ammo.createTag(registries));
        if (owner != null) tag.putUUID("Owner", owner);
        tag.putInt("Target", targetId);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ammo.fromTag(tag.getList("Ammo", 10), registries);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        targetId = tag.contains("Target") ? tag.getInt("Target") : -1;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Target", targetId);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tycooncraft.turret");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ChestMenu(MenuType.GENERIC_9x1, id, inventory, ammo, 1);
    }
}
