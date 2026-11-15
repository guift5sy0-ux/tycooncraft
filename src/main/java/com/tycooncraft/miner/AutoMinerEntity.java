package com.tycooncraft.miner;

import com.tycooncraft.registry.ModItems;
import com.tycooncraft.registry.ModSounds;
import com.tycooncraft.vehicle.Fuel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/**
 * 자동 굴착기 TC-X1: 연료를 넣으면 앞으로 3×3 터널을 뚫으며 전진.
 * 철 곡괭이 등급(다이아몬드 광석까지) 채굴, 단단한 블록일수록 느리고 연료를 더 씀.
 * 광물은 저장통(27칸)에 모으고, 흔한 돌·흙은 버린다.
 */
public class AutoMinerEntity extends Entity {
    public static final int MAX_FUEL = 24000;
    private static final EntityDataAccessor<Integer> FUEL = SynchedEntityData.defineId(AutoMinerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(AutoMinerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> WORKING = SynchedEntityData.defineId(AutoMinerEntity.class, EntityDataSerializers.BOOLEAN);
    private static final ItemStack TOOL = new ItemStack(Items.IRON_PICKAXE);
    private static final Set<net.minecraft.world.item.Item> JUNK = Set.of(Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.DIRT, Items.GRAVEL,
            Items.STONE, Items.GRANITE, Items.DIORITE, Items.ANDESITE, Items.TUFF, Items.NETHERRACK, Items.CALCITE, Items.DRIPSTONE_BLOCK, Items.SMOOTH_BASALT);

    public final SimpleContainer storage = new SimpleContainer(27);
    private BlockPos digPos;
    private int digProgress, digNeeded, soundTimer;
    private UUID owner;
    public int minedBlocks;
    public float drillAngle, drillAngleO, wheelAngle, wheelAngleO;

    public AutoMinerEntity(EntityType<? extends AutoMinerEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(FUEL, 0);
        b.define(RUNNING, false);
        b.define(WORKING, false);
    }

    public int getFuel() { return entityData.get(FUEL); }
    public void setFuel(int f) { entityData.set(FUEL, Mth.clamp(f, 0, MAX_FUEL)); }
    public boolean isRunning() { return entityData.get(RUNNING); }
    public void setRunning(boolean r) { entityData.set(RUNNING, r); }
    public boolean isWorking() { return entityData.get(WORKING); }
    public void setOwner(UUID id) { owner = id; }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            drillAngleO = drillAngle;
            wheelAngleO = wheelAngle;
            if (isWorking()) drillAngle += 0.55f;
            double moved = Math.sqrt((getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo));
            wheelAngle += (float) (moved / (11 / 32.0));
            if (isWorking() && random.nextInt(3) == 0) {
                Vec3 f = Vec3.directionFromRotation(0, getYRot());
                level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + f.x * 0.3 + random.nextGaussian() * 0.05, getY() + 1.95, getZ() + f.z * 0.3, 0, 0.06, 0);
            }
            return;
        }
        Vec3 v = getDeltaMovement();
        double vy = onGround() ? -0.04 : Math.max(v.y - 0.08, -1.5);
        double vx = 0, vz = 0;
        boolean working = false;
        if (isRunning() && getFuel() > 0) {
            Direction dir = getDirection();
            if (digPos == null || !isTarget(level().getBlockState(digPos))) {
                digPos = findTarget(dir);
                digProgress = 0;
                if (digPos != null) {
                    BlockState s = level().getBlockState(digPos);
                    if (tooHard(s)) {
                        stop("message.tycooncraft.miner.too_hard", s.getBlock().getName());
                        digPos = null;
                    } else {
                        digNeeded = 3 + (int) Math.ceil(Math.max(0, s.getDestroySpeed(level(), digPos)) * 3);
                    }
                }
            }
            if (digPos != null) {
                working = true;
                digProgress++;
                level().destroyBlockProgress(getId(), digPos, Math.min(9, digProgress * 10 / Math.max(1, digNeeded)));
                if (digProgress % 4 == 0 && level() instanceof ServerLevel sl)
                    sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level().getBlockState(digPos)), digPos.getX() + 0.5, digPos.getY() + 0.5, digPos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.1);
                if (digProgress >= digNeeded) harvest(digPos);
            } else if (isRunning()) {
                // 앞이 비었으면 전진
                working = true;
                vx = dir.getStepX() * 0.1;
                vz = dir.getStepZ() * 0.1;
                if (tickCount % 2 == 0) setFuel(getFuel() - 1);
            }
            if (getFuel() <= 0) stop("message.tycooncraft.miner.no_fuel", null);
        }
        setDeltaMovement(vx, vy, vz);
        move(MoverType.SELF, getDeltaMovement());
        if (working != isWorking()) entityData.set(WORKING, working);
        if (working && --soundTimer <= 0) {
            soundTimer = 39;
            level().playSound(null, getX(), getY(), getZ(), ModSounds.MINER_DRILL.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        if (getY() < level().getMinBuildHeight() - 32) discard();
    }

    private boolean isTarget(BlockState s) {
        return !s.isAir() && !(s.getBlock() instanceof LiquidBlock) && !s.is(net.minecraft.world.level.block.Blocks.FIRE);
    }

    /** 다이아몬드 광석까지(철 곡괭이 등급). 흑요석·기반암 등은 못 캠 */
    private boolean tooHard(BlockState s) {
        float h = s.getDestroySpeed(level(), BlockPos.ZERO);
        return h < 0 || h >= 30 || (s.requiresCorrectToolForDrops() && s.is(BlockTags.INCORRECT_FOR_IRON_TOOL));
    }

    /** 몸통 앞 1칸, 드릴 끝 2칸: 폭 3 × 높이 3 단면을 위에서부터 */
    private BlockPos findTarget(Direction dir) {
        BlockPos base = BlockPos.containing(getX(), getY() + 0.2, getZ());
        Direction side = dir.getClockWise();
        for (int d = 1; d <= 2; d++) {
            for (int y = 2; y >= 0; y--) {
                for (int s : new int[]{0, -1, 1}) {
                    BlockPos p = base.relative(dir, d).relative(side, s).above(y);
                    if (isTarget(level().getBlockState(p))) return p;
                }
            }
        }
        return null;
    }

    private void harvest(BlockPos pos) {
        ServerLevel sl = (ServerLevel) level();
        BlockState s = sl.getBlockState(pos);
        float h = Math.max(0, s.getDestroySpeed(sl, pos));
        for (ItemStack drop : Block.getDrops(s, sl, pos, sl.getBlockEntity(pos), this, TOOL)) {
            if (JUNK.contains(drop.getItem())) continue;
            ItemStack rest = storage.addItem(drop);
            if (!rest.isEmpty()) {
                spawnAtLocation(rest);
                stop("message.tycooncraft.miner.full", null);
            }
        }
        sl.levelEvent(2001, pos, Block.getId(s));
        sl.removeBlock(pos, false);
        sl.destroyBlockProgress(getId(), pos, -1);
        // 단단할수록 연료를 더 씀 (흙 6, 돌 18, 심층암·다이아 광석 30)
        setFuel(getFuel() - (6 + Math.round(h * 8)));
        minedBlocks++;
        digPos = null;
    }

    private void stop(String key, Component arg) {
        if (!isRunning()) return;
        setRunning(false);
        entityData.set(WORKING, false);
        if (digPos != null) level().destroyBlockProgress(getId(), digPos, -1);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1f, 0.6f);
        Player p = owner == null ? null : level().getPlayerByUUID(owner);
        if (p != null) p.displayClientMessage(arg == null ? Component.translatable(key).withStyle(ChatFormatting.GOLD)
                : Component.translatable(key, arg).withStyle(ChatFormatting.GOLD), false);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide) return InteractionResult.SUCCESS;
        if (owner == null) owner = player.getUUID();
        if (Fuel.value(stack) > 0) {
            Fuel.refuel(this, player, hand, getFuel(), MAX_FUEL, this::setFuel);
            if (getFuel() > 0 && !isRunning()) {
                setRunning(true);
                owner = player.getUUID();
            }
            return InteractionResult.CONSUME;
        }
        if (player.isShiftKeyDown()) {
            boolean on = !isRunning() && getFuel() > 0;
            setRunning(on);
            owner = player.getUUID();
            player.displayClientMessage(Component.translatable(on ? "message.tycooncraft.miner.on" : getFuel() > 0 ? "message.tycooncraft.miner.off" : "message.tycooncraft.miner.no_fuel")
                    .withStyle(on ? ChatFormatting.GREEN : ChatFormatting.YELLOW), true);
            level().playSound(null, getX(), getY(), getZ(), on ? ModSounds.ENGINE_START.get() : SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1f, 1f);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer sp)
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, storage), Component.translatable("container.tycooncraft.auto_miner")));
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || isRemoved()) return false;
        if (source.getEntity() instanceof Player p) {
            // 플레이어가 때리면 회수: 굴착기 + 저장통 내용물 (남은 연료는 아이템에 저장)
            ItemStack item = new ItemStack(ModItems.AUTO_MINER.get());
            if (getFuel() > 0) item.set(com.tycooncraft.registry.ModComponents.AMMO.get(), getFuel());
            if (!p.getAbilities().instabuild || getFuel() > 0) spawnAtLocation(item);
            for (int i = 0; i < storage.getContainerSize(); i++) if (!storage.getItem(i).isEmpty()) spawnAtLocation(storage.getItem(i));
            storage.clearContent();
            if (digPos != null) level().destroyBlockProgress(getId(), digPos, -1);
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 1f, 0.8f);
            discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isPickable() { return !isRemoved(); }

    @Override
    public boolean canBeCollidedWith() { return true; }

    @Override
    public boolean fireImmune() { return true; }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Fuel", getFuel());
        tag.putBoolean("Running", isRunning());
        tag.putInt("Mined", minedBlocks);
        if (owner != null) tag.putUUID("Owner", owner);
        tag.put("Items", storage.createTag(registryAccess()));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        setFuel(tag.getInt("Fuel"));
        setRunning(tag.getBoolean("Running"));
        minedBlocks = tag.getInt("Mined");
        if (tag.hasUUID("Owner")) owner = tag.getUUID("Owner");
        storage.fromTag(tag.getList("Items", Tag.TAG_COMPOUND), registryAccess());
    }
}
