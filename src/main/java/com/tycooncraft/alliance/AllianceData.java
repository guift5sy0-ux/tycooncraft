package com.tycooncraft.alliance;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 동맹: 이름 → (맹주, 구성원), 플레이어 → 소속 동맹, 받은 초대 */
public class AllianceData extends SavedData {
    private static final String NAME = "tycooncraft_alliances";

    public static final class Alliance {
        public String name;
        public UUID leader;
        public final Set<UUID> members = new LinkedHashSet<>();
    }

    public final Map<String, Alliance> alliances = new LinkedHashMap<>();
    public final Map<UUID, String> memberOf = new HashMap<>();
    public final Map<UUID, Set<String>> invites = new HashMap<>();
    public final Map<UUID, String> names = new HashMap<>();

    public static AllianceData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(AllianceData::new, AllianceData::load, null), NAME);
    }

    public Alliance of(UUID player) {
        String n = memberOf.get(player);
        return n == null ? null : alliances.get(n);
    }

    public String nameOf(UUID id) {
        return names.getOrDefault(id, id.toString().substring(0, 8));
    }

    private static AllianceData load(CompoundTag tag, HolderLookup.Provider reg) {
        AllianceData d = new AllianceData();
        for (Tag t : tag.getList("alliances", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) t;
            Alliance a = new Alliance();
            a.name = c.getString("name");
            a.leader = c.getUUID("leader");
            for (Tag m : c.getList("members", Tag.TAG_STRING)) a.members.add(UUID.fromString(m.getAsString()));
            d.alliances.put(a.name, a);
            for (UUID m : a.members) d.memberOf.put(m, a.name);
        }
        CompoundTag inv = tag.getCompound("invites");
        for (String k : inv.getAllKeys()) {
            Set<String> s = new LinkedHashSet<>();
            for (Tag x : inv.getList(k, Tag.TAG_STRING)) s.add(x.getAsString());
            d.invites.put(UUID.fromString(k), s);
        }
        CompoundTag nm = tag.getCompound("names");
        for (String k : nm.getAllKeys()) d.names.put(UUID.fromString(k), nm.getString(k));
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider reg) {
        ListTag list = new ListTag();
        for (Alliance a : alliances.values()) {
            CompoundTag c = new CompoundTag();
            c.putString("name", a.name);
            c.putUUID("leader", a.leader);
            ListTag m = new ListTag();
            for (UUID u : a.members) m.add(StringTag.valueOf(u.toString()));
            c.put("members", m);
            list.add(c);
        }
        tag.put("alliances", list);
        CompoundTag inv = new CompoundTag();
        invites.forEach((k, v) -> {
            ListTag l = new ListTag();
            for (String s : v) l.add(StringTag.valueOf(s));
            if (!l.isEmpty()) inv.put(k.toString(), l);
        });
        tag.put("invites", inv);
        CompoundTag nm = new CompoundTag();
        names.forEach((k, v) -> nm.putString(k.toString(), v));
        tag.put("names", nm);
        return tag;
    }
}
