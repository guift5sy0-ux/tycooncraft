package com.tycooncraft.devtest;

import com.tycooncraft.alliance.AllianceData;
import com.tycooncraft.alliance.Alliances;
import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.PriceTable;
import com.tycooncraft.mob.DestroyerGolem;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.gun.Ballistics;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.turret.TurretAI;
import com.tycooncraft.turret.TurretBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.UUID;

/** v0.18.5: 동맹·포탑, 디스트로이어 적대, 물가, 진압용 방패 */
public final class V185Test {
    static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000b0b0");
    static final UUID CAROL = UUID.fromString("00000000-0000-0000-0000-0000000ca201");
    static Zombie dummy;
    static float dummyHp;
    static int shots0;
    static DestroyerGolem golem;
    static BlockPos turret;
    static net.minecraft.world.entity.Mob gang, queen, warden;

    static String price(Item it) {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it).getPath() + "=" + PriceTable.basePrice(it);
    }

    static void setup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        // ── 동맹 ──
        BankData.get(sp.server).account(sp.getUUID()).balance += 50_000;
        var r = Alliances.create(sp, "테스트동맹");
        AllianceData d = AllianceData.get(sp.server);
        var a = d.of(sp.getUUID());
        if (a != null) {
            a.members.add(BOB);
            d.memberOf.put(BOB, a.name);
            d.names.put(BOB, "Bob");
        }
        var fpAlly = net.neoforged.neoforge.common.util.FakePlayerFactory.get(sl, new com.mojang.authlib.GameProfile(BOB, "Bob"));
        var fpFoe = net.neoforged.neoforge.common.util.FakePlayerFactory.get(sl, new com.mojang.authlib.GameProfile(CAROL, "Carol"));
        fpAlly.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        fpFoe.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        Zombie z = EntityType.ZOMBIE.create(sl);
        System.out.println("[AUTOTEST] v185 alliance created=" + r.ok() + " friendlyBob=" + Alliances.friendly(sp.server, sp.getUUID(), BOB)
                + " friendlyCarol=" + Alliances.friendly(sp.server, sp.getUUID(), CAROL)
                + " turretAlly=" + TurretAI.targetable(sl, fpAlly, sp.getUUID(), true) + " turretFoe=" + TurretAI.targetable(sl, fpFoe, sp.getUUID(), true)
                + " turretFoeMonstersOnly=" + TurretAI.targetable(sl, fpFoe, sp.getUUID(), false) + " turretZombie=" + TurretAI.targetable(sl, z, sp.getUUID(), true)
                + " ownerlessFoe=" + TurretAI.targetable(sl, fpFoe, null, true) + " hitAlly=" + TurretAI.canHit(sl, fpAlly, sp.getUUID()));

        // ── 포탑 연사 측정 ──
        BlockPos base = BlockPos.containing(-40, sp.getY(), -90);
        turret = base;
        sl.setBlockAndUpdate(base, ModBlocks.TURRET.get().defaultBlockState());
        if (sl.getBlockEntity(base) instanceof TurretBlockEntity t) {
            t.setOwner(sp.getUUID());
            t.ammo().setItem(0, new ItemStack(ModItems.LIGHT_AMMO.get(), 64));
        }
        dummy = EntityType.ZOMBIE.create(sl);
        dummy.moveTo(base.getX() + 8.5, base.getY(), base.getZ() + 0.5, 90, 0);
        dummy.setNoAi(true);
        dummy.setPersistenceRequired();
        dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        dummy.setHealth(1000);
        dummy.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        sl.addFreshEntity(dummy);
        dummyHp = dummy.getHealth();
        shots0 = TurretBlockEntity.SHOTS;

        // ── 디스트로이어 vs 몹 ──
        BlockPos gp = base.offset(0, 0, 30);
        golem = ModEntities.DESTROYER_GOLEM.get().spawn(sl, gp, MobSpawnType.MOB_SUMMONED);
        var cow = EntityType.COW.spawn(sl, gp.offset(5, 0, 0), MobSpawnType.MOB_SUMMONED);
        if (cow != null) cow.setNoAi(true);
        gang = ModEntities.GANGSTER.get().spawn(sl, gp.offset(-12, 0, 0), MobSpawnType.MOB_SUMMONED);
        queen = ModEntities.MUTANT_SPIDER.get().spawn(sl, gp.offset(0, 0, 12), MobSpawnType.MOB_SUMMONED);
        warden = EntityType.WARDEN.spawn(sl, gp.offset(10, 0, 10), MobSpawnType.MOB_SUMMONED);

        // ── 물가 ──
        StringBuilder b = new StringBuilder();
        for (Item it : new Item[]{Items.IRON_INGOT, ModItems.IRON_PLATE.get(), ModItems.STEEL_INGOT.get(), ModItems.STEEL_PLATE.get(), ModItems.STEEL_ROD.get(),
                ModItems.IRON_GEAR.get(), ModItems.CIRCUIT.get(), ModItems.MECHANISM.get(), ModItems.ADVANCED_CIRCUIT.get(), ModItems.GUN_RECEIVER.get(),
                ModItems.GUN_BARREL.get(), ModItems.PISTOL.get(), ModItems.RIFLE.get(), ModItems.SNIPER.get(), ModItems.LIGHT_AMMO.get(), ModItems.ENGINE.get(),
                ModItems.CAR.get(), ModItems.TANK.get(), ModItems.TURRET.get(), ModItems.POLICE_SHIELD.get(), ModItems.GUN_WORKBENCH.get(),
                ModItems.VEST_3.get(), ModItems.ELECTRIC_MOTOR.get(), ModItems.INDUSTRIAL_ROBOT_ARM.get()})
            b.append(price(it)).append(' ');
        int missing = 0;
        StringBuilder miss = new StringBuilder();
        for (var it : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(it);
            if (!id.getNamespace().equals("tycooncraft") || it instanceof com.tycooncraft.economy.CoinItem || it instanceof net.minecraft.world.item.SpawnEggItem) continue;
            if (PriceTable.basePrice(it) <= 0) { missing++; if (miss.length() < 300) miss.append(id.getPath()).append(','); }
        }
        double manual = 100 * PriceTable.basePrice(Items.IRON_INGOT) + 150 * PriceTable.basePrice(Items.COAL) + 40 * PriceTable.basePrice(Items.COPPER_INGOT)
                + 20 * PriceTable.basePrice(Items.GOLD_INGOT) + 3 * PriceTable.basePrice(Items.DIAMOND) + 10 * PriceTable.basePrice(Items.REDSTONE) + 10 * PriceTable.basePrice(Items.LAPIS_LAZULI);
        System.out.println(String.format("[AUTOTEST] v186 manualHour≈%.0f (100철·150석탄·40구리·20금·3다이아·10레드·10청금) ironIngot=%.1f→plate=%.1f→gear=%.1f",
                manual, PriceTable.basePrice(Items.IRON_INGOT), PriceTable.basePrice(ModItems.IRON_PLATE.get()), PriceTable.basePrice(ModItems.IRON_GEAR.get())));
        StringBuilder tg = new StringBuilder();
        for (String k : new String[]{"gear", "machines"}) {
            for (MerchantOffer o : com.tycooncraft.world.StructureBuilder.traderOffers("trader.tycooncraft." + k)) {
                Item it = o.getResult().getItem();
                if (it == ModItems.PISTOL.get() || it == ModItems.RIFLE.get() || it == ModItems.SMG.get() || it == ModItems.POLICE_SHIELD.get()
                        || it == ModItems.machineItem(com.tycooncraft.machine.MachineType.ASSEMBLER) || it == ModItems.machineItem(com.tycooncraft.machine.MachineType.PRESS)) {
                    var one = com.tycooncraft.world.StructureBuilder.stockOf(k, o);
                    tg.append(o.getResult().getHoverName().getString()).append('=').append(one).append("원 ");
                }
            }
        }
        System.out.println("[AUTOTEST] v186 traderPrices " + tg);
        System.out.println("[AUTOTEST] v185 prices recipes=" + PriceTable.SOURCE_RECIPES + " " + b + "| noPrice=" + missing + " " + miss);
        StringBuilder tr = new StringBuilder();
        var st = com.tycooncraft.world.StructureBuilder.traderStock("trader.tycooncraft.gear", sl.random);
        for (int i = 0; i < Math.min(5, st.size()); i++) {
            MerchantOffer o = st.get(i);
            tr.append(o.getResult().getHoverName().getString()).append(" ← ").append(o.getItemCostA().count()).append('x')
                    .append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(o.getItemCostA().item().value()).getPath())
                    .append(o.getItemCostB().map(c -> "+" + c.count() + "x" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(c.item().value()).getPath()).orElse("")).append("; ");
        }
        System.out.println("[AUTOTEST] v185 trader gear: " + tr);

        // ── 진압용 방패를 손에 ──
        sp.getInventory().clearContent();
        sp.getInventory().selected = 0;
        sp.getInventory().setItem(0, new ItemStack(ModItems.POLICE_SHIELD.get()));
        sp.teleportTo(sl, base.getX() - 6.5, base.getY(), base.getZ() + 6.5, -90f, 5f);
    }

    static void check(ServerPlayer sp) {
        int shots = TurretBlockEntity.SHOTS - shots0;
        System.out.println(String.format("[AUTOTEST] v185 turret shots/60t=%d dmg=%.1f (cooldown=%d lock=%d spread=%.2f)",
                shots, dummyHp - dummy.getHealth(), TurretAI.COOLDOWN, TurretAI.LOCK_DELAY, TurretAI.SPREAD));
        System.out.println("[AUTOTEST] v185 destroyer target=" + (golem == null || golem.getTarget() == null ? "none" : golem.getTarget().getType().toShortString()));
        System.out.println("[AUTOTEST] v186 vsDestroyer gangTarget=" + (gang == null || gang.getTarget() == null ? "none" : gang.getTarget().getType().toShortString())
                + " queenTarget=" + (queen == null || queen.getTarget() == null ? "none" : queen.getTarget().getType().toShortString())
                + " wardenAngry=" + (warden instanceof net.minecraft.world.entity.monster.warden.Warden w && golem != null ? w.getAngerManagement().getActiveAnger(golem) : -1));
        if (warden != null) warden.discard();
        if (gang != null) gang.discard();
        if (queen != null) queen.discard();
        dummy.discard();
        if (golem != null) golem.discard();
        sp.serverLevel().setBlockAndUpdate(turret, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    /** v0.18.7: 공장 키트 */
    static void kit(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.getInventory().clearContent();
        var k = com.tycooncraft.event.StarterKit.kit();
        StringBuilder names = new StringBuilder();
        for (ItemStack s : k) names.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).getPath()).append(',');
        sp.teleportTo(sl, -60.5, sp.getY(), -120.5, 180f, 10f);   // 북쪽을 봄
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.FACTORY_KIT.get()));
        BlockPos o = com.tycooncraft.item.FactoryKitItem.originFor(sp);
        boolean built = com.tycooncraft.item.FactoryKitItem.build(sp);
        boolean crusher = sl.getBlockState(o.offset(3, 0, 5)).getBlock() == ModBlocks.machine(com.tycooncraft.machine.MachineType.CRUSHER);
        boolean sell = sl.getBlockState(o.offset(9, 0, 5)).is(ModBlocks.SELL_STATION.get());
        boolean again = com.tycooncraft.item.FactoryKitItem.build(sp);   // 같은 자리 다시 → 기계가 있어 거절
        System.out.println("[AUTOTEST] v187 starterKit=" + names + " built=" + built + " crusher=" + crusher + " sell=" + sell + " rebuildRefused=" + !again
                + " origin=" + o.toShortString() + " playerZ=" + sp.getZ());
        // 원석을 첫 벨트에 → 잠시 뒤 판매소까지 가는지
        var raw = new net.minecraft.world.entity.item.ItemEntity(sl, o.getX() + 2.5, o.getY() + 0.8, o.getZ() + 5.5, new ItemStack(Items.RAW_IRON, 8));
        sl.addFreshEntity(raw);
        kitOrigin = o;
        sp.teleportTo(sl, o.getX() + 7.5, o.getY(), o.getZ() + 1.5, 160f, 25f);
    }

    static BlockPos kitOrigin;

    static void kitCheck(ServerPlayer sp) {
        var sl = sp.serverLevel();
        long bal = 0;
        if (sl.getBlockEntity(kitOrigin.offset(9, 0, 5)) instanceof com.tycooncraft.blockentity.SellStationBlockEntity st) bal = st.balance();
        System.out.println("[AUTOTEST] v187 factoryLine sellStationBalance=" + bal);
        sp.teleportTo(sl, kitOrigin.getX() + 7.5, kitOrigin.getY() + 1, kitOrigin.getZ() - 8.5, 0f, 12f);
    }

    static Zombie bashTarget;
    static float bashHp;
    static net.minecraft.world.phys.Vec3 bashStart;

    /** v0.18.8: 진압용 방패 돌진 */
    static void bash(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        sp.teleportTo(sl, -90.5, sp.getY(), -150.5, 180f, 0f);   // 북쪽을 봄
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.POLICE_SHIELD.get()));
        double atk = 1;
        var mods = new ItemStack(ModItems.POLICE_SHIELD.get()).get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        if (mods != null) for (var e : mods.modifiers())
            if (e.attribute().is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) atk += e.modifier().amount();
        bashTarget = EntityType.ZOMBIE.create(sl);
        bashTarget.moveTo(-90.5, sp.getY(), -153.5, 0, 0);
        bashTarget.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        sl.addFreshEntity(bashTarget);
        bashHp = bashTarget.getHealth();
        bashStart = bashTarget.position();
        com.tycooncraft.gear.PoliceShieldItem.bash(sp);
        System.out.println("[AUTOTEST] v188 shieldAttack=" + atk + " cooldown=" + sp.getCooldowns().isOnCooldown(ModItems.POLICE_SHIELD.get()));
    }

    static void bashCheck(ServerPlayer sp) {
        System.out.println(String.format("[AUTOTEST] v188 bash hits=%d dmg=%.1f flung=%.2f stunned=%s slow=%s",
                com.tycooncraft.gear.PoliceShieldItem.BASH_HITS, bashHp - bashTarget.getHealth(), bashTarget.position().subtract(bashStart).horizontalDistance(),
                com.tycooncraft.gear.PoliceShieldItem.stunned(bashTarget), bashTarget.hasEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN)));
        bashTarget.discard();
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    }

    static net.minecraft.world.entity.boss.enderdragon.EnderDragon restDragon;
    static net.minecraft.world.entity.npc.Villager restBait;
    static int rush0;

    /** 드래곤: 큰 기술 뒤 숨 고르기 */
    static void dragonRest(ServerPlayer sp) {
        var sl = sp.serverLevel();
        double gy = sp.getY();
        double x = sp.getX(), z = sp.getZ();
        var v = EntityType.VILLAGER.create(sl);
        v.moveTo(x, gy, z - 50, 0, 0);
        v.setNoAi(true);
        v.setInvulnerable(true);
        v.addTag("tc_dummy");
        sl.addFreshEntity(v);
        restBait = v;
        restDragon = EntityType.ENDER_DRAGON.create(sl);
        restDragon.moveTo(x, gy + 25, z - 20, 0, 0);
        sl.addFreshEntity(restDragon);
        restDragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
        com.tycooncraft.mob.VanillaBossBuffs.forceMove(restDragon, 1);
        rush0 = com.tycooncraft.mob.VanillaBossBuffs.DSTATS[0];
    }

    static void keepAirborne(ServerPlayer sp) {
        if (restDragon == null || !restDragon.isAlive()) return;
        var ph = restDragon.getPhaseManager().getCurrentPhase().getPhase();
        if (ph != net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN)
            restDragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
    }

    static void dragonRestCheck(ServerPlayer sp) {
        double ground = sp.getY();
        System.out.println(String.format("[AUTOTEST] v188 dragonRest rests=%d heightAboveGround=%.1f rushes=%d phase=%s baitAlive=%s",
                com.tycooncraft.mob.VanillaBossBuffs.REST_COUNT, restDragon.getY() - ground, com.tycooncraft.mob.VanillaBossBuffs.DSTATS[0] - rush0,
                restDragon.getPhaseManager().getCurrentPhase().getPhase(), restBait.isAlive()));
        restDragon.discard();
        restBait.discard();
    }

    public static BlockPos cctvScreen, cctvCamA;

    /** CCTV 머리가 보는 방향을 따라 돌았는지 + 바깥에서 본 모습 */
    static void cctvHeadCheck(ServerPlayer sp) {
        var sl = sp.serverLevel();
        if (sl.getBlockEntity(cctvCamA) instanceof com.tycooncraft.cctv.CctvBlockEntity be)
            System.out.printf("[AUTOTEST] v192 cctvHead yaw=%.1f pitch=%.1f%n", be.yaw, be.pitch);
        // 카메라 앞쪽(북쪽) 아래에서 올려다봄
        sp.teleportTo(sl, cctvCamA.getX() + 0.5 - 1.5, cctvCamA.getY() - 1.6, cctvCamA.getZ() - 4.5, -18f, -14f);
    }
    public static java.util.List<BlockPos> cctvLinks = java.util.List.of();

    /** v0.18.9: CCTV 2대 + 없는 자리 1 → 스크린에 연결 */
    static void cctv(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos base = BlockPos.containing(-60, sp.getY(), -120);   // 공장 키트로 세운 공장 근처
        BlockPos camA = cctvCamA = kitOrigin != null ? kitOrigin.offset(7, 4, 9) : base.offset(0, 3, 0);    // 공장 안 북쪽 벽 → 남쪽(라인) 을 봄
        BlockPos camB = kitOrigin != null ? kitOrigin.offset(2, 2, -4) : base.offset(6, 1, 0);   // 공장 밖 남쪽 → 공장 정면
        sl.setBlockAndUpdate(camA, ModBlocks.CCTV.get().defaultBlockState().setValue(com.tycooncraft.cctv.CctvBlock.FACING, net.minecraft.core.Direction.NORTH));
        sl.setBlockAndUpdate(camB, ModBlocks.CCTV.get().defaultBlockState().setValue(com.tycooncraft.cctv.CctvBlock.FACING, net.minecraft.core.Direction.SOUTH));
        cctvScreen = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, kitOrigin != null ? kitOrigin.offset(7, 1, -6) : base.offset(3, 0, 6));
        sl.setBlockAndUpdate(cctvScreen, ModBlocks.CCTV_SCREEN.get().defaultBlockState().setValue(com.tycooncraft.cctv.ScreenBlock.FACING, net.minecraft.core.Direction.NORTH));
        if (sl.getBlockEntity(cctvScreen) instanceof com.tycooncraft.cctv.ScreenBlockEntity be) {
            be.add(camA);
            be.add(camB);
            be.add(camA.offset(0, 30, 0));   // 없는 카메라 → 신호 없음
            cctvLinks = be.links();
        }
        sp.teleportTo(sl, cctvScreen.getX() + 0.5, cctvScreen.getY(), cctvScreen.getZ() - 2.5, 0f, 15f);
        System.out.println("[AUTOTEST] v189 cctv links=" + cctvLinks.size() + " camA=" + sl.getBlockState(camA).getBlock().getClass().getSimpleName());
    }

    /** v0.18.9: 차·탱크 탑승 중 피해 → 차체가 대신 맞음 */
    static void vehicleArmor(ServerPlayer sp) {
        var sl = sp.serverLevel();
        Zombie z = EntityType.ZOMBIE.create(sl);
        z.moveTo(sp.getX() + 3, sp.getY(), sp.getZ(), 0, 0); sl.addFreshEntity(z);
        StringBuilder out = new StringBuilder("[AUTOTEST] v189 vehicleArmor mode=" + sp.gameMode.getGameModeForPlayer());
        var prevMode = sp.gameMode.getGameModeForPlayer();
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        for (var type : java.util.List.of(ModEntities.CAR.get(), ModEntities.TANK.get())) {
            var v = type.create(sl);
            v.moveTo(sp.getX(), sp.getY(), sp.getZ(), 0, 0); sl.addFreshEntity(v);
            sp.startRiding(v, true);
            sp.setHealth(20f);
            float[] gun = com.tycooncraft.vehicle.VehicleArmor.probe(sp, sl.damageSources().mobAttack(z), 6f);
            float[] arrow = com.tycooncraft.vehicle.VehicleArmor.probe(sp, sl.damageSources().arrow(null, z), 5f);
            float[] fall = com.tycooncraft.vehicle.VehicleArmor.probe(sp, sl.damageSources().inFire(), 1f);
            out.append(String.format(" %s: hit rider=%.1f body=%.1f arrow rider=%.1f body=%.1f fire rider=%.1f", type.toShortString(), gun[0], gun[1], arrow[0], arrow[1], fall[0]));
            sp.stopRiding(); v.discard();
        }
        z.discard();
        sp.setGameMode(prevMode);
        sp.setHealth(20f);
        System.out.println(out);
    }

    // ── v0.19.0 프로토 디스트로이어 ──
    static com.tycooncraft.mech.ProtoDestroyer proto;
    static Zombie protoTarget;
    static float protoTargetHp0;

    static void protoSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos p = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(40, 0, -200));
        proto = com.tycooncraft.mech.ProtoDestroyerItem.spawn(sl, p, sp, new ItemStack(ModItems.PROTO_DESTROYER.get()), 0f);
        sp.teleportTo(sl, p.getX() + 0.5, p.getY(), p.getZ() - 2.5, 0f, 0f);
        var r = proto.interact(sp, InteractionHand.MAIN_HAND);
        sp.setYRot(0); sp.setXRot(8);
        for (var e : sl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, proto.getBoundingBox().inflate(40), e -> e != proto)) e.discard();   // 시야 가리는 슬라임 등 정리
        protoTarget = EntityType.ZOMBIE.create(sl);
        protoTarget.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 12.5, 180, 0);
        protoTarget.setNoAi(true);
        protoTarget.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));   // 햇빛에 타지 않게
        protoTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
        protoTarget.setHealth(400);
        sl.addFreshEntity(protoTarget);
        protoTargetHp0 = 400;
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);   // 탄약 소모 확인
        sp.getInventory().clearContent();
        sp.getInventory().add(new ItemStack(ModItems.HEAVY_AMMO.get(), 64));
        sp.getInventory().add(new ItemStack(ModItems.ROCKET.get(), 2));   // 2발만 → 3연발 중 2발 발사

        System.out.println("[AUTOTEST] v190 proto spawned=" + (proto != null) + " mount=" + r + " riding=" + (sp.getVehicle() == proto)
                + " hp=" + proto.getHealth() + " recipe=" + sl.getRecipeManager().byKey(com.tycooncraft.TycoonCraft.id("proto_destroyer")).isPresent());
    }

    static void protoFire(ServerPlayer sp) {
        if (proto == null) return;
        Vec3 c = protoTarget.getBoundingBox().getCenter().subtract(sp.getEyePosition());
        sp.setYRot((float) (Math.atan2(-c.x, c.z) * 180 / Math.PI));
        sp.setXRot((float) (-Math.atan2(c.y, c.horizontalDistance()) * 180 / Math.PI));
        proto.action(sp, com.tycooncraft.mech.ProtoDestroyer.ACT_FIRE);
    }

    static float hpAfterFire, heatAfterFire;
    static int shotsAfterFire;

    static void protoRockets(ServerPlayer sp) {
        hpAfterFire = protoTarget.getHealth();
        heatAfterFire = proto.heat();
        shotsAfterFire = com.tycooncraft.mech.ProtoDestroyer.shots;
        protoTarget.setHealth(400);
        protoFire(sp);   // 조준만 갱신 (쿨다운 중)
        proto.action(sp, com.tycooncraft.mech.ProtoDestroyer.ACT_ROCKETS);

    }

    static float hpAfterRockets;

    static void protoSlam(ServerPlayer sp) {   // 높이 띄웠다 떨어뜨려 착지 충격파
        hpAfterRockets = protoTarget.isAlive() ? protoTarget.getHealth() : 0;
        protoTarget.setHealth(400);
        Vec3 f = proto.local(0, 0, 3.2);
        protoTarget.teleportTo(f.x, proto.getY(), f.z);
        System.out.println("[AUTOTEST] v191 jumpStart y=" + proto.getY() + " ground=" + proto.onGround());
    }

    static void protoCheck(ServerPlayer sp) {
        float hpAfterSlam = protoTarget.getHealth();
        float[] armor = new float[2];
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        float hp = sp.getHealth(), mh = proto.getHealth();
        sp.invulnerableTime = 0;
        sp.hurt(sp.serverLevel().damageSources().mobAttack(protoTarget), 8f);
        armor[0] = hp - sp.getHealth();
        armor[1] = mh - proto.getHealth();
        int ammoLeft = com.tycooncraft.mech.ProtoDestroyer.count(sp, ModItems.HEAVY_AMMO.get()), rocketLeft = com.tycooncraft.mech.ProtoDestroyer.count(sp, ModItems.ROCKET.get());
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        System.out.printf("[AUTOTEST] v191 ammo heavyLeft=%d/64 rocketsLeft=%d/2 dry=%d%n", ammoLeft, rocketLeft, com.tycooncraft.mech.ProtoDestroyer.dryFires);
        System.out.printf("[AUTOTEST] v190 fire shots=%d targetDmg=%.1f heat=%.0f | rockets=%d targetDmg=%.1f | slams=%d targetDmg=%.1f | riderHit rider=%.1f hull=%.1f%n",
                shotsAfterFire, 400 - hpAfterFire, heatAfterFire, com.tycooncraft.mech.ProtoDestroyer.rockets, 400 - hpAfterRockets,
                com.tycooncraft.mech.ProtoDestroyer.slams, 400 - hpAfterSlam, armor[0], armor[1]);
    }

    static void protoShowcase(ServerPlayer sp) {
        sp.stopRiding();
        protoTarget.discard();
        Vec3 f = proto.local(0, 0, 7.5);
        sp.teleportTo(sp.serverLevel(), f.x, proto.getY() + 0.5, f.z, proto.getYRot() + 180 - 25, 5f);
        Vec3 g = proto.local(-2.5, 0, 7);
        sp.teleportTo(sp.serverLevel(), g.x, proto.getY() + 0.5, g.z, proto.getYRot() + 180 + 18, 8f);
    }

    static void protoShowcase2(ServerPlayer sp) {
        Vec3 g = proto.local(3, 0, -6.5);
        sp.teleportTo(sp.serverLevel(), g.x, proto.getY() + 1.5, g.z, proto.getYRot() - 25, 12f);
        // 회수 테스트
    }

    static com.tycooncraft.vehicle.LocomotiveEntity loco;
    static double locoZ0;
    static int locoT0;

    /** v0.19.0: 기관차 최고 속력 1.8배 확인 — 직선 선로에서 계속 가속 */
    static void locoSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos g = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(60, 0, -200));
        int y = g.getY();
        for (int z = -200; z < 100; z++) {
            sl.setBlock(new BlockPos(60, y - 1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
            sl.setBlock(new BlockPos(60, y, z), net.minecraft.world.level.block.Blocks.RAIL.defaultBlockState(), 2);
            sl.setBlock(new BlockPos(60, y + 1, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            sl.setBlock(new BlockPos(60, y + 2, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        loco = ModEntities.LOCOMOTIVE.get().create(sl);
        loco.moveTo(60.5, y + 0.0625, -198.5, 0, 0);
        loco.setFuel(loco.maxFuel());
        sl.addFreshEntity(loco);
        sp.teleportTo(sl, 60.5, y + 1, -198.5, 0, 0);
        sp.startRiding(loco, true);
        locoT0 = 0;
    }

    static int locoPushes;

    static void locoPush(ServerPlayer sp) {
        if (loco == null) return;
        loco.setDeltaMovement(0, 0, 4.0);   // 상한에 걸리도록 크게
        if (++locoPushes == 4) { locoZ0 = loco.getZ(); locoT0 = sp.server.getTickCount(); }   // 앞의 몇 번은 가속·청크 로딩
    }

    static void locoCheck(ServerPlayer sp) {
        double v = (loco.getZ() - locoZ0) / Math.max(1, sp.server.getTickCount() - locoT0);
        System.out.printf("[AUTOTEST] v190 loco avgSpeed=%.2f b/t (%.0f km/h) cap=%.2f riding=%s z=%.1f%n", v, v * 20 * 3.6, loco.getMaxSpeedWithRail(), sp.getVehicle() == loco, loco.getZ());
        sp.stopRiding();
        loco.discard();
    }

    // ── v0.19.4 포탑 ──
    static BlockPos[] turrets = new BlockPos[4];
    static String[] turretWho = {"gangster", "destroyer", "spiderQueen", "explosion"};

    static void turretSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        for (int i = 0; i < 4; i++) {
            BlockPos g = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, BlockPos.containing(140, 0, -300 + new int[]{0, 40, 110, 150}[i]));
            turrets[i] = g;
            sl.setBlockAndUpdate(g, ModBlocks.TURRET.get().defaultBlockState());
        }
        var gang = ModEntities.GANGSTER.get().create(sl);
        gang.moveTo(turrets[0].getX() + 10.5, turrets[0].getY(), turrets[0].getZ() + 0.5, 90, 0);
        gang.setPersistenceRequired();
        sl.addFreshEntity(gang);
        var golem = ModEntities.DESTROYER_GOLEM.get().create(sl);
        golem.moveTo(turrets[1].getX() + 10.5, turrets[1].getY(), turrets[1].getZ() + 0.5, 90, 0);
        golem.setPersistenceRequired();
        sl.addFreshEntity(golem);
        var queen = ModEntities.MUTANT_SPIDER.get().create(sl);
        queen.moveTo(turrets[2].getX() + 10.5, turrets[2].getY(), turrets[2].getZ() + 0.5, 90, 0);
        queen.setPersistenceRequired();
        sl.addFreshEntity(queen);
        // 플레이어(크리에이티브)는 갱단원·포탑을 옆에서 봄
        sp.teleportTo(sl, turrets[0].getX() + 5.5, turrets[0].getY() + 1, turrets[0].getZ() - 7.5, 20f, 10f);
        System.out.printf("[AUTOTEST] v194 turretDmg light=%.2f heavy=%.2f protoMaxHeat=%d tankShell power=5 direct=%.0f%n",
                com.tycooncraft.turret.TurretAI.DMG_LIGHT, com.tycooncraft.turret.TurretAI.DMG_HEAVY, com.tycooncraft.mech.ProtoDestroyer.MAX_HEAT,
                com.tycooncraft.entity.RocketEntity.SHELL_DIRECT);
    }

    static String turretState() {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            b.append(' ').append(turretWho[i]).append('=');
            if (sp0 != null && sp0.serverLevel().getBlockEntity(turrets[i]) instanceof com.tycooncraft.turret.TurretBlockEntity t) b.append(String.format("%.0f", t.hp));
            else b.append("DESTROYED");
        }
        return b.toString();
    }

    static ServerPlayer sp0;

    static void turretMid(ServerPlayer sp) {
        sp0 = sp;
        // 폭발: TNT 급(반경 4) 이 2칸 옆에서 터짐 → 부서지지 않고 체력만 깎임
        sp.serverLevel().explode(null, turrets[3].getX() + 2.5, turrets[3].getY() + 0.5, turrets[3].getZ() + 0.5, 4f, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        System.out.println("[AUTOTEST] v194 turretMid" + turretState());
        sp.teleportTo(sp.serverLevel(), turrets[1].getX() + 5.5, turrets[1].getY() + 2, turrets[1].getZ() - 8.5, 20f, 15f);   // 디스트로이어 쪽
    }

    static void turretCheck(ServerPlayer sp) {
        sp0 = sp;
        // 수리: 폭발 맞은 포탑을 강철 주괴로
        float before = -1, after = -1;
        if (sp.serverLevel().getBlockEntity(turrets[3]) instanceof com.tycooncraft.turret.TurretBlockEntity t) { before = t.hp; t.repair(40); after = t.hp; }
        System.out.println("[AUTOTEST] v194 turretEnd" + turretState() + " destroyed=" + com.tycooncraft.turret.TurretBlockEntity.DESTROYED
                + " rangedShots=" + com.tycooncraft.turret.TurretHuntGoal.RANGED_SHOTS + " meleeHits=" + com.tycooncraft.turret.TurretHuntGoal.MELEE_HITS
                + String.format(" repair %.0f→%.0f", before, after));
        for (var e : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, new net.minecraft.world.phys.AABB(turrets[0]).inflate(220, 40, 40))) e.discard();
    }

    /** v0.19.5: RT-20 피해 70, 전차·디스트로이어 1.1배 + 방어구 방어력 */
    static void rt20Check(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos base = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, BlockPos.containing(sp.getX() + 10, 0, sp.getZ() + 20));
        StringBuilder out = new StringBuilder("[AUTOTEST] v195 rt20");
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RT20.get()));   // 총을 든 상태 (빈손이면 차량은 근접 판정)
        net.minecraft.world.entity.Entity[] ts = {EntityType.ZOMBIE.create(sl), ModEntities.DESTROYER_GOLEM.get().create(sl), ModEntities.TANK.get().create(sl)};
        String[] names = {"zombie", "destroyer", "tank"};
        for (int i = 0; i < ts.length; i++) {
            var t = ts[i];
            t.moveTo(base.getX() + i * 12 + 0.5, base.getY(), base.getZ() + 0.5, 0, 0);
            if (t instanceof net.minecraft.world.entity.Mob m) { m.setNoAi(true); m.setPersistenceRequired(); }
            sl.addFreshEntity(t);
            for (boolean anti : new boolean[]{false, true}) {
                float before = t instanceof net.minecraft.world.entity.LivingEntity le ? le.getHealth() : ((com.tycooncraft.vehicle.VehicleEntity) t).getDamage();
                if (t instanceof net.minecraft.world.entity.LivingEntity le) { le.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000); le.setHealth(1000); before = 1000; }
                t.invulnerableTime = 0;
                Vec3 c = t.getBoundingBox().getCenter();
                Vec3 from = c.add(0, 0, -4);
                Ballistics.nextAntiMaterial = anti;
                Ballistics.shoot(sl, sp, from, new Vec3(0, 0, 1), 10, 70f, 0, 1f, false, false, sl.damageSources().playerAttack(sp), e -> e == t);
                float after = t instanceof net.minecraft.world.entity.LivingEntity le ? le.getHealth() : ((com.tycooncraft.vehicle.VehicleEntity) t).getDamage();
                out.append(String.format(" %s%s=%.1f", names[i], anti ? "(rt20)" : "", Math.abs(before - after)));
            }
            t.discard();
        }
        var gun = (com.tycooncraft.gun.GunItem) ModItems.RT20.get();
        var rec = com.tycooncraft.gun.GunCrafting.all().stream().filter(r -> r.result().is(ModItems.RT20.get())).findFirst();
        out.append(" recipe=").append(rec.map(r -> r.needs().toString()).orElse("NONE"));
        out.append(" dmg=").append(gun.stats().damage()).append(" ammo=").append(gun.stats().ammo().get() == ModItems.HISPANO_AMMO.get());
        System.out.println(out);
        StringBuilder a = new StringBuilder("[AUTOTEST] v195 armor");
        for (var mat : java.util.List.of(com.tycooncraft.registry.ModArmorMaterials.TACTICAL_LIGHT, com.tycooncraft.registry.ModArmorMaterials.TACTICAL_MEDIUM,
                com.tycooncraft.registry.ModArmorMaterials.TACTICAL_HEAVY, com.tycooncraft.registry.ModArmorMaterials.JUGGERNAUT)) {
            var m = mat.get();
            int sum = 0;
            for (var ty : new net.minecraft.world.item.ArmorItem.Type[]{net.minecraft.world.item.ArmorItem.Type.HELMET, net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,
                    net.minecraft.world.item.ArmorItem.Type.LEGGINGS, net.minecraft.world.item.ArmorItem.Type.BOOTS}) sum += m.getDefense(ty);
            a.append(" ").append(mat.getId().getPath()).append("=").append(sum).append("/t").append(m.toughness());
        }
        a.append(" | iron=15 diamond=20/t2 netherite=20/t3");
        System.out.println(a);
    }

    static Vec3 kick0;
    static BlockPos kickBase;

    static void rt20Kick(ServerPlayer sp) {
        var sl = sp.serverLevel();
        if (kickBase == null) kickBase = sp.blockPosition().offset(0, 12, 0);
        int y = kickBase.getY(), bx = kickBase.getX(), bz = kickBase.getZ();
        for (int x = -3; x <= 3; x++) for (int z = -12; z <= 4; z++) {   // 평평한 시험대 (가장자리·낙하 영향 없게)
            sl.setBlockAndUpdate(new BlockPos(bx + x, y - 1, bz + z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            for (int h = 0; h < 3; h++) sl.setBlockAndUpdate(new BlockPos(bx + x, y + h, bz + z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        sp.teleportTo(sl, bx + 0.5, y, bz + 0.5, 0f, 0f);
        sp.setDeltaMovement(Vec3.ZERO);
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        sp.setHealth(20f);
        sp.setYRot(0);
        sp.setXRot(0);
        kick0 = sp.position();
        com.tycooncraft.gun.GunItem.rt20Kick(sp.serverLevel(), sp);
        System.out.printf("[AUTOTEST] v195 rt20Recoil hpLostNow=%.1f%n", 20f - sp.getHealth());
    }

    static void rt20KickMeasure(ServerPlayer sp) {
        Vec3 d = sp.position().subtract(kick0);
        System.out.printf("[AUTOTEST] v195 rt20Kick sneak=%s pushed=%.2f (dz=%.2f) hpLost=%.1f reload=%d%n", sp.isShiftKeyDown(), Math.hypot(d.x, d.z), d.z, 20f - sp.getHealth(),
                ((com.tycooncraft.gun.GunItem) ModItems.RT20.get()).reloadTicks(new ItemStack(ModItems.RT20.get())));
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        sp.setHealth(20f);
    }

    /** v0.20.0 이펙트 시험장: 남쪽 7칸에 돌벽, 소총을 들고 벽을 봄 */
    static void fxSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos b = sp.blockPosition();
        for (int x = -4; x <= 4; x++) for (int y = 0; y <= 4; y++) {
            sl.setBlockAndUpdate(b.offset(x, y, 7), net.minecraft.world.level.block.Blocks.STONE_BRICKS.defaultBlockState());
            for (int z = 1; z < 7; z++) sl.setBlockAndUpdate(b.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        sp.teleportTo(sl, b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0f, 4f);
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RIFLE.get()));
        sl.setDayTime(6000);
    }

    static void night(ServerPlayer sp) {
        sp.serverLevel().setDayTime(18000);
    }

    static void day(ServerPlayer sp) {
        sp.serverLevel().setDayTime(6000);
    }

    /** v0.20.1: 이펙트 시험장으로 돌아와 RT-20 을 듦 */
    static void rt20Hold(ServerPlayer sp) {
        fxSetup(sp);
        sp.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.RT20.get()));
    }

    // ── v0.21.0 증기 동력 ──
    static BlockPos pwBase;

    static void powerSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        pwBase = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(0, 0, -14));
        for (int x = -2; x <= 7; x++) for (int y = 0; y <= 3; y++) for (int z = -2; z <= 2; z++)
            sl.setBlockAndUpdate(pwBase.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        sl.setBlockAndUpdate(pwBase, ModBlocks.machine(com.tycooncraft.machine.MachineType.CRUSHER).defaultBlockState());
        if (sl.getBlockEntity(pwBase) instanceof com.tycooncraft.blockentity.MachineBlockEntity m)
            m.inputs().insertItem(0, new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 32), false);
        sp.teleportTo(sl, pwBase.getX() + 2.5, pwBase.getY(), pwBase.getZ() + 6.5, 180f, 12f);
        System.out.println("[AUTOTEST] v210 powerSetup crusher placed (no power yet)");
    }

    static void powerMid(ServerPlayer sp) {
        var sl = sp.serverLevel();
        int out = sl.getBlockEntity(pwBase) instanceof com.tycooncraft.blockentity.MachineBlockEntity m ? m.output().getStackInSlot(0).getCount() : -1;
        System.out.println("[AUTOTEST] v210 noPower crusherOut=" + out + " ratio=" + com.tycooncraft.power.PowerGrid.ratio(sl, pwBase));
        // 회전축 3개(기계 위) → 증기기관 → 보일러
        for (int x = 0; x <= 2; x++)
            sl.setBlockAndUpdate(pwBase.offset(x, 1, 0), ModBlocks.SHAFT.get().defaultBlockState().setValue(com.tycooncraft.power.ShaftBlock.AXIS, net.minecraft.core.Direction.Axis.X));
        sl.setBlockAndUpdate(pwBase.offset(3, 0, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        sl.setBlockAndUpdate(pwBase.offset(3, 1, 0), ModBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(com.tycooncraft.power.SteamEngineBlock.FACING, net.minecraft.core.Direction.SOUTH));
        sl.setBlockAndUpdate(pwBase.offset(4, 0, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        sl.setBlockAndUpdate(pwBase.offset(4, 1, 0), ModBlocks.BOILER.get().defaultBlockState().setValue(com.tycooncraft.power.BoilerBlock.FACING, net.minecraft.core.Direction.SOUTH));
        if (sl.getBlockEntity(pwBase.offset(4, 1, 0)) instanceof com.tycooncraft.power.BoilerBlockEntity b)
            b.fuel().insertItem(0, new ItemStack(net.minecraft.world.item.Items.COAL, 8), false);
    }

    static void powerEnd(ServerPlayer sp) {
        var sl = sp.serverLevel();
        int out = sl.getBlockEntity(pwBase) instanceof com.tycooncraft.blockentity.MachineBlockEntity m ? m.output().getStackInSlot(0).getCount() : -1;
        var net = com.tycooncraft.power.PowerGrid.net(sl, pwBase);
        boolean spin = sl.getBlockEntity(pwBase.offset(1, 1, 0)) instanceof com.tycooncraft.power.ShaftBlockEntity sh && sh.spinning();
        int water = sl.getBlockEntity(pwBase.offset(4, 1, 0)) instanceof com.tycooncraft.power.BoilerBlockEntity b ? b.water : -1;
        System.out.println("[AUTOTEST] v210 powered crusherOut=" + out + " ratio=" + com.tycooncraft.power.PowerGrid.ratio(sl, pwBase)
                + " supply=" + (net == null ? -1 : net.supply) + " demand=" + (net == null ? -1 : net.demand) + " nodes=" + (net == null ? 0 : net.nodes.size())
                + " shaftSpin=" + spin + " coalBurned=" + com.tycooncraft.power.BoilerBlockEntity.coalBurned + " water=" + water + " rebuilds=" + com.tycooncraft.power.PowerGrid.rebuilds);
        // 공장 키트에 동력이 이어져 있는지 (분쇄기 위치)
        if (kitOrigin != null) {
            BlockPos kc = kitOrigin.offset(3, 0, 5);
            var kn = com.tycooncraft.power.PowerGrid.net(sl, kc);
            System.out.println("[AUTOTEST] v210 kit crusherNet=" + (kn != null) + " nodes=" + (kn == null ? 0 : kn.nodes.size())
                    + " boiler=" + sl.getBlockState(kitOrigin.offset(11, 0, 5)).getBlock().getClass().getSimpleName()
                    + " water=" + (sl.getBlockEntity(kitOrigin.offset(11, 0, 5)) instanceof com.tycooncraft.power.BoilerBlockEntity kb ? kb.water + " fuel=" + kb.fuel().getStackInSlot(0).getCount() : "-"));
            sp.teleportTo(sl, kitOrigin.getX() + 12.5, kitOrigin.getY() + 1, kitOrigin.getZ() + 8.5, 120f, 18f);
        }
    }

    // ── v0.22.0 전기 ──
    static BlockPos elBase;

    private static void put(net.minecraft.server.level.ServerLevel sl, BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        sl.setBlockAndUpdate(p, s);
    }

    private static com.tycooncraft.blockentity.MachineBlockEntity mach(net.minecraft.server.level.ServerLevel sl, BlockPos p, com.tycooncraft.machine.MachineType t,
                                                                      net.minecraft.world.item.Item a, int an, net.minecraft.world.item.Item b, int bn) {
        put(sl, p, ModBlocks.machine(t).defaultBlockState());
        var m = (com.tycooncraft.blockentity.MachineBlockEntity) sl.getBlockEntity(p);
        m.inputs().insertItem(0, new ItemStack(a, an), false);
        if (b != null) m.inputs().insertItem(1, new ItemStack(b, bn), false);
        return m;
    }

    static void elecSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sl.setDayTime(6000);
        elBase = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(-4, 0, -26));
        for (int x = -2; x <= 13; x++) for (int y = 0; y <= 4; y++) for (int z = -3; z <= 4; z++)
            put(sl, elBase.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var S = net.minecraft.core.Direction.SOUTH;
        put(sl, elBase, ModBlocks.BOILER.get().defaultBlockState().setValue(com.tycooncraft.power.BoilerBlock.FACING, S));
        ((com.tycooncraft.power.BoilerBlockEntity) sl.getBlockEntity(elBase)).fuel().insertItem(0, new ItemStack(net.minecraft.world.item.Items.COAL, 16), false);
        put(sl, elBase.offset(1, 0, 0), ModBlocks.STEAM_ENGINE.get().defaultBlockState().setValue(com.tycooncraft.power.SteamEngineBlock.FACING, S));
        put(sl, elBase.offset(2, 0, 0), ModBlocks.GENERATOR.get().defaultBlockState().setValue(com.tycooncraft.power.elec.GeneratorBlock.FACING, S));
        for (int x = 3; x <= 8; x++) put(sl, elBase.offset(x, 0, 0), ModBlocks.POWER_CABLE.get().defaultBlockState());
        var I = net.minecraft.world.item.Items.RAW_IRON;
        mach(sl, elBase.offset(4, 0, 1), com.tycooncraft.machine.MachineType.INDUCTION_FURNACE, I, 64, null, 0);
        mach(sl, elBase.offset(5, 0, 1), com.tycooncraft.machine.MachineType.CRUSHER, I, 64, null, 0);
        put(sl, elBase.offset(6, 0, -1), ModBlocks.ACCUMULATOR.get().defaultBlockState());
        ((com.tycooncraft.power.elec.AccumulatorBlockEntity) sl.getBlockEntity(elBase.offset(6, 0, -1))).stored = 50_000;
        put(sl, elBase.offset(7, 0, -1), ModBlocks.SOLAR_GENERATOR.get().defaultBlockState());
        // 전동기 → 회전축 → 프레스 (전기로 옛 축 기계 돌리기)
        put(sl, elBase.offset(9, 0, 0), ModBlocks.ELECTRIC_MOTOR.get().defaultBlockState().setValue(com.tycooncraft.power.elec.GeneratorBlock.FACING, net.minecraft.core.Direction.WEST));
        put(sl, elBase.offset(10, 0, 0), ModBlocks.SHAFT.get().defaultBlockState().setValue(com.tycooncraft.power.ShaftBlock.AXIS, net.minecraft.core.Direction.Axis.X));
        put(sl, elBase.offset(11, 0, 0), ModBlocks.SHAFT.get().defaultBlockState().setValue(com.tycooncraft.power.ShaftBlock.AXIS, net.minecraft.core.Direction.Axis.X));
        mach(sl, elBase.offset(11, 0, 1), com.tycooncraft.machine.MachineType.PRESS, net.minecraft.world.item.Items.JUNGLE_LOG, 32, null, 0);
        // 전기 전용 기계를 전선 없이 → 멈춰 있어야 함
        mach(sl, elBase.offset(0, 0, 3), com.tycooncraft.machine.MachineType.ARC_FURNACE, net.minecraft.world.item.Items.IRON_INGOT, 16, ModItems.GRAPHITE.get(), 8);
        sp.teleportTo(sl, elBase.getX() + 5.5, elBase.getY() + 4.5, elBase.getZ() + 7.5, 180f, 35f);
        System.out.println("[AUTOTEST] v220 elecSetup cable0=" + sl.getBlockState(elBase.offset(3, 0, 0)));
    }

    private static int outOf(net.minecraft.server.level.ServerLevel sl, BlockPos p) {
        return sl.getBlockEntity(p) instanceof com.tycooncraft.blockentity.MachineBlockEntity m ? m.output().getStackInSlot(0).getCount() : -1;
    }

    private static void elecLog(ServerPlayer sp, String tag) {
        var sl = sp.serverLevel();
        var L = com.tycooncraft.power.Layer.ELEC;
        var e = com.tycooncraft.power.PowerGrid.net(sl, elBase.offset(3, 0, 0), L);
        var m = com.tycooncraft.power.PowerGrid.net(sl, elBase.offset(10, 0, 0));
        var acc = (com.tycooncraft.power.elec.AccumulatorBlockEntity) sl.getBlockEntity(elBase.offset(6, 0, -1));
        var gen = (com.tycooncraft.power.elec.GeneratorBlockEntity) sl.getBlockEntity(elBase.offset(2, 0, 0));
        var sol = (com.tycooncraft.power.elec.SolarBlockEntity) sl.getBlockEntity(elBase.offset(7, 0, -1));
        var mot = (com.tycooncraft.power.elec.MotorBlockEntity) sl.getBlockEntity(elBase.offset(9, 0, 0));
        System.out.println("[AUTOTEST] v220 " + tag + " induction=" + outOf(sl, elBase.offset(4, 0, 1)) + " crusher=" + outOf(sl, elBase.offset(5, 0, 1))
                + " press=" + outOf(sl, elBase.offset(11, 0, 1)) + " arcNoCable=" + outOf(sl, elBase.offset(0, 0, 3))
                + " elecNodes=" + (e == null ? 0 : e.nodes.size()) + " supply=" + (e == null ? -1 : e.supply) + " demand=" + (e == null ? -1 : e.demand)
                + " ratio=" + (e == null ? -1 : e.ratio) + " stored=" + acc.stored + " gen=" + gen.output() + " solar=" + sol.output() + " motor=" + mot.output()
                + " shaftNet=" + (m == null ? -1 : m.supply + "/" + m.demand) + " coal=" + com.tycooncraft.power.BoilerBlockEntity.coalBurned
                + " cableState=" + sl.getBlockState(elBase.offset(4, 0, 0)).getValue(com.tycooncraft.power.elec.CableBlock.prop(net.minecraft.core.Direction.SOUTH)));
    }

    static void elecMid(ServerPlayer sp) {
        elecLog(sp, "mid");
    }

    static void elecView2(ServerPlayer sp) {
        sp.teleportTo(sp.serverLevel(), elBase.getX() + 6.5, elBase.getY() + 1.8, elBase.getZ() + 3.6, 160f, 30f);
    }

    static void elecEnd(ServerPlayer sp) {
        elecLog(sp, "end");
        // 밤: 태양광 0 확인
        sp.serverLevel().setDayTime(18000);
    }

    // ── v0.23.0 자동화 ──
    static BlockPos auBase;

    private static int count(net.minecraft.server.level.ServerLevel sl, BlockPos p, net.minecraft.world.item.Item it) {
        if (!(sl.getBlockEntity(p) instanceof com.tycooncraft.automation.WarehouseBlockEntity w)) return -1;
        int n = 0;
        for (int i = 0; i < w.inv.getContainerSize(); i++) if (w.inv.getItem(i).is(it)) n += w.inv.getItem(i).getCount();
        return n;
    }

    static void autoSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        auBase = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(-4, 0, -40));
        for (int x = -2; x <= 9; x++) for (int y = 0; y <= 4; y++) for (int z = -3; z <= 3; z++)
            put(sl, auBase.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var E = net.minecraft.core.Direction.EAST;
        put(sl, auBase, ModBlocks.WAREHOUSE.get().defaultBlockState());
        var wa = (com.tycooncraft.automation.WarehouseBlockEntity) sl.getBlockEntity(auBase);
        wa.inv.setItem(0, new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 64));
        wa.inv.setItem(1, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 32));
        put(sl, auBase.offset(1, 0, 0), ModBlocks.ROBOT_ARM.get().defaultBlockState().setValue(com.tycooncraft.automation.RobotArmBlock.FACING, E));
        ((com.tycooncraft.automation.RobotArmBlockEntity) sl.getBlockEntity(auBase.offset(1, 0, 0))).setFilter(new ItemStack(net.minecraft.world.item.Items.RAW_IRON));
        put(sl, auBase.offset(2, 0, 0), ModBlocks.machine(com.tycooncraft.machine.MachineType.INDUCTION_FURNACE).defaultBlockState()
                .setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, net.minecraft.core.Direction.SOUTH));
        put(sl, auBase.offset(3, 0, 0), ModBlocks.ROBOT_ARM.get().defaultBlockState().setValue(com.tycooncraft.automation.RobotArmBlock.FACING, E));
        put(sl, auBase.offset(4, 0, 0), ModBlocks.CONVEYOR.get().defaultBlockState().setValue(com.tycooncraft.block.ConveyorBlock.FACING, E));
        put(sl, auBase.offset(5, 0, 0), ModBlocks.SORTER.get().defaultBlockState().setValue(com.tycooncraft.automation.SorterBlock.FACING, E));
        ((com.tycooncraft.automation.SorterBlockEntity) sl.getBlockEntity(auBase.offset(5, 0, 0))).setFilter(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        put(sl, auBase.offset(5, 0, -1), ModBlocks.WAREHOUSE.get().defaultBlockState());
        put(sl, auBase.offset(6, 0, 0), ModBlocks.WAREHOUSE.get().defaultBlockState());
        // 전기: 꽉 찬 축전지 + 전선
        put(sl, auBase.offset(1, 0, 1), ModBlocks.ACCUMULATOR.get().defaultBlockState());
        ((com.tycooncraft.power.elec.AccumulatorBlockEntity) sl.getBlockEntity(auBase.offset(1, 0, 1))).stored = 200_000;
        put(sl, auBase.offset(2, 0, 1), ModBlocks.POWER_CABLE.get().defaultBlockState());
        put(sl, auBase.offset(3, 0, 1), ModBlocks.POWER_CABLE.get().defaultBlockState());
        // 분류 안 되는 물건(조약돌)을 벨트에 올림 → 앞(창고 C)으로 가야 함
        com.tycooncraft.blockentity.ItemTransfer.spawnAt(sl, auBase.offset(4, 0, 0), E, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 8));
        sp.teleportTo(sl, auBase.getX() + 3.5, auBase.getY() + 1.9, auBase.getZ() - 3.8, 0f, 20f);
        System.out.println("[AUTOTEST] v230 autoSetup ok");
    }

    private static void autoLog(ServerPlayer sp, String tag) {
        var sl = sp.serverLevel();
        var I = net.minecraft.world.item.Items.IRON_INGOT;
        var raw = net.minecraft.world.item.Items.RAW_IRON;
        var cob = net.minecraft.world.item.Items.COBBLESTONE;
        System.out.println("[AUTOTEST] v230 " + tag + " A.raw=" + count(sl, auBase, raw) + " A.cobble=" + count(sl, auBase, cob)
                + " B.ingot(left)=" + count(sl, auBase.offset(5, 0, -1), I) + " B.cobble=" + count(sl, auBase.offset(5, 0, -1), cob)
                + " C.ingot(fwd)=" + count(sl, auBase.offset(6, 0, 0), I) + " C.cobble=" + count(sl, auBase.offset(6, 0, 0), cob)
                + " armMoves=" + com.tycooncraft.automation.RobotArmBlockEntity.moves + " sortedLeft=" + com.tycooncraft.automation.SorterBlockEntity.sortedLeft
                + " passed=" + com.tycooncraft.automation.SorterBlockEntity.passed
                + " armPower=" + com.tycooncraft.power.PowerGrid.ratio(sl, auBase.offset(1, 0, 0), com.tycooncraft.power.Layer.ELEC)
                + " arm2Power=" + com.tycooncraft.power.PowerGrid.ratio(sl, auBase.offset(3, 0, 0), com.tycooncraft.power.Layer.ELEC));
    }

    static void autoMid(ServerPlayer sp) {
        autoLog(sp, "mid");
    }

    static void autoEnd(ServerPlayer sp) {
        autoLog(sp, "end");
    }

    // ── v0.24.0 로봇 팔 모션 · 산업 마을 ──
    static BlockPos armBase, villOrigin;

    static void armShowSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        armBase = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(-6, 0, -52));
        for (int x = -2; x <= 15; x++) for (int y = 0; y <= 4; y++) for (int z = -2; z <= 5; z++)
            put(sl, armBase.offset(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        for (int i = 0; i < 5; i++) {
            BlockPos a = armBase.offset(i * 3, 0, 0);
            put(sl, a.west(), net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState());
            put(sl, a, ModBlocks.ROBOT_ARM.get().defaultBlockState().setValue(com.tycooncraft.automation.RobotArmBlock.FACING, net.minecraft.core.Direction.EAST));
            put(sl, a.east(), ModBlocks.WAREHOUSE.get().defaultBlockState());
        }
        put(sl, armBase.offset(-1, 0, 3), ModBlocks.CONVEYOR.get().defaultBlockState().setValue(com.tycooncraft.block.ConveyorBlock.FACING, net.minecraft.core.Direction.EAST));
        put(sl, armBase.offset(0, 0, 3), ModBlocks.SORTER.get().defaultBlockState().setValue(com.tycooncraft.automation.SorterBlock.FACING, net.minecraft.core.Direction.EAST));
        ((com.tycooncraft.automation.SorterBlockEntity) sl.getBlockEntity(armBase.offset(0, 0, 3))).setFilter(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        put(sl, armBase.offset(1, 0, 3), ModBlocks.CONVEYOR.get().defaultBlockState().setValue(com.tycooncraft.block.ConveyorBlock.FACING, net.minecraft.core.Direction.EAST));
        sp.teleportTo(sl, armBase.getX() + 6.5, armBase.getY() + 2.2, armBase.getZ() + 6.8, 180f, 16f);
    }

    static void villageGo(ServerPlayer sp) {
        var sl = sp.serverLevel();
        villOrigin = sp.blockPosition().offset(180, 0, -40);
        sp.teleportTo(sl, villOrigin.getX() + 32, sp.getY() + 60, villOrigin.getZ() + 32, 0f, 90f);
        sp.getAbilities().flying = true;
        sp.onUpdateAbilities();
    }

    static void villageBuild(ServerPlayer sp) {
        long t0 = System.nanoTime();
        var sl = sp.serverLevel();
        // 청크가 다 올라온 뒤에 땅 높이를 잼 (안 올라온 청크는 높이가 바닥으로 나옴)
        villOrigin = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, villOrigin.offset(32, 0, 32)).offset(-32, 0, -32);
        com.tycooncraft.world.StructureBuilder.village(sl, villOrigin);
        System.out.println("[AUTOTEST] v240 villageBuilt ms=" + (System.nanoTime() - t0) / 1_000_000 + " y=" + villOrigin.getY());
    }

    static void villageCheck(ServerPlayer sp) {
        var sl = sp.serverLevel();
        var box = new net.minecraft.world.phys.AABB(villOrigin).expandTowards(64, 12, 64).inflate(2);
        int traders = 0, residents = 0, babies = 0, minOffers = 999, maxOffers = 0, totalOffers = 0;
        StringBuilder names = new StringBuilder();
        for (var v : sl.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, box)) {
            if (v.hasCustomName()) {
                traders++;
                int n = v.getOffers().size();
                minOffers = Math.min(minOffers, n);
                maxOffers = Math.max(maxOffers, n);
                totalOffers += n;
                names.append(v.getCustomName().getString()).append('(').append(n).append(") ");
            } else if (v.isBaby()) babies++;
            else residents++;
        }
        int golems = sl.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, box).size();
        int pools = 0;
        for (String k : new String[]{"food", "builder", "gear", "machines", "buyer", "gun", "engineer", "scrap_buyer", "miner", "chemist", "vehicle", "pharmacist"})
            pools += com.tycooncraft.world.StructureBuilder.traderOffers("trader.tycooncraft." + k).size();
        // 새 상인 예시 거래
        StringBuilder ex = new StringBuilder();
        for (var v : sl.getEntitiesOfClass(net.minecraft.world.entity.npc.Villager.class, box)) {
            String key = v.getPersistentData().getString("tycooncraft_trader");
            if (!key.endsWith("gun") && !key.endsWith("engineer") && !key.endsWith("scrap_buyer")) continue;
            ex.append(key.substring(19)).append(": ");
            for (int i = 0; i < Math.min(3, v.getOffers().size()); i++) {
                var o = v.getOffers().get(i);
                ex.append(o.getItemCostA().count()).append(' ').append(o.getItemCostA().item().value().getDescription().getString())
                        .append(" → ").append(o.getResult().getCount()).append(' ').append(o.getResult().getHoverName().getString()).append("; ");
            }
            ex.append(" | ");
        }
        System.out.println("[AUTOTEST] v240 village traders=" + traders + " residents=" + residents + " babies=" + babies + " golems=" + golems
                + " offers=" + totalOffers + " (min " + minOffers + " max " + maxOffers + ") pools=" + pools + " names=" + names);
        System.out.println("[AUTOTEST] v240 examples " + ex);
        sp.teleportTo(sl, villOrigin.getX() + 32, villOrigin.getY() + 62, villOrigin.getZ() + 32, 0f, 90f);
    }

    static void villageView(ServerPlayer sp, double x, double y, double z, float yaw, float pitch) {
        sp.teleportTo(sp.serverLevel(), villOrigin.getX() + x, villOrigin.getY() + y, villOrigin.getZ() + z, yaw, pitch);
    }

    static com.tycooncraft.vehicle.JetEntity landJet;

    static void jetLandSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos g = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(0, 0, -12));
        landJet = ModEntities.FIGHTER_JET.get().create(sl);
        landJet.moveTo(g.getX() + 0.5, g.getY() + 12, g.getZ() + 0.5, 180f, 8f);
        landJet.setFuel(landJet.maxFuel());
        landJet.setDeltaMovement(0, -0.1, -1.0);
        sl.addFreshEntity(landJet);
        sp.startRiding(landJet, true);
        System.out.printf("[AUTOTEST] v200 jetLand start y=%.1f ground=%d%n", landJet.getY(), g.getY());
    }

    static void jetLandCheck(ServerPlayer sp) {
        var j = landJet;
        System.out.printf("[AUTOTEST] v200 jetLand alive=%s onGround=%s speed=%.2f pitch=%.1f yaw=%.1f agl=%.2f riding=%s%n", j.isAlive(), j.onGround(),
                j.getDeltaMovement().horizontalDistance(), j.getXRot(), j.getYRot(), j.heightAboveGround(8), sp.getVehicle() == j);
    }

    private V185Test() {}
}
