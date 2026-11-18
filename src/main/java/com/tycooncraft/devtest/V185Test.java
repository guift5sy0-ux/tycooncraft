package com.tycooncraft.devtest;

import com.tycooncraft.alliance.AllianceData;
import com.tycooncraft.alliance.Alliances;
import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.PriceTable;
import com.tycooncraft.mob.DestroyerGolem;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.turret.TurretAI;
import com.tycooncraft.turret.TurretBlockEntity;
import net.minecraft.core.BlockPos;
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
    static net.minecraft.world.entity.Mob gang, queen;

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
                + " queenTarget=" + (queen == null || queen.getTarget() == null ? "none" : queen.getTarget().getType().toShortString()));
        if (gang != null) gang.discard();
        if (queen != null) queen.discard();
        dummy.discard();
        if (golem != null) golem.discard();
        sp.serverLevel().setBlockAndUpdate(turret, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
    }

    private V185Test() {}
}
