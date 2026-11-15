package com.tycooncraft.vehicle;

/** 계기판(HUD)에 보여줄 정보 */
public interface FueledVehicle {
    int getFuel();

    int maxFuel();

    /** 0~1 */
    float healthFraction();

    /** HUD 제목 번역 키 */
    String hudName();

    default boolean showAltitude() {
        return false;
    }
}
