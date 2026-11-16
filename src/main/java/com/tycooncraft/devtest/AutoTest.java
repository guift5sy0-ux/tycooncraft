package com.tycooncraft.devtest;

import com.tycooncraft.gun.GunEnchants;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.registry.ModComponents;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModBlocks;
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

    static com.tycooncraft.miner.AutoMinerEntity miner;
    private static net.minecraft.world.entity.LivingEntity fragHusk, flashHusk, villager;
    private static com.tycooncraft.entity.GrenadeEntity smoke;
    private static com.tycooncraft.mob.MutantSpider queen;

    private static String storageSummary() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < miner.storage.getContainerSize(); i++) {
            var st = miner.storage.getItem(i);
            if (!st.isEmpty()) b.append(st.getItem().toString().replace("minecraft:", "").replace("tycooncraft:", "")).append('x').append(st.getCount()).append(',');
        }
        return b.toString();
    }

    static com.tycooncraft.mob.DestroyerGolem golem;
    static com.tycooncraft.mob.Behemoth behemoth;
    static com.tycooncraft.mob.StormWyvern wyvern;
    static net.minecraft.world.entity.LivingEntity golemDummy, beastDummy, wyvernDummy;

    private static net.minecraft.world.entity.LivingEntity dummy(ServerLevel level, double x, double z) {
        var h = net.minecraft.world.entity.EntityType.HUSK.create(level);
        h.moveTo(x, gy, z, 0, 0);
        h.setNoAi(true);
        h.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        h.setHealth(1000);
        h.setPersistenceRequired();
        level.addFreshEntity(h);
        return h;
    }

    static net.minecraft.world.entity.boss.wither.WitherBoss wither;
    static net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon;

    private static void v14Setup(ServerLevel level) {
        wither = net.minecraft.world.entity.EntityType.WITHER.create(level);
        wither.moveTo(-15, gy + 4, 8, 0, 0);
        wither.makeInvulnerable();
        level.addFreshEntity(wither);
        for (double[] p : new double[][]{{-15, 18}, {-6, 8}, {-22, 2}}) dummyVillager(level, p[0], p[1]);
        dragon = net.minecraft.world.entity.EntityType.ENDER_DRAGON.create(level);
        dragon.moveTo(0, gy + 24, -20, 0, 0);
        level.addFreshEntity(dragon);
        for (double[] p : new double[][]{{0, -26}, {8, -18}}) dummyVillager(level, p[0], p[1]);
    }

    private static void dummyVillager(ServerLevel level, double x, double z) {
        var v = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
        v.moveTo(x, gy, z, 0, 0);
        v.setNoAi(true);
        v.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        v.setHealth(1000);
        v.addTag("tc_dummy");
        v.setPersistenceRequired();
        level.addFreshEntity(v);
    }

    private static void v13Setup(ServerLevel level) {
        golem = ModEntities.DESTROYER_GOLEM.get().create(level);
        golem.moveTo(34, gy, 40, 180, 0);
        level.addFreshEntity(golem);
        golemDummy = dummy(level, 34, 53);
        behemoth = ModEntities.BEHEMOTH.get().create(level);
        behemoth.moveTo(42, gy, -10, 0, 0);
        level.addFreshEntity(behemoth);
        behemoth.setHealth(280);                       // 2페이즈(운석) 확인용
        beastDummy = dummy(level, 42, 5);
        wyvern = ModEntities.STORM_WYVERN.get().create(level);
        wyvern.moveTo(10, gy + 14, 30, 0, 0);
        level.addFreshEntity(wyvern);
        wyvernDummy = dummy(level, 12, 36);
    }

    private static void v12Setup(ServerLevel level) {
        // 굴착기: 동쪽으로 돌벽 3×3×18 (중간에 광석들, 끝에 흑요석)
        for (int x = -24; x <= -6; x++) for (int z = 20; z <= 24; z++) for (int y = 0; y < 4; y++)
            level.setBlockAndUpdate(new BlockPos(x, gy + y, z), (x > -14 ? Blocks.DEEPSLATE : Blocks.STONE).defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(-20, gy + 1, 22), Blocks.DIAMOND_ORE.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(-18, gy + 2, 21), Blocks.IRON_ORE.defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(-16, gy, 23), ModBlocks.SULFUR_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(-10, gy + 1, 22), ModBlocks.DEEPSLATE_SULFUR_ORE.get().defaultBlockState());
        level.setBlockAndUpdate(new BlockPos(-5, gy + 1, 22), Blocks.OBSIDIAN.defaultBlockState());
        miner = ModEntities.AUTO_MINER.get().create(level);
        miner.moveTo(-26.5, gy, 22.5, -90, 0);
        miner.setFuel(8000);
        miner.setRunning(true);
        level.addFreshEntity(miner);
        // 수류탄: 파편(허스크 옆), 섬광(허스크를 향해), 연막
        fragHusk = net.minecraft.world.entity.EntityType.HUSK.create(level);
        fragHusk.moveTo(-10, gy, -30, 0, 0);
        level.addFreshEntity(fragHusk);
        flashHusk = net.minecraft.world.entity.EntityType.HUSK.create(level);
        flashHusk.moveTo(-18, gy, -30, 0, 0);   // 남쪽(+Z)을 봄
        level.addFreshEntity(flashHusk);
        var frag = new com.tycooncraft.entity.GrenadeEntity(ModEntities.GRENADE.get(), level);
        frag.setItem(new ItemStack(ModItems.FRAG_GRENADE.get()));
        frag.setPos(-10.5, gy + 1, -29);
        level.addFreshEntity(frag);
        var fl = new com.tycooncraft.entity.GrenadeEntity(ModEntities.GRENADE.get(), level);
        fl.setItem(new ItemStack(ModItems.FLASH_GRENADE.get()));
        fl.setPos(-18, gy + 1, -26);
        level.addFreshEntity(fl);
        smoke = new com.tycooncraft.entity.GrenadeEntity(ModEntities.GRENADE.get(), level);
        smoke.setItem(new ItemStack(ModItems.SMOKE_GRENADE.get()));
        smoke.setPos(-4, gy + 1, -30);
        smoke.setDeltaMovement(0.15, 0.2, 0);
        level.addFreshEntity(smoke);
        // 여왕거미 vs 주민
        queen = ModEntities.MUTANT_SPIDER.get().create(level);
        queen.moveTo(-20, gy, 44, 0, 0);
        level.addFreshEntity(queen);
        villager = net.minecraft.world.entity.EntityType.VILLAGER.create(level);
        villager.moveTo(-20, gy, 56, 180, 0);
        level.addFreshEntity(villager);
    }

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
        if (t >= 1100 && t % 40 == 0) {
            var v = player.getVehicle();
            System.out.println("[AUTOTEST] t=" + t + " ride=" + (v == null ? "none" : v.getType().toShortString())
                    + String.format(" pos=%.1f,%.1f,%.1f", player.getX(), player.getY(), player.getZ())
                    + " mobs=" + level.getEntities(ModEntities.GANGSTER.get(), e -> true).size() + "/" + level.getEntities(ModEntities.MERCENARY.get(), e -> true).size()
                    + "/" + level.getEntities(ModEntities.OUTLAW.get(), e -> true).size() + " boss=" + level.getEntities(ModEntities.CARTEL_BOSS.get(), e -> true).size()
                    + " rockets=" + level.getEntities(ModEntities.ROCKET.get(), e -> true).size()
                    + " chute=" + com.tycooncraft.gear.Parachute.open(player));
        }
        if (t >= 1100 && t % 10 == 0 && false) {
            long drones = level.getEntities(ModEntities.DRONE.get(), e -> true).size();
            System.out.println("[AUTOTEST] t=" + t + " drones=" + drones);
        }
        // v0.12: 자동 굴착기 · 수류탄 · 여왕거미
        if (t == 32) v12Setup(level);
        if (t == 36) v13Setup(level);
        if (t == 1500) v14Setup(level);
        if (t == 1700) {
            if (wither != null && wither.isAlive()) wither.setHealth(wither.getMaxHealth() * 0.45f);
            if (dragon != null && dragon.isAlive()) dragon.setHealth(dragon.getMaxHealth() * 0.45f);
        }
        if (t == 1600 || t == 2400) System.out.println("[AUTOTEST] vanilla t=" + t + " wither=" + (wither == null ? "none" : (int) wither.getHealth() + "/" + (int) wither.getMaxHealth())
                + " dragon=" + (dragon == null ? "none" : (int) dragon.getHealth() + "/" + (int) dragon.getMaxHealth())
                + " stats=" + java.util.Arrays.toString(com.tycooncraft.mob.VanillaBossBuffs.STATS));
        if (t > 40 && t % 20 == 0) {
            if (golem != null && golem.isAlive() && golemDummy.isAlive()) golem.setTarget(golemDummy);
            if (behemoth != null && behemoth.isAlive() && beastDummy.isAlive()) behemoth.setTarget(beastDummy);
            if (wyvern != null && wyvern.isAlive() && wyvernDummy.isAlive()) wyvern.setTarget(wyvernDummy);
        }
        if (t == 600 || t == 1400) {
            System.out.println("[AUTOTEST] bosses t=" + t + " golem shots=" + com.tycooncraft.mob.DestroyerGolem.shots + " rockets=" + com.tycooncraft.mob.DestroyerGolem.rockets
                    + " slams=" + com.tycooncraft.mob.DestroyerGolem.slams + " dummyHp=" + golemDummy.getHealth());
            System.out.println("[AUTOTEST] bosses t=" + t + " behemoth charges=" + com.tycooncraft.mob.Behemoth.charges + " stomps=" + com.tycooncraft.mob.Behemoth.stomps
                    + " breaths=" + com.tycooncraft.mob.Behemoth.breaths + " meteors=" + com.tycooncraft.mob.Behemoth.meteors + " dummyHp=" + beastDummy.getHealth());
            System.out.println("[AUTOTEST] bosses t=" + t + " wyvern volleys=" + com.tycooncraft.mob.StormWyvern.volleys + " dives=" + com.tycooncraft.mob.StormWyvern.dives
                    + " thunders=" + com.tycooncraft.mob.StormWyvern.thunders + " y=" + (wyvern == null ? 0 : (int) wyvern.getY()) + " dummyHp=" + wyvernDummy.getHealth());
        }
        if (t >= 40 && t <= 600 && t % 20 == 0 && queen != null && villager != null && villager.isAlive()) queen.setTarget(villager);
        if (t == 100 || t == 300 || t == 700 || t == 1400) {
            System.out.println("[AUTOTEST] miner t=" + t + (miner == null ? " none" : String.format(" x=%.1f fuel=%d running=%s mined=%d items=%s",
                    miner.getX(), miner.getFuel(), miner.isRunning(), miner.minedBlocks, storageSummary())));
        }
        if (t == 100) System.out.println("[AUTOTEST] grenades t=100 det=" + com.tycooncraft.entity.GrenadeEntity.detonations
                + " fragHusk=" + (fragHusk.isAlive() ? fragHusk.getHealth() : -1f) + " flashHusk=" + flashHusk.hasEffect(com.tycooncraft.registry.ModEffects.FLASHED)
                + " smoke=" + (smoke.isAlive() ? smoke.smokeTicks() : -1));
        if (t == 520) System.out.println("[AUTOTEST] grenades t=520 smokeGone=" + smoke.isRemoved());
        if (t == 600) System.out.println("[AUTOTEST] queen web=" + com.tycooncraft.mob.MutantSpider.webShots + " venom=" + com.tycooncraft.mob.MutantSpider.venomShots
                + " webs=" + com.tycooncraft.entity.SpiderShot.WEBS.size());
        // 침수 테스트: 물웅덩이에 자동차 → 3초 뒤 고장
        if (t == 30) {
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) for (int y = 0; y < 2; y++)
                level.setBlockAndUpdate(new net.minecraft.core.BlockPos(20 + x, gy + y, 26 + z), net.minecraft.world.level.block.Blocks.WATER.defaultBlockState());
            var car = ModEntities.CAR.get().create(level);
            car.moveTo(20, gy, 26, 0, 0);
            level.addFreshEntity(car);
        }
        if (t == 50 || t == 110) System.out.println("[AUTOTEST] flood t=" + t + " cars_in_pool="
                + level.getEntities(ModEntities.CAR.get(), new net.minecraft.world.phys.AABB(15, gy - 3, 21, 25, gy + 5, 31), e -> true).size());
        if (t >= 40 && t <= 400 && t % 20 == 0) {
            System.out.println("[AUTOTEST] turret t=" + t + " gunners=" + (level.getEntities(ModEntities.GANGSTER.get(), e -> true).size()
                    + level.getEntities(ModEntities.MERCENARY.get(), e -> true).size() + level.getEntities(ModEntities.OUTLAW.get(), e -> true).size()));
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
        // 신규: 전차·전투기·포탑·몹·장비
        var tank = ModEntities.TANK.get().create(level);
        tank.moveTo(10, gy, -20, 0, 0);
        tank.setFuel(tank.maxFuel());
        level.addFreshEntity(tank);
        var jet = ModEntities.FIGHTER_JET.get().create(level);
        jet.moveTo(-30, gy, 30, 180, 0);
        jet.setFuel(jet.maxFuel());
        level.addFreshEntity(jet);
        BlockPos tp = new BlockPos(40, gy, -36);
        level.setBlockAndUpdate(tp, ModBlocks.TURRET.get().defaultBlockState());
        if (level.getBlockEntity(tp) instanceof com.tycooncraft.turret.TurretBlockEntity te) {
            te.ammo().setItem(0, new ItemStack(ModItems.HEAVY_AMMO.get(), 64));
        }
        int k = 0;
        for (var type : java.util.List.of(ModEntities.GANGSTER.get(), ModEntities.MERCENARY.get(), ModEntities.OUTLAW.get())) {
            var g = type.create(level);
            g.moveTo(44 + k * 3, gy, -44, 90, 0);
            g.finalizeSpawn(level, level.getCurrentDifficultyAt(g.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
            level.addFreshEntity(g);
            k++;
        }
        var boss = ModEntities.CARTEL_BOSS.get().create(level);
        boss.moveTo(26, gy, -58, 0, 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(boss.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
        level.addFreshEntity(boss);
        var spider = ModEntities.MUTANT_SPIDER.get().create(level);
        spider.moveTo(14, gy, -60, 0, 0);
        level.addFreshEntity(spider);
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.LEGS, new ItemStack(ModItems.PARACHUTE.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(ModItems.VEST_3.get()));
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(ModItems.HELMET_2.get()));
        ItemStack vss = new ItemStack(ModItems.VSS.get());
        vss.set(ModComponents.AMMO.get(), 10);
        player.getInventory().setItem(1, vss);
        ItemStack dual = new ItemStack(ModItems.DUAL_PISTOLS.get());
        dual.set(ModComponents.AMMO.get(), 24);
        player.getInventory().setItem(2, dual);
        ItemStack rpg = new ItemStack(ModItems.RPG.get());
        rpg.set(ModComponents.AMMO.get(), 1);
        player.getInventory().setItem(3, rpg);
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
