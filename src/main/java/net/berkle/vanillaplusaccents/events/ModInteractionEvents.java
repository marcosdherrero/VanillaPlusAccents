package net.berkle.vanillaplusaccents.events;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.BlockEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.server.level.ServerLevel;

import net.berkle.vanillaplusaccents.fence.FenceLeadHandler;
import net.berkle.vanillaplusaccents.fence.FenceLeadVisuals;
import net.berkle.vanillaplusaccents.flower.FlowerPatchHandler;
import net.berkle.vanillaplusaccents.itemframe.InvisibleFrameHandler;
import net.berkle.vanillaplusaccents.seat.PiggybackHandler;
import net.berkle.vanillaplusaccents.seat.SeatHandler;
import net.berkle.vanillaplusaccents.sign.SignItemDisplayHandler;

/** Server-side interaction callbacks. */
public final class ModInteractionEvents {

	private ModInteractionEvents() {
	}

	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			for (ServerLevel serverLevel : server.getAllLevels()) {
				FenceLeadHandler.purgeAndSync(serverLevel);
			}
		});

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level instanceof ServerLevel serverLevel && serverLevel.getGameTime() % 20L == 0L) {
				FenceLeadVisuals.ensureAllKnots(serverLevel);
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
