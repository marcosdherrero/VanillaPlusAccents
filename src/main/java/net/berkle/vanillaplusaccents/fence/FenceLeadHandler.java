package net.berkle.vanillaplusaccents.fence;

import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
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
 *   <li>Lead on a fence — consume one lead, start a pending rope to the player</li>
 *   <li>Same fence again while pending — cancel and refund</li>
 *   <li>Another fence (≤16) while pending — place that link, then stop (no auto-next rope)</li>
 *   <li>Lead on a fence again — only way to start another rope</li>
 *   <li>Empty hand on a knot — pick up all links on that post</li>
 *   <li>Breaking a linked fence — removes its connections; survival returns leads to inventory, creative drops them as items at the break</li>
 * </ul>
 */
public final class FenceLeadHandler {

	public static final int MAX_RANGE = 16;

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

		if (level.isClientSide()) {
			if (hasPendingRope(player, level) || held.is(Items.LEAD) || (held.isEmpty() && hasLinksAt(player, level, pos))) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (hasPendingRope(serverPlayer, serverLevel)) {
			return finishPending(serverPlayer, serverLevel, hand, pos, held);
		}

		if (held.is(Items.LEAD)) {
			return startPending(serverPlayer, serverLevel, hand, pos, held);
		}

		if (held.isEmpty()) {
			return tryPickup(serverPlayer, serverLevel, pos);
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

		if (level.isClientSide()) {
			if (hasPendingRope(player, level) || held.is(Items.LEAD)) {
				return InteractionResult.SUCCESS;
			}
			return InteractionResult.PASS;
		}

		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.PASS;
		}

		if (hasPendingRope(serverPlayer, serverLevel)) {
			return finishPending(serverPlayer, serverLevel, hand, pos, held);
		}

		if (held.is(Items.LEAD)) {
			return startPending(serverPlayer, serverLevel, hand, pos, held);
		}

		return InteractionResult.PASS;
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

	private static InteractionResult tryPickup(ServerPlayer player, ServerLevel level, BlockPos pos) {
		if (!level.getBlockState(pos).is(BlockTags.FENCES)) {
			return InteractionResult.PASS;
		}

		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		var dimension = level.dimension().identifier();
		int removed = data.removeLinksAt(dimension, pos);
		if (removed <= 0) {
			return InteractionResult.PASS;
		}

		FenceLeadVisuals.clearPendingFor(player);
		FenceLeadVisuals.removeLinksAt(level, pos);
		FenceLeadVisuals.resync(level);

		if (!player.getAbilities().instabuild) {
			giveLeads(player, InteractionHand.MAIN_HAND, removed);
		}
		level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.BLOCKS, 1.0f, 1.0f);
		VpaNetworking.syncFenceLeads(level);
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
		FenceLeadSavedData.get(level).setPending(player.getUUID(), level.dimension().identifier(), pos);
		FenceLeadVisuals.spawnPending(level, player, pos);
		level.playSound(null, pos, SoundEvents.LEAD_TIED, SoundSource.BLOCKS, 1.0f, 1.2f);
		VpaNetworking.syncFenceLeads(player);
		player.sendSystemMessage(
			Component.literal("Lead anchored — right-click another fence (empty hand is fine)."),
			true
		);
		return InteractionResult.SUCCESS_SERVER;
	}

	/** Finish or cancel the current pending rope. Does not start another. */
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

