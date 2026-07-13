package net.berkle.vanillaplusaccents.flower;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.TriState;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Shared rules for which blocks can form flower patches. */
public final class FlowerPatchSupport {

	private FlowerPatchSupport() {
	}

	public static boolean canStackInPatch(Block block) {
		BlockState state = block.defaultBlockState();
		return state.is(BlockTags.SMALL_FLOWERS) || isMushroom(block);
	}

	public static boolean isMushroom(Block block) {
		return block == Blocks.RED_MUSHROOM || block == Blocks.BROWN_MUSHROOM;
	}

	public static boolean isEyeblossom(Block block) {
		return block == Blocks.OPEN_EYEBLOSSOM || block == Blocks.CLOSED_EYEBLOSSOM;
	}

	/** Open and closed eyeblossoms stack into the same patch. */
	public static boolean samePatchFlower(Identifier storedId, Identifier heldId) {
		if (storedId.equals(heldId)) {
			return true;
		}
		Block stored = BuiltInRegistries.BLOCK.getValue(storedId);
		Block held = BuiltInRegistries.BLOCK.getValue(heldId);
		return isEyeblossom(stored) && isEyeblossom(held);
	}

	/**
	 * Resolve the block state that should be drawn for a stored flower id.
	 * Eyeblossoms follow {@link EnvironmentAttributes#EYEBLOSSOM_OPEN} like singles.
	 */
	public static BlockState resolveDisplayState(Level level, BlockPos pos, Identifier flowerId) {
		Block flower = BuiltInRegistries.BLOCK.getValue(flowerId);
		if (flower == null || flower == Blocks.AIR) {
			return Blocks.AIR.defaultBlockState();
		}
		if (!isEyeblossom(flower)) {
			return flower.defaultBlockState();
		}

		boolean storedOpen = flower == Blocks.OPEN_EYEBLOSSOM;
		TriState attribute = level.environmentAttributes().getValue(EnvironmentAttributes.EYEBLOSSOM_OPEN, pos);
		boolean wantOpen = attribute.toBoolean(storedOpen);
		return (wantOpen ? Blocks.OPEN_EYEBLOSSOM : Blocks.CLOSED_EYEBLOSSOM).defaultBlockState();
	}

	/** Identifier for the eyeblossom variant matching the current open/closed environment state. */
	public static Identifier resolveEyeblossomId(Level level, BlockPos pos, Identifier storedId) {
		BlockState display = resolveDisplayState(level, pos, storedId);
		return BuiltInRegistries.BLOCK.getKey(display.getBlock());
	}
}
