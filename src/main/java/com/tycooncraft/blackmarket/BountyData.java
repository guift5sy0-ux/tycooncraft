package com.tycooncraft.blackmarket;

import com.tycooncraft.economy.BankData;
import com.tycooncraft.economy.Coins;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** v0.26.0 현상금: 건 사람은 익명, 대상을 처치한 다른 플레이어가 받음, 7일 지나면 건 사람에게 돌려줌 */
public class BountyData extends SavedData {
    public static final long MIN = 1_000, EXPIRE_MS = 7L * 24 * 60 * 60 * 1000;

    public static final class Bounty {
        public UUID target, placer;
        public String targetName;
        public long amount, expires;
    }

    public final List<Bounty> list = new ArrayList<>();

    public static BountyData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(BountyData::new, BountyData::load), "tycooncraft_bounties");
    }

    public long totalOn(UUID target) {
        long t = 0;
        for (Bounty b : list) if (b.target.equals(target)) t += b.amount;
        return t;
    }

    /** 대상별 합계 (게시판 표시용, 큰 순) */
    public List<Map.Entry<String, Long>> board() {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Bounty b : list) m.merge(b.targetName, b.amount, Long::sum);
        List<Map.Entry<String, Long>> out = new ArrayList<>(m.entrySet());
        out.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));
        return out;
    }

    public static Component post(ServerPlayer placer, String targetName, long amount) {
        MinecraftServer s = placer.server;
        if (amount < MIN) return Component.translatable("message.tycooncraft.bounty.min", Coins.format(MIN));
        var prof = s.getProfileCache() == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty() : s.getProfileCache().get(targetName);
        ServerPlayer online = s.getPlayerList().getPlayerByName(targetName);
        UUID target = online != null ? online.getUUID() : prof.map(com.mojang.authlib.GameProfile::getId).orElse(null);
        // 이 서버에 한 번이라도 들어온 사람만 (오타로 돈이 묶이지 않게)
        if (online == null && target != null && !java.nio.file.Files.exists(s.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(target + ".dat")))
            target = null;
        if (target == null) return Component.translatable("message.tycooncraft.bounty.no_player", targetName);
        if (target.equals(placer.getUUID())) return Component.translatable("message.tycooncraft.bounty.self");
        var acc = BankData.get(s).account(placer.getUUID());
        if (acc.balance < amount) return Component.translatable("message.tycooncraft.bounty.no_money", Coins.format(acc.balance));
        acc.balance -= amount;
        BankData.get(s).setDirty();
        BountyData d = get(s);
        Bounty b = new Bounty();
        b.target = target;
        b.placer = placer.getUUID();
        b.targetName = online != null ? online.getGameProfile().getName() : targetName;
        b.amount = amount;
        b.expires = System.currentTimeMillis() + EXPIRE_MS;
        d.list.add(b);
        d.setDirty();
        posted++;
        if (online != null) online.sendSystemMessage(Component.translatable("message.tycooncraft.bounty.on_you", Coins.format(d.totalOn(target))).withStyle(ChatFormatting.DARK_RED));
        return Component.translatable("message.tycooncraft.bounty.posted", b.targetName, Coins.format(amount));
    }

    public static int posted, paid;

    /** 대상이 다른 플레이어에게 죽음 → 현상금 전부 지급 */
    public static long claim(ServerPlayer victim, ServerPlayer killer) {
        MinecraftServer s = victim.server;
        BountyData d = get(s);
        long sum = 0;
        for (var it = d.list.iterator(); it.hasNext(); ) {
            Bounty b = it.next();
            if (!b.target.equals(victim.getUUID()) || b.placer.equals(killer.getUUID())) continue;
            sum += b.amount;
            it.remove();
        }
        if (sum <= 0) return 0;
        d.setDirty();
        BankData.get(s).account(killer.getUUID()).balance += sum;
        BankData.get(s).setDirty();
        paid++;
        s.getPlayerList().broadcastSystemMessage(Component.translatable("message.tycooncraft.bounty.claimed", killer.getGameProfile().getName(),
                victim.getGameProfile().getName(), Coins.format(sum)).withStyle(ChatFormatting.GOLD), false);
        return sum;
    }

    /** 기한 지난 현상금은 건 사람 은행으로 돌려줌 */
    public static void expire(MinecraftServer s) {
        BountyData d = get(s);
        long now = System.currentTimeMillis();
        boolean ch = false;
        for (var it = d.list.iterator(); it.hasNext(); ) {
            Bounty b = it.next();
            if (b.expires > now) continue;
            BankData.get(s).account(b.placer).balance += b.amount;
            it.remove();
            ch = true;
        }
        if (ch) {
            d.setDirty();
            BankData.get(s).setDirty();
        }
    }

    static BountyData load(CompoundTag t, HolderLookup.Provider r) {
        BountyData d = new BountyData();
        for (Tag x : t.getList("list", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) x;
            Bounty b = new Bounty();
            b.target = c.getUUID("t");
            b.placer = c.getUUID("p");
            b.targetName = c.getString("n");
            b.amount = c.getLong("a");
            b.expires = c.getLong("e");
            d.list.add(b);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        ListTag l = new ListTag();
        for (Bounty b : list) {
            CompoundTag c = new CompoundTag();
            c.putUUID("t", b.target);
            c.putUUID("p", b.placer);
            c.putString("n", b.targetName);
            c.putLong("a", b.amount);
            c.putLong("e", b.expires);
            l.add(c);
        }
        t.put("list", l);
        return t;
    }
}
