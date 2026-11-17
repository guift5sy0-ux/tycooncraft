package com.tycooncraft.devtest;

import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.vehicle.DroneEntity;
import com.tycooncraft.vehicle.DroneItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;

/** v0.16: 드론 우클릭 자동 돌진 — 벽에 박으면 폭발, 허공이면 사정거리 끝에서 폭발, 돌진 중엔 조종이 풀림 */
public final class DroneDiveTest {
    static DroneEntity wall, sky;

    static DroneEntity launch(ServerLevel level, double x, double y, double z) {
        var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        DroneEntity d = ModEntities.DRONE.get().create(level);
        d.moveTo(x, y, z, 0, 0);
        d.setOwner(fp);
        level.addFreshEntity(d);
        DroneItem.startPiloting(fp, d);
        return d;
    }

    static void tick(ServerLevel level, int t, int gy) {
        var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        if (t == 1800) {
            fp.setPos(150.5, gy, 131.5);
            for (int y = gy; y < gy + 6; y++) for (int z = 128; z <= 136; z++) level.setBlockAndUpdate(new BlockPos(158, y, z), Blocks.STONE.defaultBlockState());
            for (int x = 148; x < 158; x++) for (int y = gy; y < gy + 6; y++) for (int z = 128; z <= 136; z++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
            wall = launch(level, 150.5, gy + 2, 132.5);
            wall.startDive(-90f, 0f);   // 동쪽 벽으로
            System.out.println("[AUTOTEST] v16 dive start piloted=" + wall.isPiloted() + " diving=" + wall.isDiving());
        }
        if (t == 1830) {
            System.out.println("[AUTOTEST] v16 dive wall removed=" + wall.isRemoved() + " end=" + DroneEntity.LAST_DIVE_END);
            sky = launch(level, 150.5, gy + 2, 132.5);
            sky.startDive(0f, -85f);    // 거의 수직 위로 → 사정거리(100) 끝
        }
        if (t == 1955) System.out.println("[AUTOTEST] v16 dive sky removed=" + sky.isRemoved() + " end=" + DroneEntity.LAST_DIVE_END + " dives=" + DroneEntity.DIVES);
    }

    private DroneDiveTest() {}
}
