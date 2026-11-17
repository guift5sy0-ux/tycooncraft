package com.tycooncraft.ship;

import com.tycooncraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 조종기에서 시작해 맞닿은 블록을 모아 배 엔티티로 바꾼다 */
public final class ShipAssembler {
    public static final int MAX_BLOCKS = 2048;

    /** 자연 지형·물풀 등은 배에 붙지 않는다 (해저·해안과 붙어도 조립 가능) */
    static boolean partOfShip(BlockState s) {
        if (s.isAir() || s.getBlock() instanceof LiquidBlock || s.is(Blocks.BUBBLE_COLUMN) || s.canBeReplaced()) return false;
        return !(s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(BlockTags.BASE_STONE_OVERWORLD) || s.is(BlockTags.BASE_STONE_NETHER)
                || s.is(Blocks.GRAVEL) || s.is(Blocks.CLAY) || s.is(Blocks.KELP) || s.is(Blocks.KELP_PLANT) || s.is(Blocks.SEAGRASS)
                || s.is(Blocks.TALL_SEAGRASS) || s.is(Blocks.SEA_PICKLE) || s.is(BlockTags.CORAL_BLOCKS) || s.is(BlockTags.CORALS)
                || s.is(BlockTags.WALL_CORALS) || s.is(Blocks.BEDROCK) || s.is(BlockTags.ICE) || s.is(BlockTags.LEAVES)
                || s.is(Blocks.LILY_PAD) || s.is(Blocks.SUGAR_CANE) || s.is(BlockTags.SNOW) || s.is(Blocks.MAGMA_BLOCK)
                || s.is(BlockTags.TERRACOTTA) || s.is(BlockTags.COAL_ORES) || s.is(BlockTags.IRON_ORES));
    }

    /** 엔진에서 이어진 구조물 전체: {엔진 수, 연료 합, 용량, 부유체 수, 블록 수} */
    public static int[] structureFuel(ServerLevel level, BlockPos start) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        int[] r = new int[5];
        q.add(start);
        while (!q.isEmpty() && r[4] <= MAX_BLOCKS) {
            BlockPos p = q.poll();
            if (!seen.add(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!partOfShip(s)) continue;
            r[4]++;
            if (s.getBlock() instanceof ShipMotorBlock) {
                r[0]++;
                r[2] += ShipFuel.MOTOR_CAPACITY;
                if (level.getBlockEntity(p) instanceof ShipMotorBlockEntity m) r[1] += m.fuel();
            }
            if (s.getBlock() instanceof ShipFloatBlock) r[3]++;
            for (Direction d : Direction.values()) q.add(p.relative(d));
        }
        return r;
    }

    /** 흑요석·우는 흑요석·네더라이트 블록·모루 등 폭발 저항 1000 이상(준 파괴 불가급)은 선체 금지 */
    public static boolean tooStrong(BlockState s) {
        return s.getBlock().getExplosionResistance() >= 1000f;
    }

