package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.berkle.vanillaplusaccents.accessor.SignBlockEntityAccess;
import net.berkle.vanillaplusaccents.sign.SignDisplayedItemSync;

@Mixin(SignBlockEntity.class)
public abstract class SignBlockEntityMixin implements SignBlockEntityAccess {

	@Unique
	private ItemStack vpa$frontDisplayedItem = ItemStack.EMPTY;

	@Unique
	private ItemStack vpa$backDisplayedItem = ItemStack.EMPTY;

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
		SignBlockEntity self = (SignBlockEntity) (Object) this;
		self.setChanged();
		SignDisplayedItemSync.broadcast(self);
	}

	@Override
	public boolean vpa$hasDisplayedItem(boolean front) {
		return !vpa$getDisplayedItem(front).isEmpty();
	}

	@Override
	public boolean vpa$hasAnyDisplayedItem() {
		return vpa$hasDisplayedItem(true) || vpa$hasDisplayedItem(false);
	}

	@Inject(method = "loadAdditional", at = @At("TAIL"))
	private void vpa$loadAdditional(ValueInput input, CallbackInfo ci) {
		vpa$frontDisplayedItem = input.read("vpa_front_item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		vpa$backDisplayedItem = input.read("vpa_back_item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);

		if (vpa$frontDisplayedItem.isEmpty()) {
			vpa$frontDisplayedItem = input.read("vpa_displayed_item", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
		}
	}

	@Inject(method = "saveAdditional", at = @At("TAIL"))
	private void vpa$saveAdditional(ValueOutput output, CallbackInfo ci) {
		if (!vpa$frontDisplayedItem.isEmpty()) {
			output.store("vpa_front_item", ItemStack.CODEC, vpa$frontDisplayedItem);
		}
		if (!vpa$backDisplayedItem.isEmpty()) {
			output.store("vpa_back_item", ItemStack.CODEC, vpa$backDisplayedItem);
		}
	}
}
