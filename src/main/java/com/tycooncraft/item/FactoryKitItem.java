package com.tycooncraft.item;

import com.tycooncraft.world.StructureBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.List;

/**
 * 공장 키트: 들고 우클릭하면 바라보는 방향 앞에 기본 공장(15×11)을 세운다.
 * 웅크리고 우클릭: 세워질 자리를 미리 보기. 상자·기계 같은 블록이 자리에 있으면 거절(남의 건물 보호).
 */
public class FactoryKitItem extends Item {
    public FactoryKitItem(Properties props) {
        super(props);
    }

    /** 플레이어 앞쪽에 놓일 공장의 원점 (북서쪽 아래 모서리) */
    public static BlockPos originFor(Player p) {
        Direction d = p.getDirection();
        BlockPos feet = p.blockPosition();
        int w = StructureBuilder.KIT_W, dep = StructureBuilder.KIT_D;
        int along = d.getAxis() == Direction.Axis.X ? w : dep;
        BlockPos center = feet.relative(d, along / 2 + 2);
        return center.offset(-w / 2, 0, -dep / 2);
    }

    /** 자리에 보호해야 할 블록(블록 엔티티·기반암)이 있으면 그 위치 */
    public static BlockPos blocked(ServerLevel sl, BlockPos o) {
        for (int x = 0; x < StructureBuilder.KIT_W; x++)
            for (int z = 0; z < StructureBuilder.KIT_D; z++)
                for (int y = -1; y <= 7; y++) {
                    BlockPos p = o.offset(x, y, z);
                    if (sl.getBlockEntity(p) != null || sl.getBlockState(p).is(Blocks.BEDROCK)) return p;
                }
        return null;
    }

    public static boolean build(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        BlockPos o = originFor(p);
        if (o.getY() - 2 <= sl.getMinBuildHeight() || o.getY() + 8 >= sl.getMaxBuildHeight()) {
            p.displayClientMessage(Component.translatable("message.tycooncraft.factory_kit.height").withStyle(ChatFormatting.RED), true);
            return false;
        }
        BlockPos b = blocked(sl, o);
        if (b != null) {
            p.displayClientMessage(Component.translatable("message.tycooncraft.factory_kit.blocked", b.getX(), b.getY(), b.getZ()).withStyle(ChatFormatting.RED), true);
            return false;
        }
        StructureBuilder.starterFactory(sl, o);
        sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.8f, 0.8f);
        sl.sendParticles(ParticleTypes.CLOUD, o.getX() + 7.5, o.getY() + 3, o.getZ() + 5.5, 80, 6, 2, 4, 0.02);
        p.sendSystemMessage(Component.translatable("message.tycooncraft.factory_kit.built").withStyle(ChatFormatting.GOLD));
        return true;
    }

    private static void preview(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        BlockPos o = originFor(p);
        int w = StructureBuilder.KIT_W, d = StructureBuilder.KIT_D;
        for (int i = 0; i <= w; i++)
            for (int j : new int[]{0, d}) {
                sl.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, o.getX() + i, o.getY() + 0.2, o.getZ() + j, 1, 0, 0, 0, 0);
                sl.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, o.getX() + i, o.getY() + 6.2, o.getZ() + j, 1, 0, 0, 0, 0);
            }
        for (int j = 0; j <= d; j++)
            for (int i : new int[]{0, w}) {
                sl.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, o.getX() + i, o.getY() + 0.2, o.getZ() + j, 1, 0, 0, 0, 0);
                sl.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, o.getX() + i, o.getY() + 6.2, o.getZ() + j, 1, 0, 0, 0, 0);
            }
        BlockPos b = blocked(sl, o);
        p.displayClientMessage(b == null ? Component.translatable("message.tycooncraft.factory_kit.preview").withStyle(ChatFormatting.GREEN)
                : Component.translatable("message.tycooncraft.factory_kit.blocked", b.getX(), b.getY(), b.getZ()).withStyle(ChatFormatting.RED), true);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp)) return InteractionResultHolder.success(stack);
        if (player.isShiftKeyDown()) {
            preview(sp);
            return InteractionResultHolder.success(stack);
        }
        if (build(sp) && !player.getAbilities().instabuild) stack.shrink(1);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.tycooncraft.factory_kit").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.tycooncraft.factory_kit2").withStyle(ChatFormatting.DARK_GRAY));
    }
}
