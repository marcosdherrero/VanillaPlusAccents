package net.berkle.vanillaplusaccents.flower;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.features.VegetationFeatures;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FeatureTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.SimpleBlockFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SimpleBlockConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Vanilla {@code GrassBlock.performBonemeal} plants flowers only from
 * {@code BiomeGenerationSettings.getBoneMealFeatures()} (configured features tagged
 * {@code minecraft:can_spawn_from_bone_meal}). Worldgen packs such as Geophilic replace
 * biome vegetation with untagged features, so that list is empty and the 1/8 flower
 * branch skips — leaving only short/tall grass.
 *
 * <p>After vanilla grass placement, recover the biome's real flower configured features
 * (tagged ∩ placed, then flower-like vegetation in the biome, then vanilla feature keys
 * for that biome id) and place single vanilla plants. Never VPA patches.
 */
public final class GrassBonemealFeatures {

	private static final int SCAN_RADIUS = 4;
	private static final int PLACE_ATTEMPTS = 128;
	private static final int PROVIDER_PROBE_ATTEMPTS = 16;

	private GrassBonemealFeatures() {
	}

	public static void ensureVanillaFlowers(ServerLevel level, RandomSource random, BlockPos grassPos, BlockState grassState) {
		BlockPos origin = grassPos.above();
		List<ConfiguredFeature<?, ?>> boneMeal = List.copyOf(
			level.getBiome(grassPos).value().getGenerationSettings().getBoneMealFeatures()
		);
		if (!boneMeal.isEmpty() && hasBonemealFlowerNearby(level, origin)) {
			return;
		}

		List<ConfiguredFeature<?, ?>> pool = boneMeal.isEmpty()
			? recoverFlowerFeatures(level, grassPos)
			: boneMeal;
		if (pool.isEmpty()) {
			return;
		}

		int want = 3 + random.nextInt(4);
		int placed = placeAround(level, random, origin, grassState.getBlock(), pool, want, false);
		if (placed == 0) {
			placeAround(level, random, origin, grassState.getBlock(), pool, want, true);
		}
	}

	private static List<ConfiguredFeature<?, ?>> recoverFlowerFeatures(ServerLevel level, BlockPos pos) {
		Holder<Biome> biome = level.getBiome(pos);
		List<ConfiguredFeature<?, ?>> tagged = new ArrayList<>();
		List<ConfiguredFeature<?, ?>> smallLike = new ArrayList<>();
		List<ConfiguredFeature<?, ?>> tallLike = new ArrayList<>();
		Set<ConfiguredFeature<?, ?>> seen = new LinkedHashSet<>();

		for (HolderSet<PlacedFeature> step : biome.value().getGenerationSettings().features()) {
			for (Holder<PlacedFeature> placed : step) {
				placed.value().getFeatures().forEach(holder -> {
					ConfiguredFeature<?, ?> feature = holder.value();
					if (!seen.add(feature)) {
						return;
					}
					if (holder.is(FeatureTags.CAN_SPAWN_FROM_BONE_MEAL)) {
						tagged.add(feature);
						return;
					}
					int kind = classifyFlowerLeaf(level, pos, holder);
					if (kind == 1) {
						smallLike.add(feature);
					} else if (kind == 2) {
						tallLike.add(feature);
					}
				});
			}
		}

		if (!tagged.isEmpty()) {
			return tagged;
		}
		if (!smallLike.isEmpty()) {
			return smallLike;
		}
		if (!tallLike.isEmpty()) {
			return tallLike;
		}
		return vanillaConfiguredForBiome(level, biome);
	}

