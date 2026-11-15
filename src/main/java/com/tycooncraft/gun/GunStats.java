package com.tycooncraft.gun;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * 총 성능.
 * @param damage      한 발(산탄은 한 알) 피해
 * @param pellets     한 번에 나가는 탄 수 (산탄총 > 1)
 * @param spread      탄 퍼짐
 * @param range       사거리 (블록)
 * @param magazine    탄창 크기
 * @param fireDelay   연사 간격 / 단발 쿨타임 (틱)
 * @param reloadTicks 재장전 시간 (틱)
 * @param auto        누르고 있으면 연사
 * @param scoped      꾹 눌러 조준(줌), 떼면 발사
 * @param recoil      반동 (화면이 위로 튀는 정도)
 */
public record GunStats(float damage, int pellets, float spread, double range, int magazine, int fireDelay,
                       int reloadTicks, boolean auto, boolean scoped, float recoil,
                       Supplier<Item> ammo, Supplier<SoundEvent> fireSound, Supplier<SoundEvent> reloadSound) {
}
