package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.EnderMan;

import net.berkle.vanillaplusaccents.data.AccentSettingsSavedData;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanLeaveBlockGoal")
public abstract class EndermanLeaveBlockGoalMixin {

	@Shadow
	@Final
	private EnderMan enderman;

	@Inject(method = "canUse()Z", at = @At("HEAD"), cancellable = true)
	private void vpa$blockEndermanPlace(CallbackInfoReturnable<Boolean> cir) {
		if (!(enderman.level() instanceof ServerLevel level)) {
			return;
		}
		if (!AccentSettingsSavedData.get(level).isEndermanGrief()) {
			cir.setReturnValue(false);
		}
	}
}
