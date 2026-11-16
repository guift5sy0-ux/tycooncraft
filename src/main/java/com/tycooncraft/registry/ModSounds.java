package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 총·탈것 효과음 (assets/tycooncraft/sounds.json, 직접 합성한 ogg) */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TycoonCraft.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = sound("gun.pistol.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_WHIZ = sound("gun.bullet.whiz");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_STONE = sound("gun.bullet.impact_stone");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_DIRT = sound("gun.bullet.impact_dirt");
    public static final DeferredHolder<SoundEvent, SoundEvent> BULLET_IMPACT_METAL = sound("gun.bullet.impact_metal");
    public static final DeferredHolder<SoundEvent, SoundEvent> EMP_CHARGE = sound("machine.emp.charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> EMP_BLAST = sound("machine.emp.blast");
    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_RELOAD = sound("gun.pistol.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMG_FIRE = sound("gun.smg.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMG_RELOAD = sound("gun.smg.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_FIRE = sound("gun.rifle.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> RIFLE_RELOAD = sound("gun.rifle.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_FIRE = sound("gun.shotgun.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHOTGUN_RELOAD = sound("gun.shotgun.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNIPER_FIRE = sound("gun.sniper.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNIPER_RELOAD = sound("gun.sniper.reload");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRY_FIRE = sound("gun.dry_fire");

    // 탈것
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_ENGINE = sound("vehicle.car.engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> CAR_HORN = sound("vehicle.car.horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> ENGINE_START = sound("vehicle.engine_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> HELI_ROTOR = sound("vehicle.heli.rotor");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRAIN_ENGINE = sound("vehicle.train.engine");
    public static final DeferredHolder<SoundEvent, SoundEvent> TRAIN_HORN = sound("vehicle.train.horn");
    public static final DeferredHolder<SoundEvent, SoundEvent> SILENCED_FIRE = sound("gun.silenced.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_BUZZ = sound("vehicle.drone.buzz");
    public static final DeferredHolder<SoundEvent, SoundEvent> DRONE_ARM = sound("vehicle.drone.arm");
    public static final DeferredHolder<SoundEvent, SoundEvent> RPG_FIRE = sound("gun.rpg.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINIGUN_SPIN = sound("mob.destroyer.minigun_spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> SERVO = sound("mob.destroyer.servo");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEHEMOTH_ROAR = sound("mob.behemoth.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> BEHEMOTH_STOMP = sound("mob.behemoth.stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> WYVERN_SCREECH = sound("mob.storm_wyvern.screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> STORM_ORB = sound("mob.storm_wyvern.orb");
    public static final DeferredHolder<SoundEvent, SoundEvent> JUDGE_FIRE = sound("gun.judge.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_EXPLODE = sound("gun.grenade.explode");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRENADE_PIN = sound("gun.grenade.pin");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLASHBANG = sound("gun.grenade.flash");
    public static final DeferredHolder<SoundEvent, SoundEvent> TINNITUS = sound("gun.grenade.tinnitus");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMOKE_HISS = sound("gun.grenade.smoke");
    public static final DeferredHolder<SoundEvent, SoundEvent> MINER_DRILL = sound("vehicle.miner.drill");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIDER_WEB = sound("mob.queen_spider.web");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPIDER_VENOM = sound("mob.queen_spider.venom");
    public static final DeferredHolder<SoundEvent, SoundEvent> TANK_FIRE = sound("vehicle.tank.fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> JET_ENGINE = sound("vehicle.jet.engine");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TycoonCraft.id(name)));
    }

    private ModSounds() {}
}
