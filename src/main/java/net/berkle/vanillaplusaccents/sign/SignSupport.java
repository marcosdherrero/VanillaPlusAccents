package net.berkle.vanillaplusaccents.sign;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignApplicator;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.vanillaplusaccents.accessor.SignBlockEntityAccess;

/** Sign detection helpers that work for vanilla and modded wood types. */
public final class SignSupport {

	private SignSupport() {
	}

	public static boolean isSignBlock(BlockState state) {
		return state.is(BlockTags.ALL_SIGNS);
	}

	public static boolean canAcceptDisplayItem(BlockEntity blockEntity) {
		return blockEntity instanceof SignBlockEntity sign && isSignBlock(sign.getBlockState());
	}

	public static SignBlockEntityAccess asAccess(SignBlockEntity sign) {
		return (SignBlockEntityAccess) sign;
	}

	/** True for vanilla sign items and modded items that place any sign block. */
	public static boolean isSignPlacementItem(ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}

		Item item = stack.getItem();
		if (item instanceof SignItem) {
			return true;
		}

		if (item instanceof BlockItem blockItem) {
			return blockItem.getBlock().defaultBlockState().is(BlockTags.ALL_SIGNS);
		}

		return false;
	}

	/** Dye, ink sac, glow ink, and honeycomb — keep vanilla sign coloring/waxing. */
	public static boolean isSignTextTool(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() instanceof SignApplicator;
	}

	public static boolean isEmptySign(SignBlockEntity sign) {
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
