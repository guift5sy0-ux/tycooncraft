package com.tycooncraft.machine;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

import java.util.LinkedHashMap;
import java.util.Map;

/** 재료 여러 개 → 결과물 1종 */
public record MachineRecipe(Map<Item, Integer> inputs, Item output, int outputCount) {

    public static Builder of(ItemLike output, int count) {
        return new Builder(output.asItem(), count);
    }

    public ItemStack result() {
        return new ItemStack(output, outputCount);
    }

    public Component describe() {
        MutableComponent text = Component.empty();
        boolean first = true;
        for (Map.Entry<Item, Integer> e : inputs.entrySet()) {
            if (!first) text.append(" + ");
            text.append(e.getKey().getDescription());
            if (e.getValue() > 1) text.append(" x" + e.getValue());
            first = false;
        }
        text.append(" → ").append(output.getDescription());
        if (outputCount > 1) text.append(" x" + outputCount);
        return text;
    }

    public static final class Builder {
        private final Item output;
        private final int count;
        private final Map<Item, Integer> inputs = new LinkedHashMap<>();

        private Builder(Item output, int count) {
            this.output = output;
            this.count = count;
        }

        public Builder in(ItemLike item, int n) {
            inputs.put(item.asItem(), n);
            return this;
        }

        public Builder in(ItemLike item) {
            return in(item, 1);
        }

        public MachineRecipe build() {
            return new MachineRecipe(Map.copyOf(inputs), output, count);
        }
    }
}
