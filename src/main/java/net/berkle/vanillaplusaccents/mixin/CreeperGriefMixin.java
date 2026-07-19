package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;

import net.berkle.vanillaplusaccents.data.AccentSettingsSavedData;

@Mixin(Creeper.class)
public abstract class CreeperGriefMixin {

	@ModifyArg(
		method = "explodeCreeper",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"
		),
		index = 5
	)
	private Level.ExplosionInteraction vpa$creeperExplosionInteraction(Level.ExplosionInteraction interaction) {
		Creeper self = (Creeper) (Object) this;
		if (!(self.level() instanceof ServerLevel level)) {
			return interaction;
		}
		if (!AccentSettingsSavedData.get(level).isCreeperGrief()) {
			return Level.ExplosionInteraction.NONE;
		}
		return interaction;
	}
}
