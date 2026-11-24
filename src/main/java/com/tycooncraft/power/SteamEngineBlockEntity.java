package com.tycooncraft.power;

import com.tycooncraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

public class SteamEngineBlockEntity extends RotatingBlockEntity {
    public static final int OUTPUT = 32;
    private boolean producing;
    private int idle;

    public SteamEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STEAM_ENGINE.get(), pos, state);
    }

    @Override
    public boolean conducts(Direction d) {
        return true;
    }

    @Override
    public int supply() {
        return producing ? OUTPUT : 0;
    }

    @Override
    protected float maxSpeed() {
        return 12f;
    }

    public void serverTick() {
        producing = false;
        if (level == null) return;
        // 일할 기계가 있을 때만 증기를 씀 (놀 때 석탄 낭비 없음). 잠깐 쉬는 틈엔 관성으로 1초 정도 더 돎
        var net = PowerGrid.net(level, worldPosition);
        boolean wanted = net != null && net.demand > 0;
        if (!wanted && ++idle > 20) return;
        if (wanted) idle = 0;
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof BoilerBlockEntity b && b.takeSteam(1)) {
                producing = true;
                break;
            }
        }
        if (producing && level.getGameTime() % 16 == 0)
            level.playSound(null, worldPosition, SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.25f, 0.55f + level.random.nextFloat() * 0.05f);
    }
}
