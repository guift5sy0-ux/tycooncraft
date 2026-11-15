package com.tycooncraft.client.vehicle;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/** 조작 키 (설정 → 조작에서 바꿀 수 있음) */
public final class VehicleKeyBinds {
    public static final String CATEGORY = "key.categories.tycooncraft";
    /** 헬기·드론 하강 (Ctrl 도 됨) */
    public static final KeyMapping DESCEND = new KeyMapping("key.tycooncraft.descend", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, CATEGORY);

    private VehicleKeyBinds() {}
}
