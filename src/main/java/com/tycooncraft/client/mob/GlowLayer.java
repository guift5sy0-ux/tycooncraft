package com.tycooncraft.client.mob;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** 발광 레이어: 어두운 곳에서도 빛나는 부분(눈·용암 균열·번개 줄무늬) */
public class GlowLayer<T extends Entity, M extends EntityModel<T>> extends EyesLayer<T, M> {
    private final RenderType type;

    public GlowLayer(RenderLayerParent<T, M> parent, ResourceLocation tex) {
        super(parent);
        this.type = RenderType.eyes(tex);
    }

    @Override
    public RenderType renderType() {
        return type;
    }
}
