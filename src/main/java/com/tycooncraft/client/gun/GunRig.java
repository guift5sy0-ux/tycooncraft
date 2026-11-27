package com.tycooncraft.client.gun;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tycooncraft.TycoonCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.util.*;

/**
 * v0.32: 총 모델을 움직이는 부품(슬라이드·노리쇠·탄창·펌프·장전손잡이·로켓 탄두)으로 나눔.
 * 모델 JSON 의 요소 이름으로 부품을 정하고, 구워진 모델(소음기·파츠가 붙은 것 포함)의 면 하나하나를
 * 요소의 면과 위치·방향으로 맞춰 어느 부품인지 분류한다. 파츠 모델처럼 맞는 면이 없으면 몸통.
 */
public final class GunRig {
    /** 부품 이름 (부모 부품을 따라 같이 움직이고, 자기 움직임을 더함) */
    public enum Part {
        SLIDE(null), MAG(null), CHARGE(null), DUSTCOVER(null), PUMP(null), BOLT(null), HANDLE(BOLT), ROCKET(null);
        public final Part parent;

        Part(Part parent) {
            this.parent = parent;
        }
    }

    /** 총 하나의 부품 정의: 요소 이름 → 부품 */
    private static final Map<String, Map<String, Part>> DEFS = new HashMap<>();

    private static void def(String gun, Part part, String... names) {
        Map<String, Part> m = DEFS.computeIfAbsent(gun, k -> new HashMap<>());
        for (String n : names) m.put(n, part);
    }

    static {
        def("pistol", Part.SLIDE, "slide", "slide_top_flat", "front_sight", "rear_sight_l", "rear_sight_r", "barrel_crown", "extractor", "cover_plate");
        def("pistol", Part.MAG, "mag_base", "mag_body");
        def("judge", Part.SLIDE, "slide", "glow_top_rib", "glow_side_l", "glow_side_r", "slide_chamfer_l", "slide_chamfer_r", "rear_sight_l", "rear_sight_r", "glow_front_sight");
        def("judge", Part.MAG, "mag_base", "mag_body");
        def("smg", Part.CHARGE, "cocking_lever", "cocking_knob");
        def("smg", Part.MAG, "mag_upper", "mag_lower", "mag_base");
        def("rifle", Part.CHARGE, "charging_handle");
        def("rifle", Part.DUSTCOVER, "dust_cover");
        def("rifle", Part.MAG, "mag_upper", "mag_lower", "mag_base");
        def("shotgun", Part.PUMP, "forend", "action_bar_l", "action_bar_r");
        def("sniper", Part.BOLT, "bolt_body", "bolt_shroud");
        def("sniper", Part.HANDLE, "bolt_stem", "bolt_knob");
        def("sniper", Part.MAG, "magazine", "mag_base");
        def("vss", Part.CHARGE, "charging_handle");
        def("vss", Part.MAG, "magazine", "mag_lower", "mag_base");
        def("rt20", Part.BOLT, "bolt_rear");
        def("rt20", Part.HANDLE, "bolt_handle", "bolt_knob");
        def("rpg", Part.ROCKET, "fuze", "cone1", "cone2", "warhead", "warhead_band", "neck");
    }

    /** 모델 공간(0~1) 상자 */
    public record Box(Vector3f min, Vector3f max) {
        public Vector3f at(float fx, float fy, float fz) {
            return new Vector3f(min.x + (max.x - min.x) * fx, min.y + (max.y - min.y) * fy, min.z + (max.z - min.z) * fz);
        }
    }

    private record Face(Vector3f center, Vector3f normal, Part part) {}

    /** 총 종류별: 분류용 면 목록 + 부품별 경계 상자 + 이름별 요소 상자 */
    public static final class Def {
        final List<Face> faces = new ArrayList<>();
        public final EnumMap<Part, Box> bounds = new EnumMap<>(Part.class);
        public final Map<String, Box> elements = new HashMap<>();
    }

