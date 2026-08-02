package net.berkle.vanillaplusaccents.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.berkle.vanillaplusaccents.path.PathSpeedHandler;

/**
 * Vanilla FOV scales with {@link Attributes#MOVEMENT_SPEED}. Path/mud boosts use that attribute for
 * movement feel and jump persistence, but should not change FOV.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerFovMixin {

	@Redirect(
		method = "getFieldOfViewModifier",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/player/AbstractClientPlayer;getAttributeValue(Lnet/minecraft/core/Holder;)D"
		)
	)
	private double vpa$fovIgnorePathSpeed(AbstractClientPlayer player, Holder<Attribute> attribute) {
		if (Attributes.MOVEMENT_SPEED.equals(attribute)) {
			return PathSpeedHandler.movementSpeedForFov(player);
		}
		return player.getAttributeValue(attribute);
	}
}
