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
import net.minecraft.world.level.entity.EntityTypeTest;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;

/**
 * Keeps fence-lead markers and knots aligned with {@link FenceLeadSavedData}.
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
			} else {
				matched.add(match);
				ensureKnotsIfLoaded(level, match.from(), match.to());
			}
		}

		for (FenceLeadLink link : links) {
			if (!isChunkLoaded(level, link.from()) || !isChunkLoaded(level, link.to())) {
				continue;
			}
			if (!matched.contains(link)) {
				spawnLink(level, link);
			} else {
				ensureKnots(level, link.from(), link.to());
			}
		}
	}

	/** Re-create knots for saved links whose chunks are already loaded. */
	public static void ensureAllKnots(ServerLevel level) {
		Identifier dimension = level.dimension().identifier();
		for (FenceLeadLink link : FenceLeadSavedData.get(level).linksFor(dimension)) {
			ensureKnotsIfLoaded(level, link.from(), link.to());
		}
	}

	private static void ensureKnotsIfLoaded(ServerLevel level, BlockPos a, BlockPos b) {
		if (isChunkLoaded(level, a) && isChunkLoaded(level, b)) {
			ensureKnots(level, a, b);
		}
	}

	private static boolean isChunkLoaded(ServerLevel level, BlockPos pos) {
		return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
	}

	public static FenceLeadEntity spawnPending(ServerLevel level, ServerPlayer player, BlockPos fence) {
		clearPendingFor(player, true);
		ensureKnot(level, fence);
		FenceLeadEntity entity = new FenceLeadEntity(level, fence, player.getUUID());
		level.addFreshEntity(entity);
		FenceLeadSavedData.get(level).setPendingEntity(player.getUUID(), entity.getUUID());
		return entity;
	}

	public static void completePending(ServerPlayer player, BlockPos from, BlockPos to) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		UUID entityId = data.clearPendingEntity(player.getUUID());
		ensureKnots(level, from, to);

		if (entityId != null) {
			Entity entity = level.getEntity(entityId);
			if (entity instanceof FenceLeadEntity lead) {
				safeRemoveLeash(lead);
				lead.setCompleted(from, to);
				lead.forceApplyLeash(level);
				VanillaPlusAccentsMain.LOGGER.debug(
					"[fence-lead] connected {} -> {} entity={}",
					from.toShortString(),
					to.toShortString(),
					lead.getId()
				);
				return;
			}
		}

		VanillaPlusAccentsMain.LOGGER.warn(
			"[fence-lead] pending marker missing; spawning fresh link {} -> {}",
			from.toShortString(),
			to.toShortString()
		);
		spawnLink(level, new FenceLeadLink(level.dimension().identifier(), from, to));
	}

	public static void clearPendingFor(ServerPlayer player) {
		clearPendingFor(player, true);
	}

	public static void clearPendingFor(ServerPlayer player, boolean dropUnusedKnot) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}
		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		UUID entityId = data.clearPendingEntity(player.getUUID());
		if (entityId == null) {
			return;
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

	public static void spawnLink(ServerLevel level, FenceLeadLink link) {
		ensureKnots(level, link.from(), link.to());
		FenceLeadEntity primary = new FenceLeadEntity(level, link.from(), link.to());
		level.addFreshEntity(primary);
		primary.forceApplyLeash(level);
	}

	public static void removeLinksAt(ServerLevel level, BlockPos pos) {
		List<FenceLeadEntity> existing = new ArrayList<>();
		level.getEntities(
			EntityTypeTest.forClass(FenceLeadEntity.class),
			e -> e.getFrom().equals(pos) || e.getTo().filter(pos::equals).isPresent(),
			existing
		);
		for (FenceLeadEntity entity : existing) {
			BlockPos from = entity.getFrom();
			BlockPos to = entity.getTo().orElse(null);
			safeRemoveLeash(entity);
			entity.discard();
			discardKnotsIfUnused(level, from, to);
		}
	}

	private static void ensureKnots(ServerLevel level, BlockPos a, BlockPos b) {
		ensureKnot(level, a);
		ensureKnot(level, b);
	}

	private static void ensureKnot(ServerLevel level, BlockPos pos) {
		LeashFenceKnotEntity.getOrCreateKnot(level, pos);
	}

	private static void discardKnotsIfUnused(ServerLevel level, BlockPos from, BlockPos to) {
		if (!isEndpointUsed(level, from)) {
			discardKnot(level, from);
		}
		if (to != null && !isEndpointUsed(level, to)) {
			discardKnot(level, to);
		}
	}

	private static void discardKnot(ServerLevel level, BlockPos pos) {
		LeashFenceKnotEntity.getKnot(level, pos).ifPresent(knot -> {
			if (Leashable.leashableLeashedTo(knot).isEmpty()) {
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
			if (entry.getValue().dimension().equals(dimension) && entry.getValue().pos().equals(pos)) {
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
