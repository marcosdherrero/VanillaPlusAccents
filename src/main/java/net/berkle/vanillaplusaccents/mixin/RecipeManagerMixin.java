package net.berkle.vanillaplusaccents.mixin;

import java.util.ArrayList;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;

import net.berkle.vanillaplusaccents.woodcutting.WoodcuttingRecipes;

/** Injects convention-based woodcutting stonecutter recipes after datapack recipes load. */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {

	@Inject(method = "prepare", at = @At("RETURN"), cancellable = true)
	private void vpa$injectWoodcuttingRecipes(
		ResourceManager resourceManager,
		ProfilerFiller profiler,
		CallbackInfoReturnable<RecipeMap> cir
	) {
		RecipeMap loaded = cir.getReturnValue();
		List<RecipeHolder<?>> generated = WoodcuttingRecipes.generate();
		if (generated.isEmpty()) {
			return;
		}
		List<RecipeHolder<?>> merged = new ArrayList<>(loaded.values().size() + generated.size());
		merged.addAll(loaded.values());
		merged.addAll(generated);
		cir.setReturnValue(RecipeMap.create(merged));
	}
}
