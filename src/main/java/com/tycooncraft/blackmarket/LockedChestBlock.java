package com.tycooncraft.blackmarket;

import com.mojang.serialization.MapCodec;
import com.tycooncraft.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * v0.29 언더시티 갱단 금고 상자. 잠겨 있으면 "언더시티 열쇠" 1개를 써야 열림(열쇠는 사라짐).
 * 한 번 열면 하루 동안은 그냥 열리고, 하루가 지나면 다시 채워지고 잠김.
 */
public class LockedChestBlock extends BaseEntityBlock {
    public static final MapCodec<LockedChestBlock> CODEC = simpleCodec(LockedChestBlock::new);
    public static final BooleanProperty LOCKED = BooleanProperty.create("locked");
    public static int unlocks, denied;

    public LockedChestBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH).setValue(LOCKED, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(HorizontalDirectionalBlock.FACING, LOCKED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState s) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedChestBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack st, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof LockedChestBlockEntity be) || !(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        tryOpen(sp, be);
        return InteractionResult.CONSUME;
    }

    /** 열기 시도: 결과 true = 열림 */
    public static boolean tryOpen(ServerPlayer sp, LockedChestBlockEntity be) {
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        long now = level.getGameTime();
        if (!be.locked && be.openedAt != -1 && now - be.openedAt > LockedChestBlockEntity.RESTOCK) {
            be.restock(level.random, be.tier);   // 하루 지남 → 다시 채우고 잠금
            setLockedState(level, pos, true);
        }
        if (be.locked) {
            ItemStack key = findKey(sp);
            if (key.isEmpty()) {
                denied++;
                level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 1f, 1f);
                sp.displayClientMessage(Component.translatable("message.tycooncraft.locked_chest.locked").withStyle(ChatFormatting.RED), true);
                return false;
            }
            key.shrink(1);   // 열쇠는 1회용
            be.locked = false;
            be.openedAt = now;
            be.setChanged();
            setLockedState(level, pos, false);
            unlocks++;
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1f, 1.4f);
            sp.displayClientMessage(Component.translatable("message.tycooncraft.locked_chest.unlocked").withStyle(ChatFormatting.GOLD), true);
        }
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 0.8f, 0.9f);
        sp.openMenu(be);
        return true;
    }

    static ItemStack findKey(ServerPlayer sp) {
        if (sp.getMainHandItem().is(ModItems.UNDERCITY_KEY.get())) return sp.getMainHandItem();
        if (sp.getOffhandItem().is(ModItems.UNDERCITY_KEY.get())) return sp.getOffhandItem();
        var inv = sp.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) if (inv.getItem(i).is(ModItems.UNDERCITY_KEY.get())) return inv.getItem(i);
        return ItemStack.EMPTY;
    }

    static void setLockedState(Level l, BlockPos p, boolean locked) {
        BlockState s = l.getBlockState(p);
        if (s.hasProperty(LOCKED) && s.getValue(LOCKED) != locked) l.setBlock(p, s.setValue(LOCKED, locked), 2);
    }
}
