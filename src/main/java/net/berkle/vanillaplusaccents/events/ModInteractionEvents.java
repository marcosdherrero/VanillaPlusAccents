package net.berkle.vanillaplusaccents.events;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import net.berkle.vanillaplusaccents.fence.FenceLeadHandler;
import net.berkle.vanillaplusaccents.fence.FenceLeadVisuals;
import net.berkle.vanillaplusaccents.flower.FlowerPatchHandler;
import net.berkle.vanillaplusaccents.ghast.HappyGhastSpeedHandler;
import net.berkle.vanillaplusaccents.itemframe.InvisibleFrameHandler;
import net.berkle.vanillaplusaccents.network.VpaNetworking;
import net.berkle.vanillaplusaccents.path.PathSpeedHandler;
import net.berkle.vanillaplusaccents.seat.PiggybackHandler;
import net.berkle.vanillaplusaccents.seat.SeatHandler;
import net.berkle.vanillaplusaccents.sign.SignItemDisplayHandler;

/** Server-side interaction callbacks. */
public final class ModInteractionEvents {

	private ModInteractionEvents() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			server.execute(() -> {
				if (player.isRemoved()) {
					return;
				}
				VpaNetworking.syncFenceLeads(player);
				if (player.level() instanceof ServerLevel level) {
					FenceLeadVisuals.resync(level);
				}
			});
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			PathSpeedHandler.clear(player);
			HappyGhastSpeedHandler.clearPlayer(player);
		});

		ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
			VpaNetworking.syncFenceLeads(player);
			FenceLeadVisuals.resync(destination);
		});

		ServerChunkEvents.CHUNK_LOAD.register((level, chunk, newlyCreated) -> {
			level.getServer().execute(() -> {
				if (level.getChunkSource().getChunkNow(chunk.getPos().x(), chunk.getPos().z()) == null) {
					return;
				}
				FenceLeadVisuals.ensureLinksInChunk(level, chunk.getPos());
			});
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				PathSpeedHandler.tickPlayer(player);
				HappyGhastSpeedHandler.tickPlayer(player);
			}
		});

		PlayerBlockBreakEvents.AFTER.register(FenceLeadHandler::onFenceBroken);

		UseEntityCallback.EVENT.register(InvisibleFrameHandler::onUseEntity);
		UseEntityCallback.EVENT.register(PiggybackHandler::onUseEntity);
		UseEntityCallback.EVENT.register(FenceLeadHandler::onUseKnot);

		BlockEvents.USE_ITEM_ON.register(FlowerPatchHandler::onUseItemOn);

		UseBlockCallback.EVENT.register(FenceLeadHandler::onUseBlock);
		UseBlockCallback.EVENT.register(FlowerPatchHandler::onUseBlock);
		UseBlockCallback.EVENT.register(SignItemDisplayHandler::onUseBlock);
		UseBlockCallback.EVENT.register(SeatHandler::onUseBlock);
	}
}
