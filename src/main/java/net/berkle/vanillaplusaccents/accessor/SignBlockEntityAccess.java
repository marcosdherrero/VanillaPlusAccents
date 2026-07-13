package net.berkle.vanillaplusaccents.accessor;

import net.minecraft.world.item.ItemStack;

/** Sign block entity extension for item-on-sign display. */
public interface SignBlockEntityAccess {

	ItemStack vpa$getDisplayedItem(boolean front);

	void vpa$setDisplayedItem(boolean front, ItemStack stack);

	boolean vpa$hasDisplayedItem(boolean front);

	boolean vpa$hasAnyDisplayedItem();
}
