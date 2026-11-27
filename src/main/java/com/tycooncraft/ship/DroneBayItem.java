package com.tycooncraft.ship;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** v0.34: 운송기 선체(이동 모터가 이어진 구조물)에만 놓을 수 있음 */
public class DroneBayItem extends BlockItem {
    public static int REFUSED;

    public DroneBayItem(Block b, Properties p) {
        super(b, p);
    }

    @Override
    public InteractionResult place(BlockPlaceContext ctx) {
        if (!DroneBayBlock.onShipStructure(ctx.getLevel(), ctx.getClickedPos())) {
            if (!ctx.getLevel().isClientSide && ctx.getPlayer() != null) {
                REFUSED++;
                ctx.getPlayer().displayClientMessage(Component.translatable("message.tycooncraft.drone_bay.need_ship").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        return super.place(ctx);
    }

    public static int drones(ItemStack st) {
        CustomData d = st.get(DataComponents.CUSTOM_DATA);
        return d != null && d.contains("Drones") ? d.copyTag().getInt("Drones") : DroneBayBlockEntity.START;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.tycooncraft.drone_swarm").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.tycooncraft.drone_bay.count", drones(st), DroneBayBlockEntity.MAX).withStyle(ChatFormatting.AQUA));
    }
}
