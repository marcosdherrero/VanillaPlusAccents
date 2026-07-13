package net.berkle.vanillaplusaccents.client.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.HangingSignRenderState;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.berkle.vanillaplusaccents.accessor.SignRenderStateAccess;

/**
 * Renders a flat item on the sign face in text pose space.
 * Uses only positive scales after undoing the text Y-flip so block models keep correct face winding
 * (a negative X mirror was culling front faces and showing hollow backs).
 */
public final class SignDisplayedItemRenderer {

	/** Standing / wall sign text scale ({@code 1 / 96}). */
	private static final float STANDING_TEXT_SCALE = 0.010416667f;
	/** Hanging sign text scale. */
	private static final float HANGING_TEXT_SCALE = 0.0140625f;
	/** One block pixel in front of the sign board. */
	private static final float ITEM_SURFACE_OFFSET = 0.0625f;
	/** Default item size, matching item frames ({@code 0.5} blocks). */
	private static final float ITEM_DISPLAY_SCALE = 0.5f;
	private static final int TEXT_LINE_COUNT = 4;
	private static final float AREA_MARGIN = 2.0f;

	private SignDisplayedItemRenderer() {
	}

	/**
	 * Renders inside the current sign-text pose (after {@code frontText()} / {@code backText()}).
	 * Text lines use {@code y = line * height - 2 * height}; the vertical center is at {@code -height / 2}.
	 */
	public static void submitInTextSpace(
		SignRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		SignRenderStateAccess renderAccess,
		boolean front
	) {
		ItemStackRenderState itemState = renderAccess.vpa$itemState(front);
		if (renderAccess.vpa$getDisplayedItem(front).isEmpty() || itemState.isEmpty()) {
			return;
		}

		float textScale = textScale(state);
		float invTextScale = 1.0f / textScale;
		float displayScale = computeDisplayScale(state, itemState, textScale);
		float centerYTextPx = -state.textLineHeight * 0.5f;
		float zTextPx = ITEM_SURFACE_OFFSET / textScale;

		poseStack.pushPose();
		poseStack.translate(0.0f, centerYTextPx, zTextPx);
		// Cancel text scale (s, -s, s): product is (1, 1, 1) — preserves winding for solid faces.
		poseStack.scale(invTextScale, -invTextScale, invTextScale);
		centerOnModel(itemState, poseStack);
		poseStack.scale(displayScale, displayScale, displayScale);
		itemState.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
		poseStack.popPose();
	}

	private static float textScale(SignRenderState state) {
		return state instanceof HangingSignRenderState ? HANGING_TEXT_SCALE : STANDING_TEXT_SCALE;
	}

	private static void centerOnModel(ItemStackRenderState itemState, PoseStack poseStack) {
		AABB bbox = itemState.getModelBoundingBox();
		if (bbox.getXsize() <= 1.0E-4f && bbox.getYsize() <= 1.0E-4f && bbox.getZsize() <= 1.0E-4f) {
			return;
		}
		Vec3 center = bbox.getCenter();
		poseStack.translate(-(float) center.x, -(float) center.y, -(float) center.z);
	}

	private static float computeDisplayScale(SignRenderState state, ItemStackRenderState itemState, float textScale) {
		float areaWidth = (state.maxTextLineWidth - AREA_MARGIN) * textScale;
		float areaHeight = (TEXT_LINE_COUNT * state.textLineHeight - AREA_MARGIN) * textScale;
		if (areaWidth <= 0.0f || areaHeight <= 0.0f) {
			return ITEM_DISPLAY_SCALE;
		}

		AABB bbox = itemState.getModelBoundingBox();
		float modelWidth = Math.max((float) bbox.getXsize(), (float) bbox.getZsize());
		float modelHeight = (float) bbox.getYsize();
		if (modelWidth <= 1.0E-4f && modelHeight <= 1.0E-4f) {
			modelWidth = 1.0f;
			modelHeight = 1.0f;
		} else {
			modelWidth = Math.max(modelWidth, 1.0E-4f);
			modelHeight = Math.max(modelHeight, 1.0E-4f);
		}

		float itemWidth = modelWidth * ITEM_DISPLAY_SCALE;
		float itemHeight = modelHeight * ITEM_DISPLAY_SCALE;
		float fit = Math.min(1.0f, Math.min(areaWidth / itemWidth, areaHeight / itemHeight));
		return ITEM_DISPLAY_SCALE * fit;
	}
}
