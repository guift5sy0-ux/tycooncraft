package com.tycooncraft.devtest;

import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.mob.GangTruck;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** v0.18.3: 운전수 없는 갱단 트럭, 쌍권총 왼손 제한, 경찰 방패 */
public final class V183Test {
    static GangTruck empty, crewed;
    static Vec3 emptyStart, crewedStart;
    static Zombie front, behind, back, bait;
    static Vec3 base;

    static Zombie zombie(net.minecraft.server.level.ServerLevel sl, Vec3 at, float yaw) {
        Zombie z = EntityType.ZOMBIE.create(sl);
        z.moveTo(at.x, at.y, at.z, yaw, 0);
        z.setYHeadRot(yaw);
        z.yBodyRot = yaw;
        z.setNoAi(true);
        z.setPersistenceRequired();
        z.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));   // 낮에 타지 않게
        sl.addFreshEntity(z);
        return z;
    }

    static void setup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        base = new Vec3(-21.5, sp.getY(), -30.5);
        sp.teleportTo(sl, base.x, base.y, base.z - 7, 0f, 8f);   // 남쪽(+z)을 봄
        // 트럭 두 대: 승무원 있음 / 없음, 둘 다 같은 미끼를 노림
        bait = zombie(sl, base.add(0, 0, 30), 0);
        crewed = ModEntities.GANG_TRUCK.get().spawn(sl, BlockPos.containing(base.add(-25, 0, 10)), MobSpawnType.MOB_SUMMONED);
        empty = ModEntities.GANG_TRUCK.get().spawn(sl, BlockPos.containing(base.add(25, 0, 10)), MobSpawnType.MOB_SUMMONED);
        int crew = empty.getPassengers().size();
        for (Entity p : java.util.List.copyOf(empty.getPassengers())) {
            p.stopRiding();
            p.discard();
        }
        crewed.setTarget(bait);
        empty.setTarget(bait);
        crewedStart = crewed.position();
        emptyStart = empty.position();
        System.out.println("[AUTOTEST] v183 truck spawnedCrew=" + crew + " crewedHasDriver=" + crewed.hasDriver() + " emptyHasDriver=" + empty.hasDriver());

        // 경찰 방패 든 좀비 (북쪽 = 플레이어 쪽을 봄), 그 뒤에 한 마리, 등을 돌린 한 마리
        front = zombie(sl, base, 180f);
        behind = zombie(sl, base.add(0, 0, 2), 180f);
        back = zombie(sl, base.add(4, 0, 0), 0f);
        for (Zombie z : new Zombie[]{front, back}) {
            z.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(ModItems.POLICE_SHIELD.get()));
            z.startUsingItem(InteractionHand.OFF_HAND);
        }

        // 쌍권총: 왼손에 무언가 있으면 못 씀
        ItemStack dual = new ItemStack(ModItems.DUAL_PISTOLS.get());
        GunItem.setAmmo(dual, 10);
        sp.setItemInHand(InteractionHand.MAIN_HAND, dual);
        sp.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TORCH));
        var r1 = dual.getItem().use(sl, sp, InteractionHand.MAIN_HAND).getResult();
        int ammo1 = GunItem.ammo(sp.getMainHandItem());
        sp.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        sp.getCooldowns().removeCooldown(dual.getItem());
        var r2 = dual.getItem().use(sl, sp, InteractionHand.MAIN_HAND).getResult();
        int ammo2 = GunItem.ammo(sp.getMainHandItem());
        System.out.println("[AUTOTEST] v183 dual withOffhand=" + r1 + " ammo=" + ammo1 + " emptyOffhand=" + r2 + " ammo=" + ammo2);
        sp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        sp.stopUsingItem();
    }

    static void check(ServerPlayer sp) {
        var sl = sp.serverLevel();
        double em = empty.position().subtract(emptyStart).horizontalDistance(), cm = crewed.position().subtract(crewedStart).horizontalDistance();
        System.out.println(String.format("[AUTOTEST] v183 truck emptyMoved=%.2f crewedMoved=%.2f", em, cm));
        empty.discard();
        crewed.ejectPassengers();
        crewed.discard();
        bait.discard();

        // 경찰 방패: 정면 사격은 막히고 뒤로 관통하지 않음 / 등 뒤 사격은 맞음
        var shooter = EntityType.ZOMBIE.create(sl);
        shooter.moveTo(base.x, base.y, base.z - 6, 0, 0);
        float hf = front.getHealth(), hb = behind.getHealth(), hk = back.getHealth();
        int blocked0 = Ballistics.BLOCKED;
        boolean blocking = front.isBlocking();
        Vec3 from = new Vec3(base.x, base.y + 1.2, base.z - 6);
        Ballistics.shoot(sl, shooter, from, new Vec3(0, 0, 1), 30, 8f, 2, 1.5f, false, false, sl.damageSources().mobAttack(shooter), e -> e != shooter);
        Vec3 kb0 = front.getDeltaMovement();
        front.knockback(2.0, 0, -1);
        boolean noKb = front.getDeltaMovement().distanceTo(kb0) < 1e-6;
        Vec3 from2 = new Vec3(base.x + 4, base.y + 1.2, base.z - 6);   // back 좀비는 남쪽을 봄 → 북쪽에서 쏘면 등 뒤
        Ballistics.shoot(sl, shooter, from2, new Vec3(0, 0, 1), 30, 8f, 0, 1.5f, false, false, sl.damageSources().mobAttack(shooter), e -> e != shooter);
        System.out.println(String.format("[AUTOTEST] v183 shield blocking=%s frontDmg=%.1f behindDmg=%.1f backTurnedDmg=%.1f blockedHits=%d noKnockback=%s",
                blocking, hf - front.getHealth(), hb - behind.getHealth(), hk - back.getHealth(), Ballistics.BLOCKED - blocked0, noKb));
        System.out.println("[AUTOTEST] v183 dragonKb=" + com.tycooncraft.mob.VanillaBossBuffs.DRAGON_KB
                + " vest=" + ModItems.VEST_1.get().getDefense() + "/" + ModItems.VEST_2.get().getDefense() + "/" + ModItems.VEST_3.get().getDefense() + "/" + ModItems.VEST_4.get().getDefense());
        // 화면용: 플레이어가 방패 좀비를 바라봄
        sp.getInventory().clearContent();
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.POLICE_SHIELD.get()));
        sp.teleportTo(sl, base.x + 1.5, base.y, base.z - 3.2, 25f, 12f);
    }

    static void cleanup(ServerPlayer sp) {
        for (Zombie z : new Zombie[]{front, behind, back}) if (z != null) z.discard();
    }

    private V183Test() {}
}
