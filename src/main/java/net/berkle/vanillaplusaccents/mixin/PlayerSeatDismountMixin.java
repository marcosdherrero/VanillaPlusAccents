package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.player.Player;

import net.berkle.vanillaplusaccents.seat.SeatEntity;

/**
 * Sitting starts while Shift is held; vanilla would immediately dismount via
 * {@link Player#wantsToStopRiding()}. Ignore sneak-dismount until Shift is released once.
 */
@Mixin(Player.class)
public abstract class PlayerSeatDismountMixin {

	@Inject(method = "wantsToStopRiding", at = @At("HEAD"), cancellable = true)
	private void vpa$seatSneakGrace(CallbackInfoReturnable<Boolean> cir) {
		Player self = (Player) (Object) this;
		if (self.getVehicle() instanceof SeatEntity seat && !seat.allowsSneakDismount()) {
			cir.setReturnValue(false);
		}
	}
}
