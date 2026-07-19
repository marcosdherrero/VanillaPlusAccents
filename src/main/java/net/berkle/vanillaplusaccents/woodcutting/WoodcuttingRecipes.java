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
 * paired with stripped / planks / stairs / slab in the same namespace.
 */
public final class WoodcuttingRecipes {

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
		Item planks = item(ns, wood + "_planks");

		if (primary != null && strippedPrimary != null) {
			add(recipes, emitted, ns, primary, strippedPrimary, 1);
		}
		if (secondary != null && strippedSecondary != null) {
			add(recipes, emitted, ns, secondary, strippedSecondary, 1);
		}

		if (planks != null) {
			for (Item src : new Item[] { primary, strippedPrimary, secondary, strippedSecondary }) {
				if (src != null) {
					add(recipes, emitted, ns, src, planks, 4);
				}
			}
			addPlankProducts(recipes, emitted, ns, wood, planks);
		}
	}

	private static void addBambooFamily(List<RecipeHolder<?>> recipes, Set<String> emitted, String ns) {
		Item block = item(ns, "bamboo_block");
		Item stripped = item(ns, "stripped_bamboo_block");
		Item planks = item(ns, "bamboo_planks");
		if (block != null && stripped != null) {
			add(recipes, emitted, ns, block, stripped, 1);
		}
		if (planks != null) {
			if (block != null) {
				add(recipes, emitted, ns, block, planks, 4);
			}
			if (stripped != null) {
				add(recipes, emitted, ns, stripped, planks, 4);
			}
			addPlankProducts(recipes, emitted, ns, "bamboo", planks);
		}
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
			add(recipes, emitted, ns, planks, stairs, 1);
		}
		if (slab != null) {
			add(recipes, emitted, ns, planks, slab, 2);
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
}
