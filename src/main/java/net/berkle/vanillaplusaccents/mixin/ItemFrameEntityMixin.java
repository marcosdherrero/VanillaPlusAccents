package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.berkle.vanillaplusaccents.accessor.ItemFrameEntityAccess;

@Mixin(ItemFrame.class)
public abstract class ItemFrameEntityMixin implements ItemFrameEntityAccess {

	@Unique
	private static final EntityDataAccessor<Boolean> VPA_INVISIBLE_FRAME = SynchedEntityData.defineId(
		ItemFrame.class,
		EntityDataSerializers.BOOLEAN
	);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void vpa$defineSynchedData(SynchedEntityData.Builder builder, CallbackInfo ci) {
		builder.define(VPA_INVISIBLE_FRAME, false);
	}

	@Override
	public boolean vpa$isFrameInvisible() {
		ItemFrame self = (ItemFrame) (Object) this;
		return self.getEntityData().get(VPA_INVISIBLE_FRAME) || self.isInvisible();
	}

	@Override
	public void vpa$setFrameInvisible(boolean invisible) {
		ItemFrame frame = (ItemFrame) (Object) this;
		frame.getEntityData().set(VPA_INVISIBLE_FRAME, invisible);
		frame.setInvisible(invisible);
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void vpa$saveInvisible(ValueOutput output, CallbackInfo ci) {
		boolean invisible = vpa$isFrameInvisible() || ((ItemFrame) (Object) this).isInvisible();
		output.putBoolean("Invisible", invisible);
		output.putBoolean("vpa_invisible", invisible);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void vpa$loadInvisible(ValueInput input, CallbackInfo ci) {
		boolean invisible = input.getBooleanOr("vpa_invisible", false)
			|| input.getBooleanOr("Invisible", false)
			|| ((ItemFrame) (Object) this).isInvisible();
		vpa$setFrameInvisible(invisible);
	}
}
