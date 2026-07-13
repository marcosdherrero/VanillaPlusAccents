package net.berkle.vanillaplusaccents.flower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import net.berkle.vanillaplusaccents.block.VpaBlocks;
import net.berkle.vanillaplusaccents.block.entity.FlowerPatchBlockEntity;

/** Stack small flowers and mushrooms into 1–4 flower patches. */
public final class FlowerPatchHandler {

	private FlowerPatchHandler() {
	}

	/**
	 * Server-side stacking during {@code BlockState.useItemOn}. Kept as a backup when the player is not sneaking;
	 * sneaking skips {@code useItemOn} and goes straight to block-item placement.
	 */
	public static InteractionResult onUseItemOn(
		ItemStack held,
		BlockState blockState,
		Level level,
		BlockPos pos,
		Player player,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		if (level.isClientSide()) {
			return null;
		}

		return tryApplyFlowerUse(level, player, hand, pos, blockState, held);
	}

	/**
	 * Primary stacking path via {@code UseBlockCallback}. Runs before sneak-place skips {@code useItemOn}, so the
	 * server always consumes the click instead of letting vanilla place an adjacent flower.
	 */
	public static InteractionResult onUseBlock(
		Player player,
		Level level,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		ItemStack held = player.getItemInHand(hand);
		if (held.is(Items.BONE_MEAL)) {
			return onBonemeal(player, level, hitResult, held);
		}

		BlockPos target = hitResult.getBlockPos();
		BlockState targetState = level.getBlockState(target);
		if (!shouldCancelFlowerPlacement(level, target, targetState, held)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		InteractionResult applied = tryApplyFlowerUse(level, player, hand, target, targetState, held);
		return applied != null ? applied : InteractionResult.PASS;
	}

	private static InteractionResult tryApplyFlowerUse(
		Level level,
		Player player,
		InteractionHand hand,
		BlockPos target,
		BlockState targetState,
		ItemStack held
	) {
		PatchUse patchUse = resolveFlowerUse(level, target, targetState, held);
		if (patchUse == null) {
			return null;
		}

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return null;
		}

		return applyFlowerUse(serverLevel, serverPlayer, hand, patchUse);
	}

	private static InteractionResult onBonemeal(
		Player player,
		Level level,
		BlockHitResult hitResult,
		ItemStack held
	) {
		BlockPos target = hitResult.getBlockPos();
		BlockState targetState = level.getBlockState(target);

		// Patches use BonemealableBlock on the block itself; let BoneMealItem handle them.
		if (targetState.is(VpaBlocks.FLOWER_PATCH) || !isConvertibleSinglePlant(targetState)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level instanceof ServerLevel serverLevel)) {
			return InteractionResult.PASS;
		}

		Identifier flowerId = BuiltInRegistries.BLOCK.getKey(targetState.getBlock());
		FlowerPatchOperations.createPatch(serverLevel, target, flowerId, 2);
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		FlowerPatchOperations.playBonemealEffect(serverLevel, target);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static boolean shouldCancelFlowerPlacement(
		Level level,
		BlockPos target,
		BlockState targetState,
		ItemStack held
	) {
		if (resolveFlowerUse(level, target, targetState, held) != null) {
			return true;
		}

		// Block state can arrive before block-entity data on the client.
		if (level.isClientSide()
			&& targetState.is(VpaBlocks.FLOWER_PATCH)
			&& held.getItem() instanceof BlockItem blockItem
			&& FlowerPatchSupport.canStackInPatch(blockItem.getBlock())
			&& level.getBlockEntity(target) == null) {
			return true;
		}

		return false;
	}

	private static PatchUse resolveFlowerUse(Level level, BlockPos target, BlockState targetState, ItemStack held) {
		if (held.isEmpty() || !(held.getItem() instanceof BlockItem blockItem)) {
			return null;
		}

		Block flowerBlock = blockItem.getBlock();
		if (!FlowerPatchSupport.canStackInPatch(flowerBlock)) {
			return null;
		}

		Identifier flowerId = BuiltInRegistries.BLOCK.getKey(flowerBlock);

		if (targetState.is(VpaBlocks.FLOWER_PATCH)) {
			BlockEntity blockEntity = level.getBlockEntity(target);
			if (!(blockEntity instanceof FlowerPatchBlockEntity patch)) {
				return null;
			}
			if (!FlowerPatchSupport.samePatchFlower(patch.getFlowerId(), flowerId) || patch.getCount() >= 4) {
				return null;
			}
			return new PatchUse(target, flowerId, PatchAction.GROW);
		}

		if (isConvertibleSinglePlant(targetState)) {
			Identifier existing = BuiltInRegistries.BLOCK.getKey(targetState.getBlock());
			if (!FlowerPatchSupport.samePatchFlower(existing, flowerId)) {
				return null;
			}
			return new PatchUse(target, flowerId, PatchAction.CONVERT);
		}

		return null;
	}

	private static InteractionResult applyFlowerUse(
		ServerLevel level,
		ServerPlayer player,
		InteractionHand hand,
		PatchUse patchUse
	) {
		ItemStack held = player.getItemInHand(hand);
		return switch (patchUse.action()) {
			case CONVERT -> convertSingleToPatch(level, player, hand, patchUse.pos(), patchUse.flowerId(), held);
			case GROW -> growPatch(level, player, hand, patchUse.pos(), patchUse.flowerId(), held);
		};
	}

	private static boolean isConvertibleSinglePlant(BlockState state) {
		return state.is(BlockTags.SMALL_FLOWERS) || FlowerPatchSupport.isMushroom(state.getBlock());
	}

	private static InteractionResult convertSingleToPatch(
		ServerLevel level,
		ServerPlayer player,
		InteractionHand hand,
		BlockPos pos,
		Identifier flowerId,
		ItemStack held
	) {
		FlowerPatchOperations.createPatch(level, pos, flowerId, 2, player);
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		FlowerPatchOperations.playPlaceSound(level, pos);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static InteractionResult growPatch(
		ServerLevel level,
		ServerPlayer player,
		InteractionHand hand,
		BlockPos pos,
		Identifier flowerId,
		ItemStack held
	) {
		if (!FlowerPatchOperations.growPatch(level, pos, flowerId, player)) {
			return InteractionResult.PASS;
		}
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		FlowerPatchOperations.playPlaceSound(level, pos);
		return InteractionResult.SUCCESS_SERVER;
	}

	private enum PatchAction {
		CONVERT,
		GROW
	}

	private record PatchUse(BlockPos pos, Identifier flowerId, PatchAction action) {
	}
}
