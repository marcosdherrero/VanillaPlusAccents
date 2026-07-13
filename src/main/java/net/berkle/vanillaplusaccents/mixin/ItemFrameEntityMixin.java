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
		return ((ItemFrame) (Object) this).getEntityData().get(VPA_INVISIBLE_FRAME);
	}

	@Override
	public void vpa$setFrameInvisible(boolean invisible) {
		((ItemFrame) (Object) this).getEntityData().set(VPA_INVISIBLE_FRAME, invisible);
	}
}