    private static final Map<String, Def> LOADED = new HashMap<>();
    private static final Map<BakedModel, Split> SPLITS = new WeakHashMap<>();

    public static void clear() {
        LOADED.clear();
        SPLITS.clear();
    }

    public static Def def(String gun) {
        return LOADED.computeIfAbsent(gun, GunRig::load);
    }

    private static Def load(String gun) {
        Def d = new Def();
        Map<String, Part> names = DEFS.getOrDefault(gun, Map.of());
        // 기본 모델 + 소음기 변형 (요소 좌표가 다르므로 모두 등록)
        for (String variant : new String[]{gun, gun + "_silenced", gun + "_silenced2"}) {
            JsonObject json = read(variant);
            if (json == null || !json.has("elements")) continue;
            boolean primary = variant.equals(gun);
            for (JsonElement je : json.getAsJsonArray("elements")) {
                JsonObject e = je.getAsJsonObject();
                String name = e.has("name") ? e.get("name").getAsString() : "";
                Vector3f from = vec(e.getAsJsonArray("from")).div(16f), to = vec(e.getAsJsonArray("to")).div(16f);
                if (primary && !name.isEmpty()) d.elements.putIfAbsent(name, new Box(from, to));
                Part part = names.get(name);
                if (part == null) continue;
                if (primary) {
                    Box b = d.bounds.get(part);
                    d.bounds.put(part, b == null ? new Box(new Vector3f(from), new Vector3f(to))
                            : new Box(new Vector3f(b.min()).min(from), new Vector3f(b.max()).max(to)));
                }
                Rot rot = e.has("rotation") ? new Rot(e.getAsJsonObject("rotation")) : null;
                JsonObject faces = e.getAsJsonObject("faces");
                for (Direction dir : Direction.values()) {
                    if (faces == null || !faces.has(dir.getName())) continue;
                    Vector3f c = new Vector3f((from.x + to.x) / 2, (from.y + to.y) / 2, (from.z + to.z) / 2);
                    Vector3f n = new Vector3f(dir.getStepX(), dir.getStepY(), dir.getStepZ());
                    switch (dir) {
                        case DOWN -> c.y = from.y;
                        case UP -> c.y = to.y;
                        case NORTH -> c.z = from.z;
                        case SOUTH -> c.z = to.z;
                        case WEST -> c.x = from.x;
                        case EAST -> c.x = to.x;
                    }
                    if (rot != null) {
                        rot.apply(c, true);
                        rot.apply(n, false);
                    }
                    d.faces.add(new Face(c, n, part));
                }
            }
        }
        return d;
    }

