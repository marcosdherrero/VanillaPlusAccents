package net.berkle.vanillaplusaccents.sign;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;

import net.berkle.vanillaplusaccents.accessor.SignBlockEntityAccess;

/** Place or remove items on empty signs like item frames. Sign items display instead of opening the editor. */
public final class SignItemDisplayHandler {

	private SignItemDisplayHandler() {
	}

	public static InteractionResult onUseBlock(
		Player player,
		Level level,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		if (hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		BlockEntity blockEntity = level.getBlockEntity(hitResult.getBlockPos());
		if (!SignSupport.canAcceptDisplayItem(blockEntity)) {
			return InteractionResult.PASS;
		}

		SignBlockEntity sign = (SignBlockEntity) blockEntity;
		if (!SignSupport.isEmptySign(sign)) {
			return InteractionResult.PASS;
		}

		boolean front = sign.isFacingFrontText(player);
		SignBlockEntityAccess access = SignSupport.asAccess(sign);
		ItemStack held = player.getItemInHand(hand);

		if (held.isEmpty()) {
			if (!access.vpa$hasDisplayedItem(front)) {
				return InteractionResult.PASS;
			}
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			ItemStack removed = access.vpa$getDisplayedItem(front).copy();
			access.vpa$setDisplayedItem(front, ItemStack.EMPTY);
			if (!player.getInventory().add(removed)) {
				player.drop(removed, false);
			}
			level.playSound(null, sign.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
			return InteractionResult.SUCCESS;
		}

		if (SignSupport.isSignTextTool(held) || access.vpa$hasDisplayedItem(front)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}

		if (!(level instanceof ServerLevel)) {
			return InteractionResult.PASS;
		}

		access.vpa$setDisplayedItem(front, held);
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		level.playSound(null, sign.getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
		return InteractionResult.SUCCESS;
	}
}
