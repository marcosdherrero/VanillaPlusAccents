package net.berkle.vanillaplusaccents.path;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;
import net.berkle.vanillaplusaccents.data.AccentSettingsSavedData;

/** Transient movement modifiers for dirt paths (boost + full-block step-up) and mud. */
public final class PathSpeedHandler {

	public static final Identifier PATH_MODIFIER_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "path_speed");
	public static final Identifier PATH_STEP_MODIFIER_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "path_step");
	public static final Identifier MUD_MODIFIER_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "mud_speed");

	/** Player base step height is 0.6; +0.4 → 1.0 so full blocks step like slabs. */
	private static final double PATH_STEP_HEIGHT_BONUS = 0.4;

	private PathSpeedHandler() {
	}

	public static void tickPlayer(ServerPlayer player) {
		AccentSettingsSavedData settings = AccentSettingsSavedData.get(player.level());
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed == null) {
			return;
		}
		tickPath(player, speed, settings);
		tickMud(player, speed, settings);
	}

	public static void clear(ServerPlayer player) {
		AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.removeModifier(PATH_MODIFIER_ID);
			speed.removeModifier(MUD_MODIFIER_ID);
		}
		AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
		if (step != null) {
			step.removeModifier(PATH_STEP_MODIFIER_ID);
		}
	}

	private static void tickPath(ServerPlayer player, AttributeInstance speed, AccentSettingsSavedData settings) {
		boolean onPath = isOn(player, Blocks.DIRT_PATH);
		double factor = settings.getPathSpeed();

		if (!onPath || Math.abs(factor - 1.0) < 1.0e-6) {
			speed.removeModifier(PATH_MODIFIER_ID);
		} else {
			speed.addOrUpdateTransientModifier(new AttributeModifier(
				PATH_MODIFIER_ID,
				factor - 1.0,
				AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
			));
		}

		AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
		if (step == null) {
			return;
		}
		// While on dirt path, walk up full blocks the way slabs auto-step.
		if (onPath) {
			step.addOrUpdateTransientModifier(new AttributeModifier(
				PATH_STEP_MODIFIER_ID,
				PATH_STEP_HEIGHT_BONUS,
				AttributeModifier.Operation.ADD_VALUE
			));
		} else {
			step.removeModifier(PATH_STEP_MODIFIER_ID);
		}
	}

	private static void tickMud(ServerPlayer player, AttributeInstance speed, AccentSettingsSavedData settings) {
		double factor = settings.getMudSpeed();
		// 1.0 = normal speed → no modifier. Otherwise ADD_MULTIPLIED_TOTAL of (factor - 1).
		if (Math.abs(factor - 1.0) < 1.0e-6 || !isOn(player, Blocks.MUD)) {
			speed.removeModifier(MUD_MODIFIER_ID);
			return;
		}
		speed.addOrUpdateTransientModifier(new AttributeModifier(
			MUD_MODIFIER_ID,
			factor - 1.0,
			AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
		));
	}

	private static boolean isOn(ServerPlayer player, net.minecraft.world.level.block.Block block) {
		BlockPos feet = player.blockPosition();
		BlockPos below = feet.below();
		return player.level().getBlockState(feet).is(block)
			|| player.level().getBlockState(below).is(block);
	}
}
