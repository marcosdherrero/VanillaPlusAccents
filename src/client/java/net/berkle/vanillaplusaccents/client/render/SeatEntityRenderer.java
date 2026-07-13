package net.berkle.vanillaplusaccents.client.render;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.NoopRenderer;

import net.berkle.vanillaplusaccents.entity.VpaEntityTypes;

/** Invisible seat marker — no geometry to draw. */
public final class SeatEntityRenderer {

	private SeatEntityRenderer() {
	}

	public static void register() {
		EntityRendererRegistry.register(VpaEntityTypes.SEAT, NoopRenderer::new);
	}
}