		if (pending.pos().equals(pos)) {
			data.clearPending(player.getUUID());
			FenceLeadVisuals.clearPendingFor(player);
			if (!player.getAbilities().instabuild) {
				refundLead(player, held);
			}
			level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.BLOCKS, 0.5f, 1.0f);
			VpaNetworking.syncFenceLeads(player);
			player.sendSystemMessage(Component.literal("Lead placement cancelled."), true);
			return InteractionResult.SUCCESS_SERVER;
		}

		if (!withinRange(pending.pos(), pos)) {
			data.clearPending(player.getUUID());
			FenceLeadVisuals.clearPendingFor(player);
			if (!player.getAbilities().instabuild) {
				refundLead(player, held);
			}
			level.playSound(null, pos, SoundEvents.LEAD_UNTIED, SoundSource.BLOCKS, 0.5f, 0.5f);
			VpaNetworking.syncFenceLeads(player);
			player.sendSystemMessage(
				Component.literal("Too far — leads reach " + MAX_RANGE + " blocks."),
				true
			);
			return InteractionResult.SUCCESS_SERVER;
		}

		BlockPos from = pending.pos();
		data.purgeInvalid(level, dimension);
		data.addLink(new FenceLeadLink(dimension, from, pos));
		data.clearPending(player.getUUID());
		FenceLeadVisuals.completePending(player, from, pos);

		level.playSound(null, pos, SoundEvents.LEAD_TIED, SoundSource.BLOCKS, 1.0f, 0.9f);
		VpaNetworking.syncFenceLeads(level);
		player.sendSystemMessage(Component.literal("Lead connected."), true);
		return InteractionResult.SUCCESS_SERVER;
	}

	private static void refundLead(Player player, ItemStack held) {
		if (held.isEmpty()) {
			if (player.getMainHandItem().isEmpty()) {
				player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD));
			} else {
				giveLeads(player, InteractionHand.MAIN_HAND, 1);
			}
			return;
		}
		if (held.is(Items.LEAD) && held.getCount() < held.getMaxStackSize()) {
			held.grow(1);
			return;
		}
		giveLeads(player, InteractionHand.MAIN_HAND, 1);
	}

	/** Return leads to the used/main hand or main inventory — never the off-hand. */
	private static void giveLeads(Player player, InteractionHand preferHand, int count) {
		if (count <= 0) {
			return;
		}

		count = mergeInto(player.getItemInHand(preferHand), count);
		if (count > 0 && preferHand != InteractionHand.MAIN_HAND) {
			count = mergeInto(player.getMainHandItem(), count);
		}
		// Hotbar + main storage only (0–35). Off-hand is slot 40.
		for (int i = 0; i < 36 && count > 0; i++) {
			count = mergeInto(player.getInventory().getItem(i), count);
		}
		if (count <= 0) {
			return;
		}

		ItemStack prefer = player.getItemInHand(preferHand);
		if (prefer.isEmpty()) {
			player.setItemInHand(preferHand, new ItemStack(Items.LEAD, count));
			return;
		}
		if (player.getMainHandItem().isEmpty()) {
			player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.LEAD, count));
			return;
		}
		for (int i = 0; i < 36; i++) {
			if (player.getInventory().getItem(i).isEmpty()) {
				player.getInventory().setItem(i, new ItemStack(Items.LEAD, count));
				return;
			}
		}
		player.drop(new ItemStack(Items.LEAD, count), false);
	}

	private static int mergeInto(ItemStack stack, int count) {
		if (count <= 0 || !stack.is(Items.LEAD) || stack.getCount() >= stack.getMaxStackSize()) {
			return count;
		}
		int add = Math.min(stack.getMaxStackSize() - stack.getCount(), count);
		stack.grow(add);
		return count - add;
	}

	public static void onFenceBroken(
		Level level,
		Player player,
		BlockPos pos,
		BlockState state,
		BlockEntity blockEntity
	) {
		if (!(level instanceof ServerLevel serverLevel) || !state.is(BlockTags.FENCES)) {
			return;
		}

		FenceLeadSavedData data = FenceLeadSavedData.get(serverLevel);
		var dimension = level.dimension().identifier();

		List<UUID> pendingOwners = new ArrayList<>();
		for (var entry : data.pendingEntries()) {
			FenceLeadSavedData.PendingLink pending = entry.getValue();
			if (pending.dimension().equals(dimension) && pending.pos().equals(pos)) {
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
		int leadCount = removed + pendingRemoved;
		if (leadCount <= 0) {
			return;
		}

		FenceLeadVisuals.removeLinksAt(serverLevel, pos);
		FenceLeadVisuals.resync(serverLevel);

		if (player != null && player.getAbilities().instabuild) {
			// Creative still drops connected leads as world items at the broken post.
			Block.popResource(serverLevel, pos, new ItemStack(Items.LEAD, leadCount));
		} else if (player != null) {
			giveLeads(player, InteractionHand.MAIN_HAND, leadCount);
		} else {
			Block.popResource(serverLevel, pos, new ItemStack(Items.LEAD, leadCount));
		}
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
}
