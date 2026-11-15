package com.tycooncraft.event;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.economy.Coins;
import com.tycooncraft.economy.MarketData;
import com.tycooncraft.economy.PriceTable;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.world.WorldGen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;

public final class TycoonEvents {
    private static final int MARKET_RECOVERY_INTERVAL = 1200;

    /** 첫 접속 시 스타터 키트 */
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide && !StarterKit.received(player)) {
            StarterKit.give(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server.getTickCount() % MARKET_RECOVERY_INTERVAL == 0) {
            MarketData.get(server).recover();
        }
        WorldGen.tick(server);
    }

    /** 플레이어가 기계를 설치한 곳은 "기지"로 기록 → 근처에 구조물이 생기지 않음 */
    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof Player)) return;
        var key = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (TycoonCraft.MODID.equals(key.getNamespace())) {
            WorldGen.markBase(level, event.getPos());
        }
    }

    // ── 몹 코인 드랍 ──
    private static final Map<EntityType<?>, Object[]> BOSS_DROPS = Map.of(
            EntityType.WITHER, new Object[]{1_000, 2, 4},
            EntityType.ENDER_DRAGON, new Object[]{1_000, 5, 8},
            EntityType.WARDEN, new Object[]{1_000, 1, 2},
            EntityType.ELDER_GUARDIAN, new Object[]{100, 3, 6},
            EntityType.EVOKER, new Object[]{100, 1, 2},
            EntityType.RAVAGER, new Object[]{100, 1, 2});

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(event.getSource().getEntity() instanceof Player)) return;
        var random = entity.getRandom();

        Object[] boss = BOSS_DROPS.get(entity.getType());
        if (boss != null) {
            int count = (int) boss[1] + random.nextInt((int) boss[2] - (int) boss[1] + 1);
            entity.spawnAtLocation(new ItemStack(Coins.coinFor((int) boss[0]), count));
            return;
        }
        if (!(entity instanceof Enemy)) return;

        double r = random.nextDouble();
        Item coin = null;
        int count = 0;
        if (r < 0.01) { coin = ModItems.COIN_GOLD.get(); count = 1; }
        else if (r < 0.09) { coin = ModItems.COIN_IRON.get(); count = 1 + random.nextInt(2); }
        else if (r < 0.44) { coin = ModItems.COIN_COPPER.get(); count = 1 + random.nextInt(6); }
        if (coin != null) entity.spawnAtLocation(new ItemStack(coin, count));
    }

    /** 판매 가능한 아이템에 기준가 표시 */
    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        double base = PriceTable.basePrice(event.getItemStack().getItem());
        if (base > 0) {
            event.getToolTip().add(Component.translatable("tooltip.tycooncraft.base_price", Coins.format(base))
                    .withStyle(ChatFormatting.GOLD));
        }
    }

    private TycoonEvents() {}
}
