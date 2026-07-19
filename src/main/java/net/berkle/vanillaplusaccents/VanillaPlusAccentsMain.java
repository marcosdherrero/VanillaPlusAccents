package net.berkle.vanillaplusaccents;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.berkle.vanillaplusaccents.block.VpaBlocks;
import net.berkle.vanillaplusaccents.command.ModCommands;
import net.berkle.vanillaplusaccents.entity.VpaEntityTypes;
import net.berkle.vanillaplusaccents.events.ModInteractionEvents;
import net.berkle.vanillaplusaccents.network.VpaNetworking;

/** Vanilla-friendly decorative and interaction accents. */
public class VanillaPlusAccentsMain implements ModInitializer {

	public static final String MOD_ID = "vanillaplusaccents";
	public static final String COMMAND_ROOT = "vanillaplusaccents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("[{}] Initializing vanilla plus accents…", MOD_ID);
		VpaEntityTypes.register();
		VpaBlocks.register();
		VpaNetworking.registerPayloadTypes();
		CommandRegistrationCallback.EVENT.register(ModCommands::register);
		ModInteractionEvents.register();
		LOGGER.info("[{}] Ready — use /{} help (alias /vpa).", MOD_ID, COMMAND_ROOT);
	}
}
