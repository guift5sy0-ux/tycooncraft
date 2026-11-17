package com.tycooncraft.setter;

import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.ship.ShipFloatBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DispenserMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import java.util.*;

/** 세터의 상태: 블록 9칸, 충전(1회 = {@link #STEPS_PER_CHARGE}칸), 진행 방향 */
public class SetterBlockEntity extends BlockEntity implements MenuProvider {
    public static final int STEPS_PER_CHARGE = 64, MAX_CHARGES = 8, STEP_TICKS = 5, MAX_GROUP = 32;
    public static int STEPS, PLACED, REFUSED;
    public static String LAST_STOP = "";

    private final SimpleContainer items = new SimpleContainer(9) {
        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.getItem() instanceof BlockItem;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            SetterBlockEntity.this.setChanged();
        }
    };
    private final IItemHandler handler = new InvWrapper(items);
    private int energy, timer, travelled;
    private Direction move;
    private UUID starter;
    /** 블록을 옮겨 놓는 중 (onRemove 에서 내용물 쏟지 않게) */
    boolean relocating;

    public SetterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SETTER.get(), pos, state);
    }

    public SimpleContainer items() {
        return items;
    }

    public IItemHandler getItemHandler() {
        return handler;
    }

    public boolean running() {
        return move != null;
    }

    public int energy() {
        return energy;
    }

    /** 레드스톤 블록 4개 → 1회 충전 */
    public boolean charge() {
        if (energy + STEPS_PER_CHARGE > STEPS_PER_CHARGE * MAX_CHARGES) return false;
        energy += STEPS_PER_CHARGE;
        setChanged();
        return true;
    }

    private Direction facing() {
        return getBlockState().getValue(DirectionalBlock.FACING);
    }

    int blockCount() {
        int n = 0;
        for (int i = 0; i < 9; i++) if (items.getItem(i).getItem() instanceof BlockItem) n += items.getItem(i).getCount();
        return n;
    }

    public void report(Player p) {
        p.displayClientMessage(Component.translatable("message.tycooncraft.setter.status", energy / STEPS_PER_CHARGE, energy, blockCount())
                .withStyle(ChatFormatting.GOLD), true);
    }

    /** 세터와 이어진 부유체 무리 (세터 앞쪽 = 설치 방향 칸은 제외) */
    List<BlockPos> group() {
        List<BlockPos> out = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        BlockPos front = worldPosition.relative(facing());
        for (Direction d : Direction.values()) if (d != facing()) q.add(worldPosition.relative(d));
        while (!q.isEmpty() && out.size() < MAX_GROUP) {
            BlockPos p = q.poll();
            if (!seen.add(p) || p.equals(front) || p.equals(worldPosition)) continue;
            if (!(level.getBlockState(p).getBlock() instanceof ShipFloatBlock)) continue;
            out.add(p);
            for (Direction d : Direction.values()) q.add(p.relative(d));
        }
        return out;
    }

    /** 우클릭: 보는 방향(축에 충분히 가까울 때만)으로 출발 */
    public void start(Player p, Vec3 look) {
        Direction d = axisOf(look);
        String fail = null;
        if (d == null) fail = "message.tycooncraft.setter.diagonal";
        else if (energy <= 0) fail = "message.tycooncraft.setter.no_charge";
        else if (group().isEmpty()) fail = "message.tycooncraft.setter.no_float";
        else if (d == facing()) fail = "message.tycooncraft.setter.into_build";
        if (fail != null) {
            REFUSED++;
            p.displayClientMessage(Component.translatable(fail).withStyle(ChatFormatting.RED), true);
            return;
        }
        move = d;
        starter = p.getUUID();
        timer = 0;
        travelled = 0;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.8f, 0.8f);
        p.displayClientMessage(Component.translatable("message.tycooncraft.setter.started", Component.translatable("direction.tycooncraft." + d.getName()),
                Component.translatable("direction.tycooncraft." + facing().getName())).withStyle(ChatFormatting.AQUA), true);
    }

    /** 바라보는 방향이 한 축에 가까우면 그 방향, 대각선(애매)이면 null — 가장 큰 성분이 0.8 이상이어야 함 */
    public static Direction axisOf(Vec3 v) {
        v = v.normalize();
        double ax = Math.abs(v.x), ay = Math.abs(v.y), az = Math.abs(v.z);
        double m = Math.max(ax, Math.max(ay, az));
        if (m < 0.8) return null;
        if (m == ax) return v.x > 0 ? Direction.EAST : Direction.WEST;
        if (m == ay) return v.y > 0 ? Direction.UP : Direction.DOWN;
        return v.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    public void stop(Player p, String why) {
        if (move == null) return;
        LAST_STOP = why;
        move = null;
        setChanged();
        Player who = p != null ? p : starter != null ? level.getPlayerByUUID(starter) : null;
        if (who != null) who.displayClientMessage(Component.translatable(why, travelled).withStyle(ChatFormatting.YELLOW), true);
        level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.8f, 0.8f);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SetterBlockEntity be) {
        if (be.move == null || !(level instanceof ServerLevel sl)) return;
        if (++be.timer < STEP_TICKS) return;
        be.timer = 0;
        be.step(sl);
    }

    private static boolean free(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.isAir() || s.canBeReplaced();   // 공기·풀·물 등
    }

    /** 한 칸: 앞에 블록을 놓고, 세터+부유체를 진행 방향으로 한 칸 옮긴다 */
    private void step(ServerLevel sl) {
        if (energy <= 0) {
            stop(null, "message.tycooncraft.setter.out_of_charge");
            return;
        }
        Direction f = facing();
        // 1) 설치
        BlockPos front = worldPosition.relative(f);
        if (free(sl, front)) {
            for (int i = 0; i < 9; i++) {
                ItemStack s = items.getItem(i);
                if (s.getItem() instanceof BlockItem bi) {
                    BlockState place = bi.getBlock().defaultBlockState();
                    if (!place.canSurvive(sl, front)) continue;
                    sl.setBlock(front, place, Block.UPDATE_ALL);
                    sl.playSound(null, front, place.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.8f, 1f);
                    s.shrink(1);
                    items.setChanged();
                    PLACED++;
                    break;
                }
            }
        }
        // 2) 이동
        List<BlockPos> group = group();
        if (group.isEmpty()) {
            stop(null, "message.tycooncraft.setter.no_float");
            return;
        }
        List<BlockPos> all = new ArrayList<>(group);
        all.add(worldPosition);
        Set<BlockPos> own = new HashSet<>(all);
        for (BlockPos p : all) {
            BlockPos n = p.relative(move);
            if (!sl.isLoaded(n) || n.getY() >= sl.getMaxBuildHeight() || n.getY() < sl.getMinBuildHeight()) {
                stop(null, "message.tycooncraft.setter.edge");
                return;
            }
            if (!own.contains(n) && !free(sl, n)) {
                stop(null, "message.tycooncraft.setter.blocked");
                return;
            }
        }
        Direction m = move;
        CompoundTag data = saveWithoutMetadata(sl.registryAccess());
        BlockState me = getBlockState();
        relocating = true;
        // 옮기기: 먼저 모두 지우고(드롭 없이), 새 자리에 놓기
        Map<BlockPos, BlockState> states = new HashMap<>();
        for (BlockPos p : all) states.put(p, sl.getBlockState(p));
        for (BlockPos p : all) sl.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS | Block.UPDATE_MOVE_BY_PISTON);
        for (BlockPos p : group) sl.setBlock(p.relative(m), states.get(p), Block.UPDATE_ALL);
        BlockPos np = worldPosition.relative(m);
        sl.setBlock(np, me, Block.UPDATE_ALL);
        if (sl.getBlockEntity(np) instanceof SetterBlockEntity nb) {
            nb.loadWithComponents(data, sl.registryAccess());
            nb.energy = energy - 1;
            nb.travelled = travelled + 1;
            nb.timer = 0;
            nb.setChanged();
            STEPS++;
            if (nb.travelled % 4 == 0) sl.playSound(null, np, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.35f, 1.4f);
            if (nb.energy <= 0) nb.stop(null, "message.tycooncraft.setter.out_of_charge");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.put("Items", items.createTag(reg));
        tag.putInt("Energy", energy);
        tag.putInt("Travelled", travelled);
        if (move != null) tag.putString("Move", move.getName());
        if (starter != null) tag.putUUID("Starter", starter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        items.fromTag(tag.getList("Items", 10), reg);
        energy = tag.getInt("Energy");
        travelled = tag.getInt("Travelled");
        move = tag.contains("Move") ? Direction.byName(tag.getString("Move")) : null;
        starter = tag.hasUUID("Starter") ? tag.getUUID("Starter") : null;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.tycooncraft.setter");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new DispenserMenu(id, inv, items);
    }
}
