package net.berkle.vanillaplusaccents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.AbstractSignRenderer;
import net.minecraft.client.renderer.blockentity.state.SignRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import net.berkle.vanillaplusaccents.accessor.SignBlockEntityAccess;
import net.berkle.vanillaplusaccents.accessor.SignRenderStateAccess;
import net.berkle.vanillaplusaccents.client.render.SignDisplayedItemRenderer;

@Mixin(AbstractSignRenderer.class)
public abstract class AbstractSignRendererMixin {

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void vpa$extractSignItem(
		SignBlockEntity blockEntity,
		SignRenderState state,
		float partialTick,
		net.minecraft.world.phys.Vec3 cameraPos,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
		CallbackInfo ci
	) {
		SignBlockEntityAccess access = (SignBlockEntityAccess) blockEntity;
		SignRenderStateAccess renderAccess = (SignRenderStateAccess) state;
		int seed = blockEntity.getBlockPos().hashCode();

		vpa$extractSideItem(blockEntity, renderAccess, access.vpa$getDisplayedItem(true), true, seed);
		vpa$extractSideItem(blockEntity, renderAccess, access.vpa$getDisplayedItem(false), false, seed + 1);
	}

	@Unique
	private static void vpa$extractSideItem(
		SignBlockEntity blockEntity,
		SignRenderStateAccess renderAccess,
		ItemStack displayed,
		boolean front,
		int seed
	) {
		renderAccess.vpa$setDisplayedItem(front, displayed);
		if (displayed.isEmpty()) {
			renderAccess.vpa$itemState(front).clear();
			return;
		}
		Minecraft.getInstance().getItemModelResolver().updateForTopItem(
			renderAccess.vpa$itemState(front),
			displayed,
			ItemDisplayContext.FIXED,
			blockEntity.getLevel(),
			null,
			seed
		);
	}

	@Inject(
		method = "submitSignWithText",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;submitSignText(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/level/block/entity/SignText;)V",
			ordinal = 0,
			shift = At.Shift.BEFORE
		)
	)
	private void vpa$renderFrontSignItem(
		SignRenderState state,
		PoseStack poseStack,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
		SubmitNodeCollector submitNodeCollector,
		CallbackInfo ci
	) {
		SignDisplayedItemRenderer.submitInTextSpace(
			state,
			poseStack,
			submitNodeCollector,
			(SignRenderStateAccess) state,
			true
		);
	}

	@Inject(
		method = "submitSignWithText",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/blockentity/AbstractSignRenderer;submitSignText(Lnet/minecraft/client/renderer/blockentity/state/SignRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/world/level/block/entity/SignText;)V",
			ordinal = 1,
			shift = At.Shift.BEFORE
		)
	)
	private void vpa$renderBackSignItem(
		SignRenderState state,
		PoseStack poseStack,
		ModelFeatureRenderer.CrumblingOverlay crumblingOverlay,
		SubmitNodeCollector submitNodeCollector,
		CallbackInfo ci
	) {
		SignDisplayedItemRenderer.submitInTextSpace(
			state,
			poseStack,
			submitNodeCollector,
			(SignRenderStateAccess) state,
			false
		);
	}
}
