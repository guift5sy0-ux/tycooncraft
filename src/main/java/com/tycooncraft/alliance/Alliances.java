package com.tycooncraft.alliance;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 동맹 시스템. 같은 동맹끼리는 포탑이 쏘지 않고, 요격 포대도 서로의 로켓·드론을 요격하지 않는다.
 * 창설비 ₩2,000 (통장에서), 최대 10명. /alliance (= /동맹)
 */
public final class Alliances {
    public static final long CREATE_COST = 2_000;
    public static final int MAX_MEMBERS = 10;

    public record Result(boolean ok, Component msg) {
        static Result ok(String key, Object... a) {
            return new Result(true, Component.translatable("alliance.tycooncraft." + key, a));
        }

        static Result fail(String key, Object... a) {
            return new Result(false, Component.translatable("alliance.tycooncraft." + key, a));
        }
    }

    /** 같은 편인가: 본인, 같은 동맹, (기존) 스코어보드 팀 */
    public static boolean friendly(MinecraftServer server, UUID a, UUID b) {
        if (a == null || b == null) return false;
        if (a.equals(b)) return true;
        AllianceData d = AllianceData.get(server);
        String x = d.memberOf.get(a), y = d.memberOf.get(b);
        if (x != null && x.equals(y)) return true;
        Player pa = server.getPlayerList().getPlayer(a), pb = server.getPlayerList().getPlayer(b);
        return pa != null && pb != null && pa.isAlliedTo(pb);
    }

    static void touch(AllianceData d, ServerPlayer p) {
        d.names.put(p.getUUID(), p.getGameProfile().getName());
        d.setDirty();
    }

