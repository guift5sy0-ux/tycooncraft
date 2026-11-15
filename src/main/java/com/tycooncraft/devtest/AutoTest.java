package com.tycooncraft.devtest;

import com.tycooncraft.gun.GunEnchants;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import com.tycooncraft.vehicle.CarEntity;
import com.tycooncraft.vehicle.HelicopterEntity;
import com.tycooncraft.vehicle.LocomotiveEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * 개발용 자동 시험 (-Dtycooncraft.autotest=true 일 때만 등록됨, 일반 플레이에는 영향 없음).
 * 서버 쪽: 탈것을 꺼내 놓고 정해진 시간에 시점 이동·탑승을 바꾼다. 클라이언트 쪽 {@code AutoTestClient}가 스크린샷을 찍는다.
 */
public final class AutoTest {
    public static final boolean ENABLED = Boolean.getBoolean("tycooncraft.autotest");

    private static ServerPlayer player;
    private static int t = -1;
    private static int gy;
    private static CarEntity car;
    private static HelicopterEntity heli;
    private static LocomotiveEntity loco;
    private static boolean droneGiven;

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            player = sp;
            t = 0;
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (player == null || t < 0) return;
        t++;
        ServerLevel level = player.serverLevel();
        try {
            switch (t) {
                case 30 -> setup(level);
                case 60 -> look(level, new Vec3(9, gy + 5.5, -13), new Vec3(-3, gy + 1, 2));       // 전체
                case 140 -> look(level, new Vec3(4.2, gy + 1.9, 4.6), new Vec3(0, gy + 0.8, 0));   // 자동차 앞
                case 220 -> look(level, new Vec3(-7.5, gy + 3.2, -4.0), new Vec3(-14, gy + 1.6, -12)); // 헬기
                case 300 -> look(level, new Vec3(1.0, gy + 2.6, 5.0), new Vec3(6.5, gy + 1.0, 10.5)); // 기관차
                case 380 -> player.startRiding(car, true);
                case 620 -> {
                    player.stopRiding();
                    player.startRiding(heli, true);
                }
                case 900 -> {
                    player.stopRiding();
                    player.teleportTo(level, loco.getX(), loco.getY() + 1, loco.getZ() - 2, 0, 0);
                }
                case 905 -> player.startRiding(loco, true);
                case 1060 -> {
                    player.stopRiding();
                    look(level, new Vec3(-20, gy + 1.62, -24), new Vec3(-20, gy + 2.5, -60));
                    ItemStack gun = new ItemStack(ModItems.PISTOL.get());
                    gun.set(ModComponents.SILENCED.get(), true);
                    gun.set(ModComponents.AMMO.get(), 12);
                    var reg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
                    gun.enchant(reg.getHolderOrThrow(GunEnchants.FIREPOWER), 3);
                    gun.enchant(reg.getHolderOrThrow(GunEnchants.PIERCING), 2);
                    player.getInventory().selected = 0;
                    player.getInventory().setItem(0, gun);
                    System.out.println("[AUTOTEST] silenced gun dmg=" + ((GunItem) gun.getItem()).damage(gun) + " mag=" + ((GunItem) gun.getItem()).magazine(gun));
                }
                default -> {
                }
            }
        } catch (Exception e) {
            System.out.println("[AUTOTEST] server step " + t + " failed: " + e);
            e.printStackTrace();
        }
        // 소음기 총을 한 발 쏘면(탄 12 → 11) 드론을 손에 쥐여줌
        if (t > 1060 && !droneGiven) {
            ItemStack held = player.getMainHandItem();
            if ((held.getItem() instanceof GunItem && GunItem.ammo(held) < 12) || t > 1700) {
                player.getInventory().setItem(0, new ItemStack(ModItems.DRONE.get()));
                droneGiven = true;
                System.out.println("[AUTOTEST] drone given at t=" + t);
            }
        }
        if (t >= 1100 && t % 10 == 0) {
            long drones = level.getEntities(ModEntities.DRONE.get(), e -> true).size();
            System.out.println("[AUTOTEST] t=" + t + " drones=" + drones);
        }
        if (t % 40 == 0 && t >= 380 && t < 1100) {
            Entity v = player.getVehicle();
            System.out.println("[AUTOTEST] t=" + t + " vehicle=" + (v == null ? "none" : v.getType().toShortString())
                    + (v == null ? "" : String.format(" pos=%.2f,%.2f,%.2f", v.getX(), v.getY(), v.getZ()))
                    + (car != null ? " carFuel=" + car.getFuel() : "") + (heli != null ? " heliFuel=" + heli.getFuel() : "")
                    + (loco != null ? " locoFuel=" + loco.getFuel() : ""));
        }
    }

    private static void setup(ServerLevel level) {
        player.setGameMode(GameType.CREATIVE);
        level.setDayTime(6000);
        level.setWeatherParameters(12000, 0, false, false);
        gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, 0, 0);
        // 도로와 레일
        for (int x = -40; x <= 40; x++) {
            for (int z = -4; z <= 4; z++) level.setBlockAndUpdate(new BlockPos(x, gy - 1, z), Blocks.GRAY_CONCRETE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, gy - 1, 0), Blocks.YELLOW_CONCRETE.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, gy - 1, 10), Blocks.GRAVEL.defaultBlockState());
            level.setBlockAndUpdate(new BlockPos(x, gy, 10), Blocks.RAIL.defaultBlockState().setValue(RailBlock.SHAPE, RailShape.EAST_WEST));
        }
        car = ModEntities.CAR.get().create(level);
        car.moveTo(0, gy, -0.5, -60, 0);
        car.setFuel(car.maxFuel());
        level.addFreshEntity(car);
        heli = ModEntities.HELICOPTER.get().create(level);
        heli.moveTo(-14, gy, -12, 30, 0);
        heli.setFuel(heli.maxFuel());
        level.addFreshEntity(heli);
        loco = ModEntities.LOCOMOTIVE.get().create(level);
        loco.moveTo(6.5, gy + 0.0625, 10.5, 0, 0);
        loco.setFuel(loco.maxFuel());
        level.addFreshEntity(loco);
        System.out.println("[AUTOTEST] setup done ground=" + gy);
    }

    private static void look(ServerLevel level, Vec3 eye, Vec3 target) {
        Vec3 d = target.subtract(eye);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        player.teleportTo(level, eye.x, eye.y - 1.62, eye.z, yaw, pitch);
    }

    private AutoTest() {}
}
