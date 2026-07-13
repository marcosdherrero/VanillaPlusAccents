package net.berkle.vanillaplusaccents.sign;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.berkle.vanillaplusaccents.accessor.SignBlockEntityAccess;

/** Drops displayed items when a sign block entity is removed. */
public final class SignDisplayedItemDropper {

	private SignDisplayedItemDropper() {
	}

	public static void dropAll(Level level, BlockPos pos, SignBlockEntityAccess access) {
		dropIfPresent(level, pos, access.vpa$getDisplayedItem(true));
		dropIfPresent(level, pos, access.vpa$getDisplayedItem(false));
		access.vpa$setDisplayedItem(true, ItemStack.EMPTY);
		access.vpa$setDisplayedItem(false, ItemStack.EMPTY);
	}

	private static void dropIfPresent(Level level, BlockPos pos, ItemStack stack) {
		if (!stack.isEmpty()) {
			Containers.dropItemStack(
				level,
				pos.getX() + 0.5,
				pos.getY() + 0.5,
				pos.getZ() + 0.5,
				stack.copy()
			);
		}
	}
}
