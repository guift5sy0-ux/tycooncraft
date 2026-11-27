package com.tycooncraft.client.gun;

import com.tycooncraft.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * v0.32: 총 종류별 1인칭 동작 값.
 * hands: 시야 공간 손 위치 {오른손 x,y,z, 왼손 x,y,z} (gen_guns.py 손잡이 좌표에서 계산한 값)
 * back/up/side/roll: 반동 (뒤로 블록, 총구 들림·좌우·기울기 도)
 * equip: 꺼내는 시간(초), heavy: 무게감 (흔들림 늦게 따라옴)
 */
public enum GunKind {
    PISTOL("pistol", new float[]{0.36f, -0.34f, -0.62f, 0.309f, -0.429f, -0.587f}, 0.05f, 7f, 1.6f, 3f, 0.32f, 0.2f, Casing.PISTOL, "slide", 1f, 0.62f, 0.42f),
    JUDGE("judge", new float[]{0.36f, -0.34f, -0.62f, 0.312f, -0.417f, -0.591f}, 0.075f, 11f, 2.2f, 4.5f, 0.36f, 0.3f, Casing.RIFLE, "slide", 1f, 0.6f, 0.42f),
    SMG("smg", new float[]{0.34f, -0.33f, -0.60f, 0.308f, -0.275f, -1.062f}, 0.024f, 2.2f, 1.1f, 1.4f, 0.4f, 0.35f, Casing.PISTOL, "receiver", 1f, 0.66f, 0.3f),
    RIFLE("rifle", new float[]{0.33f, -0.32f, -0.60f, 0.296f, -0.291f, -1.083f}, 0.032f, 2.7f, 1.0f, 1.6f, 0.48f, 0.5f, Casing.RIFLE, "dust_cover", 1f, 0.5f, 0.5f),
    SHOTGUN("shotgun", new float[]{0.33f, -0.32f, -0.60f, 0.289f, -0.339f, -1.189f}, 0.11f, 9f, 2.0f, 3.5f, 0.5f, 0.55f, Casing.SHOTGUN, "receiver", 1f, 0.68f, 0.32f),
    SNIPER("sniper", new float[]{0.33f, -0.33f, -0.62f, 0.30f, -0.264f, -1.048f}, 0.09f, 7.5f, 1.5f, 2.5f, 0.55f, 0.65f, Casing.BIG, "receiver", 1f, 0.78f, 0.78f),
    VSS("vss", new float[]{0.31f, -0.33f, -0.62f, 0.29f, -0.285f, -0.98f}, 0.026f, 2.0f, 0.8f, 1.2f, 0.45f, 0.45f, Casing.RIFLE, "receiver", 1f, 0.72f, 0.38f),
    RT20("rt20", new float[]{0.34f, -0.33f, -0.64f, 0.326f, -0.285f, -0.846f}, 0.26f, 17f, 3f, 6f, 0.75f, 1f, Casing.RT20, "loading_port", 0.5f, 0f, 0.5f),
    RPG("rpg", new float[]{0.34f, -0.28f, -0.58f, 0.331f, -0.314f, -0.752f}, 0.12f, 5f, 1f, 2f, 0.6f, 0.8f, null, "tube", 0.5f, 0.5f, 0.9f),
    DUAL("pistol", new float[]{0.36f, -0.34f, -0.62f, -0.36f, -0.34f, -0.62f}, 0.045f, 7f, 1.5f, 3f, 0.38f, 0.25f, Casing.PISTOL, "slide", 1f, 0.62f, 0.42f);

    public final String rig;
    public final float[] hands;
    public final float back, up, side, roll, equip, heavy;
    @Nullable public final Casing casing;
    /** 탄피 배출구: 요소 이름 + 그 상자 안 비율 */
    public final String port;
    public final float portX, portY, portZ;

    GunKind(String rig, float[] hands, float back, float up, float side, float roll, float equip, float heavy, @Nullable Casing casing,
            String port, float px, float py, float pz) {
        this.rig = rig;
        this.hands = hands;
        this.back = back;
        this.up = up;
        this.side = side;
        this.roll = roll;
        this.equip = equip;
        this.heavy = heavy;
        this.casing = casing;
        this.port = port;
        this.portX = px;
        this.portY = py;
        this.portZ = pz;
    }

    public boolean pistolGrip() {
        return this == PISTOL || this == JUDGE || this == DUAL;
    }

    /** 쏜 뒤 탄피가 나오는 시점(초): 펌프·노리쇠는 손으로 당길 때 */
    public float ejectDelay() {
        return switch (this) {
            case SHOTGUN -> 0.26f;
            case SNIPER -> 0.56f;
            case RT20, RPG -> -1f;
            default -> 0.018f;
        };
    }

    public static @Nullable GunKind of(ItemStack s) {
        if (s.is(ModItems.PISTOL.get())) return PISTOL;
        if (s.is(ModItems.JUDGE.get())) return JUDGE;
        if (s.is(ModItems.SMG.get())) return SMG;
        if (s.is(ModItems.RIFLE.get())) return RIFLE;
        if (s.is(ModItems.SHOTGUN.get())) return SHOTGUN;
        if (s.is(ModItems.SNIPER.get())) return SNIPER;
        if (s.is(ModItems.VSS.get())) return VSS;
        if (s.is(ModItems.RT20.get())) return RT20;
        if (s.is(ModItems.RPG.get())) return RPG;
        if (s.is(ModItems.DUAL_PISTOLS.get())) return DUAL;
        return null;
    }

    /** 탄피 종류: 길이·굵기(블록) */
    public enum Casing {
        PISTOL(0.05f, 0.019f, false), RIFLE(0.075f, 0.021f, false), BIG(0.1f, 0.026f, false), RT20(0.16f, 0.042f, false), SHOTGUN(0.095f, 0.034f, true);
        public final float len, rad;
        public final boolean shotshell;

        Casing(float len, float rad, boolean shotshell) {
            this.len = len;
            this.rad = rad;
            this.shotshell = shotshell;
        }
    }
}
