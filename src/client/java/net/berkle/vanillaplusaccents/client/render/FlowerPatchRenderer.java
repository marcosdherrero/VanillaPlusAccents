package net.berkle.vanillaplusaccents.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.vanillaplusaccents.block.entity.FlowerPatchBlockEntity;
import net.berkle.vanillaplusaccents.client.render.state.FlowerPatchRenderState;
import net.berkle.vanillaplusaccents.flower.FlowerPatchOffsets;
import net.berkle.vanillaplusaccents.flower.FlowerPatchSupport;

/**
 * Renders stacked flowers via {@code submitMovingBlock} (terrain {@code CUTOUT_BLOCK}),
 * matching planted-flower brightness. Entity/item BER paths shade cross models too dark.
 */
public final class FlowerPatchRenderer implements BlockEntityRenderer<FlowerPatchBlockEntity, FlowerPatchRenderState> {

	public FlowerPatchRenderer(BlockEntityRendererProvider.Context context) {
	}

	@Override
	public FlowerPatchRenderState createRenderState() {
		return new FlowerPatchRenderState();
	}

	@Override
	public void extractRenderState(
		FlowerPatchBlockEntity blockEntity,
		FlowerPatchRenderState state,
		float partialTick,
		net.minecraft.world.phys.Vec3 cameraPos,
		net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumblingOverlay
	) {
		BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTick, cameraPos, crumblingOverlay);
		state.flowerId = blockEntity.getFlowerId();
		state.count = blockEntity.getCount();
		state.placements = FlowerPatchOffsets.forCount(state.count, blockEntity.getBlockPos()).placements();
		FlowerPatchClientCache.put(blockEntity.getBlockPos(), state.flowerId);

		MovingBlockRenderState moving = state.movingBlock;
		Block flower = BuiltInRegistries.BLOCK.getValue(state.flowerId);
		if (flower == null || flower == Blocks.AIR || !(blockEntity.getLevel() instanceof ClientLevel level)) {
			moving.blockState = Blocks.AIR.defaultBlockState();
			return;
		}

		BlockPos pos = blockEntity.getBlockPos();
		moving.randomSeedPos = pos;
		moving.blockPos = pos;
		// Eyeblossoms follow the same open/close environment attribute as singles.
		moving.blockState = FlowerPatchSupport.resolveDisplayState(level, pos, state.flowerId);
		moving.biome = level.getBiome(pos);
		moving.cardinalLighting = level.cardinalLighting();
		moving.lightEngine = level.getLightEngine();
	}

	@Override
	public void submit(
		FlowerPatchRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState
	) {
		BlockState flowerState = state.movingBlock.blockState;
		if (state.count <= 0
			|| state.placements.isEmpty()
			|| flowerState == null
			|| flowerState.isAir()
			|| flowerState.getRenderShape() != RenderShape.MODEL) {
			return;
		}

		int drawCount = Math.min(state.count, state.placements.size());
		for (int i = 0; i < drawCount; i++) {
			FlowerPatchOffsets.Placement placement = state.placements.get(i);
			poseStack.pushPose();
			poseStack.translate(placement.x(), 0.0, placement.z());
			poseStack.mulPose(Axis.YP.rotationDegrees(placement.yawDegrees()));
			poseStack.mulPose(Axis.ZP.rotationDegrees(placement.leanDegrees()));
			poseStack.translate(-0.5, 0.0, -0.5);
			submitNodeCollector.submitMovingBlock(poseStack, state.movingBlock);
			poseStack.popPose();
		}
	}
}
