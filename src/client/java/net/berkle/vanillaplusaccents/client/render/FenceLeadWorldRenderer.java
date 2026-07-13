package net.berkle.vanillaplusaccents.client.render;

import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import net.berkle.vanillaplusaccents.fence.FenceLeadEntity;
import net.berkle.vanillaplusaccents.fence.FenceLeadLink;

/**
 * Backup rope submit for saved links whose marker entity has not arrived on the client yet.
 * Pending ropes and live entities are rendered only by {@link FenceLeadEntityRenderer}.
 */
public final class FenceLeadWorldRenderer {

	private FenceLeadWorldRenderer() {
	}

	public static void register() {
		LevelRenderEvents.COLLECT_SUBMITS.register(FenceLeadWorldRenderer::collectSubmits);
	}

	private static void collectSubmits(LevelRenderContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		ClientLevel level = minecraft.level;
		if (level == null) {
			return;
		}

		List<FenceLeadLink> links = FenceLeadClientCache.linksFor(level.dimension().identifier());
		if (links.isEmpty()) {
			return;
		}

		SubmitNodeCollector collector = context.submitNodeCollector();
		PoseStack poseStack = context.poseStack();
		CameraRenderState camera = context.levelState().cameraRenderState;
		Vec3 cameraPos = camera.pos;

		List<FenceLeadEntity> live = level.getEntitiesOfClass(
			FenceLeadEntity.class,
			new AABB(cameraPos, cameraPos).inflate(96.0),
			entity -> !entity.isPending()
		);

		for (FenceLeadLink link : links) {
			if (live.stream().anyMatch(entity -> entity.matches(link))) {
				continue;
			}
			submitRope(
				level,
				poseStack,
				collector,
				cameraPos,
				FenceLeadEntity.attachPoint(link.from()),
				FenceLeadEntity.attachPoint(link.to())
			);
		}
	}

	private static void submitRope(
		ClientLevel level,
		PoseStack poseStack,
		SubmitNodeCollector collector,
		Vec3 cameraPos,
		Vec3 start,
		Vec3 end
	) {
		EntityRenderState.LeashState state = new EntityRenderState.LeashState();
		state.offset = Vec3.ZERO;
		state.start = start;
		state.end = end;
		state.slack = true;

		int startPacked = LevelRenderer.getLightCoords(level, BlockPos.containing(start));
		int endPacked = LevelRenderer.getLightCoords(level, BlockPos.containing(end));
		state.startBlockLight = LightCoordsUtil.block(startPacked);
		state.startSkyLight = LightCoordsUtil.sky(startPacked);
		state.endBlockLight = LightCoordsUtil.block(endPacked);
		state.endSkyLight = LightCoordsUtil.sky(endPacked);

		poseStack.pushPose();
		poseStack.translate(
			start.x - cameraPos.x,
			start.y - cameraPos.y,
			start.z - cameraPos.z
		);
		collector.submitLeash(poseStack, state);
		poseStack.popPose();
	}
}
