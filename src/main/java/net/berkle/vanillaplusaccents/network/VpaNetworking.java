package net.berkle.vanillaplusaccents.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.berkle.vanillaplusaccents.fence.FenceLeadSavedData;
import net.berkle.vanillaplusaccents.seat.AccentActionHandler;

/** Custom payload registration for accent interactions. */
public final class VpaNetworking {

	private VpaNetworking() {
	}

	public static void registerPayloadTypes() {
		PayloadTypeRegistry.serverboundPlay().register(AccentActionPayload.TYPE, AccentActionPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SyncFenceLeadsPayload.TYPE, SyncFenceLeadsPayload.CODEC);

		ServerPlayNetworking.registerGlobalReceiver(AccentActionPayload.TYPE, (payload, context) ->
			context.server().execute(() -> {
				ServerPlayer player = context.player();
				if (player == null) {
					return;
				}
				AccentActionHandler.handle(player, payload);
			})
		);
	}

	public static void syncFenceLeads(ServerLevel level) {
		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		for (ServerPlayer player : PlayerLookup.all(level.getServer())) {
			ServerPlayNetworking.send(player, payloadFor(data, player));
		}
	}

	public static void syncFenceLeads(ServerPlayer player) {
		if (player.level() instanceof ServerLevel serverLevel) {
			ServerPlayNetworking.send(player, payloadFor(FenceLeadSavedData.get(serverLevel), player));
		}
	}

	private static SyncFenceLeadsPayload payloadFor(FenceLeadSavedData data, ServerPlayer player) {
		FenceLeadSavedData.PendingLink pending = data.getPending(player.getUUID());
		if (pending == null) {
			return SyncFenceLeadsPayload.of(data.allLinks(), null, null, null);
		}
		return SyncFenceLeadsPayload.of(data.allLinks(), player.getUUID(), pending.dimension(), pending.pos());
	}

	public static void registerClientReceivers() {
		// Handled in VanillaPlusAccentsClient.
	}
}
