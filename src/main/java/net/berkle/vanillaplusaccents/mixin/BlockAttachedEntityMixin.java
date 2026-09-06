package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;

import net.berkle.vanillaplusaccents.fence.FenceLeadEntity;

/**
 * {@code isPickable} is declared on {@link Entity} and overridden on
 * {@link BlockAttachedEntity} — not on {@link LeashFenceKnotEntity}. Injecting
 * the knot class crashes 26.1.2. VPA-only knots must not steal block-place/break
 * hits (that also swallows vanilla place/break sounds). Animal hitches stay pickable.
 * Item frames and paintings are left alone.
 */
@Mixin(BlockAttachedEntity.class)
public abstract class BlockAttachedEntityMixin {

	@Inject(method = "isPickable", at = @At("HEAD"), cancellable = true)
	private void vpa$passHitsThroughVpaKnots(CallbackInfoReturnable<Boolean> cir) {
		BlockAttachedEntity self = (BlockAttachedEntity) (Object) this;
		if (!(self instanceof LeashFenceKnotEntity knot)) {
			return;
		}
		for (Leashable leashed : Leashable.leashableLeashedTo(knot)) {
			if (leashed instanceof FenceLeadEntity) {
				continue;
			}
			if (leashed instanceof Entity entity && entity.isAlive()) {
				return;
			}
		}
		cir.setReturnValue(false);
	}
}
