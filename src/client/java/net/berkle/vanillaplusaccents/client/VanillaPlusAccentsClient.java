package net.berkle.vanillaplusaccents.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.berkle.vanillaplusaccents.client.render.FenceLeadClientCache;
import net.berkle.vanillaplusaccents.client.render.FenceLeadEntityRenderer;
import net.berkle.vanillaplusaccents.client.render.FenceLeadWorldRenderer;
import net.berkle.vanillaplusaccents.client.render.FlowerPatchBlockEntityRenderer;
import net.berkle.vanillaplusaccents.client.render.FlowerPatchClientCache;
import net.berkle.vanillaplusaccents.client.render.SeatEntityRenderer;
import net.berkle.vanillaplusaccents.network.SyncFenceLeadsPayload;

/** Client entry: accent-action packets and custom renderers. */
public class VanillaPlusAccentsClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		AccentClientInteraction.register();
		FlowerPatchBlockEntityRenderer.register();
		SeatEntityRenderer.register();
		FenceLeadEntityRenderer.register();
		FenceLeadWorldRenderer.register();

		ClientPlayNetworking.registerGlobalReceiver(SyncFenceLeadsPayload.TYPE, (payload, context) ->
			context.client().execute(() -> {
				FenceLeadClientCache.PendingAnchor pending = null;
				if (payload.hasPending()) {
					pending = new FenceLeadClientCache.PendingAnchor(
						payload.pendingPlayer(),
						payload.pendingDimension(),
						payload.pendingPos()
					);
				}
				FenceLeadClientCache.replaceAll(payload.links(), pending);
			})
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			FenceLeadClientCache.clear();
			FlowerPatchClientCache.clear();
		});
	}
}
