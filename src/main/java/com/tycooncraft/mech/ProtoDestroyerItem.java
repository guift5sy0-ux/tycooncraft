package com.tycooncraft.mech;

import com.tycooncraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** 땅에 우클릭 → 프로토 디스트로이어 배치 (주인 = 놓은 사람) */
public class ProtoDestroyerItem extends Item {
    /** v0.27.1: 정찰형(true)이면 프로토 디스트로이어-정찰 */
    private final boolean scout;

    public ProtoDestroyerItem(Properties p) {
        this(p, false);
    }

    public ProtoDestroyerItem(Properties p, boolean scout) {
        super(p);
        this.scout = scout;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel() instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
        BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
        if (!sl.noCollision((scout ? ModEntities.PROTO_SCOUT.get() : ModEntities.PROTO_DESTROYER.get()).getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
            if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.translatable("message.tycooncraft.proto.no_room"), true);
            return InteractionResult.FAIL;
        }
        ItemStack st = ctx.getItemInHand();
        if (scout) spawnScout(sl, pos, ctx.getPlayer(), st, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0);
        else spawn(sl, pos, ctx.getPlayer(), st, ctx.getPlayer() != null ? ctx.getPlayer().getYRot() : 0);
        st.shrink(1);
        return InteractionResult.CONSUME;
    }

    public static ProtoDestroyer spawn(ServerLevel sl, BlockPos pos, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player owner, ItemStack st, float yaw) {
        ProtoDestroyer m = ModEntities.PROTO_DESTROYER.get().create(sl);
        if (m == null) return null;
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        m.setYBodyRot(yaw);
        m.setYHeadRot(yaw);
        if (owner != null) m.setOwner(owner.getUUID());
        CustomData cd = st.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains("Health")) m.setHealth(Math.max(1f, cd.copyTag().getFloat("Health")));
        sl.addFreshEntity(m);
        m.playSound(net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 2f, 0.6f);
        return m;
    }

    public static ProtoScout spawnScout(ServerLevel sl, BlockPos pos, @org.jetbrains.annotations.Nullable net.minecraft.world.entity.player.Player owner, ItemStack st, float yaw) {
        ProtoScout m = ModEntities.PROTO_SCOUT.get().create(sl);
        if (m == null) return null;
        m.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        m.setYBodyRot(yaw);
        m.setYHeadRot(yaw);
        if (owner != null) m.setOwner(owner.getUUID());
        CustomData cd = st.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains("Health")) m.setHealth(Math.max(1f, cd.copyTag().getFloat("Health")));
        sl.addFreshEntity(m);
        m.playSound(net.minecraft.sounds.SoundEvents.RAVAGER_ROAR, 1.5f, 1.6f);
        return m;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        CustomData cd = st.get(DataComponents.CUSTOM_DATA);
        if (cd != null && cd.contains("Health"))
            tip.add(Component.translatable("tooltip.tycooncraft.proto.health", Math.round(cd.copyTag().getFloat("Health")), scout ? ProtoScout.MAX_HP : ProtoDestroyer.MAX_HP).withStyle(net.minecraft.ChatFormatting.RED));
        for (int i = 1; i <= 4; i++) tip.add(Component.translatable("tooltip.tycooncraft." + (scout ? "proto_scout." : "proto.") + i).withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
