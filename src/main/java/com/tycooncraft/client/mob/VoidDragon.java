package com.tycooncraft.client.mob;

import com.tycooncraft.TycoonCraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EnderDragonRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 공허의 군주 엔더 드래곤 외형: 바닐라 렌더러·애니메이션은 그대로 두고,
 * 머리(거대한 휜 뿔·왕관 가시·눈썹 장갑·이마 수정)·턱(가시)·몸통(빛나는 등 칼날·어깨 장갑)에
 * 추가 부품을 바닐라 뼈의 자식으로 붙인다 → 비행·목 움직임·죽음 연출까지 그대로 따라감.
 * 텍스처(새로 칠한 비늘·날개막 핏줄·눈)는 assets/minecraft/.../enderdragon 에서 교체.
 */
public final class VoidDragon {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TycoonCraft.id("void_dragon_armor"), "main");

    public static class Renderer extends EnderDragonRenderer {
        public Renderer(EntityRendererProvider.Context ctx) {
            super(ctx);
            try {
                ModelPart armor = ctx.bakeLayer(LAYER);
                Object model = field(EnderDragonRenderer.class, "model").get(this);
                attach(model, "head", armor.getChild("head"));
                attach(model, "jaw", armor.getChild("jaw"));
                attach(model, "body", armor.getChild("body"));
            } catch (Exception e) {
                com.mojang.logging.LogUtils.getLogger().warn("Void dragon armor attach failed", e);
            }
        }

        @SuppressWarnings("unchecked")
        private static void attach(Object model, String partName, ModelPart extra) throws Exception {
            ModelPart part = (ModelPart) field(model.getClass(), partName).get(model);
            Map<String, ModelPart> children = (Map<String, ModelPart>) field(ModelPart.class, "children").get(part);
            children.put("tycooncraft_armor", extra);
        }

        private static Field field(Class<?> c, String name) throws NoSuchFieldException {
            for (Class<?> k = c; k != null; k = k.getSuperclass()) {
                try {
                    Field f = k.getDeclaredField(name);
                    f.setAccessible(true);
                    return f;
                } catch (NoSuchFieldException ignored) {
                }
            }
            throw new NoSuchFieldException(name);
        }
    }

    private VoidDragon() {}
}