	private static List<ConfiguredFeature<?, ?>> vanillaConfiguredForBiome(ServerLevel level, Holder<Biome> biome) {
		String path = biome.unwrapKey().map(key -> key.identifier().getPath()).orElse("");
		Registry<ConfiguredFeature<?, ?>> registry = level.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);
		List<ConfiguredFeature<?, ?>> out = new ArrayList<>();
		for (ResourceKey<ConfiguredFeature<?, ?>> key : vanillaFlowerKeys(path)) {
			registry.get(key).map(Holder::value).ifPresent(out::add);
		}
		return out;
	}

	/**
	 * Official 26.x biome → bone-meal flower configured features, from vanilla biome /
	 * {@code VegetationFeatures} data (not a global poppy/dandelion table).
	 */
	private static List<ResourceKey<ConfiguredFeature<?, ?>>> vanillaFlowerKeys(String biomePath) {
		return switch (biomePath) {
			case "plains", "sunflower_plains", "dripstone_caves", "deep_dark" ->
				List.of(VegetationFeatures.FLOWER_PLAIN);
			case "flower_forest" -> List.of(VegetationFeatures.FLOWER_FLOWER_FOREST);
			case "swamp" -> List.of(VegetationFeatures.FLOWER_SWAMP);
			case "meadow" -> List.of(VegetationFeatures.FLOWER_MEADOW, VegetationFeatures.WILDFLOWER);
			case "cherry_grove" -> List.of(VegetationFeatures.FLOWER_CHERRY);
			case "pale_garden" -> List.of(VegetationFeatures.FLOWER_PALE_GARDEN);
			case "birch_forest", "old_growth_birch_forest" ->
				List.of(VegetationFeatures.FLOWER_DEFAULT, VegetationFeatures.WILDFLOWER);
			case "bamboo_jungle", "jungle", "sparse_jungle", "savanna", "savanna_plateau",
				"beach", "desert", "forest", "dark_forest", "taiga",
				"old_growth_pine_taiga", "old_growth_spruce_taiga",
				"river", "frozen_river", "ocean", "cold_ocean", "deep_ocean", "deep_cold_ocean",
				"deep_frozen_ocean", "deep_lukewarm_ocean", "frozen_ocean", "lukewarm_ocean", "warm_ocean",
				"snowy_beach", "snowy_plains", "snowy_taiga", "ice_spikes", "stony_shore",
				"windswept_forest", "windswept_gravelly_hills", "windswept_hills", "windswept_savanna" ->
				List.of(VegetationFeatures.FLOWER_DEFAULT);
			default -> List.of();
		};
	}

	/** 0 = not a flower leaf, 1 = small bone-meal flower, 2 = tall flower only. */
	private static int classifyFlowerLeaf(
		ServerLevel level,
		BlockPos pos,
		Holder<ConfiguredFeature<?, ?>> holder
	) {
		ConfiguredFeature<?, ?> configured = holder.value();
		if (!(configured.feature() instanceof SimpleBlockFeature)
			|| !(configured.config() instanceof SimpleBlockConfiguration config)) {
			return 0;
		}
		boolean small = false;
		boolean tall = false;
		RandomSource probe = RandomSource.create(pos.asLong());
		for (int i = 0; i < PROVIDER_PROBE_ATTEMPTS; i++) {
			BlockState state = config.toPlace().getOptionalState(level, probe, pos);
			if (state == null) {
				continue;
			}
			if (isSmallBonemealFlowerState(state)) {
				small = true;
				break;
			}
			if (isBonemealFlowerState(state)) {
				tall = true;
			}
		}
		if (small) {
			return 1;
		}
		if (tall) {
			return 2;
		}
		return holder.unwrapKey().map(key -> looksLikeFlowerFeaturePath(key.identifier().getPath()) ? 1 : 0).orElse(0);
	}

	private static boolean looksLikeFlowerFeaturePath(String path) {
		int slash = path.lastIndexOf('/');
		String last = slash >= 0 ? path.substring(slash + 1) : path;
		return last.startsWith("flower_")
			|| last.startsWith("wildflower")
			|| last.equals("wildflower")
			|| last.endsWith("_flower")
			|| last.endsWith("_flowers")
			|| last.contains("sunflower");
	}

	private static boolean hasBonemealFlowerNearby(ServerLevel level, BlockPos center) {
		for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
			for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
				for (int y = -1; y <= 1; y++) {
					if (isBonemealFlowerState(level.getBlockState(center.offset(x, y, z)))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static boolean isSmallBonemealFlowerState(BlockState state) {
		return state.is(BlockTags.SMALL_FLOWERS)
			|| state.is(Blocks.PINK_PETALS)
			|| state.is(Blocks.WILDFLOWERS);
	}

	private static boolean isBonemealFlowerState(BlockState state) {
		if (isSmallBonemealFlowerState(state)) {
			return true;
		}
		if (!state.is(BlockTags.FLOWERS)) {
			return false;
		}
		Block block = state.getBlock();
		return !(block instanceof LeavesBlock)
			&& block != Blocks.FLOWERING_AZALEA
			&& block != Blocks.MANGROVE_PROPAGULE
			&& block != Blocks.CHORUS_FLOWER
			&& block != Blocks.SPORE_BLOSSOM
			&& block != Blocks.CACTUS_FLOWER;
	}

	private static int placeAround(
		ServerLevel level,
		RandomSource random,
		BlockPos origin,
		Block grass,
		List<ConfiguredFeature<?, ?>> pool,
		int want,
		boolean replaceShortGrass
	) {
		ChunkGenerator generator = level.getChunkSource().getGenerator();
		int placed = 0;
		for (int i = 0; i < PLACE_ATTEMPTS && placed < want; i++) {
			BlockPos test = spreadFrom(origin, random, i);
			if (tryPlaceAt(level, generator, random, test, grass, pool, replaceShortGrass)) {
				placed++;
			}
		}
		return placed;
	}

	/** Same random walk as vanilla {@code GrassBlock.performBonemeal}. */
	private static BlockPos spreadFrom(BlockPos origin, RandomSource random, int iteration) {
		BlockPos test = origin;
		for (int j = 0; j < iteration / 16; j++) {
			test = test.offset(
				random.nextInt(3) - 1,
				(random.nextInt(3) - 1) * random.nextInt(3) / 2,
				random.nextInt(3) - 1
			);
		}
		return test;
	}

	private static boolean tryPlaceAt(
		ServerLevel level,
		ChunkGenerator generator,
		RandomSource random,
		BlockPos pos,
		Block grass,
		List<ConfiguredFeature<?, ?>> pool,
		boolean replaceShortGrass
	) {
		if (level.isOutsideBuildHeight(pos)) {
			return false;
		}
		BlockState current = level.getBlockState(pos);
		if (replaceShortGrass) {
			if (!current.is(Blocks.SHORT_GRASS)) {
				return false;
			}
		} else if (!current.isAir()) {
			return false;
		}
		if (!level.getBlockState(pos.below()).is(grass)) {
			return false;
		}

		ConfiguredFeature<?, ?> feature = Util.getRandom(pool, random);
		if (feature.place(level, generator, random, pos)) {
			return true;
		}
		return placeFromStateProvider(level, random, pos, feature);
	}

	private static boolean placeFromStateProvider(
		ServerLevel level,
		RandomSource random,
		BlockPos pos,
		ConfiguredFeature<?, ?> feature
	) {
		if (!(feature.config() instanceof SimpleBlockConfiguration config)) {
			return false;
		}
		BlockState flower = config.toPlace().getOptionalState(level, random, pos);
		if (flower == null || !isBonemealFlowerState(flower) || !flower.canSurvive(level, pos)) {
			return false;
		}
		if (flower.getBlock() instanceof DoublePlantBlock) {
			if (!level.isEmptyBlock(pos.above())) {
				return false;
			}
			DoublePlantBlock.placeAt(level, flower, pos, Block.UPDATE_ALL);
			return true;
		}
		return level.setBlock(pos, flower, Block.UPDATE_ALL);
	}
}
