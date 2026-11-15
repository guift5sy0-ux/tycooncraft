package com.tycooncraft.vehicle;

import net.minecraft.world.entity.Entity;

/**
 * 공용 코드 ↔ 클라이언트 연결점. 서버에는 클라이언트 클래스가 없으므로
 * 클라이언트 쪽이 시작할 때 {@link #client}를 실제 구현으로 바꿔 끼운다.
 */
public final class VehicleHooks {

    /** 조종 입력: forward(W=+1,S=-1), strafe(A=+1,D=-1), up(스페이스), down(Ctrl) */
    public record Input(float forward, float strafe, boolean up, boolean down) {
        public static final Input NONE = new Input(0, 0, false, false);
    }

    public interface Client {
        Input input();

        /** 매 틱(클라이언트): 엔진 소리, 배기·손상 연기 */
        void tick(Entity vehicle);
    }

    public static Client client = new Client() {
        @Override
        public Input input() {
            return Input.NONE;
        }

        @Override
        public void tick(Entity vehicle) {
        }
    };

    private VehicleHooks() {}
}
