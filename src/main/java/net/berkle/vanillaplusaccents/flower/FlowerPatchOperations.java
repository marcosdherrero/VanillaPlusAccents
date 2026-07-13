package net.berkle.vanillaplusaccents.flower;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.vanillaplusaccents.block.VpaBlocks;
import net.berkle.vanillaplusaccents.block.entity.FlowerPatchBlockEntity;

/** Server-side flower patch block mutations. */
public final class FlowerPatchOperations {

	private FlowerPatchOperations() {
	}

	public static void createPatch(ServerLevel level, BlockPos pos, Identifier flowerId, int count) {
		createPatch(level, pos, flowerId, count, null);
	}

	public static void createPatch(ServerLevel level, BlockPos pos, Identifier flowerId, int count, ServerPlayer player) {
		BlockState patchState = VpaBlocks.FLOWER_PATCH.defaultBlockState();
		level.setBlock(pos, patchState, Block.UPDATE_ALL);
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof FlowerPatchBlockEntity patch) {
			patch.configure(flowerId, count, false);
			FlowerPatchSync.syncChange(level, pos, patchState, patch, player);
		}
	}

	public static boolean growPatch(ServerLevel level, BlockPos pos, Identifier flowerId) {
		return growPatch(level, pos, flowerId, null);
	}

	public static boolean growPatch(ServerLevel level, BlockPos pos, Identifier flowerId, ServerPlayer player) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof FlowerPatchBlockEntity patch)) {
			return false;
		}
		if (!FlowerPatchSupport.samePatchFlower(patch.getFlowerId(), flowerId) || patch.getCount() >= 4) {
			return false;
		}
		// Keep the patch's current eyeblossom open/closed id; stacking open onto closed (or vice versa) is fine.
		patch.configure(patch.getFlowerId(), patch.getCount() + 1, false);
		FlowerPatchSync.syncChange(level, pos, level.getBlockState(pos), patch, player);
		return true;
	}

	public static boolean growPatch(ServerLevel level, BlockPos pos) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof FlowerPatchBlockEntity patch)) {
			return false;
		}
		if (patch.getCount() >= 4) {
			return false;
		}
		patch.configure(patch.getFlowerId(), patch.getCount() + 1, false);
		FlowerPatchSync.syncChange(level, pos, level.getBlockState(pos), patch, null);
		return true;
	}

	public static void playPlaceSound(ServerLevel level, BlockPos pos) {
		level.playSound(null, pos, SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1.0f, 1.0f);
	}

	public static void playBonemealEffect(ServerLevel level, BlockPos pos) {
		level.levelEvent(1505, pos, 15);
	}
}
