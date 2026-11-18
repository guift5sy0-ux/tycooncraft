package com.tycooncraft.machine;

import com.tycooncraft.registry.ModBlockEntities;
import com.tycooncraft.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class EmpGeneratorBlockEntity extends BlockEntity {
    public static final int MAX_CHARGES = 4, ARM_TIME = 60, COOLDOWN = 400, DISABLE_TIME = 300;
    public static final double RADIUS = 48;
    private static final DustParticleOptions BLUE = new DustParticleOptions(new Vector3f(0.3f, 0.75f, 1f), 1.6f);

    int charges, countdown = -1, cooldown, ring = -1;
    boolean wasPowered;

    public EmpGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EMP_GENERATOR.get(), pos, state);
    }

    public int charges() {
        return charges;
    }

    public void charge(Player player, ItemStack stack) {
        if (charges >= MAX_CHARGES) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.emp.full", MAX_CHARGES).withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        charges++;
        setChanged();
        level.playSound(null, worldPosition, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1f, 1.3f);
        player.displayClientMessage(Component.translatable("message.tycooncraft.emp.charged", charges, MAX_CHARGES).withStyle(ChatFormatting.AQUA), true);
    }

    /** 가동 시작 (player 는 레드스톤이면 null) */
    public void arm(@Nullable Player player) {
        if (countdown >= 0) return;
        if (cooldown > 0) {
            if (player != null) player.displayClientMessage(Component.translatable("message.tycooncraft.emp.cooldown", (cooldown + 19) / 20).withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        if (charges <= 0) {
            if (player != null) player.displayClientMessage(Component.translatable("message.tycooncraft.emp.no_charge").withStyle(ChatFormatting.RED), true);
            return;
        }
        charges--;
        countdown = ARM_TIME;
        setChanged();
        level.setBlock(worldPosition, getBlockState().setValue(EmpGeneratorBlock.ACTIVE, true), 3);
        level.playSound(null, worldPosition, ModSounds.EMP_CHARGE.get(), SoundSource.BLOCKS, 3f, 1f);
        if (level instanceof ServerLevel sl)
            for (var p : sl.players())
                if (p.distanceToSqr(Vec3.atCenterOf(worldPosition)) < (RADIUS + 16) * (RADIUS + 16))
                    p.displayClientMessage(Component.translatable("message.tycooncraft.emp.armed").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
    }

    public void serverTick() {
        if (!(level instanceof ServerLevel sl)) return;
        if (cooldown > 0) cooldown--;
        Vec3 top = Vec3.atBottomCenterOf(worldPosition).add(0, 1.05, 0);
        if (countdown > 0) {
            countdown--;
            int k = ARM_TIME - countdown;
            // 코일로 모여드는 전기
            for (int i = 0; i < 1 + k / 15; i++) {
                double a = sl.random.nextDouble() * Math.PI * 2, r = 3 - k * 0.04;
                sl.sendParticles(BLUE, top.x + Math.cos(a) * r, top.y - 0.4 + sl.random.nextDouble(), top.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
            }
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.x, top.y - 0.3, top.z, 2 + k / 10, 0.35, 0.25, 0.35, 0.15);
            if (countdown == 0) fire(sl, top);
        }
        if (ring >= 0) {
            double r = ring * 3.2;
            int n = (int) (r * 3) + 8;
            for (int i = 0; i < n; i++) {
                double a = i * Math.PI * 2 / n;
                sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.x + Math.cos(a) * r, top.y - 0.5, top.z + Math.sin(a) * r, 1, 0, 0.4, 0, 0.05);
                if (i % 3 == 0) sl.sendParticles(BLUE, top.x + Math.cos(a) * r, top.y + 0.5, top.z + Math.sin(a) * r, 1, 0, 0.3, 0, 0);
            }
            if (++ring * 3.2 > RADIUS) ring = -1;
        }
    }

    private void fire(ServerLevel sl, Vec3 top) {
        countdown = -1;
        cooldown = COOLDOWN;
        ring = 0;
        setChanged();
        sl.setBlock(worldPosition, getBlockState().setValue(EmpGeneratorBlock.ACTIVE, false), 3);
        sl.playSound(null, worldPosition, ModSounds.EMP_BLAST.get(), SoundSource.BLOCKS, 6f, 1f);
        sl.sendParticles(ParticleTypes.FLASH, top.x, top.y, top.z, 1, 0, 0, 0, 0);
        sl.sendParticles(ParticleTypes.ELECTRIC_SPARK, top.x, top.y, top.z, 150, 1.5, 1.5, 1.5, 1.0);
        int n = EmpPulse.pulse(sl, top, RADIUS, DISABLE_TIME);
        for (var p : sl.players())
            if (p.distanceToSqr(top) < (RADIUS + 16) * (RADIUS + 16))
                {} /* v0.18.9: 전투 안내 문구 제거 */
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.saveAdditional(tag, reg);
        tag.putInt("Charges", charges);
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider reg) {
        super.loadAdditional(tag, reg);
        charges = tag.getInt("Charges");
        cooldown = tag.getInt("Cooldown");
    }
}
