package net.berkle.vanillaplusaccents.fence;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

/** Immutable fence-to-fence lead link in one dimension. */
public record FenceLeadLink(
	Identifier dimension,
	BlockPos from,
	BlockPos to
) {
	public boolean involves(BlockPos pos) {
		return from.equals(pos) || to.equals(pos);
	}
}
