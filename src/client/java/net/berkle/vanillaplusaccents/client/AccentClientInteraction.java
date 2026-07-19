package net.berkle.vanillaplusaccents.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import net.berkle.vanillaplusaccents.network.AccentActionKind;
import net.berkle.vanillaplusaccents.network.AccentActionPayload;

/** Sends ctrl+shift piggyback requests from the client to the server. */
public final class AccentClientInteraction {

	private AccentClientInteraction() {
	}

	public static void register() {
		UseEntityCallback.EVENT.register(AccentClientInteraction::onUseEntity);
	}

	private static boolean accentModifiersActive() {
		Window window = Minecraft.getInstance().getWindow();
		boolean ctrl = InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
			|| InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
		boolean shift = InputConstants.isKeyDown(window, InputConstants.KEY_LSHIFT)
			|| InputConstants.isKeyDown(window, InputConstants.KEY_RSHIFT);
		return ctrl && shift;
	}

	private static InteractionResult onUseEntity(
		Player player,
		Level level,
		InteractionHand hand,
		net.minecraft.world.entity.Entity entity,
		EntityHitResult hitResult
	) {
		if (!level.isClientSide() || hand != InteractionHand.MAIN_HAND || !accentModifiersActive()) {
			return InteractionResult.PASS;
		}
		if (!(entity instanceof Player) || !player.getMainHandItem().isEmpty()) {
			return InteractionResult.PASS;
		}

		ClientPlayNetworking.send(new AccentActionPayload(
			AccentActionKind.PIGGYBACK_PLAYER,
			BlockPos.ZERO,
			entity.getId()
		));
		return InteractionResult.SUCCESS;
	}
}