    private static UUID find(MinecraftServer server, AllianceData d, String name) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) return online.getUUID();
        for (Map.Entry<UUID, String> e : d.names.entrySet()) if (e.getValue().equalsIgnoreCase(name)) return e.getKey();
        return null;
    }

    private static void tell(MinecraftServer server, AllianceData.Alliance a, Component msg) {
        for (UUID m : a.members) {
            ServerPlayer p = server.getPlayerList().getPlayer(m);
            if (p != null) p.sendSystemMessage(msg.copy().withStyle(ChatFormatting.AQUA));
        }
    }

    public static Result create(ServerPlayer p, String name) {
        AllianceData d = AllianceData.get(p.server);
        touch(d, p);
        name = name == null ? "" : name.trim();
        if (d.of(p.getUUID()) != null) return Result.fail("already_member");
        if (name.length() < 2 || name.length() > 16 || !name.matches("[0-9A-Za-z_가-힣]+")) return Result.fail("bad_name");
        for (String n : d.alliances.keySet()) if (n.equalsIgnoreCase(name)) return Result.fail("name_taken", name);
        BankData bank = BankData.get(p.server);
        var acc = bank.account(p.getUUID());
        if (acc.balance < CREATE_COST) return Result.fail("no_money", Coins.format(CREATE_COST));
        acc.balance -= CREATE_COST;
        bank.log(p.getUUID(), "fee", CREATE_COST, "");
        AllianceData.Alliance a = new AllianceData.Alliance();
        a.name = name;
        a.leader = p.getUUID();
        a.members.add(p.getUUID());
        d.alliances.put(name, a);
        d.memberOf.put(p.getUUID(), name);
        d.invites.remove(p.getUUID());
        d.setDirty();
        return Result.ok("created", name, Coins.format(CREATE_COST));
    }

    public static Result invite(ServerPlayer p, String target) {
        AllianceData d = AllianceData.get(p.server);
        touch(d, p);
        AllianceData.Alliance a = d.of(p.getUUID());
        if (a == null) return Result.fail("not_member");
        if (!a.leader.equals(p.getUUID())) return Result.fail("not_leader");
        if (a.members.size() >= MAX_MEMBERS) return Result.fail("full", MAX_MEMBERS);
        UUID t = find(p.server, d, target == null ? "" : target.trim());
        if (t == null) return Result.fail("unknown_player", target);
        if (t.equals(p.getUUID())) return Result.fail("self");
        if (d.of(t) != null) return Result.fail("target_in_alliance", d.nameOf(t));
        d.invites.computeIfAbsent(t, k -> new LinkedHashSet<>()).add(a.name);
        d.setDirty();
        ServerPlayer tp = p.server.getPlayerList().getPlayer(t);
        if (tp != null) tp.sendSystemMessage(Component.translatable("alliance.tycooncraft.invited_you", p.getGameProfile().getName(), a.name).withStyle(ChatFormatting.AQUA));
        return Result.ok("invite_sent", d.nameOf(t));
    }

    public static Result accept(ServerPlayer p, String name) {
        AllianceData d = AllianceData.get(p.server);
        touch(d, p);
        if (d.of(p.getUUID()) != null) return Result.fail("already_member");
        Set<String> inv = d.invites.getOrDefault(p.getUUID(), Set.of());
        String match = null;
        for (String n : inv) if (n.equalsIgnoreCase(name == null ? "" : name.trim())) match = n;
        if (match == null && inv.size() == 1 && (name == null || name.isBlank())) match = inv.iterator().next();
        AllianceData.Alliance a = match == null ? null : d.alliances.get(match);
        if (a == null) return Result.fail("no_invite");
        if (a.members.size() >= MAX_MEMBERS) return Result.fail("full", MAX_MEMBERS);
        a.members.add(p.getUUID());
        d.memberOf.put(p.getUUID(), a.name);
        d.invites.remove(p.getUUID());
        d.setDirty();
        tell(p.server, a, Component.translatable("alliance.tycooncraft.joined_msg", p.getGameProfile().getName(), a.name));
        return Result.ok("joined", a.name);
    }

    public static Result decline(ServerPlayer p, String name) {
        AllianceData d = AllianceData.get(p.server);
        Set<String> inv = d.invites.get(p.getUUID());
        if (inv == null || !inv.removeIf(n -> n.equalsIgnoreCase(name == null ? "" : name.trim()))) return Result.fail("no_invite");
        d.setDirty();
        return Result.ok("declined", name);
    }

    public static Result leave(ServerPlayer p) {
        AllianceData d = AllianceData.get(p.server);
        AllianceData.Alliance a = d.of(p.getUUID());
        if (a == null) return Result.fail("not_member");
        a.members.remove(p.getUUID());
        d.memberOf.remove(p.getUUID());
        if (a.members.isEmpty()) {
            d.alliances.remove(a.name);
            d.setDirty();
            return Result.ok("disbanded", a.name);
        }
        if (a.leader.equals(p.getUUID())) a.leader = a.members.iterator().next();   // 맹주 승계
        d.setDirty();
        tell(p.server, a, Component.translatable("alliance.tycooncraft.left_msg", p.getGameProfile().getName(), d.nameOf(a.leader)));
        return Result.ok("left", a.name);
    }

    public static Result kick(ServerPlayer p, String target) {
        AllianceData d = AllianceData.get(p.server);
        AllianceData.Alliance a = d.of(p.getUUID());
        if (a == null) return Result.fail("not_member");
        if (!a.leader.equals(p.getUUID())) return Result.fail("not_leader");
        UUID t = find(p.server, d, target == null ? "" : target.trim());
        if (t == null || !a.members.contains(t)) return Result.fail("not_in_yours", target);
        if (t.equals(p.getUUID())) return Result.fail("self");
        a.members.remove(t);
        d.memberOf.remove(t);
        d.setDirty();
        ServerPlayer tp = p.server.getPlayerList().getPlayer(t);
        if (tp != null) tp.sendSystemMessage(Component.translatable("alliance.tycooncraft.kicked_you", a.name).withStyle(ChatFormatting.RED));
        return Result.ok("kicked", d.nameOf(t));
    }

    public static Result disband(ServerPlayer p) {
        AllianceData d = AllianceData.get(p.server);
        AllianceData.Alliance a = d.of(p.getUUID());
        if (a == null) return Result.fail("not_member");
        if (!a.leader.equals(p.getUUID())) return Result.fail("not_leader");
        tell(p.server, a, Component.translatable("alliance.tycooncraft.disbanded_msg", a.name));
        for (UUID m : a.members) d.memberOf.remove(m);
        d.alliances.remove(a.name);
        d.setDirty();
        return Result.ok("disbanded", a.name);
    }

    // ── 명령어 ──
    private static int run(CommandContext<CommandSourceStack> c, java.util.function.Function<ServerPlayer, Result> f) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return 0;
        Result r = f.apply(p);
        p.sendSystemMessage(r.msg().copy().withStyle(r.ok() ? ChatFormatting.GREEN : ChatFormatting.RED));
        return r.ok() ? 1 : 0;
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        ServerPlayer p = c.getSource().getPlayer();
        if (p == null) return 0;
        AllianceData d = AllianceData.get(p.server);
        AllianceData.Alliance a = d.of(p.getUUID());
        if (a == null) {
            p.sendSystemMessage(Component.translatable("alliance.tycooncraft.none", String.join(", ", d.invites.getOrDefault(p.getUUID(), Set.of()))).withStyle(ChatFormatting.GRAY));
            return 1;
        }
        StringBuilder b = new StringBuilder();
        for (UUID m : a.members) b.append(b.isEmpty() ? "" : ", ").append(d.nameOf(m)).append(m.equals(a.leader) ? "★" : "");
        p.sendSystemMessage(Component.translatable("alliance.tycooncraft.info", a.name, a.members.size(), MAX_MEMBERS, b.toString()).withStyle(ChatFormatting.AQUA));
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tree(String root) {
        return Commands.literal(root)
                .executes(Alliances::info)
                .then(Commands.literal("info").executes(Alliances::info))
                .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> run(c, p -> create(p, StringArgumentType.getString(c, "name"))))))
                .then(Commands.literal("invite").then(Commands.argument("player", StringArgumentType.word())
                        .executes(c -> run(c, p -> invite(p, StringArgumentType.getString(c, "player"))))))
                .then(Commands.literal("accept").executes(c -> run(c, p -> accept(p, "")))
                        .then(Commands.argument("name", StringArgumentType.word()).executes(c -> run(c, p -> accept(p, StringArgumentType.getString(c, "name"))))))
                .then(Commands.literal("decline").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> run(c, p -> decline(p, StringArgumentType.getString(c, "name"))))))
                .then(Commands.literal("leave").executes(c -> run(c, Alliances::leave)))
                .then(Commands.literal("kick").then(Commands.argument("player", StringArgumentType.word())
                        .executes(c -> run(c, p -> kick(p, StringArgumentType.getString(c, "player"))))))
                .then(Commands.literal("disband").executes(c -> run(c, Alliances::disband)));
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(tree("alliance"));
        d.register(tree("동맹"));
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        AllianceData d = AllianceData.get(p.server);
        touch(d, p);
        Set<String> inv = d.invites.getOrDefault(p.getUUID(), Set.of());
        if (!inv.isEmpty()) p.sendSystemMessage(Component.translatable("alliance.tycooncraft.pending", String.join(", ", inv)).withStyle(ChatFormatting.AQUA));
    }

    private Alliances() {}
}
