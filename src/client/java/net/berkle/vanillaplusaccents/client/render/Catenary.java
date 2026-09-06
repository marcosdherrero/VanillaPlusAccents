package net.berkle.vanillaplusaccents.client.render;

import net.minecraft.world.phys.Vec3;

/**
 * Fence-to-fence rope curve. Horizontal span is XZ ({@code H}); vertical is Y ({@code V}).
 * Slack grows with distance through mid-range paddock gaps, then tightens so long spans
 * stay off the ground.
 */
public final class Catenary {

	public static final int SEGMENTS = 24;
	public static final int POINTS = SEGMENTS + 1;

	/** Smallest extra length beyond the chord so the solver stays valid. */
	static final double MIN_EXTRA = 0.01;
	private static final double H_EPS = 1.0e-4;
	private static final double PEAK_H = 6.5;
	private static final double TIGHTEN_H = 16.0;
	private static final double SHORT_SAG = 0.07;
	private static final double PEAK_SAG = 0.68;
	private static final double LONG_SAG = 0.22;
	private static final int NEWTON_ITERS = 24;

	private Catenary() {
	}

	/**
	 * Extra rope length as a function of horizontal span {@code H}.
	 * Derived from a target sag: more droop until ~{@value #PEAK_H} blocks, then
	 * relative slack falls toward a taut long span.
	 */
	public static double extraLength(double horizontal) {
		double h = Math.max(horizontal, H_EPS);
		double sag = desiredSag(h);
		// Small-sag relation: extra ≈ 8 s² / (3H)
		return Math.max(MIN_EXTRA, (8.0 * sag * sag) / (3.0 * h));
	}

	public static double desiredSag(double horizontal) {
		double h = Math.max(0.0, horizontal);
		if (h <= PEAK_H) {
			double t = h / PEAK_H;
			t = t * t;
			return SHORT_SAG + (PEAK_SAG - SHORT_SAG) * t;
		}
		double t = Math.min(1.0, (h - PEAK_H) / (TIGHTEN_H - PEAK_H));
		t = t * t * (3.0 - 2.0 * t);
		return PEAK_SAG + (LONG_SAG - PEAK_SAG) * t;
	}

	/**
	 * Fills {@code dest} with world-space samples from {@code from} to {@code to}.
	 * {@code dest.length} must be {@link #POINTS}.
	 */
	public static void sample(Vec3 from, Vec3 to, Vec3[] dest) {
		if (dest.length != POINTS) {
			throw new IllegalArgumentException("dest must have " + POINTS + " slots");
		}

		double x1 = from.x;
		double y1 = from.y;
		double z1 = from.z;
		double dx = to.x - x1;
		double dy = to.y - y1;
		double dz = to.z - z1;
		double h = Math.sqrt(dx * dx + dz * dz);
		double v = dy;
		double chord = Math.sqrt(h * h + v * v);
		double extra = extraLength(h);
		double length = chord + extra;
		if (length * length - v * v <= h * h) {
			length = Math.sqrt(h * h + v * v) + MIN_EXTRA;
		}

		if (h < H_EPS) {
			lerp(x1, y1, z1, dx, dy, dz, dest);
			return;
		}

		double w = Math.sqrt(Math.max(h * h, length * length - v * v));
		double a = solveA(h, w);
		if (!(a > 0.0) || Double.isNaN(a) || Double.isInfinite(a)) {
			parabola(x1, y1, z1, dx, dy, dz, h, dest);
			return;
		}

		double sinhHalf = Math.sinh(h / (2.0 * a));
		double denom = 2.0 * a * sinhHalf;
		if (denom < 1.0e-12) {
			parabola(x1, y1, z1, dx, dy, dz, h, dest);
			return;
		}

		double h0 = h / 2.0 - a * asinh(v / denom);
		double y0 = y1 - a * Math.cosh(h0 / a);

		for (int i = 0; i < POINTS; i++) {
			double t = i / (double) SEGMENTS;
			dest[i] = new Vec3(
				x1 + t * dx,
				a * Math.cosh((t * h - h0) / a) + y0,
				z1 + t * dz
			);
		}
	}

	/**
	 * Solve {@code 2a sinh(H/(2a)) = W} for {@code a} by Newton-Raphson.
	 * {@code W = sqrt(L² - V²)} must be strictly greater than {@code H}.
	 */
	static double solveA(double h, double w) {
		double excess = w - h;
		if (excess <= 1.0e-12) {
			return Double.NaN;
		}
		// Series: W ≈ H + H³ / (24 a²)  →  a ≈ H / sqrt(24 (W/H - 1))
		double a = h / Math.sqrt(24.0 * (w / h - 1.0));
		if (!(a > 0.0) || Double.isInfinite(a)) {
			a = h;
		}

		for (int i = 0; i < NEWTON_ITERS; i++) {
			double half = h / (2.0 * a);
			double sh = Math.sinh(half);
			double ch = Math.cosh(half);
			double f = 2.0 * a * sh - w;
			double fp = 2.0 * sh - (h / a) * ch;
			if (Math.abs(fp) < 1.0e-12) {
				break;
			}
			double next = a - f / fp;
			if (!(next > 0.0) || Double.isNaN(next) || Double.isInfinite(next)) {
				return a;
			}
			if (Math.abs(next - a) <= 1.0e-10 * Math.max(1.0, a)) {
				return next;
			}
			a = next;
		}
		return a;
	}

	private static void lerp(double x1, double y1, double z1, double dx, double dy, double dz, Vec3[] dest) {
		for (int i = 0; i < POINTS; i++) {
			double t = i / (double) SEGMENTS;
			dest[i] = new Vec3(x1 + t * dx, y1 + t * dy, z1 + t * dz);
		}
	}

	private static void parabola(double x1, double y1, double z1, double dx, double dy, double dz, double h, Vec3[] dest) {
		double sag = desiredSag(h);
		for (int i = 0; i < POINTS; i++) {
			double t = i / (double) SEGMENTS;
			double droop = 4.0 * sag * t * (1.0 - t);
			dest[i] = new Vec3(x1 + t * dx, y1 + t * dy - droop, z1 + t * dz);
		}
	}

	private static double asinh(double x) {
		return Math.log(x + Math.sqrt(x * x + 1.0));
	}
}
