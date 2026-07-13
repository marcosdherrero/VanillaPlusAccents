package net.berkle.vanillaplusaccents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemStack;

import net.berkle.vanillaplusaccents.accessor.SignRenderStateAccess;

@Mixin(SignRenderState.class)
public class SignRenderStateMixin implements SignRenderStateAccess {

	@Unique
	private ItemStack vpa$frontDisplayedItem = ItemStack.EMPTY;

	@Unique
	private ItemStack vpa$backDisplayedItem = ItemStack.EMPTY;

	@Unique
	private final ItemStackRenderState vpa$frontItemState = new ItemStackRenderState();

	@Unique
	private final ItemStackRenderState vpa$backItemState = new ItemStackRenderState();

	@Override
	public ItemStack vpa$getDisplayedItem(boolean front) {
		return front ? vpa$frontDisplayedItem : vpa$backDisplayedItem;
	}

	@Override
	public void vpa$setDisplayedItem(boolean front, ItemStack stack) {
		ItemStack stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
		if (front) {
			vpa$frontDisplayedItem = stored;
		} else {
			vpa$backDisplayedItem = stored;
		}
	}

	@Override
	public ItemStackRenderState vpa$itemState(boolean front) {
		return front ? vpa$frontItemState : vpa$backItemState;
	}
}
