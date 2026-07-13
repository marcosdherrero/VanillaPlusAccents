package net.berkle.vanillaplusaccents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;

import net.berkle.vanillaplusaccents.accessor.ItemFrameEntityAccess;

@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameRendererMixin {

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void vpa$extractInvisible(ItemFrame entity, ItemFrameRenderState state, float partialTick, CallbackInfo ci) {
		if (((ItemFrameEntityAccess) entity).vpa$isFrameInvisible()) {
			state.frameModel.clear();
		}
	}
}
