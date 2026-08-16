package net.berkle.vanillaplusaccents.flower;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Deterministic per-flower placements inside a block.
 * Stems stay spaced so 3-flower patches form a triangle and 4-flower patches form a quad.
 * Flowers keep vanilla size; petals may overlap neighboring blocks.
 */
public final class FlowerPatchOffsets {

	private static final double MIN_CENTER = 0.22;
	private static final double MAX_CENTER = 0.78;
	private static final double MIN_DISTANCE = 0.47;
	/** Rejects 3-flower layouts that collapse toward a straight line. */
	private static final double MIN_TRIANGLE_AREA = 0.055;
	/** Both axes must span this far so a 4-flower patch is a quad, not a row. */
	private static final double MIN_QUAD_SPAN = 0.30;

	private final List<Placement> placements;

	private FlowerPatchOffsets(List<Placement> placements) {
		this.placements = List.copyOf(placements);
	}

	public List<Placement> placements() {
		return placements;
	}

	public static FlowerPatchOffsets forCount(int count, BlockPos pos) {
		int clamped = Mth.clamp(count, 1, 4);
		RandomSource random = RandomSource.create(seedFor(pos, clamped));

		List<Placement> list = switch (clamped) {
			case 2 -> twoFlower(random);
			case 3 -> threeFlowerTriangle(random);
			case 4 -> fourFlower(random);
			default -> List.of(single(random));
		};
		return new FlowerPatchOffsets(list);
	}

	private static long seedFor(BlockPos pos, int count) {
		return pos.asLong() ^ (0x9E3779B97F4A7C15L * count) ^ 0xC6A4A7935BD1E995L;
	}

	private static Placement single(RandomSource random) {
		double x = clamp(0.50 + (random.nextDouble() - 0.5) * 0.20);
		double z = clamp(0.50 + (random.nextDouble() - 0.5) * 0.20);
		return placement(x, z, random);
	}

	private static List<Placement> twoFlower(RandomSource random) {
		double angle = random.nextDouble() * Math.PI;
		double radius = 0.25 + random.nextDouble() * 0.02;
		double[] xs = {
			0.50 - Math.cos(angle) * radius,
			0.50 + Math.cos(angle) * radius
		};
		double[] zs = {
			0.50 - Math.sin(angle) * radius,
			0.50 + Math.sin(angle) * radius
		};
		if (!validLayout(xs, zs)) {
			xs[0] = 0.50 - Math.cos(angle) * 0.26;
			xs[1] = 0.50 + Math.cos(angle) * 0.26;
			zs[0] = 0.50 - Math.sin(angle) * 0.26;
			zs[1] = 0.50 + Math.sin(angle) * 0.26;
		}
		return toPlacements(xs, zs, random);
	}

	/**
	 * Equilateral triangle with light jitter. Layouts that flatten into a line or violate
	 * spacing/edge limits are rejected.
	 */
	private static List<Placement> threeFlowerTriangle(RandomSource random) {
		double baseAngle = random.nextDouble() * Math.PI * 2.0;
		double[] xs = new double[3];
		double[] zs = new double[3];

		for (int attempt = 0; attempt < 16; attempt++) {
			double radius = 0.272 + random.nextDouble() * 0.006;
			for (int i = 0; i < 3; i++) {
				double angle = baseAngle + i * (Math.PI * 2.0 / 3.0);
				angle += (random.nextDouble() - 0.5) * 0.12;
				double r = radius + (random.nextDouble() - 0.5) * 0.012;
				xs[i] = 0.50 + Math.cos(angle) * r;
				zs[i] = 0.50 + Math.sin(angle) * r;
			}
			if (validLayout(xs, zs)) {
				return toPlacements(xs, zs, random);
			}
		}

		for (int i = 0; i < 3; i++) {
			double angle = baseAngle + i * (Math.PI * 2.0 / 3.0);
			xs[i] = 0.50 + Math.cos(angle) * 0.275;
			zs[i] = 0.50 + Math.sin(angle) * 0.275;
		}
		return toPlacements(xs, zs, random);
	}

