package com.tycooncraft.ship;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * v0.34 드론 편대 격납고. 이동 모터가 있는 구조물(운송기 선체)에만 설치할 수 있고,
 * 조종기로 묶어 운송기가 되면 보유 드론(최대 8)을 띄워 선체 주위를 돌며 적을 쏜다.
 * 드론이 격추되면 하나 줄고, 드론(아이템)을 격납고에 넣으면 다시 만들어 채운다.
 */
public class DroneBayBlock extends BaseEntityBlock {
    public static final MapCodec<DroneBayBlock> CODEC = simpleCodec(DroneBayBlock::new);

    public DroneBayBlock(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneBayBlockEntity(pos, state);
    }

    /** 놓을 자리 옆에 붙은 구조물에 이동 모터가 있나 (= 운송기 선체) */
    public static boolean onShipStructure(Level level, BlockPos pos) {
        ArrayDeque<BlockPos> q = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        seen.add(pos);
        for (Direction d : Direction.values()) q.add(pos.relative(d));
        int n = 0;
        while (!q.isEmpty() && n <= ShipAssembler.MAX_BLOCKS) {
            BlockPos p = q.poll();
            if (!seen.add(p)) continue;
            BlockState s = level.getBlockState(p);
            if (!ShipAssembler.partOfShip(s)) continue;
            n++;
            if (s.getBlock() instanceof ShipMotorBlock) return true;
            for (Direction d : Direction.values()) q.add(p.relative(d));
        }
        return false;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player p && level.getBlockEntity(pos) instanceof DroneBayBlockEntity b) b.setOwner(p.getUUID());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack st, BlockState state, Level level, BlockPos pos, Player p, InteractionHand hand, BlockHitResult hit) {
        if (!st.is(ModItems.DRONE.get())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof DroneBayBlockEntity b)) return ItemInteractionResult.FAIL;
        if (b.drones() >= DroneBayBlockEntity.MAX) {
            if (!level.isClientSide) p.displayClientMessage(Component.translatable("message.tycooncraft.drone_bay.full", DroneBayBlockEntity.MAX).withStyle(ChatFormatting.YELLOW), true);
            return ItemInteractionResult.FAIL;
        }
        if (!level.isClientSide) {
            b.setDrones(b.drones() + 1);
            st.consume(1, p);
            level.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.8f, 1.3f);
            p.displayClientMessage(Component.translatable("message.tycooncraft.drone_bay.built", b.drones(), DroneBayBlockEntity.MAX).withStyle(ChatFormatting.AQUA), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player p, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof DroneBayBlockEntity b)
            p.displayClientMessage(Component.translatable("message.tycooncraft.drone_bay.status", b.drones(), DroneBayBlockEntity.MAX).withStyle(ChatFormatting.AQUA), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
