package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

@Mixin(Entity.class)
public abstract class PlayerPassengerOffsetMixin {

	@Inject(method = "getPassengerRidingPosition", at = @At("HEAD"), cancellable = true)
	private void vpa$piggybackOffset(Entity passenger, CallbackInfoReturnable<Vec3> cir) {
		Entity self = (Entity) (Object) this;
		if (self instanceof Player carrier && passenger instanceof Player) {
			cir.setReturnValue(carrier.position().add(0.0, carrier.getBbHeight() - 0.25, 0.0));
		}
	}
}
