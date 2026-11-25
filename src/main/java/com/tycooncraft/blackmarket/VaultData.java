package com.tycooncraft.blackmarket;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** v0.26.0 개인 금고: 사람마다 27칸, 칸당 10,000원에 영구 해금. 죽어도 그대로 */
public class VaultData extends SavedData {
    public static final int SIZE = 27;
    public static final long SLOT_PRICE = 10_000;

    public static final class Box {
        public int unlocked;
        public final NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);
    }

    public final Map<UUID, Box> boxes = new HashMap<>();

    public static VaultData get(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(new Factory<>(VaultData::new, VaultData::load), "tycooncraft_vaults");
    }

    public Box box(UUID id) {
        return boxes.computeIfAbsent(id, k -> new Box());
    }

    static VaultData load(CompoundTag t, HolderLookup.Provider r) {
        VaultData d = new VaultData();
        for (String k : t.getAllKeys()) {
            CompoundTag c = t.getCompound(k);
            Box b = new Box();
            b.unlocked = c.getInt("unlocked");
            ContainerHelper.loadAllItems(c, b.items, r);
            d.boxes.put(UUID.fromString(k), b);
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t, HolderLookup.Provider r) {
        boxes.forEach((k, b) -> {
            CompoundTag c = new CompoundTag();
            c.putInt("unlocked", b.unlocked);
            ContainerHelper.saveAllItems(c, b.items, r);
            t.put(k.toString(), c);
        });
        return t;
    }
}
