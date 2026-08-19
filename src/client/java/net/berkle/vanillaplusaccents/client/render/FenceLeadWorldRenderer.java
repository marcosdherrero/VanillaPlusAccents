package net.berkle.vanillaplusaccents.client.render;

import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import net.berkle.vanillaplusaccents.fence.FenceLeadEntity;
import net.berkle.vanillaplusaccents.fence.FenceLeadLink;

/**
 * Backup rope submit for saved links the entity pass is not actually drawing.
 * Skip only when a matching completed marker is in range and will submit;
 * a live entity in the camera AABB is not enough (frustum / empty DATA_TO).
 */
public final class FenceLeadWorldRenderer {

	private static final double DRAW_RANGE_SQR = 96.0 * 96.0;

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
			entity -> entity.isPrimaryCompleted()
		);

		for (FenceLeadLink link : links) {
			Vec3 start = FenceLeadEntity.attachPoint(level, link.from());
			Vec3 end = FenceLeadEntity.attachPoint(level, link.to());
			if (start.distanceToSqr(cameraPos) > DRAW_RANGE_SQR && end.distanceToSqr(cameraPos) > DRAW_RANGE_SQR) {
				continue;
			}
			if (live.stream().anyMatch(entity -> entity.matches(link) && entity.isDrawingCompleted(cameraPos))) {
				continue;
			}
			FenceLeadRender.submitCompleted(
				level,
				poseStack,
				collector,
				cameraPos,
				start,
				end
			);
		}
	}
}
