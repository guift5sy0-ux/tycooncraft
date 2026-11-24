package com.tycooncraft.power;

import net.minecraft.core.Direction;

/**
 * v0.21.0 산업혁명 1단계 — 동력(축) 네트워크에 참여하는 블록.
 * 전달체(축·기어박스·증기기관)는 conducts 방향으로 서로 이어지고, 소비자(기계)는 전달체가 drives 하는 면에 붙어 있으면 동력을 받는다.
 */
public interface PowerNode {
    /** 동력을 다른 전달체로 넘기는 방향 (축은 자기 축 방향만) */
    default boolean conducts(Direction d) {
        return false;
    }

    /** 옆에 붙은 기계를 돌리는 면 (축은 모든 면 — 옛 공장의 천장 '라인 샤프트'처럼) */
    default boolean drives(Direction d) {
        return conducts(d);
    }

    /** 기계 등 소비자인가 (소비자끼리는 동력이 이어지지 않음) */
    default boolean consumer() {
        return false;
    }

    /** 이번 틱에 만든 동력 (P/t) */
    default int supply() {
        return 0;
    }

    /** 이번 틱에 필요한 동력 (P/t) */
    default int demand() {
        return 0;
    }

    /** 네트워크 상태가 바뀜 (돌아가는지·비율) — 축 애니메이션 동기화 등에 사용 */
    default void onNetState(boolean running, float ratio) {}

    // ── v0.22.0 층별(축/전선) 버전. 기본은 축(MECH) 전용 블록 ──
    default boolean in(Layer l) {
        return l == Layer.MECH;
    }

    default boolean conducts(Layer l, Direction d) {
        return l == Layer.MECH && conducts(d);
    }

    default boolean drives(Layer l, Direction d) {
        return l == Layer.MECH ? drives(d) : conducts(l, d);
    }

    default boolean consumer(Layer l) {
        return consumer();
    }

    default int supply(Layer l) {
        return l == Layer.MECH ? supply() : 0;
    }

    default int demand(Layer l) {
        return l == Layer.MECH ? demand() : 0;
    }

    default void onNetState(Layer l, boolean running, float ratio) {
        if (l == Layer.MECH) onNetState(running, ratio);
    }
}
