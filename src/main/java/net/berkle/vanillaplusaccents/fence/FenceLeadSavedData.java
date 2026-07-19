package net.berkle.vanillaplusaccents.fence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;

/**
 * Persisted fence-to-fence links (overworld storage) plus ephemeral pending-anchor state.
 * Pending entity UUIDs are not saved — they are recreated while a player is mid-connect.
 */
public final class FenceLeadSavedData extends SavedData {

	public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "fence_leads");

	private static final Codec<FenceLeadLink> LINK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Identifier.CODEC.fieldOf("dimension").forGetter(FenceLeadLink::dimension),
		BlockPos.CODEC.fieldOf("from").forGetter(FenceLeadLink::from),
		BlockPos.CODEC.fieldOf("to").forGetter(FenceLeadLink::to)
	).apply(instance, FenceLeadLink::new));

	private static final Codec<FenceLeadSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		LINK_CODEC.listOf().optionalFieldOf("links", List.of()).forGetter(data -> data.links)
	).apply(instance, FenceLeadSavedData::fromCodec));

	public static final SavedDataType<FenceLeadSavedData> TYPE = new SavedDataType<>(
		DATA_ID,
		FenceLeadSavedData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private final List<FenceLeadLink> links = new ArrayList<>();
	private final Map<UUID, PendingLink> pending = new HashMap<>();
	/** Live pending rope entity id per player — not serialized. */
	private final Map<UUID, UUID> pendingEntities = new HashMap<>();

	private FenceLeadSavedData() {
	}

	private static FenceLeadSavedData fromCodec(List<FenceLeadLink> links) {
		FenceLeadSavedData data = new FenceLeadSavedData();
		data.links.addAll(links);
		return data;
	}

	public static FenceLeadSavedData get(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			overworld = level;
		}
		return overworld.getDataStorage().computeIfAbsent(TYPE);
	}

	public List<FenceLeadLink> allLinks() {
		return List.copyOf(links);
	}

	public List<FenceLeadLink> linksFor(Identifier dimension) {
		return links.stream().filter(link -> link.dimension().equals(dimension)).toList();
	}

	public void addLink(FenceLeadLink link) {
		if (link.from().equals(link.to())) {
			return;
		}
		links.removeIf(existing -> existing.dimension().equals(link.dimension())
			&& existing.involves(link.from()) && existing.involves(link.to()));
		links.add(link);
		setDirty();
	}

	public int removeLinksAt(Identifier dimension, BlockPos pos) {
		int removed = 0;
		for (Iterator<FenceLeadLink> iterator = links.iterator(); iterator.hasNext();) {
			FenceLeadLink link = iterator.next();
			if (link.dimension().equals(dimension) && link.involves(pos)) {
				iterator.remove();
				removed++;
			}
		}
		if (removed > 0) {
			setDirty();
		}
		return removed;
	}

	/** Drop links whose ends are no longer tagged fences. Skips unloaded chunks (no force-load). */
	public boolean purgeInvalid(ServerLevel level, Identifier dimension) {
		boolean changed = false;
		for (Iterator<FenceLeadLink> iterator = links.iterator(); iterator.hasNext();) {
			FenceLeadLink link = iterator.next();
			if (!link.dimension().equals(dimension)) {
				continue;
			}
			if (!isChunkLoaded(level, link.from()) || !isChunkLoaded(level, link.to())) {
				continue;
			}
			if (!level.getBlockState(link.from()).is(BlockTags.FENCES)
				|| !level.getBlockState(link.to()).is(BlockTags.FENCES)) {
				iterator.remove();
				changed = true;
			}
		}
		if (changed) {
			setDirty();
		}
		return changed;
	}

	private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
	}

	public PendingLink getPending(UUID playerId) {
		return pending.get(playerId);
	}

	public void setPending(UUID playerId, Identifier dimension, BlockPos pos) {
		pending.put(playerId, new PendingLink(dimension, pos));
	}

	public void clearPending(UUID playerId) {
		pending.remove(playerId);
	}

	/** Cancel any in-progress anchors on this post (e.g. fence broken). Returns count cleared. */
	public int clearPendingAt(Identifier dimension, BlockPos pos) {
		int cleared = 0;
		for (Iterator<Map.Entry<UUID, PendingLink>> iterator = pending.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<UUID, PendingLink> entry = iterator.next();
			PendingLink link = entry.getValue();
			if (link.dimension().equals(dimension) && link.pos().equals(pos)) {
				iterator.remove();
				pendingEntities.remove(entry.getKey());
				cleared++;
			}
		}
		if (cleared > 0) {
			setDirty();
		}
		return cleared;
	}

	public void setPendingEntity(UUID playerId, UUID entityId) {
		pendingEntities.put(playerId, entityId);
	}

	public UUID clearPendingEntity(UUID playerId) {
		return pendingEntities.remove(playerId);
	}

	/** Live pending anchors (not serialized). Used so endpoint checks keep knots alive. */
	public Iterable<Map.Entry<UUID, PendingLink>> pendingEntries() {
		return pending.entrySet();
	}

	public record PendingLink(Identifier dimension, BlockPos pos) {
	}
}
