package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;

import net.berkle.vanillaplusaccents.fence.FenceLeadVisuals;

/**
 * Vanilla discards a knot when the last leashed entity unloads. Fence-to-fence
 * markers can vanish on chunk unload while {@link net.berkle.vanillaplusaccents.fence.FenceLeadSavedData}
 * still owns the span — keep the tie until that link (and any animals) are gone.
 */
@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotEntityMixin {

	@Inject(method = "notifyLeasheeRemoved", at = @At("HEAD"), cancellable = true)
	private void vpa$keepKnotForFenceLinks(Leashable leashee, CallbackInfo ci) {
		LeashFenceKnotEntity knot = (LeashFenceKnotEntity) (Object) this;
		if (knot.level() instanceof ServerLevel level && FenceLeadVisuals.isKnotNeeded(level, knot.getPos())) {
			ci.cancel();
		}
	}
}
