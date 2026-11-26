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
                ModItems.VEST_3.get(), ModItems.SHIP_MOTOR.get(), ModItems.SHIP_FLOAT.get()})
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
                        || it == ModItems.TURRET.get() || it == ModItems.SETTER.get()) {
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

    /** 시작 도구 (v0.30: 공장 키트 삭제 → 제작대) */
    static void kit(ServerPlayer sp) {
        var k = com.tycooncraft.event.StarterKit.kit();
        StringBuilder names = new StringBuilder();
        for (ItemStack st : k) names.append(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(st.getItem()).getPath()).append(',');
        System.out.println("[AUTOTEST] v300 starterKit=" + names);
        armor40(sp);
    }

    /** v0.31: 방어력 40 · 방탄복 인챈트 */
    static void armor40(ServerPlayer sp) {
        var sl = sp.serverLevel();
        var z = EntityType.ZOMBIE.spawn(sl, sp.blockPosition().offset(3, 0, 3), MobSpawnType.MOB_SUMMONED);
        z.setNoAi(true);
        var protH = sl.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        net.minecraft.world.item.Item[] set = {ModItems.HELMET_4.get(), ModItems.VEST_4.get(), ModItems.JUGGERNAUT_LEGGINGS.get(), ModItems.COMBAT_BOOTS.get()};
        net.minecraft.world.entity.EquipmentSlot[] slots = {net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) z.setItemSlot(slots[i], new ItemStack(set[i]));
        z.tick();
        int plain = z.getArmorValue();
        for (int i = 0; i < 4; i++) { ItemStack st = new ItemStack(set[i]); st.enchant(protH, 4); z.setItemSlot(slots[i], st); }
        z.tick();
        z.setHealth(z.getMaxHealth());
        int armorSeen = z.getArmorValue();
        float hp0 = z.getHealth();
        z.hurt(sl.damageSources().playerAttack(sp), 10f);
        float taken = hp0 - z.getHealth();
        float expect = 10f * (1 - com.tycooncraft.gear.ExtendedArmor.reduction(z.getArmorValue(), (float) z.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS), 10f));
        z.invulnerableTime = 0;
        z.setHealth(z.getMaxHealth());
        com.tycooncraft.gun.Ballistics.BULLET_HIT = true;
        com.tycooncraft.gun.Ballistics.BULLET_RAW = 10f;
        z.hurt(sl.damageSources().playerAttack(sp), 10f);
        com.tycooncraft.gun.Ballistics.BULLET_HIT = false;
        float bulletTaken = z.getMaxHealth() - z.getHealth();
        float bulletExpect = 10f * (1 - com.tycooncraft.gear.ExtendedArmor.reduction(com.tycooncraft.gun.Ballistics.bulletArmor(z), (float) z.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS), 10f));
        System.out.println(String.format("[AUTOTEST] v312 bulletArmor taken=%.2f expect=%.2f", bulletTaken, bulletExpect));
        z.invulnerableTime = 0;
        z.setHealth(z.getMaxHealth());
        com.tycooncraft.gun.Ballistics.BULLET_HIT = true;
        com.tycooncraft.gun.Ballistics.BULLET_RAW = 10f;   // 조끼 등으로 이미 6까지 줄어든 총알
        z.hurt(sl.damageSources().playerAttack(sp), 6f);
        com.tycooncraft.gun.Ballistics.BULLET_HIT = false;
        System.out.println(String.format("[AUTOTEST] v313 bulletCap taken=%.2f floor=3.00 bulletArmor=%.1f", z.getMaxHealth() - z.getHealth(), com.tycooncraft.gun.Ballistics.bulletArmor(z)));
        z.discard();
        var reg = sl.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        ItemStack vest = new ItemStack(ModItems.VEST_3.get());
        var prot = reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.PROTECTION);
        var thorns = reg.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.THORNS);
        var bullet = reg.getOrThrow(com.tycooncraft.gun.GunEnchants.BULLET_PROTECTION);
        var table = net.minecraft.world.item.enchantment.EnchantmentHelper.selectEnchantment(sl.random, vest, 30,
                reg.get(net.minecraft.tags.EnchantmentTags.IN_ENCHANTING_TABLE).map(h -> h.stream()).orElseThrow());
        StringBuilder tb = new StringBuilder();
        for (var inst : table) tb.append(inst.enchantment.getRegisteredName()).append(inst.level).append(',');
        vest.enchant(bullet, 4);
        sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, vest);
        float bp = com.tycooncraft.gun.Ballistics.bulletArmor(sp);
        sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, ItemStack.EMPTY);
        System.out.println(String.format("[AUTOTEST] v310 armor40 plain=" + plain + " prot4=%d taken=%.2f expect=%.2f prot=%s thorns=%s bulletOk=%s foil=%s bulletArmorVest=%.2f table=%s",
                armorSeen, taken, expect,
                vest.supportsEnchantment(prot), vest.supportsEnchantment(thorns), vest.supportsEnchantment(bullet), vest.hasFoil(), bp, tb));
    }

    static BlockPos kitOrigin;


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

    // ── v0.24.0 로봇 팔 모션 · 산업 마을 ──
    static BlockPos villOrigin;

    private static void put(net.minecraft.server.level.ServerLevel sl, BlockPos p, net.minecraft.world.level.block.state.BlockState s) {
        sl.setBlockAndUpdate(p, s);
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
        System.out.println("[AUTOTEST] v250 guardsSpawned=" + com.tycooncraft.world.StructureBuilder.spawnGuards(sl, villOrigin, 64));
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

    // ── v0.25.0 마을 경비병 ──
    static net.minecraft.world.entity.monster.Zombie guardZombie;
    static float guardPlayerHp;

    /** 1) 마을 안에 좀비 투입 → 경비병이 쏴서 잡는지 */
    static void guardZombie(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        sp.getAbilities().invulnerable = false;
        sp.getAbilities().flying = false;
        sp.onUpdateAbilities();
        guardZombie = net.minecraft.world.entity.EntityType.ZOMBIE.create(sl);
        guardZombie.moveTo(villOrigin.getX() + 31.5, villOrigin.getY(), villOrigin.getZ() + 12.5, 0, 0);
        guardZombie.setPersistenceRequired();
        sl.addFreshEntity(guardZombie);
        sp.teleportTo(sl, villOrigin.getX() + 33.5, villOrigin.getY() + 1, villOrigin.getZ() + 20.5, 180f, 5f);
        System.out.println("[AUTOTEST] v250 zombieIn shots=" + com.tycooncraft.village.GuardAlert.shotsFired);
    }

    static void guardZombieCheck(ServerPlayer sp) {
        System.out.println("[AUTOTEST] v250 zombieDead=" + !guardZombie.isAlive() + " zombieHp=" + guardZombie.getHealth()
                + " guardShots=" + com.tycooncraft.village.GuardAlert.shotsFired);
    }

    /** 2) 플레이어가 총을 쏨: 1발째 경고, 2발째 수배 → 경비병이 플레이어를 쏨 */
    static void guardGunfire(ServerPlayer sp, int n) {
        sp.setHealth(sp.getMaxHealth());
        // 시험용: 경비대에 맞아도 죽지 않게 (뒤 시험이 이어지도록)
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 3));
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 200, 2));
        com.tycooncraft.village.GuardAlert.onGunfire(sp);
        boolean w = com.tycooncraft.village.GuardAlert.wanted(sp);
        guardPlayerHp = sp.getHealth();
        System.out.println("[AUTOTEST] v250 gunfire#" + n + " warnings=" + com.tycooncraft.village.GuardAlert.warnings + " wanted=" + w);
    }

    static void guardWantedCheck(ServerPlayer sp) {
        var sl = sp.serverLevel();
        int aiming = sl.getEntitiesOfClass(com.tycooncraft.village.VillageGuard.class, sp.getBoundingBox().inflate(48), g -> g.getTarget() == sp).size();
        System.out.println("[AUTOTEST] v250 wantedCheck hpBefore=" + guardPlayerHp + " hpNow=" + sp.getHealth() + " guardsTargeting=" + aiming
                + " shots=" + com.tycooncraft.village.GuardAlert.shotsFired);
        com.tycooncraft.village.GuardAlert.clear(sp);
        sp.setHealth(sp.getMaxHealth());
    }

    static void guardAfterClear(ServerPlayer sp) {
        var sl = sp.serverLevel();
        int aiming = sl.getEntitiesOfClass(com.tycooncraft.village.VillageGuard.class, sp.getBoundingBox().inflate(48), g -> g.getTarget() == sp).size();
        System.out.println("[AUTOTEST] v250 afterClear guardsTargeting=" + aiming + " hp=" + sp.getHealth());
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    }

    // ── v0.26.0 블랙마켓 ──
    static BlockPos bmManhole;
    static com.tycooncraft.blackmarket.BlackMarketDealer bmVictim;

    static void bmEnter(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        sp.setHealth(sp.getMaxHealth());
        if (villOrigin != null) bmManhole = villOrigin.offset(14, -1, 31);
        else {
            bmManhole = sp.blockPosition().offset(3, -1, 0);
            put(sl, bmManhole, ModBlocks.MANHOLE.get().defaultBlockState());
        }
        boolean isManhole = sl.getBlockState(bmManhole).is(ModBlocks.MANHOLE.get());
        boolean memberBefore = com.tycooncraft.blackmarket.BlackMarket.member(sp);
        // 출입증 들고 맨홀 우클릭
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.BLACK_MARKET_PASS.get(), 2));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(bmManhole), net.minecraft.core.Direction.UP, bmManhole, false);
        sl.getBlockState(bmManhole).useItemOn(sp.getMainHandItem(), sl, sp, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        System.out.println("[AUTOTEST] v260 enter manholeInVillage=" + isManhole + " memberBefore=" + memberBefore + " memberAfter=" + com.tycooncraft.blackmarket.BlackMarket.member(sp)
                + " passLeft=" + sp.getMainHandItem().getCount() + " dim=" + sp.level().dimension().location() + " pos=" + sp.blockPosition());
        sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
    }

    static void bmCheck(ServerPlayer sp) {
        var sl = sp.serverLevel();
        var dealers = sl.getEntitiesOfClass(com.tycooncraft.blackmarket.BlackMarketDealer.class, sp.getBoundingBox().inflate(40));
        StringBuilder sb = new StringBuilder();
        for (var d : dealers) {
            var offers = d.getOffers();
            int smug = 0, buys = 0;
            for (var o : offers) {
                if (com.tycooncraft.blackmarket.DealerTrades.smuggled(o.getResult())) smug++;
                if (com.tycooncraft.economy.Coins.isCoin(o.getResult())) buys++;
            }
            sb.append(d.kind()).append(":offers=").append(offers.size()).append(",smuggled=").append(smug).append(",buys=").append(buys);
            if (!offers.isEmpty()) {
                var o = offers.get(0);
                sb.append(",ex=").append(o.getItemCostA().count()).append('x').append(o.getItemCostA().item().value().getDescription().getString())
                        .append("->").append(o.getResult().getCount()).append('x').append(o.getResult().getHoverName().getString());
            }
            sb.append(" | ");
        }
        System.out.println("[AUTOTEST] v260 hub dim=" + sl.dimension().location() + " dealers=" + dealers.size() + " exit="
                + sl.getBlockState(com.tycooncraft.blackmarket.BlackMarketHub.EXIT).getBlock().getClass().getSimpleName() + " " + sb);
        // 건축 금지
        var ev = new net.neoforged.neoforge.event.level.BlockEvent.BreakEvent(sl, sp.blockPosition().below(), sl.getBlockState(sp.blockPosition().below()), sp);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(ev);
        System.out.println("[AUTOTEST] v260 breakBlocked=" + ev.isCanceled());
        sp.teleportTo(sl, -1.5, 64, 2.5, 180f, 8f);
    }

    static void bmVault(ServerPlayer sp) {
        com.tycooncraft.economy.Coins.takeAll(sp);
        com.tycooncraft.economy.Coins.give(sp, 25_000);
        var m1 = com.tycooncraft.blackmarket.VaultBlock.buySlot(sp);
        var m2 = com.tycooncraft.blackmarket.VaultBlock.buySlot(sp);
        var m3 = com.tycooncraft.blackmarket.VaultBlock.buySlot(sp);
        var box = com.tycooncraft.blackmarket.VaultData.get(sp.server).box(sp.getUUID());
        System.out.println("[AUTOTEST] v260 vault unlocked=" + box.unlocked + " walletLeft=" + com.tycooncraft.economy.Coins.wallet(sp) + " third=" + m3.getString());
        box.items.set(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 5));
        com.tycooncraft.blackmarket.VaultBlock.open(sp);
    }

    static void bmBounty(ServerPlayer sp) {
        sp.closeContainer();
        var s = sp.server;
        var self = com.tycooncraft.blackmarket.BountyData.post(sp, sp.getGameProfile().getName(), 5000);
        var none = com.tycooncraft.blackmarket.BountyData.post(sp, "NoSuchPlayer_xyz", 5000);
        // 다른 사람(가상)이 나에게 건 현상금
        var d = com.tycooncraft.blackmarket.BountyData.get(s);
        var b = new com.tycooncraft.blackmarket.BountyData.Bounty();
        b.target = sp.getUUID();
        b.placer = java.util.UUID.randomUUID();
        b.targetName = sp.getGameProfile().getName();
        b.amount = 50_000;
        b.expires = System.currentTimeMillis() + 100_000;
        d.list.add(b);
        System.out.println("[AUTOTEST] v260 bounty self='" + self.getString() + "' none='" + none.getString() + "' totalOnMe=" + d.totalOn(sp.getUUID()));
        com.tycooncraft.blackmarket.BountyPayloads.send(sp, null);
    }

    static void bmFight(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.setHealth(sp.getMaxHealth());
        bmVictim = sl.getEntitiesOfClass(com.tycooncraft.blackmarket.BlackMarketDealer.class, sp.getBoundingBox().inflate(40), e -> e.kind() == 2).stream().findFirst().orElse(null);
        if (bmVictim == null) return;
        sp.teleportTo(sl, bmVictim.getX(), 64, -1.5, 180f, 8f);
        // 시험용: 맞아도 죽지 않게 (피해 80% 감소)
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 200, 3));
        bmVictim.hurt(sp.damageSources().playerAttack(sp), 2f);
        System.out.println("[AUTOTEST] v260 fight hostile=" + com.tycooncraft.blackmarket.BlackMarket.hostile(sp) + " hp=" + sp.getHealth());
    }

    static void bmKill(ServerPlayer sp) {
        var sl = sp.serverLevel();
        float hpAfterFight = sp.getHealth();
        sp.setHealth(sp.getMaxHealth());
        if (bmVictim != null) bmVictim.hurt(sp.damageSources().playerAttack(sp), 1000f);
        var eff = sp.getEffect(com.tycooncraft.registry.ModEffects.TRACKED);
        double speed = sp.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        sp.removeAllEffects();
        sp.removeEffect(com.tycooncraft.registry.ModEffects.TRACKED);
        boolean still = sp.hasEffect(com.tycooncraft.registry.ModEffects.TRACKED);
        int respawnSameDay = com.tycooncraft.blackmarket.BlackMarketHub.tickDealers(sl);
        System.out.println("[AUTOTEST] v260 kill hpDuringFight=" + hpAfterFight + " dealerDead=" + (bmVictim != null && !bmVictim.isAlive()) + " tracked=" + (eff != null)
                + " amp=" + (eff == null ? -1 : eff.getAmplifier()) + " ticks=" + (eff == null ? 0 : eff.getDuration()) + " speed=" + String.format("%.4f", speed)
                + " afterMilk=" + still + " reload100=" + com.tycooncraft.blackmarket.BlackMarket.reloadTicks(sp, 100) + " respawnSameDay=" + respawnSameDay);
        com.tycooncraft.blackmarket.BlackMarket.clearHostile(sp);
    }

    static void bmNextDay(ServerPlayer sp) {
        var sl = sp.serverLevel();
        var ow = sp.server.overworld();
        ow.setDayTime(ow.getDayTime() + 24000);
        int made = com.tycooncraft.blackmarket.BlackMarketHub.tickDealers(sl);
        System.out.println("[AUTOTEST] v260 nextDay respawned=" + made + " dealers=" + sl.getEntitiesOfClass(com.tycooncraft.blackmarket.BlackMarketDealer.class, sp.getBoundingBox().inflate(40)).size());
        com.tycooncraft.blackmarket.BlackMarket.leave(sp);
        System.out.println("[AUTOTEST] v260 leave dim=" + sp.level().dimension().location() + " pos=" + sp.blockPosition() + " manhole=" + bmManhole);
    }

    static void bmSquad(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sl.setDayTime(18000);
        int made = com.tycooncraft.blackmarket.GangHunters.squad(sl, sp);
        int hunters = sl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(48), m -> m.getTarget() == sp).size();
        System.out.println("[AUTOTEST] v260 squad made=" + made + " targetingMe=" + hunters);
    }

    static void bmClaim(ServerPlayer sp) {
        var fake = net.neoforged.neoforge.common.util.FakePlayerFactory.get(sp.serverLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "Hunter"));
        long got = com.tycooncraft.blackmarket.BountyData.claim(sp, fake);
        System.out.println("[AUTOTEST] v260 claim paid=" + got + " hunterBank=" + com.tycooncraft.economy.BankData.get(sp.server).account(fake.getUUID()).balance
                + " leftOnMe=" + com.tycooncraft.blackmarket.BountyData.get(sp.server).totalOn(sp.getUUID()));
        for (var m : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(64), m -> m.getTarget() == sp)) m.discard();
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        sp.setHealth(sp.getMaxHealth());
    }

    // ── v0.27.0 디스트로이어 변종 ──
    static java.util.List<com.tycooncraft.mob.carrier.ScoutDestroyer> scoutList = new java.util.ArrayList<>();
    static com.tycooncraft.mob.carrier.AerialDestroyer carrier;
    static BlockPos varBase;
    static double scoutSide;

    static void scoutSpawn(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 2400, 4, false, false));
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 2400, 2, false, false));
        sl.setDayTime(6000);   // 스크린샷이 보이게 낮에 시험
        varBase = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, sp.blockPosition().offset(-160, 0, 120));
        sp.teleportTo(sl, varBase.getX() + 0.5, varBase.getY(), varBase.getZ() + 0.5, 0f, 10f);
        scoutList.clear();
        for (int i = 0; i < 3; i++) {
            var sc = ModEntities.SCOUT_DESTROYER.get().create(sl);
            sc.moveTo(varBase.getX() + 0.5 + (i - 1) * 2, varBase.getY(), varBase.getZ() + 16.5, 180f, 0);
            sc.finalizeSpawn(sl, sl.getCurrentDifficultyAt(varBase), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
            sc.setTarget(sp);
            sl.addFreshEntity(sc);
            scoutList.add(sc);
        }
        scoutSide = 0;
    }

    /** 정찰기가 곧장 오지 않고 옆으로 꺾는지: 플레이어 쪽 축(z)에 수직인 x 이동량 누적 */
    static double[] lastX = new double[3];

    static void scoutTrack(ServerPlayer sp) {
        for (int i = 0; i < scoutList.size(); i++) {
            var sc = scoutList.get(i);
            if (lastX[i] != 0) scoutSide += Math.abs(sc.getX() - lastX[i]);
            lastX[i] = sc.getX();
        }
    }

    static void scoutCheck(ServerPlayer sp) {
        int alive = 0;
        double dist = 0;
        for (var sc : scoutList) if (sc.isAlive()) {
            alive++;
            dist += sc.distanceTo(sp);
        }
        System.out.println("[AUTOTEST] v270 scouts alive=" + alive + " avgDist=" + String.format("%.1f", alive == 0 ? -1 : dist / alive)
                + " sideMove=" + String.format("%.1f", scoutSide) + " playerHurt=" + (sp.getLastHurtByMob() instanceof com.tycooncraft.mob.carrier.ScoutDestroyer));
        for (var sc : scoutList) sc.discard();
        // 공중 모체
        var sl = sp.serverLevel();
        carrier = ModEntities.AERIAL_DESTROYER.get().create(sl);
        carrier.moveTo(varBase.getX() + 0.5, varBase.getY() + 16, varBase.getZ() + 24.5, 180f, 0);
        carrier.finalizeSpawn(sl, sl.getCurrentDifficultyAt(varBase), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
        carrier.setTarget(sp);
        sl.addFreshEntity(carrier);
        sp.teleportTo(sl, varBase.getX() + 0.5, varBase.getY(), varBase.getZ() + 0.5, 0f, -25f);
    }

    static void carrierMid(ServerPlayer sp) {
        var sl = sp.serverLevel();
        int drones = sl.getEntitiesOfClass(com.tycooncraft.mob.carrier.MiniDrone.class, sp.getBoundingBox().inflate(80)).size();
        int scouts = sl.getEntitiesOfClass(com.tycooncraft.mob.carrier.ScoutDestroyer.class, sp.getBoundingBox().inflate(80)).size();
        System.out.println("[AUTOTEST] v270 carrier alive=" + carrier.isAlive() + " y=" + String.format("%.1f", carrier.getY() - varBase.getY())
                + " dist=" + String.format("%.1f", carrier.distanceTo(sp)) + " drones=" + drones + " scouts=" + scouts
                + " launchedDrones=" + com.tycooncraft.mob.carrier.AerialDestroyer.dronesLaunched + " launchedScouts=" + com.tycooncraft.mob.carrier.AerialDestroyer.scoutsLaunched
                + " playerHp=" + sp.getHealth() + " lastHurtBy=" + (sp.getLastHurtByMob() == null ? "-" : sp.getLastHurtByMob().getType().toShortString()));
    }

    /** 왼쪽 → 오른쪽 동력원을 차례로 쏴서 떨어뜨림 */
    static void carrierShootCore(ServerPlayer sp, int side) {
        var sl = sp.serverLevel();
        var c = carrier.coreWorld(side);
        var dir = c.subtract(carrier.position().add(0, 1.2, 0)).normalize();
        var from = c.add(dir.scale(6));
        sp.teleportTo(sl, from.x, from.y, from.z, sp.getYRot(), sp.getXRot());
        float before0 = carrier.core(0), before1 = carrier.core(1), before2 = carrier.core(2);
        for (int k = 0; k < 3; k++) carrier.hurt(sp.damageSources().playerAttack(sp), 70f);
        System.out.println("[AUTOTEST] v270 shootSide=" + side + " cores " + before0 + "/" + before1 + "/" + before2 + " -> "
                + carrier.core(0) + "/" + carrier.core(1) + "/" + carrier.core(2) + " alive=" + carrier.coresAlive() + " crashing=" + carrier.crashing());
        sp.teleportTo(sl, carrier.getX() + 30, varBase.getY() + 1, carrier.getZ(), 90f, -10f);
    }

    static int beforeAir;

    static void carrierCrashCheck(ServerPlayer sp) {
        var sl = sp.serverLevel();
        int cores = sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, sp.getBoundingBox().inflate(80),
                e -> e.getItem().is(ModItems.FLIGHT_CORE.get())).size();
        int drones = sl.getEntitiesOfClass(com.tycooncraft.mob.carrier.MiniDrone.class, sp.getBoundingBox().inflate(80), e -> e.isAlive() && e.isNoGravity()).size();
        System.out.println("[AUTOTEST] v270 crash carrierDead=" + !carrier.isAlive() + " flightCoreDrops=" + cores + " flyingDronesLeft=" + drones
                + " crashes=" + com.tycooncraft.mob.carrier.AerialDestroyer.crashes + " coresBroken=" + com.tycooncraft.mob.carrier.AerialDestroyer.coresBroken + " droneMaxLeash=" + String.format("%.1f", com.tycooncraft.mob.carrier.MiniDrone.maxLeash)
                + " flightCorePrice=" + com.tycooncraft.economy.PriceTable.basePrice(ModItems.FLIGHT_CORE.get()) + " scoutCorePrice=" + com.tycooncraft.economy.PriceTable.basePrice(ModItems.SCOUT_CORE.get()));
        for (var m : sl.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(100),
                e -> e instanceof com.tycooncraft.mob.carrier.ScoutDestroyer || e instanceof com.tycooncraft.mob.carrier.MiniDrone)) m.discard();
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    }

    // ── v0.27.1 비행 코어 엔진 · 드론 편대 · 프로토-정찰 ──
    static com.tycooncraft.ship.ShipEntity coreShip;
    static double coreY0;

    static void coreShipSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos g = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, varBase.offset(30, 0, 0));
        int x0 = g.getX(), y = g.getY() + 1, z0 = g.getZ();
        BlockPos helm = AirshipTest.build(sl, x0, y, z0, 0);   // 부유체 없음
        sl.setBlockAndUpdate(new BlockPos(x0 + 1, y + 1, z0), ModBlocks.SHIP_CORE_ENGINE.get().defaultBlockState()
                .setValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING, net.minecraft.core.Direction.NORTH));
        coreShip = com.tycooncraft.ship.ShipAssembler.assemble(sl, helm, sp);
        if (coreShip != null) {
            coreShip.autopilot = true;
            coreShip.setFuel(2000);
            coreY0 = coreShip.getY();
        }
        // 비교: 엔진 2개 + 부유체 운송기의 최고속도 공식 (코어 0)
        double plain = Math.min(0.6, 0.12 + 0.16 * Math.sqrt(2) * Math.sqrt(80.0 / 80));
        System.out.println("[AUTOTEST] v271 coreShip assembled=" + (coreShip != null) + (coreShip == null ? "" : " coreEngines=" + coreShip.coreEngineCount()
                + " airship=" + coreShip.airship() + " vmax=" + String.format("%.3f", coreShip.vmaxForTest()) + " plainVmax=" + String.format("%.3f", plain))
                + " recipe=" + sl.getRecipeManager().byKey(com.tycooncraft.TycoonCraft.id("ship_core_engine")).isPresent());
        sp.teleportTo(sl, x0 + 2.5, y + 3, z0 - 14, 0f, -12f);
    }

    static void coreShipFly(ServerPlayer sp) {
        if (coreShip != null && coreShip.isAlive()) coreShip.testInput(coreShip.getY() - coreY0 < 6 ? 0 : 1, 0, coreShip.getY() - coreY0 < 6 ? 1 : 0);
    }

    static void coreShipCheck(ServerPlayer sp) {
        if (coreShip == null) return;
        System.out.println("[AUTOTEST] v271 coreShip rise=" + String.format("%.2f", coreShip.getY() - coreY0) + " speed=" + String.format("%.3f", coreShip.shipSpeed())
                + " fuelLeft=" + coreShip.fuel());
        coreShip.setFuel(0);
        coreShip.discard();
    }

    static Zombie swarmTarget;

    static void swarmLaunch(ServerPlayer sp) {
        var sl = sp.serverLevel();
        sp.teleportTo(sl, varBase.getX() + 0.5, varBase.getY(), varBase.getZ() + 0.5, 0f, -10f);
        swarmTarget = EntityType.ZOMBIE.create(sl);
        swarmTarget.moveTo(varBase.getX() + 0.5, varBase.getY(), varBase.getZ() + 10.5, 180, 0);
        swarmTarget.setNoAi(true);
        swarmTarget.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        swarmTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(300);
        swarmTarget.setHealth(300);
        sl.addFreshEntity(swarmTarget);
        int n = com.tycooncraft.mob.carrier.DroneSwarmItem.launch(sl, sp);
        System.out.println("[AUTOTEST] v271 swarm launched=" + n + " recipe=" + sl.getRecipeManager().byKey(com.tycooncraft.TycoonCraft.id("drone_swarm")).isPresent());
    }

    static void swarmCheck(ServerPlayer sp) {
        var sl = sp.serverLevel();
        var ds = sl.getEntitiesOfClass(com.tycooncraft.mob.carrier.MiniDrone.class, sp.getBoundingBox().inflate(40), d -> d.ally());
        System.out.println("[AUTOTEST] v271 swarm drones=" + ds.size() + " allyShots=" + com.tycooncraft.mob.carrier.MiniDrone.allyShots
                + " targetHp=" + Math.round(swarmTarget.getHealth()) + "/300 playerHp=" + sp.getHealth() + " seenAsEnemy=" + (!ds.isEmpty() && ds.get(0).canBeSeenAsEnemy()));
        swarmTarget.discard();
    }

    static void swarmGone(ServerPlayer sp) {
        var sl = sp.serverLevel();
        for (var d : sl.getEntitiesOfClass(com.tycooncraft.mob.carrier.MiniDrone.class, sp.getBoundingBox().inflate(60), d -> d.ally())) d.discard();
    }

    static com.tycooncraft.mech.ProtoScout pscout;
    static Zombie pscoutTarget;

    static void pscoutSetup(ServerPlayer sp) {
        var sl = sp.serverLevel();
        BlockPos p = varBase.offset(-20, 0, 0);
        p = sl.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, p);
        pscout = com.tycooncraft.mech.ProtoDestroyerItem.spawnScout(sl, p, sp, new ItemStack(ModItems.PROTO_SCOUT.get()), 0f);
        sp.teleportTo(sl, p.getX() + 0.5, p.getY(), p.getZ() - 2.5, 0f, 0f);
        var r = pscout.interact(sp, InteractionHand.MAIN_HAND);
        pscoutPlayer0 = sp.position();
        sp.setYRot(0);
        sp.setXRot(15);
        pscoutTarget = EntityType.ZOMBIE.create(sl);
        pscoutTarget.moveTo(p.getX() + 0.5, p.getY(), p.getZ() + 3.0, 180, 0);
        pscoutTarget.setNoAi(true);
        pscoutTarget.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        pscoutTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(300);
        pscoutTarget.setHealth(300);
        sl.addFreshEntity(pscoutTarget);
        System.out.println("[AUTOTEST] v271 pscout spawned=" + (pscout != null) + " remote=" + r + " piloted=" + (pscout.pilot() == sp) + " riding=" + (sp.getVehicle() == pscout)
                + " size=" + pscout.getBbWidth() + "x" + pscout.getBbHeight()
                + " hp=" + pscout.getHealth() + " recipe=" + sl.getRecipeManager().byKey(com.tycooncraft.TycoonCraft.id("proto_scout")).isPresent());
    }

    static void pscoutSlash(ServerPlayer sp) {
        if (pscout != null) pscout.action(sp, com.tycooncraft.mech.ProtoScout.ACT_SLASH);
    }

    static float pscoutHpAfterSlash;
    static Vec3 pscoutStart;

    static void pscoutPounce(ServerPlayer sp) {
        pscoutHpAfterSlash = pscoutTarget.getHealth();
        Vec3 f = pscout.forward();
        pscoutTarget.teleportTo(pscout.getX() + f.x * 8, pscout.getY(), pscout.getZ() + f.z * 8);
        pscoutStart = pscout.position();
    }

    static void pscoutPounceAct(ServerPlayer sp) {
        pscout.action(sp, com.tycooncraft.mech.ProtoScout.ACT_POUNCE);
    }

    static Vec3 pscoutWalk0;

    static void pscoutWalk(ServerPlayer sp) {
        pscoutWalk0 = pscout.position();
        pscout.testInput(1f, 0f, 0f);
    }

    static void pscoutWalkKeep(ServerPlayer sp) {
        pscout.testInput(1f, 0f, 0f);
    }

    static void pscoutWalkCheck(ServerPlayer sp) {
        pscout.testInput(0f, 0f, 0f);
        System.out.println("[AUTOTEST] v281 pscout walked=" + String.format("%.1f", pscout.position().distanceTo(pscoutWalk0)) + " piloted=" + (pscout.pilot() == sp)
                + " playerMoved=" + String.format("%.1f", sp.position().distanceTo(pscoutPlayer0)) + " links=" + com.tycooncraft.mech.ProtoScout.links);
    }

    static Vec3 pscoutPlayer0;

    static void pscoutCheck(ServerPlayer sp) {
        System.out.println("[AUTOTEST] v271 pscout slashes=" + com.tycooncraft.mech.ProtoScout.slashes + " pounces=" + com.tycooncraft.mech.ProtoScout.pounces
                + " hits=" + com.tycooncraft.mech.ProtoScout.hits + " targetHp 300->" + Math.round(pscoutHpAfterSlash) + "->" + Math.round(pscoutTarget.getHealth())
                + " piloted=" + (pscout.pilot() == sp) + " leap=" + String.format("%.1f", pscout.position().distanceTo(pscoutStart))
                + " speedAttr=" + pscout.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED));
        pscout.stopRemote();
        ItemStack back = pscout.toItem();
        pscout.discard();
        pscoutTarget.discard();
        System.out.println("[AUTOTEST] v271 pscout item=" + back.getItem() + " protoScoutPrice=" + com.tycooncraft.economy.PriceTable.basePrice(ModItems.PROTO_SCOUT.get())
                + " coreEnginePrice=" + com.tycooncraft.economy.PriceTable.basePrice(ModItems.SHIP_CORE_ENGINE.get())
                + " swarmPrice=" + com.tycooncraft.economy.PriceTable.basePrice(ModItems.DRONE_SWARM.get()));
    }

    // ── v0.28 디스트로이어 적대 범위 · 고퀄 모델 · 블랙마켓 (평판·의뢰·카지노·불법 개조) ──
    static com.tycooncraft.mob.carrier.ScoutDestroyer tScout, tScout2;
    static com.tycooncraft.mob.carrier.AerialDestroyer tCarrier;
    static net.minecraft.world.entity.animal.Cow tCow;
    static BlockPos owBase;

    static void v28Targets(ServerPlayer sp) {
        net.minecraft.server.level.ServerLevel ow = sp.server.overworld();
        for (int cx = -2; cx <= 3; cx++) for (int cz = -3; cz <= 3; cz++) ow.setChunkForced((220 >> 4) + cx, (260 >> 4) + cz, true);   // 청크를 먼저 불러야 지면 높이가 맞음
        owBase = ow.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, new BlockPos(220, 0, 260));
        sp.teleportTo(ow, owBase.getX() + 0.5, owBase.getY() + 30, owBase.getZ() - 30.5, 0f, 30f);
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        ow.setDayTime(6000);
        tCow = EntityType.COW.create(ow);
        tCow.moveTo(owBase.getX() + 0.5, owBase.getY(), owBase.getZ() + 0.5, 0, 0);
        tCow.setNoAi(true);
        tCow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(500);
        tCow.setHealth(500);
        tCow.setPersistenceRequired();
        ow.addFreshEntity(tCow);
        tScout = ModEntities.SCOUT_DESTROYER.get().create(ow);
        tScout.moveTo(owBase.getX() + 4.5, owBase.getY(), owBase.getZ() + 0.5, 0, 0);
        tScout.setPersistenceRequired();
        ow.addFreshEntity(tScout);
        tScout2 = ModEntities.SCOUT_DESTROYER.get().create(ow);
        tScout2.moveTo(owBase.getX() - 3.5, owBase.getY(), owBase.getZ() + 0.5, 0, 0);
        tScout2.setPersistenceRequired();
        ow.addFreshEntity(tScout2);
        tCarrier = ModEntities.AERIAL_DESTROYER.get().create(ow);
        tCarrier.moveTo(owBase.getX() + 0.5, owBase.getY() + 14, owBase.getZ() + 10.5, 0, 0);
        tCarrier.setPersistenceRequired();
        ow.addFreshEntity(tCarrier);
        System.out.println("[AUTOTEST] v280 targets setup cow=" + tCow.blockPosition() + " playerCreative=" + sp.isCreative());
    }

    static void v28TargetsCheck(ServerPlayer sp) {
        String a = tScout.getTarget() == null ? "none" : tScout.getTarget().getType().toShortString();
        String b = tScout2.getTarget() == null ? "none" : tScout2.getTarget().getType().toShortString();
        String c = tCarrier.getTarget() == null ? "none" : tCarrier.getTarget().getType().toShortString();
        System.out.println("[AUTOTEST] v280 dbg scoutTick=" + tScout.tickCount + " alive=" + tScout.isAlive() + " pos=" + tScout.blockPosition() + " lvl=" + tScout.level().dimension().location()
                + " player=" + sp.blockPosition() + " " + sp.level().dimension().location() + " canAtk=" + tScout.canAttack(tCow) + " dist=" + tScout.distanceTo(tCow) + " removed=" + tScout.getRemovalReason() + " diff=" + sp.serverLevel().getDifficulty() + " carrierAlive=" + tCarrier.isAlive() + " " + tCarrier.getRemovalReason());
        System.out.println("[AUTOTEST] v280 targets scout1=" + a + " scout2=" + b + " carrier=" + c + " cowHp=" + Math.round(tCow.getHealth())
                + " scoutVsScout=" + (tScout.getTarget() instanceof com.tycooncraft.mob.carrier.ScoutDestroyer)
                + " maxDrones=" + com.tycooncraft.mob.carrier.AerialDestroyer.MAX_DRONES + " maxScouts=" + com.tycooncraft.mob.carrier.AerialDestroyer.MAX_SCOUTS
                + " coreHp=" + com.tycooncraft.mob.carrier.AerialDestroyer.CORE_HP + " droneDmg=" + com.tycooncraft.mob.carrier.MiniDrone.DAMAGE
                + " leashMax=" + String.format("%.1f", com.tycooncraft.mob.carrier.MiniDrone.maxLeash));
        for (var m : sp.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(120),
                e -> com.tycooncraft.mob.carrier.Destroyers.isDestroyer(e) || e == tCow)) m.discard();
    }

    static void v28Showcase(ServerPlayer sp) {
        net.minecraft.server.level.ServerLevel ow = sp.server.overworld();
        sp.stopRiding();
        sp.getInventory().armor.set(2, ItemStack.EMPTY);   // 낙하산 가방 등 벗기 (화면 가림)
        sp.teleportTo(ow, owBase.getX() + 0.5, owBase.getY() + 1.2, owBase.getZ() - 3.5, 0f, 14f);
        var sc = ModEntities.SCOUT_DESTROYER.get().create(ow);
        sc.moveTo(owBase.getX() - 0.6, owBase.getY(), owBase.getZ() + 0.0, 150f, 0);
        sc.setNoAi(true);
        sc.setYBodyRot(150f);
        sc.setYHeadRot(150f);
        sc.setPersistenceRequired();
        ow.addFreshEntity(sc);
        var d1 = ModEntities.MINI_DRONE.get().create(ow);
        d1.moveTo(owBase.getX() + 1.8, owBase.getY() + 1.6, owBase.getZ() - 1.0, 200f, 0);
        d1.setNoAi(true);
        d1.setPersistenceRequired();
        ow.addFreshEntity(d1);
        var d2 = ModEntities.MINI_DRONE.get().create(ow);
        d2.moveTo(owBase.getX() + 2.6, owBase.getY() + 0.9, owBase.getZ() + 0.6, 220f, 0);
        d2.setOwner(sp, 400);
        d2.setNoAi(true);
        ow.addFreshEntity(d2);
        System.out.println("[AUTOTEST] v280 showcase allySynced=" + d2.allyClient());
    }

    static void v28ShowCarrier(ServerPlayer sp) {
        net.minecraft.server.level.ServerLevel ow = sp.server.overworld();
        for (var m : ow.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(30), com.tycooncraft.mob.carrier.Destroyers::isDestroyer)) m.discard();
        var c = ModEntities.AERIAL_DESTROYER.get().create(ow);
        c.moveTo(owBase.getX() + 0.5, owBase.getY() + 6, owBase.getZ() + 15, 120f, 0);
        c.setNoAi(true);
        c.setPersistenceRequired();
        c.setYBodyRot(120f);
        c.setYHeadRot(120f);
        ow.addFreshEntity(c);
        sp.teleportTo(ow, owBase.getX() + 0.5, owBase.getY() + 6, owBase.getZ() - 4.5, 0f, 0f);
        sp.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, c.position().add(0, 2, 0));
    }

    static long bank0;
    static int rep0;

    static long bank(ServerPlayer sp) {
        return com.tycooncraft.economy.BankData.get(sp.server).account(sp.getUUID()).balance;
    }

    static void v28Enter(ServerPlayer sp) {
        for (var m : sp.server.overworld().getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(60), com.tycooncraft.mob.carrier.Destroyers::isDestroyer)) m.discard();
        com.tycooncraft.blackmarket.BlackMarket.enter(sp, sp.blockPosition().below());
        var bm = sp.serverLevel();
        com.tycooncraft.blackmarket.Reputation.set(sp, 0);
        int t0 = com.tycooncraft.blackmarket.Reputation.tier(sp);
        com.tycooncraft.blackmarket.Reputation.add(sp, 120);
        System.out.println("[AUTOTEST] v280 bm dim=" + bm.dimension().location() + " missionBoard=" + bm.getBlockState(com.tycooncraft.blackmarket.BlackMarketHub.MISSION).getBlock()
                + " modBench=" + bm.getBlockState(com.tycooncraft.blackmarket.BlackMarketHub.MODS).getBlock() + " roulette=" + bm.getBlockState(com.tycooncraft.blackmarket.BlackMarketHub.ROULETTE).getBlock()
                + " slot=" + bm.getBlockState(new BlockPos(20, 64, -6)).getBlock() + " door=" + bm.getBlockState(new BlockPos(17, 65, 0)).isAir()
                + " tier0=" + t0 + " tierAfter120=" + com.tycooncraft.blackmarket.Reputation.tier(sp));
        sp.teleportTo(bm, 8.5, 64, 4.5, 0f, 10f);
    }

    static void v28MissionScreen(ServerPlayer sp) {
        com.tycooncraft.blackmarket.BMBlock.open(sp, com.tycooncraft.blackmarket.BMPayloads.MISSIONS);
    }

    static void v28Supply(ServerPlayer sp) {
        var offers = com.tycooncraft.blackmarket.Missions.offers(sp);
        int idx = -1;
        for (int i = 0; i < offers.size(); i++) if (offers.get(i).type == com.tycooncraft.blackmarket.Missions.SUPPLY) idx = i;
        var m = offers.get(idx);
        bank0 = bank(sp);
        rep0 = com.tycooncraft.blackmarket.Reputation.points(sp);
        var r = com.tycooncraft.blackmarket.Missions.accept(sp, idx);
        var early = com.tycooncraft.blackmarket.Missions.turnIn(sp).getString();
        sp.getInventory().add(new ItemStack(m.itemType(), m.count));
        var done = com.tycooncraft.blackmarket.Missions.turnIn(sp).getString();
        System.out.println("[AUTOTEST] v280 supply item=" + m.item + "x" + m.count + " reward=" + m.reward + " accepted=" + r.getString()
                + " | early=" + early + " | done=" + done + " bankDelta=" + (bank(sp) - bank0) + " repDelta=" + (com.tycooncraft.blackmarket.Reputation.points(sp) - rep0)
                + " offersLeft=" + com.tycooncraft.blackmarket.Missions.offers(sp).size());
        com.tycooncraft.blackmarket.BMPayloads.missions(sp, null);
    }

    static void v28Casino(ServerPlayer sp) {
        sp.teleportTo(sp.serverLevel(), 20.5, 64, 0.5, -90f, 5f);
        // 환급률 시뮬레이션
        var r = net.minecraft.util.RandomSource.create(7);
        double slot = 0, rou = 0;
        int N = 200_000;
        for (int i = 0; i < N; i++) {
            slot += com.tycooncraft.blackmarket.Casino.slotPay(com.tycooncraft.blackmarket.Casino.symbol(r), com.tycooncraft.blackmarket.Casino.symbol(r), com.tycooncraft.blackmarket.Casino.symbol(r));
            rou += com.tycooncraft.blackmarket.Casino.roulettePay(r.nextInt(37), i % 3);
        }
        System.out.println("[AUTOTEST] v280 casino rtpSlots=" + String.format("%.3f", slot / N) + " rtpRoulette=" + String.format("%.3f", rou / N)
                + " maxBetTier1=" + com.tycooncraft.blackmarket.Casino.maxBet(sp));
    }

    static void v28SlotsOpen(ServerPlayer sp) {
        sp.getInventory().add(com.tycooncraft.economy.Coins.toStacks(200_000).get(0));
        com.tycooncraft.economy.Coins.give(sp, 200_000);
        com.tycooncraft.blackmarket.BMBlock.open(sp, com.tycooncraft.blackmarket.BMPayloads.SLOTS);
    }

    static long fundsBefore;

    static void v28SlotsPlay(ServerPlayer sp) {
        fundsBefore = com.tycooncraft.blackmarket.Casino.funds(sp);
        com.tycooncraft.blackmarket.BMPayloads.handle(sp, new com.tycooncraft.blackmarket.BMPayloads.Act(com.tycooncraft.blackmarket.BMPayloads.SLOTS, 0, 10_000, 0));
        var over = new net.minecraft.network.chat.Component[1];
        long[] tooMuch = com.tycooncraft.blackmarket.Casino.play(sp, com.tycooncraft.blackmarket.Casino.SLOTS, 999_999, 0, over);
        System.out.println("[AUTOTEST] v280 slots spins=" + com.tycooncraft.blackmarket.Casino.spins + " wagered=" + com.tycooncraft.blackmarket.Casino.wagered
                + " paid=" + com.tycooncraft.blackmarket.Casino.paidOut + " fundsDelta=" + (com.tycooncraft.blackmarket.Casino.funds(sp) - fundsBefore)
                + " overLimitBlocked=" + (tooMuch == null) + " msg=" + (over[0] == null ? "" : over[0].getString()));
    }

    static void v28RouletteOpen(ServerPlayer sp) {
        sp.teleportTo(sp.serverLevel(), 27.5, 64, -2.5, 0f, 30f);
        com.tycooncraft.blackmarket.BMBlock.open(sp, com.tycooncraft.blackmarket.BMPayloads.ROULETTE);
    }

    static void v28RoulettePlay(ServerPlayer sp) {
        com.tycooncraft.blackmarket.BMPayloads.handle(sp, new com.tycooncraft.blackmarket.BMPayloads.Act(com.tycooncraft.blackmarket.BMPayloads.ROULETTE, 0, 10_000, com.tycooncraft.blackmarket.Casino.RED));
        System.out.println("[AUTOTEST] v280 roulette spins=" + com.tycooncraft.blackmarket.Casino.spins + " paid=" + com.tycooncraft.blackmarket.Casino.paidOut);
    }

    static float dmg0;
    static int mag0;

    static void v28ModsOpen(ServerPlayer sp) {
        sp.teleportTo(sp.serverLevel(), 13.5, 64, -5.0, 180f, 20f);
        com.tycooncraft.blackmarket.Reputation.set(sp, 1500);
        ItemStack gun = new ItemStack(ModItems.PISTOL.get());
        sp.setItemInHand(InteractionHand.MAIN_HAND, gun);
        var gi = (com.tycooncraft.gun.GunItem) gun.getItem();
        dmg0 = gi.damage(gun);
        mag0 = gi.magazine(gun);
        com.tycooncraft.blackmarket.BMBlock.open(sp, com.tycooncraft.blackmarket.BMPayloads.MODS);
    }

    static void v28ModsInstall(ServerPlayer sp) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) sb.append(com.tycooncraft.blackmarket.IllegalMods.install(sp, i).getString()).append(" / ");
        String again = com.tycooncraft.blackmarket.IllegalMods.install(sp, 0).getString();
        ItemStack gun = sp.getMainHandItem();
        var gi = (com.tycooncraft.gun.GunItem) gun.getItem();
        System.out.println("[AUTOTEST] v280 mods " + sb + " again=" + again + " mask=" + com.tycooncraft.blackmarket.IllegalMods.mask(gun)
                + " dmg " + dmg0 + "->" + gi.damage(gun) + " mag " + mag0 + "->" + gi.magazine(gun) + " delay=" + gi.fireDelay(gun)
                + " scrub=" + com.tycooncraft.blackmarket.IllegalMods.has(gun, com.tycooncraft.blackmarket.IllegalMods.SCRUB));
        com.tycooncraft.blackmarket.BMPayloads.mods(sp, null);
    }

    static void v28Discount(ServerPlayer sp) {
        var bm = sp.serverLevel();
        var dl = bm.getEntitiesOfClass(com.tycooncraft.blackmarket.BlackMarketDealer.class, sp.getBoundingBox().inflate(40));
        if (dl.isEmpty()) {
            System.out.println("[AUTOTEST] v280 discount noDealer");
            return;
        }
        var d = dl.get(0);
        var o = d.getOffers().get(0);
        long w0 = com.tycooncraft.economy.Coins.wallet(sp);
        d.setTradingPlayer(sp);
        d.notifyTrade(o);
        d.setTradingPlayer(null);
        long won = com.tycooncraft.economy.Coins.valueOf(o.getCostA()) + com.tycooncraft.economy.Coins.valueOf(o.getCostB());
        System.out.println("[AUTOTEST] v280 discount tier=" + com.tycooncraft.blackmarket.Reputation.tier(sp) + " tradeWon=" + won + " rebate=" + (com.tycooncraft.economy.Coins.wallet(sp) - w0));
    }

    static void v28Delivery(ServerPlayer sp) {
        net.minecraft.server.level.ServerLevel ow = sp.server.overworld();
        sp.teleportTo(ow, owBase.getX() + 0.5, owBase.getY(), owBase.getZ() + 0.5, 0f, 10f);
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 1200, 4, false, false));
        bank0 = bank(sp);
        rep0 = com.tycooncraft.blackmarket.Reputation.points(sp);
        var m = com.tycooncraft.blackmarket.Missions.force(sp, com.tycooncraft.blackmarket.Missions.DELIVERY, sp.blockPosition().offset(30, 0, 0));
        ItemStack pkg = ItemStack.EMPTY;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) if (sp.getInventory().getItem(i).is(ModItems.SMUGGLE_PACKAGE.get())) pkg = sp.getInventory().getItem(i);
        boolean far = com.tycooncraft.blackmarket.Missions.deliver(sp, pkg);
        sp.teleportTo(ow, m.target.getX() + 1.5, owBase.getY(), m.target.getZ() + 0.5, 0f, 10f);
        boolean ok = com.tycooncraft.blackmarket.Missions.deliver(sp, pkg);
        System.out.println("[AUTOTEST] v280 delivery farRejected=" + !far + " delivered=" + ok + " reward=" + m.reward + " bankDelta=" + (bank(sp) - bank0)
                + " repDelta=" + (com.tycooncraft.blackmarket.Reputation.points(sp) - rep0) + " active=" + (com.tycooncraft.blackmarket.Missions.active(sp) != null));
    }

    static com.tycooncraft.blackmarket.Missions.M retr;
    static int ambush0;

    static void v28Retrieval(ServerPlayer sp) {
        ambush0 = com.tycooncraft.blackmarket.Missions.ambushes;
        retr = com.tycooncraft.blackmarket.Missions.force(sp, com.tycooncraft.blackmarket.Missions.RETRIEVAL, sp.blockPosition().offset(0, 0, 14));
        System.out.println("[AUTOTEST] v280 retrieval target=" + retr.target);
    }

    static void v28RetrievalCheck(ServerPlayer sp) {
        net.minecraft.server.level.ServerLevel ow = sp.server.overworld();
        var be = ow.getBlockEntity(retr.target);
        boolean barrel = be instanceof net.minecraft.world.level.block.entity.BarrelBlockEntity;
        ItemStack cargo = barrel ? ((net.minecraft.world.level.block.entity.BarrelBlockEntity) be).removeItem(13, 1) : ItemStack.EMPTY;
        sp.getInventory().add(cargo);
        int gang = ow.getEntitiesOfClass(com.tycooncraft.mob.GunnerEntity.class, sp.getBoundingBox().inflate(60)).size()
                + ow.getEntitiesOfClass(com.tycooncraft.mob.GangTruck.class, sp.getBoundingBox().inflate(60)).size();
        bank0 = bank(sp);
        String r = com.tycooncraft.blackmarket.Missions.turnIn(sp).getString();
        System.out.println("[AUTOTEST] v280 retrieval placed=" + retr.placed + " barrel=" + barrel + " cargo=" + cargo.getItem() + " ambushes=" + (com.tycooncraft.blackmarket.Missions.ambushes - ambush0)
                + " gangNear=" + gang + " turnIn=" + r + " bankDelta=" + (bank(sp) - bank0));
        // 포기 시험
        int rp = com.tycooncraft.blackmarket.Reputation.points(sp);
        com.tycooncraft.blackmarket.Missions.force(sp, com.tycooncraft.blackmarket.Missions.SUPPLY, null);
        com.tycooncraft.blackmarket.Missions.abandon(sp);
        System.out.println("[AUTOTEST] v280 abandon repDelta=" + (com.tycooncraft.blackmarket.Reputation.points(sp) - rp) + " completed=" + com.tycooncraft.blackmarket.Missions.completed
                + " failed=" + com.tycooncraft.blackmarket.Missions.failed);
        for (var m : ow.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, sp.getBoundingBox().inflate(80), m -> m.getTarget() == sp)) m.discard();
        for (var m : ow.getEntitiesOfClass(com.tycooncraft.mob.GangTruck.class, sp.getBoundingBox().inflate(80))) m.discard();
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
    }

    // ── v0.28.2 프로토-정찰 원거리(300) 원격 조종 ──
    static com.tycooncraft.mech.ProtoScout far;
    static Vec3 farBody;
    static int farTick0;

    static void farSetup(ServerPlayer sp) {
        var ow = sp.serverLevel();
        farBody = sp.position();
        far = com.tycooncraft.mech.ProtoDestroyerItem.spawnScout(ow, sp.blockPosition().offset(2, 0, 0), sp, new ItemStack(ModItems.PROTO_SCOUT.get()), 0f);
        far.interact(sp, InteractionHand.MAIN_HAND);
        System.out.println("[AUTOTEST] v282 far start piloted=" + (far.pilot() == sp) + " invul=" + sp.isInvulnerable()
                + " bodies=" + ow.getEntitiesOfClass(com.tycooncraft.mech.PilotBody.class, sp.getBoundingBox().inflate(8)).size());
    }

    static void farMove(ServerPlayer sp) {
        // 실제로는 걸어가며 조금씩 이동 — 테스트에선 한 번에 옮기고 조종자도 같이 붙임(매 틱 하는 일과 같음)
        far.teleportTo(farBody.x + 220, farBody.y + 2, farBody.z);
        sp.absMoveTo(far.getX(), far.getY(), far.getZ());
        sp.serverLevel().getChunkSource().move(sp);
        farTick0 = far.tickCount;
    }

    static void farWalk(ServerPlayer sp) {
        far.testInput(1f, 0f, -90f);
    }

    static void farCheck(ServerPlayer sp) {
        var bodies = sp.server.overworld().getEntitiesOfClass(com.tycooncraft.mech.PilotBody.class, new net.minecraft.world.phys.AABB(BlockPos.containing(farBody)).inflate(8));
        System.out.println("[AUTOTEST] v282 far ticking=" + (far.tickCount - farTick0) + " alive=" + far.isAlive() + " piloted=" + (far.pilot() == sp)
                + " bodyDist=" + String.format("%.0f", far.bodyDistance()) + " playerAtMech=" + String.format("%.1f", sp.position().distanceTo(far.position()))
                + " bodyEntity=" + bodies.size() + " invul=" + sp.isInvulnerable());
    }

    static void farHitBody(ServerPlayer sp) {
        var bodies = sp.server.overworld().getEntitiesOfClass(com.tycooncraft.mech.PilotBody.class, new net.minecraft.world.phys.AABB(BlockPos.containing(farBody)).inflate(8));
        float hp = sp.getHealth();
        if (!bodies.isEmpty()) bodies.get(0).hurt(sp.damageSources().generic(), 3f);
        System.out.println("[AUTOTEST] v282 bodyHit forwarded=" + com.tycooncraft.mech.PilotBody.hitsForwarded + " piloted=" + (far.pilot() == sp)
                + " backHome=" + String.format("%.1f", sp.position().distanceTo(farBody)) + " invul=" + sp.isInvulnerable()
                + " invis=" + sp.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY) + " hp " + hp + "->" + sp.getHealth()
                + " tag=" + sp.getPersistentData().contains(com.tycooncraft.mech.ProtoScout.PILOT_TAG));
    }

    static void farRelink(ServerPlayer sp) {
        far.startRemote(sp);
        far.teleportTo(farBody.x + 320, farBody.y + 2, farBody.z);
        sp.absMoveTo(far.getX(), far.getY(), far.getZ());
        sp.serverLevel().getChunkSource().move(sp);
    }

    static void farLost(ServerPlayer sp) {
        System.out.println("[AUTOTEST] v282 signalLost piloted=" + (far.pilot() == sp) + " backHome=" + String.format("%.1f", sp.position().distanceTo(farBody))
                + " invul=" + sp.isInvulnerable());
        far.discard();
    }

    // ── v0.28.3 조종석 1인칭 (낮·평지) ──
    static void cockpitSetup(ServerPlayer sp) {
        var ow = sp.server.overworld();
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        ow.setDayTime(6000);
        BlockPos p = owBase.offset(-30, 0, 0);
        proto = com.tycooncraft.mech.ProtoDestroyerItem.spawn(ow, p, sp, new ItemStack(ModItems.PROTO_DESTROYER.get()), 0f);
        sp.teleportTo(ow, p.getX() + 0.5, p.getY(), p.getZ() - 2.5, 0f, 0f);
        proto.interact(sp, InteractionHand.MAIN_HAND);
        sp.setYRot(0);
        sp.setXRot(5);
        protoTarget = EntityType.ZOMBIE.create(ow);
        protoTarget.moveTo(p.getX() + 2.5, p.getY(), p.getZ() + 14.5, 180, 0);
        protoTarget.setNoAi(true);
        protoTarget.setPersistenceRequired();
        protoTarget.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        protoTarget.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(400);
        protoTarget.setHealth(400);
        ow.addFreshEntity(protoTarget);
        System.out.println("[AUTOTEST] v283 cockpit riding=" + (sp.getVehicle() == proto));
    }

    static void cockpitDone(ServerPlayer sp) {
        sp.stopRiding();
        proto.discard();
        protoTarget.discard();
    }

    // ── v0.29 언더시티 ──
    static void ucEnter(ServerPlayer sp) {
        sp.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 6000, 4, false, false));
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.REGENERATION, 6000, 3, false, false));
        long t0 = System.currentTimeMillis();
        com.tycooncraft.blackmarket.BlackMarket.enter(sp, sp.blockPosition().below());
        long ms = System.currentTimeMillis() - t0;
        var bm = sp.serverLevel();
        System.out.println("[AUTOTEST] v290 undercity buildMs=" + ms + " hideouts=" + com.tycooncraft.blackmarket.Undercity.HIDEOUTS.size()
                + " chests=" + com.tycooncraft.blackmarket.Undercity.CHESTS.size() + " gate=" + bm.getBlockState(new BlockPos(-17, 65, 0)).isAir()
                + " floor=" + bm.getBlockState(new BlockPos(-60, 63, 0)).getBlock() + " ceil=" + bm.getBlockState(new BlockPos(-60, 100, 40)).getBlock()
                + " hubIntact=" + bm.getBlockState(com.tycooncraft.blackmarket.BlackMarketHub.MISSION).getBlock());
        sp.teleportTo(bm, -24.5, 64, 0.5, 90f, 0f);
    }

    static void ucPopulate(ServerPlayer sp) {
        var bm = sp.serverLevel();
        for (int i = 0; i < 10; i++) com.tycooncraft.blackmarket.Undercity.tick(bm);
        var gangs = bm.getEntitiesOfClass(com.tycooncraft.mob.GunnerEntity.class, sp.getBoundingBox().inflate(72, 40, 72), com.tycooncraft.blackmarket.Undercity::resident);
        java.util.Map<String, Integer> kinds = new java.util.TreeMap<>();
        for (var g : gangs) kinds.merge(g.kind().name(), 1, Integer::sum);
        int inHub = 0;
        for (var g : gangs) if (com.tycooncraft.blackmarket.Undercity.inHub(g.getX(), g.getZ())) inHub++;
        double minGate = 999;
        for (var g : gangs) minGate = Math.min(minGate, Math.sqrt(g.blockPosition().distSqr(com.tycooncraft.blackmarket.Undercity.GATE)));
        System.out.println("[AUTOTEST] v291 minDistFromGate=" + String.format("%.0f", minGate));
        System.out.println("[AUTOTEST] v290 residents=" + gangs.size() + " kinds=" + kinds + " inHub=" + inHub + " spawned=" + com.tycooncraft.blackmarket.Undercity.spawned);
    }

    static com.tycooncraft.mob.GunnerEntity intruder;

    static void ucIntruder(ServerPlayer sp) {
        intruder = com.tycooncraft.blackmarket.Undercity.spawnAt(sp.serverLevel(), ModEntities.GANGSTER.get(), new BlockPos(0, 64, 0));
    }

    static void ucIntruderCheck(ServerPlayer sp) {
        com.tycooncraft.blackmarket.Undercity.tick(sp.serverLevel());
        System.out.println("[AUTOTEST] v290 dbg intruderReason=" + intruder.getRemovalReason() + " alive=" + intruder.isAlive() + " hp=" + intruder.getHealth()
                + " diff=" + sp.serverLevel().getDifficulty() + " lastDmg=" + (intruder.getLastDamageSource() == null ? "-" : intruder.getLastDamageSource().getMsgId())
                + " player=" + sp.blockPosition() + " dim=" + sp.level().dimension().location() + " ticks=" + intruder.tickCount);
        System.out.println("[AUTOTEST] v290 intruder expelled=" + !com.tycooncraft.blackmarket.Undercity.inHub(intruder.getX(), intruder.getZ())
                + " pos=" + intruder.blockPosition());
    }

    static void ucChest(ServerPlayer sp) {
        var bm = sp.serverLevel();
        BlockPos c = com.tycooncraft.blackmarket.Undercity.CHESTS.get(0);
        sp.teleportTo(bm, c.getX() + 0.5, c.getY(), c.getZ() - 1.5, 0f, 30f);
        var be = (com.tycooncraft.blackmarket.LockedChestBlockEntity) bm.getBlockEntity(c);
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) if (sp.getInventory().getItem(i).is(ModItems.UNDERCITY_KEY.get())) sp.getInventory().setItem(i, ItemStack.EMPTY);
        boolean noKey = com.tycooncraft.blackmarket.LockedChestBlock.tryOpen(sp, be);
        sp.getInventory().add(new ItemStack(ModItems.UNDERCITY_KEY.get(), 1));
        int filled = be.filled();
        boolean withKey = com.tycooncraft.blackmarket.LockedChestBlock.tryOpen(sp, be);
        int keysLeft = sp.getInventory().countItem(ModItems.UNDERCITY_KEY.get());
        boolean stateOpen = !bm.getBlockState(c).getValue(com.tycooncraft.blackmarket.LockedChestBlock.LOCKED);
        sp.closeContainer();
        boolean again = com.tycooncraft.blackmarket.LockedChestBlock.tryOpen(sp, be);   // 하루 안에는 열쇠 없이 다시 열림
        sp.closeContainer();
        for (int i = 0; i < 27; i++) be.setItem(i, ItemStack.EMPTY);
        // 하루 지난 것처럼
        var tag = be.saveWithoutMetadata(bm.registryAccess());
        tag.putLong("OpenedAt", bm.getGameTime() - com.tycooncraft.blackmarket.LockedChestBlockEntity.RESTOCK - 5);
        be.loadWithComponents(tag, bm.registryAccess());
        boolean afterDay = com.tycooncraft.blackmarket.LockedChestBlock.tryOpen(sp, be);
        sp.closeContainer();
        System.out.println("[AUTOTEST] v290 chest noKeyOpened=" + noKey + " lootStacks=" + filled + " withKeyOpened=" + withKey + " keysLeft=" + keysLeft
                + " stateOpen=" + stateOpen + " reopenSameDay=" + again + " afterDayOpened=" + afterDay + " relocked=" + be.locked() + " refilled=" + be.filled()
                + " unlocks=" + com.tycooncraft.blackmarket.LockedChestBlock.unlocks + " denied=" + com.tycooncraft.blackmarket.LockedChestBlock.denied);
    }

    static void ucKeyDrops(ServerPlayer sp) {
        var bm = sp.serverLevel();
        BlockPos at = sp.blockPosition();
        int kills0 = com.tycooncraft.blackmarket.Undercity.kills;
        for (int i = 0; i < 12; i++) {
            var type = i % 3 == 0 ? ModEntities.ENFORCER.get() : i % 3 == 1 ? ModEntities.BRUTE.get() : ModEntities.MARKSMAN.get();
            var g = com.tycooncraft.blackmarket.Undercity.spawnAt(bm, type, at.offset(2, 0, 2));
            g.setNoAi(true);
            g.hurt(sp.damageSources().playerAttack(sp), 9999f);
        }
        int keys = 0;
        for (var it : bm.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, sp.getBoundingBox().inflate(8)))
            if (it.getItem().is(ModItems.UNDERCITY_KEY.get())) keys += it.getItem().getCount();
        System.out.println("[AUTOTEST] v290 variantKills=" + (com.tycooncraft.blackmarket.Undercity.kills - kills0) + " keysDropped=" + keys + " hostile=" + com.tycooncraft.blackmarket.BlackMarket.hostile(sp));
        for (var it : bm.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, sp.getBoundingBox().inflate(8))) it.discard();
    }

    static void ucHideoutView(ServerPlayer sp) {
        var bm = sp.serverLevel();
        BlockPos h = com.tycooncraft.blackmarket.Undercity.HIDEOUTS.get(0);
        sp.teleportTo(bm, h.getX() + 0.5, h.getY() + 1, h.getZ() - 9.5, 0f, -8f);
        for (var g : bm.getEntitiesOfClass(com.tycooncraft.mob.GunnerEntity.class, sp.getBoundingBox().inflate(40))) g.setNoAi(true);
    }

    static void ucLineup(ServerPlayer sp) {
        var bm = sp.serverLevel();
        sp.teleportTo(bm, -30.5, 64, 0.5, -90f, 0f);
        var types = java.util.List.of(ModEntities.ENFORCER.get(), ModEntities.BRUTE.get(), ModEntities.MARKSMAN.get());
        for (int i = 0; i < 3; i++) {
            var g = com.tycooncraft.blackmarket.Undercity.spawnAt(bm, types.get(i), new BlockPos(-26, 64, -2 + i * 2));
            g.setNoAi(true);
            g.setYRot(90f);
            g.setYBodyRot(90f);
            g.setYHeadRot(90f);
        }
    }

    static com.tycooncraft.mob.GunnerEntity sniper;
    static float sniperHp0;

    static void msSetup(ServerPlayer sp) {
        var bm = sp.serverLevel();
        for (var g : bm.getEntitiesOfClass(com.tycooncraft.mob.GunnerEntity.class, sp.getBoundingBox().inflate(200))) g.discard();
        sp.teleportTo(bm, -40.5, 64, 0.5, 90f, 0f);
        sniper = com.tycooncraft.blackmarket.Undercity.spawnAt(bm, ModEntities.MARKSMAN.get(), new BlockPos(-58, 64, 3));
        sniper.setTarget(sp);
        System.out.println("[AUTOTEST] v291 balance enforcerDps=" + String.format("%.1f", 2.5f * 20 / 15) + " bruteBurst=" + (6 * 3.0f)
                + " localCap=" + com.tycooncraft.blackmarket.Undercity.LOCAL_CAP + " gateSafe=" + com.tycooncraft.blackmarket.Undercity.GATE_SAFE);
    }

    static void msAim(ServerPlayer sp) {
        sniperHp0 = sp.getHealth();
        sniper.performRangedAttack(sp, 1f);
    }

    static void msCheck(ServerPlayer sp) {
        System.out.println("[AUTOTEST] v291 sniper aimsStarted=" + com.tycooncraft.mob.GunnerEntity.aimsStarted + " aimShots=" + com.tycooncraft.mob.GunnerEntity.aimShots
                + " aimNow=" + sniper.aimTarget() + " hp " + sniperHp0 + "->" + sp.getHealth());
        sniper.discard();
    }

    static void ucLeave(ServerPlayer sp) {
        var bm = sp.serverLevel();
        for (var g : bm.getEntitiesOfClass(com.tycooncraft.mob.GunnerEntity.class, sp.getBoundingBox().inflate(200))) g.discard();
        com.tycooncraft.blackmarket.BlackMarket.leave(sp);
        sp.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
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