    private static Vector3f vec(JsonArray a) {
        return new Vector3f(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }

    /** 요소 회전 (마크 FaceBakery 와 같음, rescale 포함) */
    private static final class Rot {
        final Vector3f origin;
        final org.joml.Quaternionf q;
        final Vector3f scale = new Vector3f(1, 1, 1);

        Rot(JsonObject r) {
            origin = vec(r.getAsJsonArray("origin")).div(16f);
            float ang = r.get("angle").getAsFloat();
            String axis = r.get("axis").getAsString();
            float rad = (float) Math.toRadians(ang);
            q = switch (axis) {
                case "x" -> new org.joml.Quaternionf().rotationX(rad);
                case "y" -> new org.joml.Quaternionf().rotationY(rad);
                default -> new org.joml.Quaternionf().rotationZ(rad);
            };
            if (r.has("rescale") && r.get("rescale").getAsBoolean()) {
                float s = Math.abs(ang) == 22.5f ? 1f / (float) Math.cos(Math.PI / 8) : 1f / (float) Math.cos(Math.PI / 4);
                switch (axis) {
                    case "x" -> scale.set(1, s, s);
                    case "y" -> scale.set(s, 1, s);
                    default -> scale.set(s, s, 1);
                }
            }
        }

        void apply(Vector3f v, boolean point) {
            if (point) {
                v.sub(origin);
                q.transform(v);
                v.mul(scale);
                v.add(origin);
            } else {
                q.transform(v);
            }
        }
    }

    private static @Nullable JsonObject read(String model) {
        var rm = Minecraft.getInstance().getResourceManager();
        var res = rm.getResource(TycoonCraft.id("models/item/" + model + ".json"));
        if (res.isEmpty()) return null;
        try (var in = new InputStreamReader(res.get().open())) {
            return JsonParser.parseReader(in).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    // ── 구워진 모델 나누기 ──

    /** 한 모델을 몸통 + 부품별 작은 모델로 */
    public static final class Split {
        public final BakedModel body;
        public final EnumMap<Part, BakedModel> parts = new EnumMap<>(Part.class);

        Split(BakedModel body) {
            this.body = body;
        }
    }

    public static Split split(String gun, BakedModel model) {
        Split s = SPLITS.get(model);
        if (s != null) return s;
        Def d = def(gun);
        Map<Part, Map<Direction, List<BakedQuad>>> out = new EnumMap<>(Part.class);
        Map<Direction, List<BakedQuad>> body = new HashMap<>();
        RandomSource rand = RandomSource.create();
        Direction[] sides = {null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
        for (Direction side : sides) {
            rand.setSeed(42L);
            for (BakedQuad q : model.getQuads(null, side, rand)) {
                Part p = classify(d, q);
                if (p == null) body.computeIfAbsent(side, k -> new ArrayList<>()).add(q);
                else out.computeIfAbsent(p, k -> new HashMap<>()).computeIfAbsent(side, k -> new ArrayList<>()).add(q);
            }
        }
        s = new Split(new Sub(model, body));
        for (var en : out.entrySet()) s.parts.put(en.getKey(), new Sub(model, en.getValue()));
        SPLITS.put(model, s);
        return s;
    }

    private static @Nullable Part classify(Def d, BakedQuad q) {
        if (d.faces.isEmpty()) return null;
        int[] v = q.getVertices();
        int stride = v.length / 4;
        Vector3f[] p = new Vector3f[4];
        for (int i = 0; i < 4; i++)
            p[i] = new Vector3f(Float.intBitsToFloat(v[i * stride]), Float.intBitsToFloat(v[i * stride + 1]), Float.intBitsToFloat(v[i * stride + 2]));
        Vector3f c = new Vector3f(p[0]).add(p[1]).add(p[2]).add(p[3]).div(4f);
        Vector3f n = new Vector3f(p[1]).sub(p[0]).cross(new Vector3f(p[2]).sub(p[0]));
        if (n.lengthSquared() < 1e-12f) n = new Vector3f(p[2]).sub(p[1]).cross(new Vector3f(p[3]).sub(p[1]));
        if (n.lengthSquared() < 1e-12f) return null;
        n.normalize();
        for (Face f : d.faces)
            if (Math.abs(n.dot(f.normal)) > 0.97f && c.distanceSquared(f.center) < 0.004f * 0.004f) return f.part;
        return null;
    }

    /** 원래 모델의 일부 면만 가진 모델 (나머지 성질은 원래 모델 그대로) */
    static final class Sub extends BakedModelWrapper<BakedModel> {
        private final Map<Direction, List<BakedQuad>> quads;
        private final List<BakedQuad> unsided;

        Sub(BakedModel original, Map<Direction, List<BakedQuad>> quads) {
            super(original);
            this.quads = quads;
            this.unsided = quads.getOrDefault(null, List.of());
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
            return side == null ? unsided : quads.getOrDefault(side, List.of());
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
            return getQuads(state, side, rand);
        }

        @Override
        public List<BakedModel> getRenderPasses(net.minecraft.world.item.ItemStack stack, boolean fabulous) {
            return List.of(this);
        }
    }

    private GunRig() {}
}
