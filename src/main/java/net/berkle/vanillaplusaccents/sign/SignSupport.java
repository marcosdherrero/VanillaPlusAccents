package net.berkle.vanillaplusaccents.sign;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SignItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
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
}
