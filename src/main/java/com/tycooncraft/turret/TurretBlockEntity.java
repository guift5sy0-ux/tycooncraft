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
import net.minecraft.world.phys.shapes.CollisionContext;
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
    /** 침입자 모드: 주인·동맹이 아닌 플레이어도 쏨 (웅크리고 우클릭으로 전환) */
    private boolean intruders = true;
    private int lock;
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

    public static int SHOTS, DESTROYED;
    public static final float MAX_HP = 60f;
    public float hp = MAX_HP;
    /** 레벨별 포탑 위치 (몹이 가장 가까운 포탑을 싸게 찾도록) */
    private static final java.util.Map<net.minecraft.resources.ResourceKey<Level>, java.util.Set<BlockPos>> ALL = new java.util.concurrent.ConcurrentHashMap<>();

    public static java.util.Set<BlockPos> all(Level level) {
        return ALL.computeIfAbsent(level.dimension(), k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
    }

    @org.jetbrains.annotations.Nullable
    public static BlockPos nearest(Level level, net.minecraft.world.phys.Vec3 from, double range) {
        BlockPos best = null;
        double bd = range * range;
        for (BlockPos p : all(level)) {
            double d = p.getCenter().distanceToSqr(from);
            if (d < bd && level.isLoaded(p) && level.getBlockEntity(p) instanceof TurretBlockEntity) { bd = d; best = p; }
        }
        return best;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) all(level).add(worldPosition.immutable());
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null && !level.isClientSide) all(level).remove(worldPosition);
    }

    /** 포탑이 맞음: 체력이 다 닳으면 터져서 부서짐 (탄약은 쏟아지고 고철 조금) */
    public void hurt(float amount, @org.jetbrains.annotations.Nullable Entity attacker) {
        if (!(level instanceof ServerLevel sl) || amount <= 0 || isRemoved()) return;
        hp -= amount;
        setChanged();
        sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        Vec3 c = Vec3.atCenterOf(worldPosition);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.CRIT, c.x, c.y + 0.3, c.z, 6, 0.3, 0.3, 0.3, 0.2);
        sl.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.ANVIL_PLACE, net.minecraft.sounds.SoundSource.BLOCKS, 0.35f, 1.6f + sl.random.nextFloat() * 0.3f);
        if (hp <= 0) {
            DESTROYED++;
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, c.x, c.y, c.z, 2, 0.2, 0.2, 0.2, 0);
            sl.sendParticles(com.tycooncraft.registry.ModParticles.SMOKE_LARGE.get(), c.x, c.y, c.z, 20, 0.3, 0.4, 0.3, 0.05);
            sl.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.BLOCKS, 1.2f, 1.1f);
            sl.destroyBlock(worldPosition, false, attacker);
            net.minecraft.world.Containers.dropItemStack(sl, c.x, c.y, c.z, new ItemStack(net.minecraft.world.item.Items.IRON_INGOT, 2));
        }
    }

    /** 수리 (철 주괴 등) */
    public boolean repair(float amount) {
        if (hp >= MAX_HP) return false;
        hp = Math.min(MAX_HP, hp + amount);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        return true;
    }

    public UUID owner() {
        return owner;
    }

    public boolean intruders() {
        return intruders;
    }

    public void toggleIntruders() {
        intruders = !intruders;
        setChanged();
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
                t.lock = TurretAI.LOCK_DELAY;
            }
        }
        if (target == null) return;
        if (t.lock > 0) {   // 조준하는 시간
            t.lock--;
            return;
        }
        if (t.cooldown > 0) return;
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
        t.cooldown = TurretAI.COOLDOWN;
        Vec3 from = t.muzzle().add(0, 0.45, 0);
        Vec3 aim = target.getBoundingBox().getCenter().add(0, target.getBbHeight() * 0.15, 0);
        double sp = TurretAI.SPREAD;
        Vec3 dir = aim.subtract(from).normalize().add(sl.random.nextGaussian() * sp, sl.random.nextGaussian() * sp, sl.random.nextGaussian() * sp).normalize();
        UUID own = t.owner;
        Ballistics.shoot(sl, null, from.add(dir.scale(0.9)), dir, RANGE + 4, heavy ? TurretAI.DMG_HEAVY : TurretAI.DMG_LIGHT, 0, 1.5f, false, true,
                sl.damageSources().generic(), e -> TurretAI.canHit(sl, e, own));
        SHOTS++;
        sl.playSound(null, from.x, from.y, from.z, ModSounds.RIFLE_FIRE.get(), SoundSource.BLOCKS, 1.6f, 1.25f + sl.random.nextFloat() * 0.1f);
        Vec3 flash = from.add(dir.scale(1.0));
        sl.sendParticles(ParticleTypes.FLAME, flash.x, flash.y, flash.z, 1, 0.02, 0.02, 0.02, 0.005);
    }

    private boolean valid(Entity e) {
        if (!(level instanceof ServerLevel sl) || !TurretAI.targetable(sl, e, owner, intruders) || e.distanceToSqr(muzzle()) > RANGE * RANGE) return false;
        Vec3 eye = e instanceof LivingEntity le ? le.getBoundingBox().getCenter().add(0, le.getBbHeight() * 0.25, 0) : e.position();
        // 포탑 블록 자체에 막히지 않게 블록 바깥(위쪽·목표 방향)에서 시야 검사
        Vec3 from = muzzle().add(0, 0.45, 0);
        Vec3 dir = eye.subtract(from).normalize();
        from = from.add(dir.scale(0.8));
        return level.clip(new ClipContext(from, eye, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty())).getType() == HitResult.Type.MISS;
    }

    private Entity findTarget(ServerLevel level) {
        Entity best = null;
        double bd = Double.MAX_VALUE;
        for (Entity e : level.getEntities((Entity) null, new AABB(worldPosition).inflate(RANGE), e -> TurretAI.targetable(level, e, owner, intruders))) {
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
        if (t.hp < MAX_HP * 0.5f && level.random.nextInt(t.hp < MAX_HP * 0.25f ? 3 : 8) == 0)   // 손상: 연기·불꽃
            level.addParticle(t.hp < MAX_HP * 0.25f ? com.tycooncraft.registry.ModParticles.SMOKE_LARGE.get() : com.tycooncraft.registry.ModParticles.SMOKE.get(),
                    pos.getX() + 0.3 + level.random.nextFloat() * 0.4, pos.getY() + 1.0, pos.getZ() + 0.3 + level.random.nextFloat() * 0.4, 0, 0.04, 0);
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
        tag.putBoolean("Intruders", intruders);
        tag.putInt("Target", targetId);
        tag.putFloat("Hp", hp);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ammo.fromTag(tag.getList("Ammo", 10), registries);
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        intruders = !tag.contains("Intruders") || tag.getBoolean("Intruders");
        targetId = tag.contains("Target") ? tag.getInt("Target") : -1;
        hp = tag.contains("Hp") ? tag.getFloat("Hp") : MAX_HP;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Target", targetId);
        tag.putFloat("Hp", hp);
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