	/**
	 * Slightly rotated 2×2 with tiny jitter. Random dart-throwing cannot fit four well-spaced
	 * flowers, so we start from a square and only keep layouts that stay a quad.
	 */
	private static List<Placement> fourFlower(RandomSource random) {
		double[] xs = new double[4];
		double[] zs = new double[4];
		double baseAngle = (random.nextDouble() - 0.5) * 0.24;
		double half = 0.245;

		for (int attempt = 0; attempt < 24; attempt++) {
			double angle = attempt == 0 ? baseAngle : baseAngle + (random.nextDouble() - 0.5) * 0.08;
			double hx = half + (attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 0.02);
			double hz = half + (attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 0.02);
			double cos = Math.cos(angle);
			double sin = Math.sin(angle);
			double[][] locals = {
				{-hx, -hz},
				{hx, -hz},
				{-hx, hz},
				{hx, hz}
			};
			for (int i = 0; i < 4; i++) {
				double lx = locals[i][0];
				double lz = locals[i][1];
				double jx = attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 0.03;
				double jz = attempt == 0 ? 0.0 : (random.nextDouble() - 0.5) * 0.03;
				xs[i] = 0.50 + lx * cos - lz * sin + jx;
				zs[i] = 0.50 + lx * sin + lz * cos + jz;
			}
			if (validLayout(xs, zs)) {
				return toPlacements(xs, zs, random);
			}
		}

		xs[0] = 0.255;
		zs[0] = 0.255;
		xs[1] = 0.745;
		zs[1] = 0.255;
		xs[2] = 0.255;
		zs[2] = 0.745;
		xs[3] = 0.745;
		zs[3] = 0.745;
		return toPlacements(xs, zs, random);
	}

	private static boolean validLayout(double[] xs, double[] zs) {
		int n = xs.length;
		for (int i = 0; i < n; i++) {
			if (xs[i] < MIN_CENTER || xs[i] > MAX_CENTER || zs[i] < MIN_CENTER || zs[i] > MAX_CENTER) {
				return false;
			}
		}
		for (int i = 0; i < n; i++) {
			for (int j = i + 1; j < n; j++) {
				double dx = xs[i] - xs[j];
				double dz = zs[i] - zs[j];
				if (dx * dx + dz * dz < MIN_DISTANCE * MIN_DISTANCE) {
					return false;
				}
			}
		}
		if (n == 3 && triangleArea(xs, zs) < MIN_TRIANGLE_AREA) {
			return false;
		}
		if (n == 4 && (span(xs) < MIN_QUAD_SPAN || span(zs) < MIN_QUAD_SPAN)) {
			return false;
		}
		return true;
	}

	private static List<Placement> toPlacements(double[] xs, double[] zs, RandomSource random) {
		List<Placement> list = new ArrayList<>(xs.length);
		for (int i = 0; i < xs.length; i++) {
			list.add(placement(clamp(xs[i]), clamp(zs[i]), random));
		}
		return list;
	}

	private static Placement placement(double x, double z, RandomSource random) {
		return new Placement(x, z, random.nextFloat() * 360.0f, 0.0f);
	}

	private static double triangleArea(double[] xs, double[] zs) {
		return 0.5 * Math.abs(
			xs[0] * (zs[1] - zs[2]) + xs[1] * (zs[2] - zs[0]) + xs[2] * (zs[0] - zs[1])
		);
	}

	private static double span(double[] values) {
		double min = values[0];
		double max = values[0];
		for (int i = 1; i < values.length; i++) {
			min = Math.min(min, values[i]);
			max = Math.max(max, values[i]);
		}
		return max - min;
	}

	private static double clamp(double value) {
		return Mth.clamp(value, MIN_CENTER, MAX_CENTER);
	}

	public record Placement(double x, double z, float yawDegrees, float leanDegrees) {
	}
}
