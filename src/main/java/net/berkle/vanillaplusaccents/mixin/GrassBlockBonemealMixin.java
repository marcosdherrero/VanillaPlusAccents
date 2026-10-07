package net.berkle.vanillaplusaccents.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.berkle.vanillaplusaccents.flower.GrassBonemealFeatures;

/**
 * Vanilla grass bone-meal only plants flowers from {@code getBoneMealFeatures()}.
 * Datapacks that replace biome vegetation leave that list empty, so only grass grows.
 * After vanilla runs, plant single flowers from the biome's vanilla/Geophilic pool
 * — never VPA patches.
 */
@Mixin(GrassBlock.class)
public abstract class GrassBlockBonemealMixin {

	@Inject(method = "performBonemeal", at = @At("RETURN"))
	private void vpa$ensureVanillaFlowers(
		ServerLevel level,
		RandomSource random,
		BlockPos pos,
		BlockState state,
		CallbackInfo ci
	) {
		GrassBonemealFeatures.ensureVanillaFlowers(level, random, pos, state);
	}
}
