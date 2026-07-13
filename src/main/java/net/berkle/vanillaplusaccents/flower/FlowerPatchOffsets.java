package net.berkle.vanillaplusaccents.flower;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * Deterministic per-flower placements inside a block.
 * 3-flower patches always form a non-degenerate triangle (never a straight line).
 */
public final class FlowerPatchOffsets {

	/** Stem positions stay on the block; full-size cross models may still reach the edges. */
	private static final double MIN = 0.18;
	private static final double MAX = 0.82;

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
		double x = clamp(0.50 + (random.nextDouble() - 0.5) * 0.28);
		double z = clamp(0.50 + (random.nextDouble() - 0.5) * 0.28);
		return placement(x, z, random);
	}

	private static List<Placement> twoFlower(RandomSource random) {
		double angle = random.nextDouble() * Math.PI;
		double radius = 0.22 + random.nextDouble() * 0.06;
		double dx = Math.cos(angle) * radius;
		double dz = Math.sin(angle) * radius;
		List<Placement> list = new ArrayList<>(2);
		list.add(placement(clamp(0.50 - dx), clamp(0.50 - dz), random));
		list.add(placement(clamp(0.50 + dx), clamp(0.50 + dz), random));
		return list;
	}

	/**
	 * Always a triangle: vertices on a circle at 120° spacing. Light jitter is rejected if the
	 * shape collapses toward a straight line.
	 */
	private static List<Placement> threeFlowerTriangle(RandomSource random) {
		double baseAngle = random.nextDouble() * Math.PI * 2.0;
		double radius = 0.24 + random.nextDouble() * 0.04;

		double[] xs = new double[3];
		double[] zs = new double[3];
		for (int attempt = 0; attempt < 12; attempt++) {
			for (int i = 0; i < 3; i++) {
				double angle = baseAngle + i * (Math.PI * 2.0 / 3.0);
				if (attempt > 0) {
					angle += (random.nextDouble() - 0.5) * 0.30;
				}
				double r = radius;
				if (attempt > 0) {
					r += (random.nextDouble() - 0.5) * 0.05;
				}
				xs[i] = clamp(0.50 + Math.cos(angle) * r);
				zs[i] = clamp(0.50 + Math.sin(angle) * r);
			}
			if (triangleArea(xs, zs) >= 0.020) {
				break;
			}
			if (attempt == 11) {
				for (int i = 0; i < 3; i++) {
					double angle = baseAngle + i * (Math.PI * 2.0 / 3.0);
					xs[i] = clamp(0.50 + Math.cos(angle) * radius);
					zs[i] = clamp(0.50 + Math.sin(angle) * radius);
				}
			}
		}

		List<Placement> list = new ArrayList<>(3);
		for (int i = 0; i < 3; i++) {
			list.add(placement(xs[i], zs[i], random));
		}
		return list;
	}

	private static List<Placement> fourFlower(RandomSource random) {
		double[][] bases = {
			{0.26, 0.26},
			{0.74, 0.26},
			{0.26, 0.74},
			{0.74, 0.74},
		};
		int turns = random.nextInt(4);
		for (int t = 0; t < turns; t++) {
			for (double[] slot : bases) {
				double x = slot[0];
				double z = slot[1];
				slot[0] = z;
				slot[1] = 1.0 - x;
			}
		}
		List<Placement> list = new ArrayList<>(4);
		for (double[] slot : bases) {
			double x = clamp(slot[0] + (random.nextDouble() - 0.5) * 0.08);
			double z = clamp(slot[1] + (random.nextDouble() - 0.5) * 0.08);
			list.add(placement(x, z, random));
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

	private static double clamp(double value) {
		return Mth.clamp(value, MIN, MAX);
	}

	public record Placement(double x, double z, float yawDegrees, float leanDegrees) {
	}
}
