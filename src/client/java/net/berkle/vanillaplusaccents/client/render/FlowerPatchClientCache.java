package net.berkle.vanillaplusaccents.client.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Client cache of patch flower types for destroy particles after the block entity is cleared. */
public final class FlowerPatchClientCache {

	private static final Map<BlockPos, Identifier> FLOWERS = new ConcurrentHashMap<>();

	private FlowerPatchClientCache() {
	}

	public static void put(BlockPos pos, Identifier flowerId) {
		FLOWERS.put(pos.immutable(), flowerId);
	}

	public static Identifier remove(BlockPos pos) {
		return FLOWERS.remove(pos);
	}

	public static void clear() {
		FLOWERS.clear();
	}
}
