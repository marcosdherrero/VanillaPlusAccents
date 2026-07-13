package net.berkle.vanillaplusaccents.seat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

/** Piggyback riding via ctrl+shift entity interaction. */
public final class PiggybackHandler {

	private PiggybackHandler() {
	}

	public static InteractionResult onUseEntity(
		Player player,
		Level level,
		InteractionHand hand,
		Entity entity,
		EntityHitResult hitResult
	) {
		// Server validates via AccentActionPayload from client (ctrl+shift).
		return InteractionResult.PASS;
	}

	public static boolean tryMount(ServerPlayer rider, int carrierEntityId) {
		Entity entity = rider.level().getEntity(carrierEntityId);
		if (!(entity instanceof ServerPlayer carrier) || carrier == rider) {
			return false;
		}
		if (carrier.isPassenger() || carrier.getVehicle() != null) {
			return false;
		}
		if (rider.getVehicle() != null) {
			rider.stopRiding();
		}
		return rider.startRiding(carrier, true, true);
	}
}
