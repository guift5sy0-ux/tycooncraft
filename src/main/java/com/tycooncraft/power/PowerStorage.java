package com.tycooncraft.power;

/** v0.22.0: 전력망에서 남는 전기를 담았다가 모자랄 때 내보내는 블록 (축전지) */
public interface PowerStorage {
    /** 이번 틱에 더 받을 수 있는 양 */
    int chargeRoom();

    /** 이번 틱에 내보낼 수 있는 양 */
    int dischargeable();

    void charge(int amount);

    void discharge(int amount);

    default long stored() {
        return 0;
    }
}
