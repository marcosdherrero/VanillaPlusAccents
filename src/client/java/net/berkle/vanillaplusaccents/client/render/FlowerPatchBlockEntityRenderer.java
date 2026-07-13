package net.berkle.vanillaplusaccents.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

import net.berkle.vanillaplusaccents.block.VpaBlocks;

/** Registers block entity renderers. */
public final class FlowerPatchBlockEntityRenderer {

	private FlowerPatchBlockEntityRenderer() {
	}

	public static void register() {
		BlockEntityRendererRegistry.register(VpaBlocks.FLOWER_PATCH_BE, FlowerPatchRenderer::new);
	}
}
