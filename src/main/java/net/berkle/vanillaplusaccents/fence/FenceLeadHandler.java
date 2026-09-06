package net.berkle.vanillaplusaccents.fence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;

import net.berkle.vanillaplusaccents.network.VpaNetworking;

/**
 * Fence-to-fence leads. A fence may have many links.
 * <ul>
 *   <li>Leading an animal + right-click fence — hitch that animal (vanilla). Extra leads in hand do not start a rope.</li>
 *   <li>Lead on a fence while not leading — consume one lead, start a pending rope to the player</li>
 *   <li>Any fence while pending (including the grabbed hub) — place every remaining span ≤16 from dest</li>
 *   <li>A dest with no in-range spans — refund leftovers (too far). Origin-click is not cancel</li>
 *   <li>Lead on a fence again — only way to start another rope</li>
 *   <li>Empty hand on a fence with VPA links — regrab those already-paid ropes (no inventory refund)</li>
 *   <li>Shears on a fence or knot — drop every fence-to-fence lead on that post (animals use vanilla shear)</li>
 *   <li>Breaking a linked fence — removes its connections; leads always drop as items at the broken post</li>
 * </ul>
 */
public final class FenceLeadHandler {

	public static final int MAX_RANGE = 16;
	private static final int CONNECT_SUPPRESS_TICKS = 2;
	private static final Map<UUID, Long> connectSuppressUntil = new HashMap<>();

	private FenceLeadHandler() {
	}

