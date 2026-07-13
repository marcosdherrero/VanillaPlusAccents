package net.berkle.vanillaplusaccents.sign;

import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.phys.BlockHitResult;

import net.berkle.vanillaplusaccents.accessor.SignBlockEntityAccess;

/** Place or remove items on empty signs like item frames. */
public final class SignItemDisplayHandler {

	private SignItemDisplayHandler() {
	}

	public static InteractionResult onUseBlock(
		Player player,
		Level level,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		if (!(level instanceof ServerLevel) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}

		BlockEntity blockEntity = level.getBlockEntity(hitResult.getBlockPos());
		if (!SignSupport.canAcceptDisplayItem(blockEntity)) {
			return InteractionResult.PASS;
		}

		SignBlockEntity sign = (SignBlockEntity) blockEntity;
		if (!isEmptySign(sign)) {
			return InteractionResult.PASS;
		}

		boolean front = sign.isFacingFrontText(player);
		SignBlockEntityAccess access = SignSupport.asAccess(sign);
		ItemStack held = player.getItemInHand(hand);

		if (held.isEmpty()) {
			if (!access.vpa$hasDisplayedItem(front)) {
				return InteractionResult.PASS;
			}
			ItemStack removed = access.vpa$getDisplayedItem(front).copy();
			access.vpa$setDisplayedItem(front, ItemStack.EMPTY);
			if (!player.getInventory().add(removed)) {
				player.drop(removed, false);
			}
			level.playSound(null, sign.getBlockPos(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
			return InteractionResult.SUCCESS;
		}

		if (SignSupport.isSignPlacementItem(held)) {
			return InteractionResult.PASS;
		}

		if (access.vpa$hasDisplayedItem(front)) {
			return InteractionResult.PASS;
		}

		access.vpa$setDisplayedItem(front, held);
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		level.playSound(null, sign.getBlockPos(), SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
		return InteractionResult.SUCCESS;
	}

	private static boolean isEmptySign(SignBlockEntity sign) {
		return isBlank(sign.getFrontText()) && isBlank(sign.getBackText());
	}

	private static boolean isBlank(SignText text) {
		for (Component line : text.getMessages(false)) {
			if (line != null && !line.getString().isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
