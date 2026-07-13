package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import net.berkle.vanillaplusaccents.accessor.SignBlockEntityAccess;

@Mixin(SignBlock.class)
public abstract class SignBlockMixin {

	@Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
	private void vpa$blockTextEditWhenItemDisplayed(Player player, SignBlockEntity sign, boolean front, CallbackInfo ci) {
		if (((SignBlockEntityAccess) sign).vpa$hasDisplayedItem(front)) {
			ci.cancel();
		}
	}
}
