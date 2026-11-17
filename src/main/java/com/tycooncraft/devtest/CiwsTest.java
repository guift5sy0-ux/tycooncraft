package com.tycooncraft.devtest;

import com.tycooncraft.entity.RocketEntity;
import com.tycooncraft.registry.ModBlocks;
import com.tycooncraft.registry.ModEntities;
import com.tycooncraft.registry.ModItems;
import com.tycooncraft.ship.ShipAssembler;
import com.tycooncraft.ship.ShipEntity;
import com.tycooncraft.turret.CiwsBlockEntity;
import com.tycooncraft.turret.Interceptor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** v0.16: 방공 요격 포대 — 적 RPG 격추, 아군 RPG 무시 / 흑요석 선체 금지 */
public final class CiwsTest {
    static BlockPos ciws;
    static int kills0, hostile, friendly;

    static void tick(ServerLevel level, int t, int gy) {
        if (t == 1460) {
            // 흑요석 선체 금지
            BlockPos helm = AirshipTest.build(level, 150, gy, 121, 1);
            level.setBlockAndUpdate(new BlockPos(151, gy, 122), Blocks.OBSIDIAN.defaultBlockState());
            ShipEntity a = ShipAssembler.assemble(level, helm, null);
            level.setBlockAndUpdate(new BlockPos(151, gy, 122), Blocks.IRON_BLOCK.defaultBlockState());
            ShipEntity b = ShipAssembler.assemble(level, helm, null);
            System.out.println("[AUTOTEST] v16 hull obsidianAssembled=" + (a != null) + " ironAssembled=" + (b != null)
                    + " anvilTooStrong=" + ShipAssembler.tooStrong(Blocks.ANVIL.defaultBlockState()) + " ironTooStrong=" + ShipAssembler.tooStrong(Blocks.IRON_BLOCK.defaultBlockState()));
            if (b != null) b.disassemble(null);
            // 요격 포대
            ciws = new BlockPos(140, gy, 128);
            level.setBlockAndUpdate(ciws.below(), Blocks.STONE.defaultBlockState());
            level.setBlockAndUpdate(ciws, ModBlocks.CIWS.get().defaultBlockState());
            if (level.getBlockEntity(ciws) instanceof CiwsBlockEntity be) {
                for (int i = 0; i < 4; i++) be.ammo().setItem(i, new ItemStack(ModItems.HEAVY_AMMO.get(), 64));
                be.setOwner(net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level).getUUID());
            }
            kills0 = Interceptor.KILLS;
        }
        if (ciws == null) return;
        // 적 RPG 6발 (주인 없음) → 격추돼야 함
        if (t >= 1470 && t <= 1570 && (t - 1470) % 20 == 0) {
            Vec3 from = new Vec3(140.5, gy + 12, 164.5), to = Vec3.atCenterOf(ciws);
            level.addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level, null, from, to.subtract(from).normalize().scale(1.6), RocketEntity.ROCKET, 3.5f));
            hostile++;
        }
        if (t == 1600) {
            int k = Interceptor.KILLS - kills0;
            boolean alive = level.getBlockState(ciws).is(ModBlocks.CIWS.get());
            System.out.println("[AUTOTEST] v16 ciws hostile=" + hostile + " intercepted=" + k + " shots=" + Interceptor.SHOTS + " ciwsSurvived=" + alive);
            kills0 = Interceptor.KILLS;
            // 아군(포대 주인) RPG 3발: 옆을 지나가게 → 쏘면 안 됨
            var fp = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            for (int i = 0; i < 3; i++) {
                Vec3 from = new Vec3(134.5, gy + 6 + i, 164.5), to = new Vec3(134.5, gy + 4, 110.5);
                level.addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level, fp, from, to.subtract(from).normalize().scale(1.6), RocketEntity.ROCKET, 3.5f));
                friendly++;
            }
        }
        if (t == 1660) {
            System.out.println("[AUTOTEST] v16 ciws friendly=" + friendly + " intercepted=" + (Interceptor.KILLS - kills0));
            ciws = null;
        }
    }

    public static void clientSetup(ServerLevel level, int gy) {
        BlockPos p = new BlockPos(-6, gy, -90);
        level.setBlockAndUpdate(p, ModBlocks.CIWS.get().defaultBlockState());
        if (level.getBlockEntity(p) instanceof CiwsBlockEntity be) be.ammo().setItem(0, new ItemStack(ModItems.HEAVY_AMMO.get(), 64));
    }

    public static void clientRocket(ServerLevel level, int gy) {
        Vec3 from = new Vec3(-6.5, gy + 14, -50.5), to = new Vec3(-5.5, gy + 1, -89.5);
        level.addFreshEntity(RocketEntity.create(ModEntities.ROCKET.get(), level, null, from, to.subtract(from).normalize().scale(1.4), RocketEntity.ROCKET, 3.5f));
    }

    private CiwsTest() {}
}
