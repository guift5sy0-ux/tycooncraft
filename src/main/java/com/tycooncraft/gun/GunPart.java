package com.tycooncraft.gun;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** 총기 파츠: 예전 총 인챈트를 대신한다. 부위(슬롯)마다 하나만 달 수 있다 */
public enum GunPart {
    HEAVY_BARREL("heavy_barrel", Slot.BARREL, true),
    RED_DOT("red_dot", Slot.OPTIC, true),
    SCOPE("scope", Slot.OPTIC, true),
    VERTICAL_GRIP("vertical_grip", Slot.UNDER, true),
    EXTENDED_MAG("extended_mag", Slot.MAG, true),
    MAG_COUPLER("mag_coupler", Slot.MAG, true),
    RAPID_TRIGGER("rapid_trigger", Slot.TRIGGER, true),
    AP_ROUNDS("ap_rounds", Slot.AMMO, false),
    INCENDIARY_ROUNDS("incendiary_rounds", Slot.AMMO, false),
    BRASS_CATCHER("brass_catcher", Slot.SIDE, true);

    public enum Slot { BARREL, OPTIC, UNDER, MAG, TRIGGER, AMMO, SIDE }

    public final String id;
    public final Slot slot;
    /** 총에 모양이 보이는 부품 */
    public final boolean visual;

    GunPart(String id, Slot slot, boolean visual) {
        this.id = id;
        this.slot = slot;
        this.visual = visual;
    }

    public static GunPart byId(String id) {
        for (GunPart p : values()) if (p.id.equals(id)) return p;
        return null;
    }

    /** 총마다 달 수 있는 파츠 (모델·현실성 기준) */
    public static final Map<String, Set<GunPart>> SUPPORT = Map.of(
            "pistol", EnumSet.of(RED_DOT, EXTENDED_MAG, RAPID_TRIGGER, AP_ROUNDS, INCENDIARY_ROUNDS),
            "smg", EnumSet.of(HEAVY_BARREL, RED_DOT, VERTICAL_GRIP, EXTENDED_MAG, MAG_COUPLER, RAPID_TRIGGER, AP_ROUNDS, INCENDIARY_ROUNDS, BRASS_CATCHER),
            "rifle", EnumSet.allOf(GunPart.class),
            "shotgun", EnumSet.of(HEAVY_BARREL, RED_DOT, VERTICAL_GRIP, EXTENDED_MAG, RAPID_TRIGGER, AP_ROUNDS, INCENDIARY_ROUNDS),
            "sniper", EnumSet.of(HEAVY_BARREL, VERTICAL_GRIP, EXTENDED_MAG, RAPID_TRIGGER, AP_ROUNDS, INCENDIARY_ROUNDS, BRASS_CATCHER),
            "vss", EnumSet.of(EXTENDED_MAG, MAG_COUPLER, RAPID_TRIGGER, AP_ROUNDS, INCENDIARY_ROUNDS, BRASS_CATCHER),
            "judge", EnumSet.of(RAPID_TRIGGER, AP_ROUNDS, INCENDIARY_ROUNDS),
            "dual_pistols", EnumSet.of(RAPID_TRIGGER, AP_ROUNDS, INCENDIARY_ROUNDS));
}
