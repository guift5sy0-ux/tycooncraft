package com.tycooncraft.ship;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 블록으로 지은 배. 조종기(키)를 우클릭하면 이어진 블록들이 이 엔티티로 바뀌어 물 위를 움직인다.
 * - 블록들은 원점(O, 최소 모서리) 기준 정수 좌표로 저장, 엔티티 위치 = 평면 중심(피벗), y = O.y (배는 수면 높이 고정)
 * - 갑판·선체는 세로 기둥(run) 상자들로 충돌 → {@link ShipCollisions} 를 통해 모든 엔티티가 걸어 다닐 수 있다
 * - 서버가 움직이고(조종석의 입력을 패킷으로 받음), 갑판 위 몹/아이템은 서버가, 각 플레이어는 자기 클라이언트가 함께 실어 나른다
 */
public class ShipEntity extends Entity implements IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> EMP = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> SPEED = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.FLOAT);

    /** 클라이언트 쪽 로컬 플레이어 (공용 코드에서 클라이언트 클래스를 직접 부르지 않기 위함) */
    public static Supplier<Entity> LOCAL_PLAYER = () -> null;

    public record Cell(int x, int y, int z, BlockState state, @Nullable CompoundTag be) {}

    /** 같은 (x,z) 의 연속된 충돌 블록 묶음. y0/y1 = 원점 기준 높이(블록 모양 반영) */
    public record Run(int x, int z, double y0, double y1, boolean helm) {}

    final List<Cell> cells = new ArrayList<>();
    final List<Run> runs = new ArrayList<>();
    private AABB[] boxes = new AABB[0];
    private AABB bounds = new AABB(0, 0, 0, 0, 0, 0);
    double pivotX, pivotZ, radius;
    int sizeY, motors, helm = -1;
    float yaw0;
    /** 렌더/클라이언트용 현재·이전 방향, 서버도 yaw 를 씀 */
    public float yaw, yawO;
    private float lastDyaw;
    private double speed;
    private static final EntityDataAccessor<CompoundTag> TURRETS = SynchedEntityData.defineId(ShipEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private float inF, inT, inV;
    /** 셀 위치 → 번호 */
    private final Map<Long, Integer> index = new HashMap<>();
    private boolean hasConveyor, hasTurret, falling;
    /** 실려 있는 포탑의 상태 (셀 위치 키) */
    public static final class Turret {
        int target = -1, cooldown, search;
        public float yaw, yawO, pitch, pitchO;
    }
    final Map<Long, Turret> turrets = new HashMap<>();
    final Map<Long, com.tycooncraft.turret.Interceptor.State> ciws = new HashMap<>();
    public static int TURRET_SHOTS, BLAST_CELLS, CONVEYOR_PUSHES, CRASHES;
    private double vspeed, fuelAcc;
    private int fuel, floats;
    private boolean air;
    private int inAge, soundT;
    private int lerpSteps;
    private double lerpX, lerpY, lerpZ;

    // 클라이언트: 이번 틱에 로컬 플레이어를 실어 나를 정보
    @Nullable Vec3 carryFrom, carryTo;
    float carryYaw;

    public static int MOVED_TICKS, CARRIED;

    @Nullable
    public Vec3 carryFromForClient() {
        return carryFrom;
    }

    public Vec3 carryPointForClient(Vec3 pt) {
        return carryPoint(pt, carryFrom, carryTo, carryYaw);
    }

    public float carryYawForClient() {
        return carryYaw;
    }

    public void clearCarry() {
        carryFrom = carryTo = null;
        carryYaw = 0;
    }

    public boolean isHelmBox(int i) {
        return i >= 0 && i < runs.size() && runs.get(i).helm();
    }

    public ShipEntity(EntityType<? extends ShipEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = true;
    }

    // ── 데이터 ─────────────────────────────────────────────────────
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        b.define(YAW, 0f);
        b.define(EMP, 0);
        b.define(SPEED, 0f);
        b.define(TURRETS, new CompoundTag());
    }

    public int empTicks() {
        return entityData.get(EMP);
    }

    public void emp(int ticks) {
        entityData.set(EMP, Math.max(empTicks(), ticks));
    }

    public float shipSpeed() {
        return entityData.get(SPEED);
    }

    public List<Cell> cells() {
        return cells;
    }

    public double pivotX() {
        return pivotX;
    }

    public double pivotZ() {
        return pivotZ;
    }

    public float yaw0() {
        return yaw0;
    }

    public AABB[] boxes() {
        return boxes;
    }

    public AABB worldBounds() {
        return bounds;
    }

    public int motorCount() {
        return motors;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key == YAW && level().isClientSide && tickCount == 0) yaw = yawO = entityData.get(YAW);
    }

    /** 조립 직후 (서버) */
    void init(List<Cell> list, float yaw0) {
        cells.clear();
        cells.addAll(list);
        this.yaw0 = yaw0;
        this.yaw = this.yawO = yaw0;
        entityData.set(YAW, yaw0);
        rebuild();
    }

    void rebuild() {
        rebuild(false);
    }

    /** keepPivot: 블록이 부서져 빠질 때 — 피벗(엔티티 위치 기준점)이 바뀌면 운송기가 순간이동하므로 유지 */
    void rebuild(boolean keepPivot) {
        int mx = 0, mz = 0, my = 0;
        index.clear();
        hasConveyor = hasTurret = false;
        motors = 0;
        floats = 0;
        helm = -1;
        for (int i = 0; i < cells.size(); i++) {
            Cell c = cells.get(i);
            mx = Math.max(mx, c.x);
            mz = Math.max(mz, c.z);
            my = Math.max(my, c.y);
            if (c.state.getBlock() instanceof ShipMotorBlock) motors++;
            if (c.state.getBlock() instanceof ShipFloatBlock) floats++;
            if (c.state.getBlock() instanceof com.tycooncraft.block.ConveyorBlock) hasConveyor = true;
            if (c.state.getBlock() instanceof com.tycooncraft.turret.TurretBlock || c.state.getBlock() instanceof com.tycooncraft.turret.CiwsBlock) hasTurret = true;
            index.put(key(c.x, c.y, c.z), i);
            if (c.state.getBlock() instanceof ShipHelmBlock) helm = i;
        }
        air = floats > 0 && floats * ShipFuel.LIFT_PER_FLOAT >= cells.size();
        if (!keepPivot) {
            pivotX = (mx + 1) / 2.0;
            pivotZ = (mz + 1) / 2.0;
        }
        sizeY = my + 1;
        radius = 0;
        Map<Long, List<Cell>> cols = new HashMap<>();
        for (Cell c : cells) {
            radius = Math.max(radius, Math.hypot(c.x + 0.5 - pivotX, c.z + 0.5 - pivotZ) + 0.71);
            VoxelShape sh = c.state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            if (sh.isEmpty()) continue;
            cols.computeIfAbsent(((long) c.x << 32) | (c.z & 0xffffffffL), k -> new ArrayList<>()).add(c);
        }
        runs.clear();
        Cell h = helm >= 0 ? cells.get(helm) : null;
        for (List<Cell> col : cols.values()) {
            col.sort(Comparator.comparingInt(Cell::y));
            int i = 0;
            while (i < col.size()) {
                int j = i;
                while (j + 1 < col.size() && col.get(j + 1).y == col.get(j).y + 1) j++;
                Cell a = col.get(i), b = col.get(j);
                double y0 = a.y + a.state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).min(Direction.Axis.Y);
                double y1 = b.y + b.state.getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).max(Direction.Axis.Y);
                boolean hasHelm = h != null && h.x == a.x && h.z == a.z && h.y >= a.y && h.y <= b.y;
                runs.add(new Run(a.x, a.z, y0, y1, hasHelm));
                i = j + 1;
            }
        }
        // 조종기에 충돌 모양이 없어도 상호작용 상자는 있어야 한다
        if (h != null && runs.stream().noneMatch(Run::helm)) runs.add(new Run(h.x, h.z, h.y, h.y + 0.9, true));
        boxes = new AABB[runs.size()];
        updateBoxes();
    }

    // ── 좌표 변환 ─────────────────────────────────────────────────
    /** 원점 기준 로컬 평면 좌표 → 피벗 기준 월드 오프셋 (현재 방향) */
    public Vec3 rot(double lx, double lz, float yawDeg) {
        return new Vec3(lx - pivotX, 0, lz - pivotZ).yRot(-(yawDeg - yaw0) * Mth.DEG_TO_RAD);
    }

    private void updateBoxes() {
        Vec3 p = position();
        double minY = p.y + sizeY + 1, maxY = p.y;
        for (int i = 0; i < runs.size(); i++) {
            Run r = runs.get(i);
            Vec3 c = rot(r.x + 0.5, r.z + 0.5, yaw).add(p);
            boxes[i] = new AABB(c.x - 0.5, p.y + r.y0, c.z - 0.5, c.x + 0.5, p.y + r.y1, c.z + 0.5);
            minY = Math.min(minY, p.y + r.y0);
            maxY = Math.max(maxY, p.y + r.y1);
        }
        bounds = new AABB(p.x - radius, Math.min(minY, p.y), p.z - radius, p.x + radius, Math.max(maxY, p.y + sizeY), p.z + radius);
    }

    public Vec3 helmWorld() {
        if (helm < 0) return position();
        Cell h = cells.get(helm);
        return rot(h.x + 0.5, h.z + 0.5, yaw).add(position()).add(0, h.y + 0.5, 0);
    }

    /** 조종석 (조종기 바로 뒤, 조종기 바닥 높이) */
    public Vec3 seatWorld() {
        if (helm < 0) return position().add(0, sizeY, 0);
        Cell h = cells.get(helm);
        Direction f = h.state.hasProperty(HorizontalDirectionalBlock.FACING) ? h.state.getValue(HorizontalDirectionalBlock.FACING) : Direction.SOUTH;
        return rot(h.x + 0.5 - f.getStepX() * 0.85, h.z + 0.5 - f.getStepZ() * 0.85, yaw).add(position()).add(0, h.y, 0);
    }

    /** e 의 발이 갑판(어떤 기둥의 윗면) 위에 있나 */
    public boolean standingOn(Entity e, double above) {
        AABB eb = e.getBoundingBox();
        if (!bounds.inflate(0.5, 2, 0.5).intersects(eb)) return false;
        for (AABB b : boxes) {
            if (b == null) continue;
            if (eb.maxX > b.minX && eb.minX < b.maxX && eb.maxZ > b.minZ && eb.minZ < b.maxZ) {
                double dy = eb.minY - b.maxY;
                if (dy > -0.12 && dy < above) return true;
            }
        }
        return false;
    }

    static Vec3 carryPoint(Vec3 pt, Vec3 from, Vec3 to, float dyaw) {
        Vec3 v = pt.subtract(from);
        v = new Vec3(v.x, 0, v.z).yRot(-dyaw * Mth.DEG_TO_RAD).add(0, v.y, 0);
        return to.add(v);
    }

    // ── 틱 ────────────────────────────────────────────────────────
    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        ShipCollisions.add(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        ShipCollisions.remove(this);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpSteps = 1;
    }

    @Override
    public void tick() {
        super.tick();
        if (cells.isEmpty()) {
            if (!level().isClientSide && tickCount > 5) discard();
            return;
        }
        if (level().isClientSide) clientTick();
        else serverTick();
    }

    private void clientTick() {
        Vec3 prev = position();
        float prevYaw = yaw;
        if (lerpSteps > 0) {
            setPos(lerpX, lerpY, lerpZ);
            lerpSteps = 0;
        }
        yawO = yaw;
        yaw = entityData.get(YAW);
        lastDyaw = Mth.wrapDegrees(yaw - prevYaw);
        Entity lp = LOCAL_PLAYER.get();
        boolean on = lp != null && !lp.isPassenger() && standingOn(lp, 1.3);
        updateBoxes();
        Vec3 now = position();
        if (on && (now.distanceToSqr(prev) > 1e-10 || lastDyaw != 0)) {
            carryFrom = prev;
            carryTo = now;
            carryYaw = lastDyaw;
        }
        if (hasTurret) clientTurrets();
        if (empTicks() > 0 && tickCount % 2 == 0) {
            Cell c = cells.get(random.nextInt(cells.size()));
            Vec3 w = rot(c.x + 0.5, c.z + 0.5, yaw).add(now);
            level().addParticle(ParticleTypes.ELECTRIC_SPARK, w.x, now.y + c.y + 1, w.z, 0, 0.05, 0);
        }
    }

    private void serverTick() {
        int emp = empTicks();
        if (emp > 0) entityData.set(EMP, emp - 1);
        if (++inAge > 10) {
            inF = 0;
            inT = 0;
            inV = 0;
        }
        Player pilot = pilot();
        if (pilot == null && !autopilot) {
            inF = 0;
            inT = 0;
            inV = 0;
        }
        if (tickCount % 20 == 0) refuelFromHoppers();
        boolean powered = motors > 0 && emp == 0 && fuel > 0;
        int n = Math.max(1, cells.size());
        double vmax = Math.min(0.6, 0.12 + 0.16 * Math.sqrt(motors) * Math.sqrt(80.0 / Math.max(80, n)));
        double target = !powered ? 0 : inF > 0 ? vmax * inF : inF < 0 ? vmax * 0.4 * inF : 0;
        speed += (target - speed) * (target == 0 ? 0.02 : 0.035);
        if (Math.abs(speed) < 1e-4) speed = 0;
        float turn = 0;
        if (powered && inT != 0) {
            double flow = Math.min(1, Math.abs(speed) / Math.max(vmax, 0.01));
            turn = (float) (inT * (0.5 + 1.8 * flow) * Math.sqrt(60.0 / Math.max(60, n)));
            if (Math.abs(turn) < 0.3f) turn = Math.signum(turn) * 0.3f;
        }
        // 부유체: 위(Space)/아래(Ctrl). 연료가 없거나 EMP 면 천천히 가라앉는다
        boolean airborne = air && !resting();
        // 부유체·바닥을 잃어 지탱이 안 되면 추락 → 부딪히면 부서지며 블록으로 흩어진다
        falling = !air && !canBeAt(position(), yaw);
        if (falling) {
            vspeed = Math.max(vspeed - 0.04, -1.2);
            Vec3 down = position().add(0, vspeed, 0);
            if (free(down, yaw)) {
                setPos(down.x, down.y, down.z);
                updateBoxes();
                for (Entity e : level().getEntities(this, bounds.inflate(0.5, 2, 0.5), e -> !(e instanceof ShipEntity) && !e.isPassenger() && standingOn(e, 0.6 - vspeed)))
                    e.setPos(e.getX(), e.getY() + vspeed, e.getZ());
            } else crash();
            return;
        }
        if (air) {
            double vt = !powered ? (airborne ? -0.05 : 0) : inV > 0 ? 0.14 : inV < 0 ? -0.12 : 0;
            vspeed += (vt - vspeed) * 0.12;
            if (Math.abs(vspeed) < 1e-3 && vt == 0) vspeed = 0;
        } else vspeed = 0;
        // 연료 소모: 클수록 많이
        if (powered) {
            double use = Math.abs(speed) / Math.max(vmax, 0.01) * ShipFuel.thrustUse(n);
            if (airborne || vspeed > 0.01) use += ShipFuel.liftUse(n) * (vspeed > 0.01 ? 1.5 : 1.0);
            fuelAcc += use;
            int whole = (int) fuelAcc;
            if (whole > 0) {
                fuel = Math.max(0, fuel - whole);
                fuelAcc -= whole;
                FUEL_USED += whole;
            }
        }
        float newYaw = yaw - turn;
        Vec3 fwd = new Vec3(-Mth.sin(newYaw * Mth.DEG_TO_RAD), 0, Mth.cos(newYaw * Mth.DEG_TO_RAD)).scale(speed);
        Vec3 from = position();
        Vec3 to = from.add(fwd).add(0, vspeed, 0);
        lastDyaw = 0;
        if (speed != 0 || turn != 0 || vspeed != 0) {
            if (!canBeAt(to, newYaw)) {
                Vec3 flat = from.add(fwd), vert = from.add(0, vspeed, 0);
                if (vspeed != 0 && canBeAt(flat, newYaw)) {          // 위아래가 막힘 → 수평만
                    to = flat;
                    vspeed = 0;
                } else if (vspeed != 0 && canBeAt(vert, yaw)) {     // 앞이 막힘 → 위아래만
                    to = vert;
                    newYaw = yaw;
                    speed *= -0.25;
                } else {
                    if (Math.abs(speed) > 0.12)
                        level().playSound(null, getX(), getY(), getZ(), SoundEvents.WOOD_BREAK, SoundSource.NEUTRAL, 1.2f, 0.6f);
                    speed *= -0.25;
                    vspeed = 0;
                    to = from;
                    if (!canBeAt(from, newYaw)) newYaw = yaw;
                }
            }
            float dy = Mth.wrapDegrees(newYaw - yaw);
            if (dy != 0 || to.distanceToSqr(from) > 0) {
                // 갑판 위 몹·아이템 (플레이어는 각자 클라이언트가 처리)
                List<Entity> riders = level().getEntities(this, bounds.inflate(0.5, 2, 0.5),
                        e -> !(e instanceof Player) && !(e instanceof ShipEntity) && !e.isPassenger() && standingOn(e, 0.6));
                setPos(to.x, to.y, to.z);
                yawO = yaw;
                yaw = newYaw;
                lastDyaw = dy;
                entityData.set(YAW, yaw);
                updateBoxes();
                for (Entity e : riders) {
                    Vec3 np = carryPoint(e.position(), from, to, dy);
                    e.setPos(np.x, np.y, np.z);
                    e.setYRot(e.getYRot() + dy);
                    e.setYHeadRot(e.getYHeadRot() + dy);
                    e.resetFallDistance();
                    CARRIED++;
                }
                MOVED_TICKS++;
            }
        }
        entityData.set(SPEED, (float) speed);
        if (hasTurret && level() instanceof ServerLevel tl) tickTurrets(tl, emp == 0);
        if (hasConveyor && emp == 0)
            for (Entity e : level().getEntities(this, bounds.inflate(0.5, 2, 0.5), e -> !(e instanceof Player) && !(e instanceof ShipEntity) && !e.isPassenger()))
                if (conveyorPush(e)) CONVEYOR_PUSHES++;
        // 서 있는 플레이어가 '공중에 떠 있다'고 쫓겨나지 않도록 + 낙하 피해 방지
        for (Player p : level().players())
            if (p instanceof ServerPlayer sp && standingOn(sp, 0.6)) {
                resetFlying(sp);
                sp.resetFallDistance();
            }
        // 조종사 계기판
        if (pilot instanceof ServerPlayer sp && tickCount % 10 == 0) {
            int cap = Math.max(1, motors * ShipFuel.MOTOR_CAPACITY);
            Component msg = Component.translatable(air ? "message.tycooncraft.ship.hud_air" : "message.tycooncraft.ship.hud",
                    fuel * 100 / cap, fuelText(fuel), String.format("%.1f", Math.abs(speed) * 20), (int) Math.floor(getY()));
            sp.displayClientMessage(msg.copy().withStyle(fuel <= 0 ? ChatFormatting.RED : fuel * 10 < cap ? ChatFormatting.YELLOW : ChatFormatting.AQUA), true);
        }
        // 프로펠러 물보라 · 배기 · 소리
        if ((Math.abs(speed) > 0.03 || (airborne && powered)) && level() instanceof ServerLevel sl) {
            boolean water = !airborne;
            if (tickCount % 2 == 0)
                for (Cell c : cells) {
                    if (!(c.state.getBlock() instanceof ShipMotorBlock)) continue;
                    Vec3 w = rot(c.x + 0.5, c.z + 0.5, yaw).add(position());
                    if (water && Math.abs(speed) > 0.03) {
                        sl.sendParticles(ParticleTypes.BUBBLE, w.x, getY() + c.y + 0.3, w.z, 4, 0.4, 0.2, 0.4, 0.05);
                        sl.sendParticles(ParticleTypes.SPLASH, w.x, getY() + c.y + 0.9, w.z, 3, 0.4, 0.1, 0.4, 0.1);
                    } else sl.sendParticles(ParticleTypes.SMOKE, w.x, getY() + c.y + 0.6, w.z, 2, 0.25, 0.1, 0.25, 0.01);
                }
            if (++soundT % 14 == 0) {
                if (water) level().playSound(null, getX(), getY(), getZ(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.NEUTRAL, 1.0f, 0.7f + (float) Math.abs(speed));
                else level().playSound(null, getX(), getY(), getZ(), SoundEvents.MINECART_RIDING, SoundSource.NEUTRAL, 0.35f, 0.6f + (float) Math.abs(speed));
            }
        }
    }

    // ── 셀 찾기 ───────────────────────────────────────────────────
    static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    @Nullable
    Cell cellAt(int x, int y, int z) {
        Integer i = index.get(key(x, y, z));
        return i == null ? null : cells.get(i);
    }

    /** 월드 좌표 → 원점 기준 로컬 좌표 */
    public Vec3 toLocal(Vec3 w) {
        Vec3 v = new Vec3(w.x - getX(), 0, w.z - getZ()).yRot((yaw - yaw0) * Mth.DEG_TO_RAD);
        return new Vec3(v.x + pivotX, w.y - getY(), v.z + pivotZ);
    }

    public Vec3 cellCenter(Cell c) {
        return rot(c.x + 0.5, c.z + 0.5, yaw).add(position()).add(0, c.y + 0.5, 0);
    }

    // ── 컨베이어: 실린 컨베이어도 위의 물건을 민다 (방향은 운송기와 함께 돈다) ──
    public boolean conveyorPush(Entity e) {
        if (e.isShiftKeyDown() || e.isSpectator()) return false;
        Vec3 l = toLocal(e.position());
        int lx = Mth.floor(l.x), lz = Mth.floor(l.z), ly = Mth.floor(l.y - 0.3);
        Cell c = cellAt(lx, ly, lz);
        if (c == null || !(c.state.getBlock() instanceof com.tycooncraft.block.ConveyorBlock)) return false;
        if (l.y < c.y + 0.4 || l.y > c.y + 0.9) return false;
        Direction dir = c.state.getValue(HorizontalDirectionalBlock.FACING);
        double sp = com.tycooncraft.block.ConveyorBlock.pushSpeed(c.state);
        double vx, vz;
        if (dir.getAxis() == Direction.Axis.X) {
            vx = dir.getStepX() * sp;
            vz = (lz + 0.5 - l.z) * 0.2;
        } else {
            vz = dir.getStepZ() * sp;
            vx = (lx + 0.5 - l.x) * 0.2;
        }
        Vec3 w = new Vec3(vx, 0, vz).yRot(-(yaw - yaw0) * Mth.DEG_TO_RAD);
        e.setDeltaMovement(w.x, e.getDeltaMovement().y, w.z);
        return true;
    }

    // ── 포탑: 실린 포탑도 적을 찾아 쏜다 (탄약은 그 포탑 블록에 들어 있던 것 + 위로 굴러온 탄약) ──
    private Vec3 turretMuzzle(Cell c) {
        return rot(c.x + 0.5, c.z + 0.5, yaw).add(position()).add(0, c.y + 0.7 + 0.45, 0);
    }

    private boolean turretSees(Vec3 from, Entity e) {
        if (!e.isAlive() || !(e instanceof net.minecraft.world.entity.monster.Enemy) || e.distanceToSqr(from) > 24 * 24) return false;
        Vec3 eye = e.getBoundingBox().getCenter().add(0, e.getBbHeight() * 0.25, 0);
        Vec3 st = from.add(eye.subtract(from).normalize().scale(0.8));
        return level().clip(new net.minecraft.world.level.ClipContext(st, eye, net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty())).getType()
                == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private void tickTurrets(ServerLevel sl, boolean powered) {
        boolean changed = false;
        for (Cell c : cells) {
            if (c.state.getBlock() instanceof com.tycooncraft.turret.CiwsBlock) {   // 실린 요격 포대
                long k = key(c.x, c.y, c.z);
                var st = ciws.computeIfAbsent(k, kk -> new com.tycooncraft.turret.Interceptor.State());
                Vec3 m = turretMuzzle(c);
                absorbAmmo(sl, c, m);
                java.util.UUID owner = c.be != null && c.be.hasUUID("Owner") ? c.be.getUUID("Owner") : null;
                if (powered) {
                    int before = st.target;
                    com.tycooncraft.turret.Interceptor.tick(sl, m, st, owner, () -> takeAmmo(c), this);
                    if (before != st.target) changed = true;
                    turrets.computeIfAbsent(k, kk -> new Turret()).target = st.target;
                }
                continue;
            }
            if (!(c.state.getBlock() instanceof com.tycooncraft.turret.TurretBlock)) continue;
            long k = key(c.x, c.y, c.z);
            Turret t = turrets.computeIfAbsent(k, kk -> new Turret());
            Vec3 m = turretMuzzle(c);
            absorbAmmo(sl, c, m);
            if (t.cooldown > 0) t.cooldown--;
            Entity target = t.target >= 0 ? sl.getEntity(t.target) : null;
            if (target == null || !turretSees(m, target)) {
                target = null;
                if (--t.search <= 0) {
                    t.search = 10;
                    double bd = Double.MAX_VALUE;
                    for (Entity e : sl.getEntities(this, new AABB(m, m).inflate(24), e -> e instanceof net.minecraft.world.entity.monster.Enemy)) {
                        double d = e.distanceToSqr(m);
                        if (d < bd && turretSees(m, e)) {
                            bd = d;
                            target = e;
                        }
                    }
                }
                int id = target == null ? -1 : target.getId();
                if (id != t.target) {
                    t.target = id;
                    changed = true;
                }
            }
            if (!powered || target == null || t.cooldown > 0) continue;
            Boolean heavy = takeAmmo(c);
            if (heavy == null) continue;
            t.cooldown = 3;
            Vec3 aim = target.getBoundingBox().getCenter().add(0, target.getBbHeight() * 0.15, 0);
            Vec3 dir = aim.subtract(m).normalize().add(sl.random.nextGaussian() * 0.02, sl.random.nextGaussian() * 0.02, sl.random.nextGaussian() * 0.02).normalize();
            ShipCollisions.IGNORE.set(this);
            try {
                com.tycooncraft.gun.Ballistics.shoot(sl, null, m.add(dir.scale(0.9)), dir, 28, heavy ? 7f : 5f, 0, 1.5f, false, true,
                        sl.damageSources().generic(), e -> !(e instanceof Player));
            } finally {
                ShipCollisions.IGNORE.remove();
            }
            TURRET_SHOTS++;
            sl.playSound(null, m.x, m.y, m.z, com.tycooncraft.registry.ModSounds.RIFLE_FIRE.get(), SoundSource.BLOCKS, 1.6f, 1.25f + sl.random.nextFloat() * 0.1f);
        }
        if (changed) {
            CompoundTag tag = new CompoundTag();
            turrets.forEach((k, t) -> tag.putInt(Long.toString(k), t.target));
            entityData.set(TURRETS, tag);
        }
    }

    /** 포탑 셀의 탄약 목록에서 1발. 중탄 우선. 없으면 null */
    @Nullable
    private Boolean takeAmmo(Cell c) {
        if (c.be == null || !c.be.contains("Ammo")) return null;
        ListTag list = c.be.getList("Ammo", Tag.TAG_COMPOUND);
        int pick = -1;
        boolean heavy = false;
        for (int i = 0; i < list.size(); i++) {
            ItemStack s = ItemStack.parseOptional(level().registryAccess(), list.getCompound(i));
            if (s.is(com.tycooncraft.registry.ModItems.HEAVY_AMMO.get())) {
                pick = i;
                heavy = true;
                break;
            }
            if (pick < 0 && s.is(com.tycooncraft.registry.ModItems.LIGHT_AMMO.get())) pick = i;
        }
        if (pick < 0) return null;
        ItemStack s = ItemStack.parseOptional(level().registryAccess(), list.getCompound(pick));
        s.shrink(1);
        if (s.isEmpty()) list.remove(pick);
        else list.set(pick, s.save(level().registryAccess()));
        return heavy;
    }

    /** 컨베이어로 굴러온 탄약이 포탑에 닿으면 넣는다 */
    private void absorbAmmo(ServerLevel sl, Cell c, Vec3 muzzle) {
        if (tickCount % 4 != 0) return;
        Vec3 ctr = cellCenter(c);
        for (net.minecraft.world.entity.item.ItemEntity it : sl.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(ctr, ctr).inflate(1.0, 1.0, 1.0))) {
            ItemStack s = it.getItem();
            if (!s.is(com.tycooncraft.registry.ModItems.HEAVY_AMMO.get()) && !s.is(com.tycooncraft.registry.ModItems.LIGHT_AMMO.get())) continue;
            CompoundTag be = c.be;
            if (be == null) continue;
            ListTag list = be.getList("Ammo", Tag.TAG_COMPOUND);
            if (list.size() >= 9) continue;
            list.add(s.save(level().registryAccess()));
            be.put("Ammo", list);
            it.discard();
        }
    }

    /** 클라이언트: 포탑 몸통이 목표를 향해 돈다 */
    private void clientTurrets() {
        CompoundTag tag = entityData.get(TURRETS);
        for (Cell c : cells) {
            boolean isCiws = c.state.getBlock() instanceof com.tycooncraft.turret.CiwsBlock;
            if (!(c.state.getBlock() instanceof com.tycooncraft.turret.TurretBlock) && !isCiws) continue;
            long k = key(c.x, c.y, c.z);
            Turret t = turrets.computeIfAbsent(k, kk -> new Turret());
            t.yawO = t.yaw;
            t.pitchO = t.pitch;
            int id = tag.contains(Long.toString(k)) ? tag.getInt(Long.toString(k)) : -1;
            Entity target = id >= 0 ? level().getEntity(id) : null;
            float ty, tp;
            if (target != null) {
                Vec3 d = target.getBoundingBox().getCenter().subtract(turretMuzzle(c));
                ty = (float) Mth.atan2(-d.x, -d.z);
                tp = (float) Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
            } else {
                ty = t.yaw + 0.01f;
                tp = 0;
            }
            float dy = Mth.wrapDegrees((ty - t.yaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            t.yaw += Mth.clamp(dy, isCiws ? -0.6f : -0.25f, isCiws ? 0.6f : 0.25f);
            t.pitch += Mth.clamp(tp - t.pitch, isCiws ? -0.4f : -0.15f, isCiws ? 0.4f : 0.15f);
        }
    }

    @Nullable
    public Turret turretAt(Cell c) {
        return turrets.get(key(c.x, c.y, c.z));
    }

    // ── 피격: 폭발에 선체 블록이 부서지고 운송기가 흔들린다 ──────────────
    public void blast(net.minecraft.world.level.Explosion ex) {
        if (isRemoved() || cells.isEmpty()) return;
        Vec3 c = ex.center();
        float r = ex.radius();
        if (!bounds.inflate(r * 2 + 1).contains(c)) return;
        List<Integer> broken = new ArrayList<>();
        double nearest = Double.MAX_VALUE;
        for (int i = 0; i < cells.size(); i++) {
            Cell cell = cells.get(i);
            double d = cellCenter(cell).distanceTo(c);
            nearest = Math.min(nearest, d);
            if (d > r * 2) continue;
            float res = cell.state.getBlock().getExplosionResistance();
            // 바닐라 폭발 광선과 같은 감쇠: 세기 r·(0.7~1.3) − 거리·0.75 − (저항+0.3)·0.3
            double f = r * (0.7 + random.nextDouble() * 0.6) - d * 0.75 - (res + 0.3) * 0.3;
            if (f > 0 && cell.state.getDestroySpeed(level(), BlockPos.ZERO) >= 0) broken.add(i);
        }
        if (nearest > r * 2) return;
        // 충격: 폭발 반대쪽으로 밀리고 기울어짐
        double k = Math.max(0, 1 - nearest / (r * 2)) * r * 0.06;
        Vec3 push = position().add(0, sizeY / 2.0, 0).subtract(c).normalize().scale(k);
        Vec3 fwd = new Vec3(-Mth.sin(yaw * Mth.DEG_TO_RAD), 0, Mth.cos(yaw * Mth.DEG_TO_RAD));
        speed += push.dot(fwd);
        if (air) vspeed += push.y;
        float jolt = (float) ((random.nextDouble() - 0.5) * k * 40);
        if (canBeAt(position(), yaw + jolt)) {
            yaw += jolt;
            entityData.set(YAW, yaw);
            updateBoxes();
        }
        if (!broken.isEmpty()) removeCells(broken, r);
    }

    /** 셀 제거 (서버): 드롭·연료 손실·동기화 */
    void removeCells(List<Integer> idx, float power) {
        idx.sort(java.util.Collections.reverseOrder());
        List<Long> keys = new ArrayList<>();
        int motorsBefore = motors;
        for (int i : idx) {
            Cell c = cells.get(i);
            Vec3 w = cellCenter(c);
            BlockPos bp = BlockPos.containing(w);
            if (random.nextFloat() < 1f / Math.max(1f, power)) Block.popResource(level(), bp, new ItemStack(c.state.getBlock().asItem()));
            if (c.be != null)
                for (String list : new String[]{"Items", "Ammo"})
                    for (Tag t : c.be.getList(list, Tag.TAG_COMPOUND))
                        Block.popResource(level(), bp, ItemStack.parseOptional(level().registryAccess(), (CompoundTag) t));
            if (level() instanceof ServerLevel sl)
                sl.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, c.state), w.x, w.y, w.z, 8, 0.3, 0.3, 0.3, 0.1);
            keys.add(key(c.x, c.y, c.z));
            turrets.remove(key(c.x, c.y, c.z));
            ciws.remove(key(c.x, c.y, c.z));
            cells.remove(i);
            BLAST_CELLS++;
        }
        boolean helmLost = helm >= 0 && idx.contains(helm);
        rebuild(true);
        if (motors < motorsBefore && motorsBefore > 0) fuel = (int) ((long) fuel * motors / motorsBefore);   // 엔진과 함께 탱크도 날아감
        setFuel(fuel);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(this, new ShipPayloads.Damage(getId(), keys));
        if (helmLost) ejectPassengers();
        if (cells.isEmpty()) discard();
    }

    /** 테스트·명령어용: 조건에 맞는 셀을 부순다 */
    public void breakWhere(java.util.function.Predicate<Cell> p) {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < cells.size(); i++) if (p.test(cells.get(i))) idx.add(i);
        if (!idx.isEmpty()) removeCells(idx, 1f);
    }

    /** 클라이언트: 서버가 알려 준 셀 제거 */
    public void removeCellsClient(List<Long> keys) {
        java.util.Set<Long> ks = new java.util.HashSet<>(keys);
        cells.removeIf(c -> ks.contains(key(c.x, c.y, c.z)));
        rebuild(true);
    }

    /** 추락해 땅에 부딪힘: 충격으로 일부가 부서지고 나머지는 블록으로 내려앉는다 */
    private void crash() {
        double v = -vspeed;
        vspeed = 0;
        if (v < 0.35) {        // 살짝 내려앉음 → 그대로 블록으로
            disassemble(null);
            return;
        }
        CRASHES++;
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 2f, 0.7f);
        if (level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.EXPLOSION, getX(), getY(), getZ(), 6, radius / 2, 1, radius / 2, 0);
        List<Integer> broken = new ArrayList<>();
        double chance = Math.min(0.6, v * 0.35);
        for (int i = 0; i < cells.size(); i++) if (i != helm && random.nextDouble() < chance) broken.add(i);
        if (!broken.isEmpty() && broken.size() < cells.size()) removeCells(broken, 3f);
        if (!isRemoved()) disassemble(null);
    }

    // ── 연료 ──────────────────────────────────────────────────────
    public static long FUEL_USED;

    public int fuel() {
        return fuel;
    }

    public void setFuel(int f) {
        fuel = Math.max(0, Math.min(Math.max(1, motors) * ShipFuel.MOTOR_CAPACITY, f));
    }

    public int floatCount() {
        return floats;
    }

    public boolean canFly() {
        return air;
    }

    public double verticalSpeed() {
        return vspeed;
    }

    public static String fuelText(int f) {
        return f >= 10_000 ? String.format("%.1fk", f / 1000.0) : Integer.toString(f);
    }

    /** 운송기에 실린 호퍼 안의 연료를 탱크로 (호퍼로도 연료 공급) */
    void refuelFromHoppers() {
        int cap = motors * ShipFuel.MOTOR_CAPACITY;
        for (Cell c : cells) {
            if (fuel >= cap) return;
            if (c.be == null || !c.state.is(Blocks.HOPPER) || !c.be.contains("Items")) continue;
            ListTag items = c.be.getList("Items", Tag.TAG_COMPOUND);
            for (int i = 0; i < items.size(); i++) {
                CompoundTag it = items.getCompound(i);
                ItemStack st = ItemStack.parseOptional(level().registryAccess(), it);
                int v = ShipFuel.value(st);
                if (v <= 0 || fuel + v > cap) continue;
                int k = Math.min(st.getCount(), (cap - fuel) / v);
                fuel += k * v;
                st.shrink(k);
                REFUELS += k;
                if (st.isEmpty()) items.remove(i--);
                else {
                    CompoundTag slot = new CompoundTag();
                    slot.putByte("Slot", it.getByte("Slot"));
                    items.set(i, st.save(level().registryAccess(), slot));
                }
            }
        }
    }

    public static int REFUELS;

    /** 조종기에 연료를 들고 우클릭 → 바로 주입 */
    boolean refuelFromHand(ServerPlayer player) {
        ItemStack hand = player.getMainHandItem();
        int v = ShipFuel.value(hand);
        if (v <= 0) return false;
        int cap = motors * ShipFuel.MOTOR_CAPACITY;
        int k = Math.min(hand.getCount(), (cap - fuel) / v);
        if (k > 0) {
            fuel += k * v;
            if (!player.getAbilities().instabuild) hand.shrink(k);
            level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUCKET_FILL_LAVA, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        player.displayClientMessage(Component.translatable("message.tycooncraft.ship.refueled", fuelText(fuel), fuelText(cap)).withStyle(ChatFormatting.GOLD), true);
        return true;
    }

    /** 바닥이 땅·물에 얹혀 있나 (그러면 부양 연료를 안 쓴다) */
    boolean resting() {
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        for (Cell c : cells) {
            if (c.y != 0) continue;
            Vec3 w = rot(c.x + 0.5, c.z + 0.5, yaw).add(position());
            bp.set(Mth.floor(w.x), Mth.floor(getY() - 0.05), Mth.floor(w.z));
            BlockState st = level().getBlockState(bp);
            if (!st.getCollisionShape(level(), bp).isEmpty() || level().getFluidState(bp).is(FluidTags.WATER)
                    || level().getFluidState(bp.above()).is(FluidTags.WATER)) return true;
        }
        return false;
    }

    private static java.lang.reflect.Field flyField;

    private static void resetFlying(ServerPlayer sp) {
        try {
            if (flyField == null) {
                flyField = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
                flyField.setAccessible(true);
            }
            flyField.setInt(sp.connection, 0);
        } catch (Throwable ignored) {
        }
    }

    /**
     * 그 위치·방향에 블록이 겹치지 않아야 갈 수 있다.
     * 배: 바닥 일부가 물 위 / 부유체 운송기: 어디든 (단 바닥줄 위로는 물에 잠기면 안 됨 — 물에 내려앉으면 뜬다)
     */
    boolean canBeAt(Vec3 pos, float yawDeg) {
        return check(pos, yawDeg, air);
    }

    /** 물 조건 없이 블록에만 안 겹치면 됨 (추락용) */
    boolean free(Vec3 pos, float yawDeg) {
        return check(pos, yawDeg, true);
    }

    private boolean check(Vec3 pos, float yawDeg, boolean air) {
        int bottom = 0, wet = 0;
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        if (air && (pos.y < level().getMinBuildHeight() || pos.y + sizeY > level().getMaxBuildHeight())) return false;
        for (Cell c : cells) {
            Vec3 w = rot(c.x + 0.5, c.z + 0.5, yawDeg).add(pos);
            int y0 = Mth.floor(pos.y + c.y + 0.5), y1 = y0;
            if (air) {            // 세로로 걸칠 수 있다
                y0 = Mth.floor(pos.y + c.y + 0.02);
                y1 = Mth.floor(pos.y + c.y + 0.98);
            }
            for (int y = y0; y <= y1; y++) {
                bp.set(Mth.floor(w.x), y, Mth.floor(w.z));
                if (!level().isLoaded(bp)) return false;
                BlockState st = level().getBlockState(bp);
                if (!st.isAir() && !(st.getBlock() instanceof LiquidBlock) && !st.is(Blocks.LILY_PAD)
                        && !st.getCollisionShape(level(), bp).isEmpty()) return false;
                if (air && !falling && c.y > 0 && y == y1 && level().getFluidState(bp).is(FluidTags.WATER)) return false;
            }
            if (c.y == 0) {
                bottom++;
                bp.set(Mth.floor(w.x), Mth.floor(pos.y + 0.5), Mth.floor(w.z));
                if (level().getFluidState(bp).is(FluidTags.WATER) || level().getFluidState(bp.below()).is(FluidTags.WATER)) wet++;
            }
        }
        return air || (wet > 0 && wet * 5 >= bottom);
    }

    @Nullable
    public Player pilot() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    /** 자동 테스트용: 조종사 없이 입력 */
    public boolean autopilot;

    public void testInput(float f, float t) {
        testInput(f, t, 0);
    }

    public void testInput(float f, float t, float v) {
        inF = f;
        inT = t;
        inV = v;
        inAge = 0;
    }

    public void input(Player from, float f, float t, float v) {
        if (from != pilot()) return;
        inF = Mth.clamp(f, -1, 1);
        inT = Mth.clamp(t, -1, 1);
        inV = Mth.clamp(v, -1, 1);
        inAge = 0;
    }

    // ── 조종기 상호작용 (클라이언트가 레이캐스트 → 패킷) ─────────────────
    public void useHelm(ServerPlayer player, boolean sneak) {
        if (player.distanceToSqr(helmWorld()) > 49) return;
        if (!sneak && refuelFromHand(player)) return;
        if (sneak) {
            if (Math.abs(speed) > 0.08 || Math.abs(vspeed) > 0.03) {
                player.displayClientMessage(Component.translatable("message.tycooncraft.ship.stop_first").withStyle(ChatFormatting.YELLOW), true);
                return;
            }
            disassemble(player);
            return;
        }
        if (pilot() != null) {
            player.displayClientMessage(Component.translatable("message.tycooncraft.ship.occupied").withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        player.startRiding(this, true);
        player.displayClientMessage(Component.translatable(air ? "message.tycooncraft.ship.controls_air" : "message.tycooncraft.ship.controls").withStyle(ChatFormatting.AQUA), true);
    }

    /** 정박: 가장 가까운 직각 방향으로 맞춰 블록으로 되돌린다 */
    public int disassemble(@Nullable ServerPlayer player) {
        if (isRemoved()) return 0;
        ejectPassengers();
        float d = Mth.wrapDegrees(yaw - yaw0);
        int k = Math.floorMod(Math.round(d / 90f), 4);
        Rotation rotation = switch (k) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        float snapped = yaw0 + k * 90f;
        if (getY() != Math.floor(getY())) {   // 공중·걸친 높이 → 가까운 정수 칸으로 (위쪽이 비어 있으면 위로)
            double up = Math.ceil(getY()), down = Math.floor(getY());
            setPos(getX(), canBeAt(new Vec3(getX(), down, getZ()), snapped) && getY() - down < 0.5 ? down : up, getZ());
        }
        List<Cell> order = new ArrayList<>(cells);
        order.sort(Comparator.<Cell>comparingInt(c -> c.state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO) ? 0 : 1).thenComparingInt(Cell::y));
        Level lv = level();
        List<BlockPos> placed = new ArrayList<>();
        int dropped = 0;
        for (Cell c : order) {
            Vec3 w = rot(c.x + 0.5, c.z + 0.5, snapped).add(position());
            BlockPos bp = BlockPos.containing(w.x, getY() + c.y + 0.5, w.z);
            BlockState here = lv.getBlockState(bp);
            if (here.isAir() || here.canBeReplaced() || here.getBlock() instanceof LiquidBlock) {
                BlockState ns = c.state.rotate(lv, bp, rotation);
                if (ns.hasProperty(BlockStateProperties.WATERLOGGED))
                    ns = ns.setValue(BlockStateProperties.WATERLOGGED, here.getFluidState().is(Fluids.WATER) && here.getFluidState().isSource());
                lv.setBlock(bp, ns, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                if (c.be != null) {
                    BlockEntity be = lv.getBlockEntity(bp);
                    if (be != null) {
                        be.loadWithComponents(c.be, lv.registryAccess());
                        be.setChanged();
                    }
                }
                placed.add(bp);
            } else {
                Block.popResource(lv, bp, new ItemStack(c.state.getBlock().asItem()));
                dropped++;
            }
        }
        // 남은 연료를 엔진들에 고르게 되돌린다 (다음 출발 때 다시 합쳐짐)
        List<ShipMotorBlockEntity> engines = new ArrayList<>();
        for (BlockPos bp : placed) if (lv.getBlockEntity(bp) instanceof ShipMotorBlockEntity m) engines.add(m);
        for (int i = 0; i < engines.size(); i++) engines.get(i).setFuel(fuel / engines.size() + (i < fuel % engines.size() ? 1 : 0));
        for (BlockPos bp : placed) lv.blockUpdated(bp, lv.getBlockState(bp).getBlock());
        lv.playSound(null, getX(), getY(), getZ(), SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.5f, 0.6f);
        if (player != null)
            player.displayClientMessage(Component.translatable("message.tycooncraft.ship.docked", placed.size()).withStyle(ChatFormatting.GREEN), true);
        discard();
        return placed.size() - dropped;
    }

    // ── 탑승 ──────────────────────────────────────────────────────
    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty() && passenger instanceof Player;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dims, float scale) {
        return seatWorld().subtract(position()).add(0, 0.45, 0);
    }

    @Override
    protected void positionRider(Entity passenger, MoveFunction move) {
        super.positionRider(passenger, move);
        if (lastDyaw != 0) {
            passenger.setYRot(passenger.getYRot() + lastDyaw);
            passenger.setYHeadRot(passenger.getYHeadRot() + lastDyaw);
        }
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        return seatWorld().add(0, 0.05, 0);
    }

    // ── 기타 엔티티 성질 ───────────────────────────────────────────
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return bounds.inflate(1);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        double r = radius * 2 + 96;
        return d < r * r;
    }

    // ── 저장 / 동기화 ─────────────────────────────────────────────
    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Cell c : cells) {
            CompoundTag t = new CompoundTag();
            t.putInt("x", c.x);
            t.putInt("y", c.y);
            t.putInt("z", c.z);
            t.put("S", NbtUtils.writeBlockState(c.state));
            if (c.be != null) t.put("BE", c.be);
            list.add(t);
        }
        tag.put("Cells", list);
        tag.putFloat("ShipYaw", yaw);
        tag.putFloat("Yaw0", yaw0);
        tag.putInt("Fuel", fuel);
        tag.putDouble("VSpeed", vspeed);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        cells.clear();
        var lookup = level().holderLookup(Registries.BLOCK);
        for (Tag t0 : tag.getList("Cells", Tag.TAG_COMPOUND)) {
            CompoundTag t = (CompoundTag) t0;
            cells.add(new Cell(t.getInt("x"), t.getInt("y"), t.getInt("z"), NbtUtils.readBlockState(lookup, t.getCompound("S")),
                    t.contains("BE") ? t.getCompound("BE") : null));
        }
        yaw0 = tag.getFloat("Yaw0");
        yaw = yawO = tag.getFloat("ShipYaw");
        entityData.set(YAW, yaw);
        rebuild();
        fuel = tag.getInt("Fuel");
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(yaw0);
        buf.writeFloat(yaw);
        buf.writeVarInt(cells.size());
        for (Cell c : cells) {
            buf.writeVarInt(c.x);
            buf.writeVarInt(c.y);
            buf.writeVarInt(c.z);
            buf.writeVarInt(Block.getId(c.state));
        }
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buf) {
        yaw0 = buf.readFloat();
        yaw = yawO = buf.readFloat();
        int n = buf.readVarInt();
        cells.clear();
        for (int i = 0; i < n; i++) cells.add(new Cell(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), Block.stateById(buf.readVarInt()), null));
        rebuild();
    }
}
