package net.berkle.vanillaplusaccents.seat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

import net.berkle.vanillaplusaccents.network.AccentActionKind;
import net.berkle.vanillaplusaccents.network.AccentActionPayload;

/** Ctrl+shift accent actions routed from the client packet. Sitting uses UseBlockCallback. */
public final class AccentActionHandler {

	private AccentActionHandler() {
	}

	public static void handle(ServerPlayer player, AccentActionPayload payload) {
		switch (payload.kind()) {
			case SIT_ON_BLOCK -> SeatSupport.trySit(player, payload.blockPos());
			case PIGGYBACK_PLAYER -> PiggybackHandler.tryMount(player, payload.entityId());
		}
	}
}
