package net.berkle.vanillaplusaccents.woodcutting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;

/**
 * Builds stonecutter woodcutting recipes from item-id conventions so vanilla and most
 * modded woods work without per-mod datapacks:
 * {@code ns:foo_log} / {@code ns:foo_wood} / {@code ns:foo_stem} / {@code ns:bamboo_block}
 * paired with stripped / planks / stairs / slab / fence / fence gate / etc. in the same namespace.
 *
 * Plank-equivalents: 1 log/wood/stripped = 4 planks, 1 stick = 0.5 plank,
 * 1 slab = 0.5 plank, 1 stair = 1.5 planks, 1 fence = 5/3 planks,
 * 1 fence gate = 4, 1 trapdoor = 3, 1 door = 2, 1 sign = 13/6, 1 shelf = 4,
 * 1 pressure plate = 2.
 *
 * From 1 log/wood/stripped: vanilla yield + ~1 stick (0.5 plank), then
 * {@code Math.round(4.5 / cost)} except planks stay 4 (no free extra plank)
 * and stripped stays 1:1.
 *
 * <pre>
 * 1-log cutter yields (same family only):
 *   stripped / wood swap     1
 *   planks                   4
 *   slabs                    9
 *   stairs                   3
 *   sticks                   9
 *   fence                    3
 *   fence gate               1
 *   trapdoor                 2
 *   door                     2
 *   sign                     2
 *   shelf                    1
 *   pressure plate           2
 *
 * From 1 plank (near-vanilla; stonecutter is 1-in):
 *   slabs 2, stairs 1, sticks 2
 *
 * From 1 slab / 1 stair (integer, non-lossy only):
 *   1 slab  → 1 stick
 *   1 stair → 3 slabs, 3 sticks
 * 2-in conversions (2 slabs = 1 plank, 2 stairs = 3 planks) cannot be stonecutter recipes.
 * </pre>
 */
public final class WoodcuttingRecipes {

	private static final int LOG_PLANKS = 4;
	private static final int LOG_SLABS = 9;
	private static final int LOG_STAIRS = 3;
	private static final int LOG_STICKS = 9;
	private static final int LOG_FENCE = 3;
	private static final int LOG_FENCE_GATE = 1;
	private static final int LOG_TRAPDOOR = 2;
	private static final int LOG_DOOR = 2;
	private static final int LOG_SIGN = 2;
	private static final int LOG_SHELF = 1;
	private static final int LOG_PRESSURE_PLATE = 2;

	private static final int PLANK_SLABS = 2;
	private static final int PLANK_STAIRS = 1;
	private static final int PLANK_STICKS = 2;

	private static final int SLAB_STICKS = 1;
	private static final int STAIR_SLABS = 3;
	private static final int STAIR_STICKS = 3;

	private WoodcuttingRecipes() {
	}

	public static List<RecipeHolder<?>> generate() {
		List<RecipeHolder<?>> recipes = new ArrayList<>();
		Set<String> emitted = new HashSet<>();

		for (Item item : BuiltInRegistries.ITEM) {
			Identifier id = BuiltInRegistries.ITEM.getKey(item);
			if (id == null) {
				continue;
			}
			String path = id.getPath();
			String ns = id.getNamespace();

			if (path.endsWith("_log") && !path.startsWith("stripped_")) {
				addLogFamily(recipes, emitted, ns, path.substring(0, path.length() - "_log".length()), "log", "wood");
			} else if (path.endsWith("_stem") && !path.startsWith("stripped_")) {
				addLogFamily(recipes, emitted, ns, path.substring(0, path.length() - "_stem".length()), "stem", "hyphae");
			} else if (path.equals("bamboo_block")) {
				addBambooFamily(recipes, emitted, ns);
			} else if (path.endsWith("_planks")) {
				String wood = path.substring(0, path.length() - "_planks".length());
				addPlankProducts(recipes, emitted, ns, wood, item);
			} else if (path.equals("bamboo_mosaic")) {
				addMosaicProducts(recipes, emitted, ns, item);
			}
		}

		VanillaPlusAccentsMain.LOGGER.info(
			"[{}] Woodcutting: generated {} stonecutter recipes from item id conventions",
			VanillaPlusAccentsMain.MOD_ID,
			recipes.size()
		);
		return recipes;
	}

	private static void addLogFamily(
		List<RecipeHolder<?>> recipes,
		Set<String> emitted,
		String ns,
		String wood,
		String primarySuffix,
		String secondarySuffix
	) {
		Item primary = item(ns, wood + "_" + primarySuffix);
		Item strippedPrimary = item(ns, "stripped_" + wood + "_" + primarySuffix);
		Item secondary = item(ns, wood + "_" + secondarySuffix);
		Item strippedSecondary = item(ns, "stripped_" + wood + "_" + secondarySuffix);
		Item[] logLikes = { primary, strippedPrimary, secondary, strippedSecondary };

		if (primary != null && strippedPrimary != null) {
			add(recipes, emitted, ns, primary, strippedPrimary, 1);
		}
		if (secondary != null && strippedSecondary != null) {
			add(recipes, emitted, ns, secondary, strippedSecondary, 1);
		}

		addLogLikeProducts(recipes, emitted, ns, wood, logLikes);
		Item planks = item(ns, wood + "_planks");
		if (planks != null) {
			addPlankProducts(recipes, emitted, ns, wood, planks);
		}
	}

