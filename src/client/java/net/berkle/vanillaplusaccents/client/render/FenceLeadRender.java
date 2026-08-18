package net.berkle.vanillaplusaccents.client.render;

import java.util.List;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * Shared fence-to-fence leash mesh: catenary samples submitted as slack-free
 * vanilla leash segments so animal leashes stay on the vanilla renderer.
 */
public final class FenceLeadRender {

	private static final Vec3[] POINTS = new Vec3[Catenary.POINTS];

	private FenceLeadRender() {
	}

	public static void fillCompletedStates(
		ClientLevel level,
		Vec3 start,
		Vec3 end,
		Vec3 origin,
		List<EntityRenderState.LeashState> out
	) {
		Catenary.sample(start, end, POINTS);
		for (int i = 0; i < Catenary.SEGMENTS; i++) {
			out.add(segment(level, POINTS[i], POINTS[i + 1], origin));
		}
	}

	public static void submitCompleted(
		ClientLevel level,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		Vec3 cameraPos,
		Vec3 start,
		Vec3 end
	) {
		Catenary.sample(start, end, POINTS);
		for (int i = 0; i < Catenary.SEGMENTS; i++) {
			Vec3 a = POINTS[i];
			Vec3 b = POINTS[i + 1];
			poseStack.pushPose();
			poseStack.translate(a.x - cameraPos.x, a.y - cameraPos.y, a.z - cameraPos.z);
			collector.submitLeash(poseStack, segment(level, a, b, a));
			poseStack.popPose();
		}
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

	private static EntityRenderState.LeashState segment(ClientLevel level, Vec3 start, Vec3 end, Vec3 origin) {
		EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
		leash.offset = start.subtract(origin);
		leash.start = start;
		leash.end = end;
		leash.slack = false;
		light(level, leash, start, end);
		return leash;
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
