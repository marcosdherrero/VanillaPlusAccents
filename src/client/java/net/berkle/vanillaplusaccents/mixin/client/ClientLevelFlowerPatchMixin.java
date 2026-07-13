package net.berkle.vanillaplusaccents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.vanillaplusaccents.block.VpaBlocks;
import net.berkle.vanillaplusaccents.block.entity.FlowerPatchBlockEntity;
import net.berkle.vanillaplusaccents.client.render.FlowerPatchClientCache;
import net.berkle.vanillaplusaccents.flower.FlowerPatchSupport;

/**
 * Spawns destroy particles from the contained flower block. The patch model particle alone looks gray because
 * terrain particles default to 0.6 brightness and the patch has no grass tint.
 */
@Mixin(ClientLevel.class)
public abstract class ClientLevelFlowerPatchMixin {

	@Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
	private void vanillaplusaccents$flowerPatchDestroyParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (!state.is(VpaBlocks.FLOWER_PATCH)) {
			return;
		}

		ClientLevel self = (ClientLevel) (Object) this;
		Identifier flowerId = null;

		if (self.getBlockEntity(pos) instanceof FlowerPatchBlockEntity patch) {
			flowerId = patch.getFlowerId();
		}
		if (flowerId == null) {
			flowerId = FlowerPatchClientCache.remove(pos);
		} else {
			FlowerPatchClientCache.remove(pos);
		}

		if (flowerId == null) {
			return;
		}

		Block flower = BuiltInRegistries.BLOCK.getValue(flowerId);
		if (flower == null || flower == Blocks.AIR || !FlowerPatchSupport.canStackInPatch(flower)) {
			return;
		}

		ci.cancel();
		self.addDestroyBlockEffect(pos, flower.defaultBlockState());
	}
}
