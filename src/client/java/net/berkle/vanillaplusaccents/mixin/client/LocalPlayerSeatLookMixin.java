package net.berkle.vanillaplusaccents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.player.LocalPlayer;

import net.berkle.vanillaplusaccents.seat.SeatEntity;

/**
 * While seated, vanilla treats the player as a vehicle passenger and lerps head yaw for
 * the camera. Use immediate yaw like standing so mouse look feels responsive.
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerSeatLookMixin {

	@Inject(method = "getViewYRot", at = @At("HEAD"), cancellable = true)
	private void vpa$seatInstantLook(float partialTick, CallbackInfoReturnable<Float> cir) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (self.getVehicle() instanceof SeatEntity) {
			cir.setReturnValue(self.getYRot());
		}
	}

	@Inject(method = "tick", at = @At("HEAD"))
	private void vpa$syncSeatHeadRot(CallbackInfo ci) {
		LocalPlayer self = (LocalPlayer) (Object) this;
		if (self.getVehicle() instanceof SeatEntity) {
			float yaw = self.getYRot();
			self.setYHeadRot(yaw);
			self.yHeadRotO = yaw;
		}
	}
}