	public static InteractionResult onUseKnot(
		Player player,
		Level level,
		InteractionHand hand,
		Entity entity,
		EntityHitResult hitResult
	) {
		if (!(entity instanceof LeashFenceKnotEntity knot)) {
			return InteractionResult.PASS;
		}
		BlockPos pos = knot.getPos();
		ItemStack held = player.getItemInHand(hand);
		if (isPlacingBlock(held)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			if (held.is(Items.SHEARS) && (hasLinksAt(player, level, pos) || knotPresent(level, pos))) {
				return InteractionResult.SUCCESS;
			}
			if (isLeadingAnimal(player)) {
				return InteractionResult.PASS;
			}
			if (hasPendingRope(player, level) || held.is(Items.LEAD) || (held.isEmpty() && hasLinksAt(player, level, pos))) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (held.is(Items.SHEARS)) {
			return tryShear(serverPlayer, serverLevel, hand, pos, held);
		}

		if (isLeadingAnimal(serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (hasPendingRope(serverPlayer, serverLevel)) {
			return finishPending(serverPlayer, serverLevel, hand, pos, held);
		}

		if (isConnectSuppressed(serverPlayer, serverLevel)) {
			return InteractionResult.SUCCESS_SERVER;
		}

		if (held.is(Items.LEAD)) {
			return startPending(serverPlayer, serverLevel, hand, pos, held);
		}

		if (held.isEmpty()) {
			return tryRegrab(serverPlayer, serverLevel, pos);
		}

		return InteractionResult.PASS;
	}

	public static InteractionResult onUseBlock(
		Player player,
		Level level,
		InteractionHand hand,
		BlockHitResult hitResult
	) {
		BlockPos pos = hitResult.getBlockPos();
		if (!level.getBlockState(pos).is(BlockTags.FENCES)) {
			return InteractionResult.PASS;
		}

		ItemStack held = player.getItemInHand(hand);
		if (isPlacingBlock(held)) {
			return InteractionResult.PASS;
		}

		if (level.isClientSide()) {
			if (held.is(Items.SHEARS) && (hasLinksAt(player, level, pos) || knotPresent(level, pos))) {
				return InteractionResult.SUCCESS;
			}
			if (isLeadingAnimal(player)) {
				return InteractionResult.PASS;
			}
			if (hasPendingRope(player, level) || held.is(Items.LEAD) || (held.isEmpty() && hasLinksAt(player, level, pos))) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (held.is(Items.SHEARS)) {
			return tryShear(serverPlayer, serverLevel, hand, pos, held);
		}

		if (isLeadingAnimal(serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (hasPendingRope(serverPlayer, serverLevel)) {
			return finishPending(serverPlayer, serverLevel, hand, pos, held);
		}

		if (isConnectSuppressed(serverPlayer, serverLevel)) {
			return InteractionResult.SUCCESS_SERVER;
		}

		if (held.is(Items.LEAD)) {
			return startPending(serverPlayer, serverLevel, hand, pos, held);
		}

		if (held.isEmpty()) {
			return tryRegrab(serverPlayer, serverLevel, pos);
		}

		return InteractionResult.PASS;
	}

	/** Fence / any block item must reach vanilla place (and BLOCK_WOOD_PLACE). */
	private static boolean isPlacingBlock(ItemStack held) {
		return held.getItem() instanceof BlockItem;
	}

	private static boolean hasPendingRope(Player player, Level level) {
		if (level instanceof ServerLevel serverLevel) {
			FenceLeadSavedData.PendingLink pending = FenceLeadSavedData.get(serverLevel).getPending(player.getUUID());
			return pending != null && pending.dimension().equals(level.dimension().identifier());
		}
		return !level.getEntitiesOfClass(
			FenceLeadEntity.class,
			player.getBoundingBox().inflate(96.0),
			entity -> entity.isPending()
				&& entity.getOwnerUuid().filter(id -> id.equals(player.getUUID())).isPresent()
		).isEmpty();
	}

	private static boolean hasLinksAt(Player player, Level level, BlockPos pos) {
		if (level instanceof ServerLevel serverLevel) {
			return FenceLeadSavedData.get(serverLevel).linksFor(level.dimension().identifier()).stream()
				.anyMatch(link -> link.involves(pos));
		}
		return !level.getEntitiesOfClass(
			FenceLeadEntity.class,
			player.getBoundingBox().inflate(96.0),
			entity -> !entity.isPending()
				&& (entity.getFrom().equals(pos) || entity.getTo().filter(pos::equals).isPresent())
		).isEmpty();
	}

	/**
	 * True when the player is holding a real leashed mob — not a pending fence-rope marker.
	 * Hitching that animal takes priority over starting a fence-to-fence span.
	 */
	private static boolean isLeadingAnimal(Player player) {
		for (Leashable leashable : Leashable.leashableLeashedTo(player)) {
			if (leashable instanceof FenceLeadEntity) {
				continue;
			}
			if (leashable instanceof Entity entity && entity.isAlive()) {
				return true;
			}
		}
		return false;
	}

	private static boolean knotPresent(Level level, BlockPos pos) {
		return LeashFenceKnotEntity.getKnot(level, pos).isPresent();
	}

	/**
	 * Shears drop every fence-to-fence lead on this post, then vanilla-cut any animals
	 * still tied to the knot. Item-frame / plant shear behavior is untouched.
	 */
	private static InteractionResult tryShear(
		ServerPlayer player,
		ServerLevel level,
		InteractionHand hand,
		BlockPos pos,
		ItemStack held
	) {
		if (!level.getBlockState(pos).is(BlockTags.FENCES) || !held.is(Items.SHEARS)) {
			return InteractionResult.PASS;
		}

		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		var dimension = level.dimension().identifier();

		List<UUID> pendingOwners = new ArrayList<>();
		for (var entry : data.pendingEntries()) {
			FenceLeadSavedData.PendingLink pending = entry.getValue();
			if (pending.dimension().equals(dimension) && pending.hasFarEnd(pos)) {
				pendingOwners.add(entry.getKey());
			}
		}
		int pendingRemoved = data.clearPendingAt(dimension, pos);
		for (UUID ownerId : pendingOwners) {
			ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null) {
				FenceLeadVisuals.clearPendingFor(owner);
			}
		}

		int removed = data.removeLinksAt(dimension, pos);
		FenceLeadVisuals.removeLinksAt(level, pos);

		boolean animals = LeashFenceKnotEntity.getKnot(level, pos)
			.map(knot -> knot.shearOffAllLeashConnections(player))
			.orElse(false);

		int leadCount = removed + pendingRemoved;
		if (leadCount <= 0 && !animals) {
			return InteractionResult.PASS;
		}

		if (leadCount > 0) {
			Block.popResource(level, pos, new ItemStack(Items.LEAD, leadCount));
			FenceLeadVisuals.resync(level);
			VpaNetworking.syncFenceLeads(level);
			level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.BLOCKS, 1.0f, 1.0f);
		}

		held.hurtAndBreak(1, player, hand.asEquipmentSlot());
		return InteractionResult.SUCCESS_SERVER;
	}

	/**
	 * Empty-hand: detach this end of every link. Other ends stay; player holds those free ends.
	 * No inventory refund. Next dest click reties the whole bundle at once.
	 */
	private static InteractionResult tryRegrab(ServerPlayer player, ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(BlockTags.FENCES)) {
			return InteractionResult.PASS;
		}

		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		var dimension = level.dimension().identifier();
		List<FenceLeadLink> atPost = data.linksAt(dimension, pos);
		if (atPost.isEmpty()) {
			return InteractionResult.PASS;
		}

		List<BlockPos> farEnds = new ArrayList<>();
		for (FenceLeadLink link : atPost) {
			BlockPos other = link.other(pos);
			if (!other.equals(pos) && !farEnds.contains(other)) {
				farEnds.add(other);
			}
		}
		if (farEnds.isEmpty()) {
			return InteractionResult.PASS;
		}

		data.removeLinksAt(dimension, pos);
		FenceLeadVisuals.removeLinksAt(level, pos);
		FenceLeadVisuals.resync(level);
		data.setPending(player.getUUID(), dimension, pos, farEnds);
		FenceLeadVisuals.spawnPending(level, player, farEnds);
		level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.BLOCKS, 1.0f, 1.0f);
		VpaNetworking.syncFenceLeads(level);
		int remaining = farEnds.size();
		if (remaining == 1) {
			player.sendSystemMessage(
				Component.literal("Lead untied — right-click a fence to reconnect."),
				true
			);
		} else {
			player.sendSystemMessage(
				Component.literal("Untied " + remaining + " leads — right-click a fence to reconnect them all."),
				true
			);
		}
		// Same-click must not immediately retie; a later click on this post is a real dest.
		suppressConnect(player, level);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Consume a lead and begin a new pending rope. Never called while already pending. */
	private static InteractionResult startPending(
		ServerPlayer player,
		ServerLevel level,
		InteractionHand hand,
		BlockPos pos,
		ItemStack held
	) {
		if (!player.getAbilities().instabuild) {
			held.shrink(1);
		}
		// Clean leftover knots before pending registers this post, or a fake knot is kept.
		FenceLeadVisuals.discardUnusedKnots(level);
		FenceLeadSavedData.get(level).setPending(player.getUUID(), level.dimension().identifier(), pos);
		FenceLeadVisuals.spawnPending(level, player, pos);
		level.playSound(null, pos, SoundEvents.LEAD_TIED, SoundSource.BLOCKS, 1.0f, 1.2f);
		VpaNetworking.syncFenceLeads(player);
		player.sendSystemMessage(
			Component.literal("Lead anchored — right-click another fence (empty hand is fine)."),
			true
		);
		suppressConnect(player, level);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Place every remaining span on dest in one click. Dest may be the original hub. */
	private static InteractionResult finishPending(
		ServerPlayer player,
		ServerLevel level,
		InteractionHand hand,
		BlockPos pos,
		ItemStack held
	) {
		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		var dimension = level.dimension().identifier();
		FenceLeadSavedData.PendingLink pending = data.getPending(player.getUUID());
		if (pending == null) {
			return InteractionResult.PASS;
		}

		// Grab/start suppress: ignore the same click that created pending. Later clicks on origin retie.
		if (isConnectSuppressed(player, level)) {
			return InteractionResult.SUCCESS_SERVER;
		}

		data.purgeInvalid(level, dimension);
		List<BlockPos> placed = new ArrayList<>();
		int leftover = 0;
		for (BlockPos far : pending.farEnds()) {
			if (far.equals(pos)) {
				leftover++;
				continue;
			}
			// Place in-range spans (origin is a valid dest); refund leftovers so a mixed bundle is never stuck.
			if (!withinRange(pos, far)) {
				leftover++;
				continue;
			}
			data.addLink(new FenceLeadLink(dimension, pos, far));
			placed.add(far);
		}

		data.clearPending(player.getUUID());
		if (placed.isEmpty()) {
			FenceLeadVisuals.clearPendingFor(player);
			if (!player.getAbilities().instabuild) {
				refundLeads(player, leftover);
			}
			level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.BLOCKS, 0.5f, 0.5f);
			VpaNetworking.syncFenceLeads(player);
			player.sendSystemMessage(
				Component.literal("Too far — leads reach " + MAX_RANGE + " blocks."),
				true
			);
			return InteractionResult.SUCCESS_SERVER;
		}

		FenceLeadVisuals.completePending(player, pos, placed);
		if (!player.getAbilities().instabuild && leftover > 0) {
			refundLeads(player, leftover);
		}
		suppressConnect(player, level);
		level.playSound(null, pos, SoundEvents.LEAD_TIED, SoundSource.BLOCKS, 1.0f, 0.9f);
		VpaNetworking.syncFenceLeads(level);
		if (leftover > 0) {
			player.sendSystemMessage(
				Component.literal("Connected " + placed.size() + " lead" + (placed.size() == 1 ? "" : "s")
					+ " — " + leftover + " out of range."),
				true
			);
		} else if (placed.size() == 1) {
			player.sendSystemMessage(Component.literal("Lead connected."), true);
		} else {
			player.sendSystemMessage(Component.literal("Connected " + placed.size() + " leads."), true);
		}
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Vanilla pickup insert. Leftover that does not fit is dropped. */
	private static void refundLeads(Player player, int count) {
		if (count <= 0) {
			return;
		}
		while (count > 0) {
			int put = Math.min(new ItemStack(Items.LEAD).getMaxStackSize(), count);
			ItemStack stack = new ItemStack(Items.LEAD, put);
			if (!player.getInventory().add(stack)) {
				player.drop(stack, false);
			}
			count -= put;
		}
	}

	public static void onFenceBroken(
		Level level,
		Player player,
		BlockPos pos,
		BlockState state,
		BlockEntity blockEntity
	) {
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		FenceLeadSavedData data = FenceLeadSavedData.get(serverLevel);
		var dimension = level.dimension().identifier();

		List<UUID> pendingOwners = new ArrayList<>();
		for (var entry : data.pendingEntries()) {
			FenceLeadSavedData.PendingLink pending = entry.getValue();
			if (pending.dimension().equals(dimension) && pending.hasFarEnd(pos)) {
				pendingOwners.add(entry.getKey());
			}
		}
		int pendingRemoved = data.clearPendingAt(dimension, pos);
		for (UUID ownerId : pendingOwners) {
			ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerId);
			if (owner != null) {
				FenceLeadVisuals.clearPendingFor(owner);
			}
		}

		int removed = data.removeLinksAt(dimension, pos);
		int markers = FenceLeadVisuals.removeLinksAt(serverLevel, pos);
		int leadCount = removed + pendingRemoved;
		if (leadCount <= 0) {
			leadCount = markers;
		}
		if (leadCount <= 0) {
			return;
		}

		FenceLeadVisuals.resync(serverLevel);
		// Always spawn lead item entities at the post. Inventory merge hid/failed the refund.
		Block.popResource(serverLevel, pos, new ItemStack(Items.LEAD, leadCount));
		level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.BLOCKS, 1.0f, 1.0f);
		VpaNetworking.syncFenceLeads(serverLevel);
	}

	public static void purgeAndSync(ServerLevel level) {
		FenceLeadSavedData.get(level).purgeInvalid(level, level.dimension().identifier());
		FenceLeadVisuals.resync(level);
		VpaNetworking.syncFenceLeads(level);
	}

	private static boolean withinRange(BlockPos a, BlockPos b) {
		return Math.max(
			Math.max(Math.abs(a.getX() - b.getX()), Math.abs(a.getY() - b.getY())),
			Math.abs(a.getZ() - b.getZ())
		) <= MAX_RANGE;
	}

	private static void suppressConnect(ServerPlayer player, ServerLevel level) {
		connectSuppressUntil.put(player.getUUID(), level.getGameTime() + CONNECT_SUPPRESS_TICKS);
	}

	private static boolean isConnectSuppressed(ServerPlayer player, ServerLevel level) {
		Long until = connectSuppressUntil.get(player.getUUID());
		if (until == null) {
			return false;
		}
		if (level.getGameTime() > until) {
			connectSuppressUntil.remove(player.getUUID());
			return false;
		}
		return true;
	}
}
