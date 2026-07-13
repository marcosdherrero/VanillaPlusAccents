package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.vanillaplusaccents.sign.SignDisplayedItemDropper;
import net.berkle.vanillaplusaccents.sign.SignSupport;

@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin {

	@Inject(method = "preRemoveSideEffects", at = @At("HEAD"))
	private void vpa$dropSignDisplayedItems(BlockPos pos, BlockState state, CallbackInfo ci) {
		BlockEntity self = (BlockEntity) (Object) this;
		if (!(self instanceof SignBlockEntity sign)) {
			return;
		}
		if (sign.getLevel() == null || sign.getLevel().isClientSide()) {
			return;
		}
		SignDisplayedItemDropper.dropAll(sign.getLevel(), pos, SignSupport.asAccess(sign));
	}
}
