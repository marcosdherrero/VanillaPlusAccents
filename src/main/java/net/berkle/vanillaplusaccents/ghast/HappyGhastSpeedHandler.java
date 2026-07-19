package net.berkle.vanillaplusaccents.ghast;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.happyghast.HappyGhast;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;
import net.berkle.vanillaplusaccents.data.AccentSettingsSavedData;

/** Applies a relative {@link Attributes#FLYING_SPEED} factor while a player rides a Happy Ghast. */
public final class HappyGhastSpeedHandler {

	public static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "happy_ghast_speed");

	/** Last Happy Ghast we modified for each rider, so we can clear on dismount. */
	private static final Map<UUID, UUID> RIDER_TO_GHAST = new ConcurrentHashMap<>();

	private HappyGhastSpeedHandler() {
	}

	public static void tickPlayer(ServerPlayer player) {
		AccentSettingsSavedData settings = AccentSettingsSavedData.get(player.level());
		UUID riderId = player.getUUID();
		UUID previousGhastId = RIDER_TO_GHAST.get(riderId);

		Entity vehicle = player.getVehicle();
		if (vehicle instanceof HappyGhast ghast && ghast.getControllingPassenger() == player) {
			apply(ghast, settings.getHappyGhastSpeed());
			UUID ghastId = ghast.getUUID();
			RIDER_TO_GHAST.put(riderId, ghastId);
			if (previousGhastId != null && !previousGhastId.equals(ghastId)) {
				clearGhast(player.level(), previousGhastId);
			}
			return;
		}

		if (previousGhastId != null) {
			clearGhast(player.level(), previousGhastId);
			RIDER_TO_GHAST.remove(riderId);
		}
	}

	public static void clearPlayer(ServerPlayer player) {
		UUID ghastId = RIDER_TO_GHAST.remove(player.getUUID());
		if (ghastId != null) {
			clearGhast(player.level(), ghastId);
		}
	}

	private static void apply(HappyGhast ghast, double factor) {
		AttributeInstance flying = ghast.getAttribute(Attributes.FLYING_SPEED);
		if (flying == null) {
			return;
		}
		if (Math.abs(factor - 1.0) < 1.0e-6) {
			flying.removeModifier(MODIFIER_ID);
			return;
		}
		flying.addOrUpdateTransientModifier(new AttributeModifier(
			MODIFIER_ID,
			factor - 1.0,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		));
	}

	private static void clearGhast(net.minecraft.world.level.Level level, UUID ghastId) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}
		Entity entity = serverLevel.getEntity(ghastId);
		if (entity instanceof HappyGhast ghast) {
			AttributeInstance flying = ghast.getAttribute(Attributes.FLYING_SPEED);
			if (flying != null) {
				flying.removeModifier(MODIFIER_ID);
			}
		}
	}
}
