package net.berkle.vanillaplusaccents.fence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityTypeTest;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;

/**
 * Keeps fence-lead markers and vanilla knots aligned with {@link FenceLeadSavedData}.
 */
public final class FenceLeadVisuals {

	private FenceLeadVisuals() {
	}

	public static void resync(ServerLevel level) {
		Identifier dimension = level.dimension().identifier();
		List<FenceLeadLink> links = FenceLeadSavedData.get(level).linksFor(dimension);

		List<FenceLeadEntity> existing = new ArrayList<>();
		level.getEntities(EntityTypeTest.forClass(FenceLeadEntity.class), e -> true, existing);

		Set<FenceLeadLink> matched = new HashSet<>();
		for (FenceLeadEntity entity : existing) {
			if (entity.isPending()) {
				continue;
			}
			FenceLeadLink match = null;
			for (FenceLeadLink link : links) {
				if (entity.matches(link)) {
					match = link;
					break;
				}
			}
			if (match == null) {
				BlockPos from = entity.getFrom();
				BlockPos to = entity.getTo().orElse(null);
				safeRemoveLeash(entity);
				entity.discard();
				discardKnotsIfUnused(level, from, to);
			} else if (!matched.add(match)) {
				safeRemoveLeash(entity);
				entity.discard();
			} else {
				ensureKnotsIfLoaded(level, match.from(), match.to());
			}
		}

		for (FenceLeadLink link : links) {
			ensureKnotsIfLoaded(level, link.from(), link.to());
			if (!isChunkLoaded(level, link.from()) || !isChunkLoaded(level, link.to())) {
				continue;
			}
			if (!matched.contains(link)) {
				spawnLink(level, link);
			}
		}

		discardUnusedKnots(level);
	}

	/** Spawn missing markers/knots for saved links that touch this chunk. */
	public static void ensureLinksInChunk(ServerLevel level, ChunkPos chunkPos) {
		Identifier dimension = level.dimension().identifier();
		for (FenceLeadLink link : FenceLeadSavedData.get(level).linksFor(dimension)) {
			boolean fromHere = inChunk(chunkPos, link.from());
			boolean toHere = inChunk(chunkPos, link.to());
			if (!fromHere && !toHere) {
				continue;
			}
			ensureKnotsIfLoaded(level, link.from(), link.to());
			if (!isChunkLoaded(level, link.from()) || !isChunkLoaded(level, link.to())) {
				continue;
			}
			if (!hasCompletedMarker(level, link)) {
				spawnLink(level, link);
			}
		}
		discardUnusedKnots(level);
	}

	/** Re-create knots for saved links whose chunks are already loaded. */
	public static void ensureAllKnots(ServerLevel level) {
		Identifier dimension = level.dimension().identifier();
		for (FenceLeadLink link : FenceLeadSavedData.get(level).linksFor(dimension)) {
			ensureKnotsIfLoaded(level, link.from(), link.to());
		}
	}

	private static void ensureKnotsIfLoaded(ServerLevel level, BlockPos a, BlockPos b) {
		if (isChunkLoaded(level, a)) {
			ensureKnot(level, a);
		}
		if (isChunkLoaded(level, b)) {
			ensureKnot(level, b);
		}
	}

	private static boolean inChunk(ChunkPos chunkPos, BlockPos pos) {
		return chunkPos.contains(pos);
	}

	private static boolean hasCompletedMarker(ServerLevel level, FenceLeadLink link) {
		List<FenceLeadEntity> existing = new ArrayList<>();
		level.getEntities(EntityTypeTest.forClass(FenceLeadEntity.class), entity -> entity.matches(link), existing);
		return !existing.isEmpty();
	}

