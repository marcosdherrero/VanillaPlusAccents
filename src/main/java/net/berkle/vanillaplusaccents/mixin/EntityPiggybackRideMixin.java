package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;

/**
 * Vanilla {@link Entity#startRiding} rejects non-serializable vehicles on the server.
 * {@link EntityType#PLAYER} is not saved, so player-on-player piggyback always fails without this.
 */
@Mixin(Entity.class)
public abstract class EntityPiggybackRideMixin {

	@Redirect(
		method = "startRiding(Lnet/minecraft/world/entity/Entity;ZZ)Z",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/EntityType;canSerialize()Z"
		)
	)
	private boolean vpa$allowPlayerPiggybackVehicle(EntityType<?> vehicleType) {
		Entity passenger = (Entity) (Object) this;
		if (vehicleType == EntityType.PLAYER && passenger instanceof Player) {
			return true;
		}
		return vehicleType.canSerialize();
	}
}
