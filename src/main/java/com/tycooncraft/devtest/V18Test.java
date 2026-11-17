package com.tycooncraft.devtest;

import com.tycooncraft.gun.GunEvents;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.mob.DestroyerGolem;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.world.StructureBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent;

import java.util.Map;
import java.util.TreeMap;

/** v0.18: 폐공장 지하실 + 골렘, 갱단 드롭률, 스코프 F 전환 */
public final class V18Test {
    static BlockPos ruin;
    static DestroyerGolem golem;

    static void build(ServerPlayer sp) {
        var sl = sp.serverLevel();
        ruin = new BlockPos(60, (int) sp.getY() + 20, 190);
        StructureBuilder.forceBasement = true;
        StructureBuilder.ruin(sl, ruin);
        StructureBuilder.forceBasement = false;
        var list = sl.getEntitiesOfClass(DestroyerGolem.class, new AABB(ruin).inflate(30), g -> g.getPersistentData().getBoolean(DestroyerGolem.BASEMENT_TAG));
        golem = list.isEmpty() ? null : list.get(0);
        if (golem != null) golem.setNoAi(true);
        boolean ladder = sl.getBlockState(ruin.offset(19, -6, 12)).is(Blocks.LADDER) && sl.getBlockState(ruin.offset(19, -1, 12)).is(Blocks.LADDER);
        boolean room = sl.getBlockState(ruin.offset(11, -9, 7)).isAir() && sl.getBlockState(ruin.offset(11, -13, 7)).is(Blocks.POLISHED_DEEPSLATE);
        System.out.println("[AUTOTEST] v18 basement golems=" + list.size() + " ladder=" + ladder + " room=" + room
                + (golem == null ? "" : String.format(" golemY=%.1f floorY=%d", golem.getY(), ruin.getY() - 12)));
        sp.getInventory().clearContent();
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
        sp.teleportTo(sl, ruin.getX() + 14.5, ruin.getY() - 9.5, ruin.getZ() + 12.2, 143f, 22f);
    }

    static void kill(ServerPlayer sp) {
        var sl = sp.serverLevel();
        if (golem != null) {
            golem.setNoAi(false);
            golem.hurt(sl.damageSources().playerAttack(sp), 100000f);
        }
        // 갱단원 300명 → 드롭 집계
        BlockPos gp = new BlockPos(60, ruin.getY() - 20, 240);
        for (int i = 0; i < 300; i++) {
            var g = ModEntities.GANGSTER.get().spawn(sl, gp, MobSpawnType.MOB_SUMMONED);
            if (g == null) continue;
            g.setNoAi(true);
            g.hurt(sl.damageSources().playerAttack(sp), 100000f);
        }
        // 스코프 F 전환
        ItemStack sniper = new ItemStack(ModItems.SNIPER.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, sniper);
        GunItem gun = (GunItem) sniper.getItem();
        boolean a = gun.isScoped(sp.getMainHandItem());
        GunEvents.onSwap(new LivingSwapItemsEvent.Hands(sp));
        boolean b = gun.isScoped(sp.getMainHandItem());
        GunEvents.onSwap(new LivingSwapItemsEvent.Hands(sp));
        boolean c = gun.isScoped(sp.getMainHandItem());
        ItemStack pistol = new ItemStack(ModItems.PISTOL.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, pistol);
        var ev = new LivingSwapItemsEvent.Hands(sp);
        GunEvents.onSwap(ev);
        ItemStack rifle = new ItemStack(ModItems.RIFLE.get());
        com.tycooncraft.gun.GunParts.install(rifle, com.tycooncraft.gun.GunPart.SCOPE);
        sp.setItemInHand(InteractionHand.MAIN_HAND, rifle);
        boolean r1 = ((GunItem) rifle.getItem()).isScoped(sp.getMainHandItem());
        GunEvents.onSwap(new LivingSwapItemsEvent.Hands(sp));
        boolean r2 = ((GunItem) rifle.getItem()).isScoped(sp.getMainHandItem());
        System.out.println("[AUTOTEST] v18 scopeToggle sniper=" + a + "->" + b + "->" + c + " pistolSwapBlocked=" + ev.isCanceled()
                + " scopedRifle=" + r1 + "->" + r2);
        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    static void check(ServerPlayer sp) {
        var sl = sp.serverLevel();
        Map<String, Integer> golemDrops = new TreeMap<>(), gangDrops = new TreeMap<>();
        if (ruin != null) for (ItemEntity e : sl.getEntitiesOfClass(ItemEntity.class, new AABB(ruin).inflate(30)))
            golemDrops.merge(e.getItem().getItem().builtInRegistryHolder().key().location().getPath(), e.getItem().getCount(), Integer::sum);
        int gunsFromGang = 0;
        for (ItemEntity e : sl.getEntitiesOfClass(ItemEntity.class, new AABB(new BlockPos(60, ruin.getY() - 20, 240)).inflate(12))) {
            gangDrops.merge(e.getItem().getItem().builtInRegistryHolder().key().location().getPath(), 1, Integer::sum);
            if (e.getItem().getItem() instanceof GunItem) gunsFromGang++;
            e.discard();
        }
        System.out.println("[AUTOTEST] v18 golemDead=" + (golem != null && !golem.isAlive()) + " basementDrops=" + DestroyerGolem.BASEMENT_DROPS + " golemLoot=" + golemDrops);
        System.out.println("[AUTOTEST] v18 gangDrops(300 kills)=" + gangDrops + " guns=" + gunsFromGang);
        sp.teleportTo(sl, -21.5, ruin.getY() - 19, -45.5, 0f, 10f);
    }

    private V18Test() {}
}
