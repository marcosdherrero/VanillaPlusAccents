package net.berkle.vanillaplusaccents.client.render;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.joml.Matrix4fc;

/**
 * Fence-to-fence leash mesh: one vanilla-width ribbon (24 steps, alternating shade)
 * sampled along a catenary so rib spacing matches animal leads.
 */
public final class FenceLeadRender {

	/** Matches {@link Catenary#SEGMENTS} / vanilla leash rib count. */
	private static final float LEASH_WIDTH = 0.05f;

	private FenceLeadRender() {
	}

	public static void submitCompleted(
		ClientLevel level,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		Vec3 poseOrigin,
		Vec3 start,
		Vec3 end
	) {
		Vec3[] points = new Vec3[Catenary.POINTS];
		Catenary.sample(start, end, points);
		int startPacked = LightCoordsUtil.getLightCoords(level, BlockPos.containing(start));
		int endPacked = LightCoordsUtil.getLightCoords(level, BlockPos.containing(end));

		poseStack.pushPose();
		poseStack.translate(start.x - poseOrigin.x, start.y - poseOrigin.y, start.z - poseOrigin.z);
		collector.submitCustomGeometry(poseStack, RenderTypes.leash(), (pose, consumer) ->
			emitRibbon(consumer, pose.pose(), points, startPacked, endPacked)
		);
		poseStack.popPose();
	}

	public static EntityRenderState.LeashState pendingState(ClientLevel level, Vec3 start, Vec3 end, Vec3 origin) {
		EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
		leash.offset = start.subtract(origin);
		leash.start = start;
		leash.end = end;
		leash.slack = true;
		light(level, leash, start, end);
		return leash;
	}

	private static void emitRibbon(
		VertexConsumer consumer,
		Matrix4fc matrix,
		Vec3[] points,
		int startPacked,
		int endPacked
	) {
		Vec3 first = points[0];
		Vec3 last = points[points.length - 1];
		float dx = (float) (last.x - first.x);
		float dz = (float) (last.z - first.z);
		float horizSq = dx * dx + dz * dz;
		float nx;
		float nz;
		if (horizSq < 1.0e-12f) {
			nx = LEASH_WIDTH * 0.5f;
			nz = 0.0f;
		} else {
			float scale = Mth.invSqrt(horizSq) * LEASH_WIDTH * 0.5f;
			nx = dz * scale;
			nz = dx * scale;
		}

		int startBlock = LightCoordsUtil.block(startPacked);
		int startSky = LightCoordsUtil.sky(startPacked);
		int endBlock = LightCoordsUtil.block(endPacked);
		int endSky = LightCoordsUtil.sky(endPacked);

		for (int i = 0; i <= Catenary.SEGMENTS; i++) {
			addVertexPair(consumer, matrix, points, first, nx, nz, i, false, LEASH_WIDTH, startBlock, startSky, endBlock, endSky);
		}
		for (int i = Catenary.SEGMENTS; i >= 0; i--) {
			addVertexPair(consumer, matrix, points, first, nx, nz, i, true, 0.0f, startBlock, startSky, endBlock, endSky);
		}
	}

	private static void addVertexPair(
		VertexConsumer consumer,
		Matrix4fc matrix,
		Vec3[] points,
		Vec3 origin,
		float nx,
		float nz,
		int step,
		boolean reverse,
		float width,
		int startBlock,
		int startSky,
		int endBlock,
		int endSky
	) {
		float t = step / (float) Catenary.SEGMENTS;
		int packed = LightCoordsUtil.pack(
			(int) Mth.lerp(t, startBlock, endBlock),
			(int) Mth.lerp(t, startSky, endSky)
		);
		float shade = (step % 2 == (reverse ? 1 : 0)) ? 0.7f : 1.0f;
		float r = 0.5f * shade;
		float g = 0.4f * shade;
		float b = 0.3f * shade;
		Vec3 point = points[step];
		float x = (float) (point.x - origin.x);
		float y = (float) (point.y - origin.y);
		float z = (float) (point.z - origin.z);

		consumer.addVertex(matrix, x - nx, y + width, z + nz)
			.setColor(r, g, b, 1.0f)
			.setLight(packed);
		consumer.addVertex(matrix, x + nx, y + LEASH_WIDTH - width, z - nz)
			.setColor(r, g, b, 1.0f)
			.setLight(packed);
	}

	private static void light(ClientLevel level, EntityRenderState.LeashState leash, Vec3 start, Vec3 end) {
		int startPacked = LightCoordsUtil.getLightCoords(level, BlockPos.containing(start));
		int endPacked = LightCoordsUtil.getLightCoords(level, BlockPos.containing(end));
		leash.startBlockLight = LightCoordsUtil.block(startPacked);
		leash.startSkyLight = LightCoordsUtil.sky(startPacked);
		leash.endBlockLight = LightCoordsUtil.block(endPacked);
		leash.endSkyLight = LightCoordsUtil.sky(endPacked);
	}
}
