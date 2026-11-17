package com.tycooncraft.devtest;

import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.setter.SetterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.phys.Vec3;

/** v0.16.9: 세터 — 대각선·설치방향·부유체 없음 거부, 서쪽으로 가며 아래에 다리 놓기 */
public final class SetterTest {
    static BlockPos start;
    static int refused0;

    static void tick(ServerLevel level, int t, int gy) {
        var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        int z = 133, y = gy + 3;
        if (t == 2200) {
            for (int x = 140; x <= 157; x++) for (int dy = 1; dy <= 5; dy++) level.setBlockAndUpdate(new BlockPos(x, gy + dy - 1, z), Blocks.AIR.defaultBlockState());
            start = new BlockPos(157, y, z);
            level.setBlockAndUpdate(start, ModBlocks.SETTER.get().defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN));
            level.setBlockAndUpdate(start.above(), ModBlocks.SHIP_FLOAT.get().defaultBlockState());
            // 부유체 없는 세터 (거부돼야 함)
            BlockPos lone = new BlockPos(150, y + 2, z);
            level.setBlockAndUpdate(lone, ModBlocks.SETTER.get().defaultBlockState().setValue(DirectionalBlock.FACING, Direction.DOWN));
            refused0 = SetterBlockEntity.REFUSED;
            if (level.getBlockEntity(lone) instanceof SetterBlockEntity l) {
                l.charge();
                l.start(fp, new Vec3(-1, 0, 0));
            }
            if (level.getBlockEntity(start) instanceof SetterBlockEntity be) {
                be.items().setItem(0, new ItemStack(Items.COBBLESTONE, 32));
                be.charge();
                be.start(fp, new Vec3(1, 0, 1));        // 대각선 → 거부
                be.start(fp, new Vec3(0, -1, 0));       // 설치 방향(아래) → 거부
                int r = SetterBlockEntity.REFUSED - refused0;
                be.start(fp, new Vec3(-0.95, 0.1, 0.05)); // 서쪽 → 출발
                System.out.println("[AUTOTEST] v16 setter refusedDiagonalFacingNoFloat=" + r + "/3 running=" + be.running() + " energy=" + be.energy());
            }
            level.setBlockAndUpdate(lone, Blocks.AIR.defaultBlockState());
        }
        if (t == 2262) {   // 12스텝(60틱) 뒤 확인
            int bridge = 0;
            BlockPos at = null;
            for (int x = 140; x <= 157; x++) {
                if (level.getBlockState(new BlockPos(x, y - 1, z)).is(Blocks.COBBLESTONE)) bridge++;
                if (level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.SETTER.get())) at = new BlockPos(x, y, z);
            }
            boolean floatFollows = at != null && level.getBlockState(at.above()).is(ModBlocks.SHIP_FLOAT.get());
            boolean oldEmpty = !level.getBlockState(start).is(ModBlocks.SETTER.get());
            int left = -1, energy = -1;
            if (at != null && level.getBlockEntity(at) instanceof SetterBlockEntity be) {
                left = be.items().getItem(0).getCount();
                energy = be.energy();
                be.stop(null, "message.tycooncraft.setter.stopped");
            }
            System.out.println("[AUTOTEST] v16 setter moved=" + (at == null ? "?" : (start.getX() - at.getX())) + " bridgeBlocks=" + bridge + " floatFollows=" + floatFollows
                    + " oldSpotEmpty=" + oldEmpty + " cobbleLeft=" + left + " energy=" + energy + " steps=" + SetterBlockEntity.STEPS + " placed=" + SetterBlockEntity.PLACED);
        }
    }

    private SetterTest() {}
}
