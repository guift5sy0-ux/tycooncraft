package com.tycooncraft.vehicle;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** v0.18.9: 차량(차·탱크·헬기·전투기)에 탄 사람은 총/공격 피해를 받지 않고, 대신 차체 체력이 먼저 깎임 */
public final class VehicleArmor {
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide) return;
        var src = event.getSource();
        if (e.getVehicle() instanceof com.tycooncraft.mech.ProtoDestroyer m && m.isAlive()) {   // 프로토 디스트로이어 조종석
            if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
            if (src.is(DamageTypeTags.IS_EXPLOSION)) { event.setCanceled(true); return; }
            if (src.getEntity() == null && src.getDirectEntity() == null) return;
            if (src.getEntity() != null && m.hasPassenger(src.getEntity())) return;
            m.hurt(src, event.getAmount());
            event.setAmount(event.getAmount() * com.tycooncraft.mech.ProtoDestroyer.PILOT_SHARE);   // 일부는 조종석까지 들어옴
            return;
        }
        if (!(e.getVehicle() instanceof VehicleEntity v) || v.isRemoved()) return;
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;   // /kill, 공허
        if (src.is(DamageTypeTags.IS_EXPLOSION)) {   // 폭발은 차체가 따로 맞으므로 탑승자는 보호만
            v.hurt(src, event.getAmount() * 0.5f);
            event.setCanceled(true);
            return;
        }
        if (src.getEntity() == null && src.getDirectEntity() == null) return;   // 불·익사·낙하 등은 그대로
        if (src.getEntity() != null && v.hasPassenger(src.getEntity())) return;   // 같이 탄 사람끼리
        v.hurt(src, event.getAmount());
        event.setCanceled(true);
    }

    /** 테스트용: 탑승자에게 피해를 주고 (탑승자 체력 변화, 차체 피해) 반환 */
    public static float[] probe(LivingEntity rider, net.minecraft.world.damagesource.DamageSource src, float amount) {
        VehicleEntity v = (VehicleEntity) rider.getVehicle();
        float hp = rider.getHealth(), dmg = v.getDamage();
        rider.invulnerableTime = 0;
        rider.hurt(src, amount);
        return new float[]{hp - rider.getHealth(), v.getDamage() - dmg};
    }

    private VehicleArmor() {}
}
