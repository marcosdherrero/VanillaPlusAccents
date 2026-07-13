package net.berkle.vanillaplusaccents.accessor;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;

/** Client render state extension for sign item display. */
public interface SignRenderStateAccess {

	ItemStack vpa$getDisplayedItem(boolean front);

	void vpa$setDisplayedItem(boolean front, ItemStack stack);

	ItemStackRenderState vpa$itemState(boolean front);
}
