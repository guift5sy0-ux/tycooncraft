package com.tycooncraft.registry;

import com.tycooncraft.TycoonCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TycoonCraft.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tycooncraft"))
                    .icon(() -> new ItemStack(ModItems.COIN_GOLD.get()))
                    .displayItems((params, out) -> ModItems.ITEMS.getEntries().forEach(holder -> out.accept(holder.get())))
                    .build());

    /** 탈것·총기 전용 탭: 크리에이티브에서 바로 꺼내기 쉽게 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> VEHICLES = TABS.register("vehicles",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.tycooncraft.vehicles"))
                    .icon(() -> new ItemStack(ModItems.HELICOPTER.get()))
                    .withTabsBefore(MAIN.getId())
                    .displayItems((params, out) -> {
                        out.accept(ModItems.CAR.get());
                        out.accept(ModItems.HELICOPTER.get());
                        out.accept(ModItems.LOCOMOTIVE.get());
                        out.accept(ModItems.FUEL_CANISTER.get());
                        out.accept(ModItems.DRONE.get());
                        out.accept(ModItems.DRONE_MK2.get());
                        out.accept(ModItems.TANK.get());
                        out.accept(ModItems.FIGHTER_JET.get());
                        out.accept(ModItems.TURRET.get());
                        out.accept(ModItems.CIWS.get());
                        out.accept(ModItems.AUTO_MINER.get());
                        out.accept(ModItems.DRILL_HEAD.get());
                        out.accept(ModItems.WHEEL.get());
                        out.accept(ModItems.ENGINE.get());
                        out.accept(ModItems.PARACHUTE.get());
                        out.accept(ModItems.PISTOL.get());
                        out.accept(ModItems.SMG.get());
                        out.accept(ModItems.RIFLE.get());
                        out.accept(ModItems.SHOTGUN.get());
                        out.accept(ModItems.SNIPER.get());
                        out.accept(ModItems.VSS.get());
                        out.accept(ModItems.DUAL_PISTOLS.get());
                        out.accept(ModItems.RPG.get());
                        out.accept(ModItems.ROCKET.get());
                        out.accept(ModItems.TANK_SHELL.get());
                        out.accept(ModItems.LIGHT_AMMO.get());
                        out.accept(ModItems.HEAVY_AMMO.get());
                        out.accept(ModItems.SHOTGUN_SHELL.get());
                        out.accept(ModItems.JUDGE.get());
                        out.accept(ModItems.FRAG_GRENADE.get());
                        out.accept(ModItems.FLASH_GRENADE.get());
                        out.accept(ModItems.SMOKE_GRENADE.get());
                        out.accept(ModItems.EMP_GENERATOR.get());
                        out.accept(ModItems.SHIP_HELM.get());
                        out.accept(ModItems.SHIP_MOTOR.get());
                        out.accept(ModItems.SHIP_BOOSTER.get());
                        out.accept(ModItems.SETTER.get());
                        out.accept(ModItems.SHIP_FLOAT.get());
                        out.accept(ModItems.SULFUR.get());
                        out.accept(ModItems.SULFUR_ORE.get());
                        out.accept(ModItems.DEEPSLATE_SULFUR_ORE.get());
                        out.accept(ModItems.SILENCER.get());
                        for (var p : ModItems.GUN_PART_ITEMS.values()) out.accept(p.get());
                        out.accept(ModItems.SILENCER_ADVANCED.get());
                        for (var v : java.util.List.of(ModItems.VEST_1, ModItems.VEST_2, ModItems.VEST_3, ModItems.HELMET_1, ModItems.HELMET_2, ModItems.HELMET_3, ModItems.PANTS_1, ModItems.PANTS_2, ModItems.PANTS_3, ModItems.BOOTS_1, ModItems.BOOTS_2, ModItems.BOOTS_3, ModItems.HELMET_4, ModItems.VEST_4, ModItems.JUGGERNAUT_LEGGINGS, ModItems.COMBAT_BOOTS)) out.accept(v.get());
                        out.accept(ModItems.POLICE_SHIELD.get());
                        out.accept(ModItems.FACTORY_KIT.get());
                        out.accept(ModItems.GUN_WORKBENCH.get());
                        out.accept(ModItems.CARTEL_FLARE.get());
                        for (var v : java.util.List.of(ModItems.GANGSTER_EGG, ModItems.MERCENARY_EGG, ModItems.OUTLAW_EGG, ModItems.SPIDER_EGG, ModItems.BOSS_EGG, ModItems.GANG_TRUCK_EGG, ModItems.GOLEM_EGG,
                                ModItems.DESTROYER_CORE)) out.accept(v.get());
                    })
                    .build());

    private ModCreativeTabs() {}
}
