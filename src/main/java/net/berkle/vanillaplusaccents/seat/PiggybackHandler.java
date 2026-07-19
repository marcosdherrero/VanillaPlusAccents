package net.berkle.vanillaplusaccents.seat;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/** Piggyback riding via ctrl+shift entity interaction. Supports stacked player towers. */
public final class PiggybackHandler {

	/** Riders who mounted while Shift was held; block sneak-dismount until Shift is released once. */
	private static final Set<UUID> SNEAK_DISMOUNT_GRACE = ConcurrentHashMap.newKeySet();

	private static final double MAX_MOUNT_DISTANCE_SQR = 6.0 * 6.0;

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
		if (!rider.getMainHandItem().isEmpty()) {
			return false;
		}
		Entity entity = rider.level().getEntity(carrierEntityId);
		if (!(entity instanceof ServerPlayer carrier) || carrier == rider) {
			return false;
		}
		if (rider.distanceToSqr(carrier) > MAX_MOUNT_DISTANCE_SQR) {
			return false;
		}
		// One rider per player: mount the top of a stack (a passenger with a free back), not a busy carrier.
		if (!carrier.getPassengers().isEmpty()) {
			return false;
		}
		// Block cycles (e.g. bottom of a tower mounting someone already on that tower).
		if (wouldCreateCycle(rider, carrier)) {
			return false;
		}
		if (rider.getVehicle() != null) {
			rider.stopRiding();
		}
		if (!rider.startRiding(carrier, true, true)) {
			return false;
		}
		SNEAK_DISMOUNT_GRACE.add(rider.getUUID());
		return true;
	}

	/**
	 * True if {@code rider} mounting {@code carrier} would loop the ride chain.
	 * Allows stacking both ways: a carrier with passengers may mount someone else
	 * (A on B, then B mounts C), and a free player may mount the top of a stack
	 * (A on B, then C mounts A).
	 */
	private static boolean wouldCreateCycle(ServerPlayer rider, ServerPlayer carrier) {
		if (rider.hasIndirectPassenger(carrier)) {
			return true;
		}
		Entity walk = carrier;
		while (walk != null) {
			if (walk == rider) {
				return true;
			}
			walk = walk.getVehicle();
		}
		return false;
	}

	/**
	 * While Shift is still held after a ctrl+shift mount, vanilla would immediately
	 * {@link Player#wantsToStopRiding()}. Keep the ride until Shift is released once.
	 */
	public static boolean blocksSneakDismount(Player player) {
		UUID id = player.getUUID();
		if (!(player.getVehicle() instanceof Player)) {
			SNEAK_DISMOUNT_GRACE.remove(id);
			return false;
		}
		if (!SNEAK_DISMOUNT_GRACE.contains(id)) {
			return false;
		}
		if (!player.isShiftKeyDown()) {
			SNEAK_DISMOUNT_GRACE.remove(id);
			return false;
		}
		return true;
	}
}
