package com.tycooncraft.client;

import com.tycooncraft.TycoonCraft;
import com.tycooncraft.gun.GunItem;
import com.tycooncraft.gun.GunPart;
import com.tycooncraft.gun.GunParts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 파츠를 단 총의 모양: 원래 총 모델(소음기 변형 포함) 위에 파츠 모델을 그대로 겹쳐 그린다.
 * 파츠 모델은 gen_gun_parts.py 가 총마다·소음기 변형마다 같은 좌표계로 만들어 둔다.
 */
@EventBusSubscriber(modid = TycoonCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class GunPartModels {
    private static final String[] GUNS = {"pistol", "smg", "rifle", "shotgun", "sniper", "vss", "judge"};

    static ModelResourceLocation loc(String id) {
        return ModelResourceLocation.standalone(TycoonCraft.id("item/" + id));
    }

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        for (String id : GunPartModelList.IDS) event.register(loc(id));
    }

    @SubscribeEvent
    public static void wrap(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        for (String g : GUNS) {
            ModelResourceLocation key = ModelResourceLocation.inventory(TycoonCraft.id(g));
            BakedModel m = models.get(key);
            if (m != null) models.put(key, new Wrapped(m, g));
        }
    }

    /** 원래 모델: 오버라이드만 바꿔 파츠 조합 모델을 돌려준다 */
    static final class Wrapped extends BakedModelWrapper<BakedModel> {
        private final ItemOverrides overrides;

        Wrapped(BakedModel original, String gun) {
            super(original);
            ItemOverrides inner = original.getOverrides();
            Map<String, BakedModel> cache = new HashMap<>();
            this.overrides = new ItemOverrides() {
                @Nullable
                @Override
                public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                    BakedModel base = inner.resolve(original, stack, level, entity, seed);
                    if (base == null) base = original;
                    List<GunPart> parts = GunParts.installed(stack);
                    if (parts.isEmpty()) return base;
                    int tier = GunItem.silencerTier(stack);
                    String variant = tier == 2 ? "s2" : tier == 1 ? "s1" : "base";
                    StringBuilder k = new StringBuilder(variant);
                    for (GunPart p : parts) if (p.visual) k.append('|').append(p.id);
                    String ck = k.toString();
                    BakedModel hit = cache.get(ck);
                    if (hit != null) return hit;
                    List<BakedModel> extra = new ArrayList<>();
                    var mm = Minecraft.getInstance().getModelManager();
                    for (GunPart p : parts) {
                        if (!p.visual) continue;
                        BakedModel pm = mm.getModel(loc("gunpart/" + gun + "_" + variant + "_" + p.id));
                        if (pm != mm.getMissingModel()) extra.add(pm);
                    }
                    BakedModel out = extra.isEmpty() ? base : new Composite(base, extra);
                    cache.put(ck, out);
                    return out;
                }
            };
        }

        @Override
        public ItemOverrides getOverrides() {
            return overrides;
        }
    }

    /** 총 + 파츠 사각형을 모두 합친 모델 (표시 변환은 총 것을 그대로) */
    static final class Composite extends BakedModelWrapper<BakedModel> {
        private final List<BakedModel> parts;

        Composite(BakedModel base, List<BakedModel> parts) {
            super(base);
            this.parts = parts;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            List<BakedQuad> out = new ArrayList<>(originalModel.getQuads(state, side, rand));
            for (BakedModel p : parts) out.addAll(p.getQuads(state, side, rand));
            return out;
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType type) {
            List<BakedQuad> out = new ArrayList<>(originalModel.getQuads(state, side, rand, data, type));
            for (BakedModel p : parts) out.addAll(p.getQuads(state, side, rand, data, type));
            return out;
        }

        @Override
        public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
            return List.of(this);
        }

        @Override
        public ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }
    }

    private GunPartModels() {}
}