    @Nullable
    public static ShipEntity assemble(ServerLevel level, BlockPos helmPos, @Nullable Player player) {
        BlockState helmState = level.getBlockState(helmPos);
        if (!(helmState.getBlock() instanceof ShipHelmBlock)) return null;
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        List<BlockPos> found = new ArrayList<>();
        q.add(helmPos);
        while (!q.isEmpty()) {
            BlockPos p = q.poll();
            if (!seen.add(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!partOfShip(s)) continue;
            if (s.getDestroySpeed(level, p) < 0) return fail(player, "unbreakable");
            if (tooStrong(s)) return fail(player, "too_strong", s.getBlock().getName(), p.getX(), p.getY(), p.getZ());
            if (s.getBlock() instanceof ShipHelmBlock && !p.equals(helmPos)) return fail(player, "two_helms");
            found.add(p);
            if (found.size() > MAX_BLOCKS) return fail(player, "too_big", MAX_BLOCKS);
            for (Direction d : Direction.values()) q.add(p.relative(d));
        }
        int motors = 0, minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        for (BlockPos p : found) {
            if (level.getBlockState(p).getBlock() instanceof ShipMotorBlock) motors++;
            minX = Math.min(minX, p.getX());
            minY = Math.min(minY, p.getY());
            minZ = Math.min(minZ, p.getZ());
        }
        if (motors == 0) return fail(player, "no_motor");
        int floats = 0, fuel = 0;
        for (BlockPos p : found) {
            if (level.getBlockState(p).getBlock() instanceof ShipFloatBlock) floats++;
            if (level.getBlockEntity(p) instanceof ShipMotorBlockEntity m) fuel += m.fuel();
        }
        boolean air = floats > 0 && floats * ShipFuel.LIFT_PER_FLOAT >= found.size();
        if (floats > 0 && !air) return fail(player, "not_enough_floats", floats, floats * ShipFuel.LIFT_PER_FLOAT, found.size(),
                (found.size() + ShipFuel.LIFT_PER_FLOAT - 1) / ShipFuel.LIFT_PER_FLOAT);
        // 바닥줄 일부가 물에 닿아 있어야 함 (부유체가 충분하면 어디서든)
        int bottom = 0, wet = 0;
        for (BlockPos p : found) {
            if (p.getY() != minY) continue;
            bottom++;
            if (level.getFluidState(p).is(FluidTags.WATER) || level.getFluidState(p.below()).is(FluidTags.WATER)) wet++;
        }
        if (!air && (wet == 0 || wet * 5 < bottom)) return fail(player, "no_water");

        List<ShipEntity.Cell> cells = new ArrayList<>(found.size());
        for (BlockPos p : found) {
            BlockState s = level.getBlockState(p);
            CompoundTag be = null;
            BlockEntity bent = level.getBlockEntity(p);
            if (bent != null) be = bent.saveWithFullMetadata(level.registryAccess());
            if (be != null && bent instanceof ShipMotorBlockEntity) be.putInt("Fuel", 0);   // 연료는 운송기 탱크 하나로 합친다
            if (s.hasProperty(BlockStateProperties.WATERLOGGED)) s = s.setValue(BlockStateProperties.WATERLOGGED, false);
            cells.add(new ShipEntity.Cell(p.getX() - minX, p.getY() - minY, p.getZ() - minZ, s, be));
        }
        // 블록 제거: 부착물(횃불 등) 먼저, 드롭·모양 갱신 없이
        found.sort((a, b) -> Boolean.compare(level.getBlockState(a).isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO),
                level.getBlockState(b).isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)));
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
        for (BlockPos p : found) {
            BlockState s = level.getBlockState(p);
            level.removeBlockEntity(p);
            boolean water = s.getFluidState().is(FluidTags.WATER) && s.getFluidState().isSource();
            level.setBlock(p, water ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), flags);
        }
        for (BlockPos p : found) level.blockUpdated(p, Blocks.AIR);

        ShipEntity ship = ModEntities.SHIP.get().create(level);
        if (ship == null) return null;
        Direction f = helmState.getValue(HorizontalDirectionalBlock.FACING);
        ship.setPos(minX, minY, minZ);
        ship.init(cells, f.toYRot());
        ship.setPos(minX + ship.pivotX(), minY, minZ + ship.pivotZ());
        ship.init(cells, f.toYRot());
        ship.setFuel(fuel);
        level.addFreshEntity(ship);
        level.playSound(null, helmPos, SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 1.5f, 0.7f);
        if (player != null) {
            player.displayClientMessage(Component.translatable(air ? "message.tycooncraft.ship.assembled_air" : "message.tycooncraft.ship.assembled",
                    cells.size(), motors, ShipEntity.fuelText(fuel), floats).withStyle(ChatFormatting.AQUA), false);
            if (fuel <= 0) player.displayClientMessage(Component.translatable("message.tycooncraft.ship.no_fuel").withStyle(ChatFormatting.RED), false);
            player.startRiding(ship, true);
        }
        return ship;
    }

    @Nullable
    private static ShipEntity fail(@Nullable Player player, String key, Object... args) {
        if (player != null)
            player.displayClientMessage(Component.translatable("message.tycooncraft.ship.fail." + key, args).withStyle(ChatFormatting.RED), true);
        return null;
    }

    private ShipAssembler() {}
}
