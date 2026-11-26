package com.tycooncraft.event;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.tycooncraft.economy.Coins;
import com.tycooncraft.economy.MarketData;
import com.tycooncraft.economy.PriceTable;
import com.tycooncraft.world.StructureBuilder;
import com.tycooncraft.world.WorldGen;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * /sell, /sell all, /price, /prices, /guide, /starter
 * /tycoon village|ruin|nearest (관리자)
 */
public final class TycoonCommands {

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();

        d.register(Commands.literal("sell")
                .executes(TycoonCommands::sellHand)
                .then(Commands.literal("all").executes(TycoonCommands::sellAll)));
        d.register(Commands.literal("price").executes(TycoonCommands::price));
        d.register(Commands.literal("prices").executes(TycoonCommands::prices));
        d.register(Commands.literal("guide").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Coins.giveStack(p, StarterKit.guideBook());
            return 1;
        }));
        d.register(Commands.literal("starter").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (StarterKit.received(p)) {
                p.sendSystemMessage(Component.translatable("message.tycooncraft.starter.already").withStyle(ChatFormatting.GOLD));
                return 0;
            }
            StarterKit.give(p);
            return 1;
        }));

        d.register(Commands.literal("tycoon")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("village").executes(ctx -> build(ctx, "village")))
                .then(Commands.literal("ruin").executes(ctx -> build(ctx, "ruin")))
                .then(Commands.literal("nearest").executes(TycoonCommands::nearest)));
    }

    private static int sellHand(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ItemStack stack = p.getMainHandItem();
        if (stack.isEmpty()) {
            p.sendSystemMessage(Component.translatable("message.tycooncraft.sell.hint").withStyle(ChatFormatting.GOLD));
            return 0;
        }
        if (!MarketData.canSell(stack)) {
            p.sendSystemMessage(Component.translatable("message.tycooncraft.not_sellable", stack.getHoverName()).withStyle(ChatFormatting.GOLD));
            return 0;
        }
        ItemStack selling = stack.copy();
        double earned = MarketData.get(p.server).sell(selling);
        stack.setCount(0);
        Coins.give(p, (long) Math.floor(earned + 1e-9));
        p.sendSystemMessage(Component.translatable("message.tycooncraft.sold",
                selling.getHoverName(), selling.getCount(), Coins.format(earned)).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int sellAll(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        Inventory inv = p.getInventory();
        MarketData market = MarketData.get(p.server);
        double total = 0;
        int kinds = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty() || Coins.isCoin(stack) || !MarketData.canSell(stack)) continue;
            total += market.sell(stack.copy());
            inv.setItem(i, ItemStack.EMPTY);
            kinds++;
        }
        if (kinds == 0) {
            p.sendSystemMessage(Component.translatable("message.tycooncraft.sell.nothing").withStyle(ChatFormatting.GOLD));
            return 0;
        }
        Coins.give(p, (long) Math.floor(total + 1e-9));
        p.sendSystemMessage(Component.translatable("message.tycooncraft.sold_all", kinds, Coins.format(total)).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int price(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        ItemStack stack = p.getMainHandItem();
        double base = PriceTable.basePrice(stack.getItem());
        if (stack.isEmpty() || base <= 0) {
            p.sendSystemMessage(Component.translatable("message.tycooncraft.price.hint").withStyle(ChatFormatting.GOLD));
            return 0;
        }
        double now = MarketData.get(p.server).currentPrice(stack.getItem());
        p.sendSystemMessage(Component.translatable("message.tycooncraft.price", stack.getHoverName(),
                Coins.format(now), Coins.format(base), Math.round(now / base * 100)).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int prices(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        MarketData market = MarketData.get(p.server);
        List<Map.Entry<Item, Double>> entries = new ArrayList<>(PriceTable.all().entrySet());
        entries.sort(Map.Entry.comparingByValue());
        p.sendSystemMessage(Component.translatable("message.tycooncraft.prices.header").withStyle(ChatFormatting.GOLD));
        for (Map.Entry<Item, Double> e : entries) {
            p.sendSystemMessage(Component.literal(" ").append(e.getKey().getDescription())
                    .append(": ₩" + Coins.format(market.currentPrice(e.getKey()))));
        }
        return 1;
    }

    private static int build(CommandContext<CommandSourceStack> ctx, String type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        BlockPos pos = p.blockPosition();
        switch (type) {
            case "village" -> StructureBuilder.village(p.serverLevel(), pos.offset(4, 0, -16));
            default -> StructureBuilder.ruin(p.serverLevel(), pos.offset(2, 0, -7));
        }
        p.sendSystemMessage(Component.translatable("message.tycooncraft.built." + type).withStyle(ChatFormatting.GOLD));
        return 1;
    }

    private static int nearest(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = ctx.getSource().getPlayerOrException();
        for (String type : new String[]{"village", "ruin"}) {
            BlockPos found = WorldGen.nearest(p.serverLevel(), type, p.blockPosition());
            Component where = found == null
                    ? Component.translatable("message.tycooncraft.nearest.none")
                    : Component.literal("X " + found.getX() + ", Z " + found.getZ() + " ("
                    + (int) Math.sqrt(found.distSqr(p.blockPosition().atY(found.getY()))) + ")");
            p.sendSystemMessage(Component.translatable("message.tycooncraft.nearest." + type, where).withStyle(ChatFormatting.GOLD));
        }
        return 1;
    }

    private TycoonCommands() {}
}