	private static void addBambooFamily(List<RecipeHolder<?>> recipes, Set<String> emitted, String ns) {
		Item block = item(ns, "bamboo_block");
		Item stripped = item(ns, "stripped_bamboo_block");
		Item[] logLikes = { block, stripped };
		if (block != null && stripped != null) {
			add(recipes, emitted, ns, block, stripped, 1);
		}
		addLogLikeProducts(recipes, emitted, ns, "bamboo", logLikes);

		Item planks = item(ns, "bamboo_planks");
		if (planks != null) {
			addPlankProducts(recipes, emitted, ns, "bamboo", planks);
		}

		Item mosaic = item(ns, "bamboo_mosaic");
		if (mosaic != null) {
			addFromAll(recipes, emitted, ns, logLikes, mosaic, LOG_PLANKS);
			addFromAll(recipes, emitted, ns, logLikes, item(ns, "bamboo_mosaic_slab"), LOG_SLABS);
			addFromAll(recipes, emitted, ns, logLikes, item(ns, "bamboo_mosaic_stairs"), LOG_STAIRS);
			addMosaicProducts(recipes, emitted, ns, mosaic);
		}
	}

	private static void addLogLikeProducts(
		List<RecipeHolder<?>> recipes,
		Set<String> emitted,
		String ns,
		String wood,
		Item[] logLikes
	) {
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_planks"), LOG_PLANKS);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_slab"), LOG_SLABS);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_stairs"), LOG_STAIRS);
		addFromAll(recipes, emitted, ns, logLikes, stick(), LOG_STICKS);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_fence"), LOG_FENCE);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_fence_gate"), LOG_FENCE_GATE);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_trapdoor"), LOG_TRAPDOOR);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_door"), LOG_DOOR);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_sign"), LOG_SIGN);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_shelf"), LOG_SHELF);
		addFromAll(recipes, emitted, ns, logLikes, item(ns, wood + "_pressure_plate"), LOG_PRESSURE_PLATE);
	}

	private static void addPlankProducts(
		List<RecipeHolder<?>> recipes,
		Set<String> emitted,
		String ns,
		String wood,
		Item planks
	) {
		Item stairs = item(ns, wood + "_stairs");
		Item slab = item(ns, wood + "_slab");
		if (stairs != null) {
			add(recipes, emitted, ns, planks, stairs, PLANK_STAIRS);
		}
		if (slab != null) {
			add(recipes, emitted, ns, planks, slab, PLANK_SLABS);
		}
		add(recipes, emitted, ns, planks, stick(), PLANK_STICKS);
		addSlabStairConversions(recipes, emitted, ns, slab, stairs, slab);
	}

	private static void addMosaicProducts(
		List<RecipeHolder<?>> recipes,
		Set<String> emitted,
		String ns,
		Item mosaic
	) {
		Item stairs = item(ns, "bamboo_mosaic_stairs");
		Item slab = item(ns, "bamboo_mosaic_slab");
		if (stairs != null) {
			add(recipes, emitted, ns, mosaic, stairs, PLANK_STAIRS);
		}
		if (slab != null) {
			add(recipes, emitted, ns, mosaic, slab, PLANK_SLABS);
		}
		add(recipes, emitted, ns, mosaic, stick(), PLANK_STICKS);
		addSlabStairConversions(recipes, emitted, ns, slab, stairs, slab);
	}

	private static void addSlabStairConversions(
		List<RecipeHolder<?>> recipes,
		Set<String> emitted,
		String ns,
		Item slab,
		Item stairs,
		Item slabOutput
	) {
		if (slab != null) {
			add(recipes, emitted, ns, slab, stick(), SLAB_STICKS);
		}
		if (stairs != null) {
			if (slabOutput != null) {
				add(recipes, emitted, ns, stairs, slabOutput, STAIR_SLABS);
			}
			add(recipes, emitted, ns, stairs, stick(), STAIR_STICKS);
		}
	}

	private static void addFromAll(
		List<RecipeHolder<?>> recipes,
		Set<String> emitted,
		String ns,
		Item[] inputs,
		Item output,
		int count
	) {
		if (output == null) {
			return;
		}
		for (Item src : inputs) {
			if (src != null) {
				add(recipes, emitted, ns, src, output, count);
			}
		}
	}

	private static void add(
		List<RecipeHolder<?>> recipes,
		Set<String> emitted,
		String sourceNamespace,
		Item input,
		Item output,
		int count
	) {
		if (input == null || output == null || input == output || count <= 0) {
			return;
		}
		Identifier inId = BuiltInRegistries.ITEM.getKey(input);
		Identifier outId = BuiltInRegistries.ITEM.getKey(output);
		if (inId == null || outId == null) {
			return;
		}
		String key = inId + "->" + outId + "x" + count;
		if (!emitted.add(key)) {
			return;
		}

		Identifier recipeId = Identifier.fromNamespaceAndPath(
			VanillaPlusAccentsMain.MOD_ID,
			"woodcutting/" + sanitize(sourceNamespace) + "/" + sanitize(inId.getPath())
				+ "_to_" + sanitize(outId.getPath()) + "_x" + count
		);
		ResourceKey<Recipe<?>> recipeKey = ResourceKey.create(Registries.RECIPE, recipeId);

		StonecutterRecipe recipe = new StonecutterRecipe(
			new Recipe.CommonInfo(false),
			Ingredient.of(input),
			new ItemStackTemplate(output, count)
		);
		recipes.add(new RecipeHolder<>(recipeKey, recipe));
	}

	private static String sanitize(String path) {
		return path.replace(':', '_').replace('/', '_');
	}

	private static @Nullable Item item(String namespace, String path) {
		Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
		return BuiltInRegistries.ITEM.getOptional(id).orElse(null);
	}

	private static @Nullable Item stick() {
		return item("minecraft", "stick");
	}
}
