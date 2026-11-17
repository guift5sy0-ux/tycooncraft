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
    private float inF, inT, inV;
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
        int mx = 0, mz = 0, my = 0;
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
            if (c.state.getBlock() instanceof ShipHelmBlock) helm = i;
        }
        air = floats > 0 && floats * ShipFuel.LIFT_PER_FLOAT >= cells.size();
        pivotX = (mx + 1) / 2.0;
        pivotZ = (mz + 1) / 2.0;
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
                if (air && c.y > 0 && y == y1 && level().getFluidState(bp).is(FluidTags.WATER)) return false;
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