	private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
	}

	public static void spawnPending(ServerLevel level, ServerPlayer player, BlockPos fence) {
		spawnPending(level, player, List.of(fence));
	}

	public static void spawnPending(ServerLevel level, ServerPlayer player, List<BlockPos> fences) {
		discardUnusedKnots(level);
		clearPendingFor(player, true);
		List<UUID> ids = new ArrayList<>();
		for (BlockPos fence : fences) {
			ensureKnot(level, fence);
			FenceLeadEntity entity = new FenceLeadEntity(level, fence, player.getUUID());
			level.addFreshEntity(entity);
			ids.add(entity.getUUID());
		}
		FenceLeadSavedData.get(level).setPendingEntities(player.getUUID(), ids);
	}

	public static void completePending(ServerPlayer player, BlockPos from, BlockPos to) {
		completePending(player, to, List.of(from));
	}

	/** Consume pending markers and place dest–each far end. Does not touch inventory. */
	public static void completePending(ServerPlayer player, BlockPos dest, List<BlockPos> farEnds) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		List<UUID> entityIds = data.clearPendingEntities(player.getUUID());
		List<FenceLeadEntity> leftovers = new ArrayList<>();
		for (UUID entityId : entityIds) {
			Entity entity = level.getEntity(entityId);
			if (entity instanceof FenceLeadEntity lead) {
				leftovers.add(lead);
			}
		}

		for (BlockPos far : farEnds) {
			ensureKnots(level, dest, far);
			FenceLeadEntity reused = null;
			for (int i = 0; i < leftovers.size(); i++) {
				if (leftovers.get(i).getFrom().equals(far)) {
					reused = leftovers.remove(i);
					break;
				}
			}
			if (reused != null) {
				safeRemoveLeash(reused);
				reused.setCompleted(far, dest);
				reused.forceApplyLeash(level);
				VanillaPlusAccentsMain.LOGGER.debug(
					"[fence-lead] connected {} -> {} entity={}",
					far.toShortString(),
					dest.toShortString(),
					reused.getId()
				);
			} else {
				VanillaPlusAccentsMain.LOGGER.warn(
					"[fence-lead] pending marker missing; spawning fresh link {} -> {}",
					far.toShortString(),
					dest.toShortString()
				);
				spawnLink(level, new FenceLeadLink(level.dimension().identifier(), dest, far));
			}
		}

		for (FenceLeadEntity extra : leftovers) {
			BlockPos from = extra.getFrom();
			safeRemoveLeash(extra);
			extra.discard();
			discardKnotsIfUnused(level, from, null);
		}
	}

	public static void clearPendingFor(ServerPlayer player) {
		clearPendingFor(player, true);
	}

	public static void clearPendingFor(ServerPlayer player, boolean dropUnusedKnot) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		discardPendingMarkers(level, FenceLeadSavedData.get(level).clearPendingEntities(player.getUUID()), null, dropUnusedKnot);
	}

	/** Discard sibling pending markers when one is released (this entity stays for the caller). */
	static void discardPendingExcept(ServerLevel level, UUID ownerId, UUID keepId) {
		discardPendingMarkers(level, FenceLeadSavedData.get(level).clearPendingEntities(ownerId), keepId, true);
	}

	private static void discardPendingMarkers(
		ServerLevel level,
		List<UUID> entityIds,
		UUID keepId,
		boolean dropUnusedKnot
	) {
		for (UUID entityId : entityIds) {
			if (keepId != null && keepId.equals(entityId)) {
				continue;
			}
			Entity entity = level.getEntity(entityId);
			if (entity instanceof FenceLeadEntity lead) {
				BlockPos from = lead.getFrom();
				safeRemoveLeash(lead);
				entity.discard();
				if (dropUnusedKnot) {
					discardKnotsIfUnused(level, from, null);
				}
			} else if (entity != null) {
				entity.discard();
			}
		}
	}

	public static void spawnLink(ServerLevel level, FenceLeadLink link) {
		ensureKnots(level, link.from(), link.to());
		FenceLeadEntity primary = new FenceLeadEntity(level, link.from(), link.to());
		level.addFreshEntity(primary);
		primary.forceApplyLeash(level);
	}

	/** Discard markers on this post. Returns how many were removed (for lead refunds). */
	public static int removeLinksAt(ServerLevel level, BlockPos pos) {
		List<FenceLeadEntity> existing = new ArrayList<>();
		level.getEntities(
			EntityTypeTest.forClass(FenceLeadEntity.class),
			e -> e.getFrom().equals(pos) || e.getTo().filter(pos::equals).isPresent(),
			existing
		);
		int removed = 0;
		for (FenceLeadEntity entity : existing) {
			BlockPos from = entity.getFrom();
			BlockPos to = entity.getTo().orElse(null);
			safeRemoveLeash(entity);
			entity.discard();
			discardKnotsIfUnused(level, from, to);
			removed++;
		}
		return removed;
	}

	private static void ensureKnots(ServerLevel level, BlockPos a, BlockPos b) {
		ensureKnot(level, a);
		ensureKnot(level, b);
	}

	private static void ensureKnot(ServerLevel level, BlockPos pos) {
		LeashFenceKnotEntity.getOrCreateKnot(level, pos);
	}

	static void discardKnotsIfUnused(ServerLevel level, BlockPos from, BlockPos to) {
		if (!isEndpointUsed(level, from)) {
			discardKnot(level, from);
		}
		if (to != null && !isEndpointUsed(level, to)) {
			discardKnot(level, to);
		}
	}

	/**
	 * Drop leftover vanilla knots that are not a saved/pending fence post and have
	 * no living animal tied to them (reload leftovers / unbreakable fake knots).
	 */
	public static void discardUnusedKnots(ServerLevel level) {
		List<LeashFenceKnotEntity> knots = new ArrayList<>();
		level.getEntities(EntityTypeTest.forClass(LeashFenceKnotEntity.class), e -> true, knots);
		for (LeashFenceKnotEntity knot : knots) {
			if (shouldKeepKnot(level, knot)) {
				continue;
			}
			knot.discard();
		}
	}

	private static boolean shouldKeepKnot(ServerLevel level, LeashFenceKnotEntity knot) {
		if (isEndpointUsed(level, knot.getPos())) {
			return true;
		}
		for (Leashable leashee : Leashable.leashableLeashedTo(knot)) {
			if (leashee instanceof FenceLeadEntity) {
				continue;
			}
			if (leashee instanceof Entity entity && entity.isAlive()) {
				return true;
			}
		}
		return false;
	}

	private static void discardKnot(ServerLevel level, BlockPos pos) {
		LeashFenceKnotEntity.getKnot(level, pos).ifPresent(knot -> {
			if (!shouldKeepKnot(level, knot)) {
				knot.discard();
			}
		});
	}

	private static boolean isEndpointUsed(ServerLevel level, BlockPos pos) {
		Identifier dimension = level.dimension().identifier();
		for (FenceLeadLink link : FenceLeadSavedData.get(level).linksFor(dimension)) {
			if (link.involves(pos)) {
				return true;
			}
		}
		for (var entry : FenceLeadSavedData.get(level).pendingEntries()) {
			if (entry.getValue().dimension().equals(dimension) && entry.getValue().hasFarEnd(pos)) {
				return true;
			}
		}
		return false;
	}

	private static void safeRemoveLeash(FenceLeadEntity entity) {
		if (entity.isLeashed()) {
			entity.removeLeash();
		}
	}
}
