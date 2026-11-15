package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 총 효과음 (assets/tycooncraft/sounds.json, 직접 합성한 ogg) */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TycoonCraft.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> PISTOL_FIRE = sound("gun.pistol.fire");
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

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TycoonCraft.id(name)));
    }

    private ModSounds() {}
}
