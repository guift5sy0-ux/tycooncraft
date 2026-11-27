package com.tycooncraft.explosive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.Nullable;

/**
 * v0.33: HDX (강화 폭약). TNT 처럼 불·레드스톤·불화살·폭발로 점화되고 4초 뒤 터지지만
 * 위력이 4.8 (TNT 4, 충전된 크리퍼 6 의 0.8배).
 */
public class HdxBlock extends TntBlock {
    public static final float POWER = 4.8f;

    public HdxBlock(Properties props) {
        super(props);
    }

    @Override
    public void onCaughtFire(BlockState state, Level level, BlockPos pos, @Nullable Direction face, @Nullable LivingEntity igniter) {
        if (level.isClientSide) return;
        PrimedHdx p = PrimedHdx.at(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter, defaultBlockState());
        level.addFreshEntity(p);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 0.85F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (level.isClientSide) return;
        PrimedHdx p = PrimedHdx.at(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, explosion.getIndirectSourceEntity(), defaultBlockState());
        int f = p.getFuse();
        p.setFuse((short) (level.random.nextInt(f / 4) + f / 8));
        level.addFreshEntity(p);
    }

    /** 점화된 HDX: 겉모습은 TNT 엔티티(블록 모양만 HDX), 폭발 위력만 다름 */
    public static class PrimedHdx extends PrimedTnt {
        public static int EXPLODED;

        private PrimedHdx(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
            super(level, x, y, z, owner);
        }

        public static PrimedHdx at(Level level, double x, double y, double z, @Nullable LivingEntity owner, BlockState look) {
            PrimedHdx p = new PrimedHdx(level, x, y, z, owner);
            p.setBlockState(look);
            return p;
        }

        @Override
        protected void explode() {
            EXPLODED++;
            level().explode(this, Explosion.getDefaultDamageSource(level(), this), null, getX(), getY(0.0625), getZ(), POWER, false, Level.ExplosionInteraction.TNT);
        }
    }
}
