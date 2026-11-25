package com.tycooncraft.mob.carrier;

import com.tycooncraft.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * v0.27.1 드론 편대 발사기 (비행 코어): 우클릭하면 미니 드론 6기가 한꺼번에 떠서 1분 동안 각자 알아서 싸움.
 * 주인이 때린 적 → 주인을 때린 적 → 주변 적대 몹 순으로 노리고, 할 일이 없으면 주인 곁을 돎. 재사용 대기 90초.
 */
public class DroneSwarmItem extends Item {
    public static final int COUNT = 6, LIFE = 1200, COOLDOWN = 1800;
    public static int launches;

    public DroneSwarmItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand hand) {
        ItemStack st = p.getItemInHand(hand);
        if (level instanceof ServerLevel sl) launch(sl, p);
        p.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    public static int launch(ServerLevel sl, Player p) {
        // 이전 편대는 정리
        for (MiniDrone d : sl.getEntitiesOfClass(MiniDrone.class, p.getBoundingBox().inflate(64), d -> p.getUUID().equals(d.ownerId()))) d.discard();
        int made = 0;
        for (int i = 0; i < COUNT; i++) {
            MiniDrone d = ModEntities.MINI_DRONE.get().create(sl);
            if (d == null) continue;
            double a = Math.PI * 2 * i / COUNT;
            d.moveTo(p.getX() + Math.cos(a) * 1.2, p.getY() + 1.8, p.getZ() + Math.sin(a) * 1.2, p.getYRot(), 0);
            d.finalizeSpawn(sl, sl.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            d.setOwner(p, LIFE);
            d.setPersistenceRequired();
            sl.addFreshEntity(d);
            made++;
        }
        launches++;
        sl.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BEE_LOOP_AGGRESSIVE, SoundSource.PLAYERS, 1.5f, 1.6f);
        p.displayClientMessage(Component.translatable("message.tycooncraft.drone_swarm.launch", made).withStyle(ChatFormatting.AQUA), true);
        return made;
    }

    @Override
    public void appendHoverText(ItemStack st, TooltipContext ctx, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("tooltip.tycooncraft.drone_swarm").withStyle(ChatFormatting.GRAY));
    }
}
