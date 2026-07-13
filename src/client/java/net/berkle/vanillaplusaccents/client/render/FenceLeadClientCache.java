package net.berkle.vanillaplusaccents.client.render;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;

import net.berkle.vanillaplusaccents.fence.FenceLeadLink;

/** Client-side cache of fence lead links / pending anchors for world rendering. */
public final class FenceLeadClientCache {

	public record PendingAnchor(UUID player, Identifier dimension, BlockPos pos) {
	}

	private static List<FenceLeadLink> links = List.of();
	private static PendingAnchor pending;

	private FenceLeadClientCache() {
	}

	public static void replaceAll(List<FenceLeadLink> next, PendingAnchor nextPending) {
		links = List.copyOf(next);
		pending = nextPending;
	}

	public static List<FenceLeadLink> allLinks() {
		return links;
	}

	public static List<FenceLeadLink> linksFor(Identifier dimension) {
		return links.stream().filter(link -> link.dimension().equals(dimension)).toList();
	}

	public static PendingAnchor pending() {
		return pending;
	}

	public static void clear() {
		links = List.of();
		pending = null;
	}
}
