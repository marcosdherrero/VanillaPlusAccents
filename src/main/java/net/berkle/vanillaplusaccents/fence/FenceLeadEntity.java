package net.berkle.vanillaplusaccents.fence;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import net.berkle.vanillaplusaccents.entity.VpaEntityTypes;
import net.berkle.vanillaplusaccents.network.VpaNetworking;

/**
 * Fence-lead rope marker. Not flagged invisible so the client entity pass still
 * extracts leashStates (pending → player, completed → destination knot).
 */
public class FenceLeadEntity extends Entity implements Leashable {

	/** Hold 55% up the post. Same Y on both ends so vanilla slack still droops. */
	public static final double ATTACH_Y = 0.55;

	private static final EntityDataAccessor<BlockPos> DATA_FROM = SynchedEntityData.defineId(
		FenceLeadEntity.class, EntityDataSerializers.BLOCK_POS
	);
	private static final EntityDataAccessor<Optional<BlockPos>> DATA_TO = SynchedEntityData.defineId(
		FenceLeadEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS
	);
	private static final EntityDataAccessor<Boolean> DATA_HAS_OWNER = SynchedEntityData.defineId(
		FenceLeadEntity.class, EntityDataSerializers.BOOLEAN
	);
	private static final EntityDataAccessor<Long> DATA_OWNER_MSB = SynchedEntityData.defineId(
		FenceLeadEntity.class, EntityDataSerializers.LONG
	);
	private static final EntityDataAccessor<Long> DATA_OWNER_LSB = SynchedEntityData.defineId(
		FenceLeadEntity.class, EntityDataSerializers.LONG
	);

	private LeashData leashData;
	private boolean leashApplied;
	private boolean releasing;

	public FenceLeadEntity(EntityType<?> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setNoGravity(true);
	}

	public FenceLeadEntity(Level level, BlockPos from, BlockPos to) {
		this(VpaEntityTypes.FENCE_LEAD, level);
		setCompleted(from, to);
	}

	public FenceLeadEntity(Level level, BlockPos from, UUID owner) {
		this(VpaEntityTypes.FENCE_LEAD, level);
		setPending(from, owner);
	}

	public void setCompleted(BlockPos from, BlockPos to) {
		entityData.set(DATA_FROM, from.immutable());
		entityData.set(DATA_TO, Optional.of(to.immutable()));
		entityData.set(DATA_HAS_OWNER, false);
		setPos(attachPoint(level(), from));
		leashApplied = false;
	}

	public void setPending(BlockPos from, UUID owner) {
		entityData.set(DATA_FROM, from.immutable());
		entityData.set(DATA_TO, Optional.empty());
		entityData.set(DATA_HAS_OWNER, true);
		entityData.set(DATA_OWNER_MSB, owner.getMostSignificantBits());
		entityData.set(DATA_OWNER_LSB, owner.getLeastSignificantBits());
		setPos(attachPoint(level(), from));
		leashApplied = false;
	}

	public BlockPos getFrom() {
		return entityData.get(DATA_FROM);
	}

	public Optional<BlockPos> getTo() {
		return entityData.get(DATA_TO);
	}

	public boolean isPending() {
		return entityData.get(DATA_TO).isEmpty();
	}

	public boolean isPrimaryCompleted() {
		return entityData.get(DATA_TO).isPresent();
	}

	public Optional<UUID> getOwnerUuid() {
		if (!entityData.get(DATA_HAS_OWNER)) {
			return Optional.empty();
		}
		return Optional.of(new UUID(entityData.get(DATA_OWNER_MSB), entityData.get(DATA_OWNER_LSB)));
	}

	public static Vec3 attachPoint(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY() + ATTACH_Y, pos.getZ() + 0.5);
	}

	public static Vec3 attachPoint(Level level, BlockPos pos) {
		return attachPoint(pos);
	}

	public boolean matches(FenceLeadLink link) {
		Optional<BlockPos> to = getTo();
		if (to.isEmpty()) {
			return false;
		}
		BlockPos from = getFrom();
		BlockPos other = to.get();
		return (from.equals(link.from()) && other.equals(link.to()))
			|| (from.equals(link.to()) && other.equals(link.from()));
	}

	@Override
	public LeashData getLeashData() {
		return leashData;
	}

	@Override
	public void setLeashData(LeashData leashData) {
		this.leashData = leashData;
	}

	@Override
	public double leashSnapDistance() {
		return 1024.0;
	}

	@Override
	public double leashElasticDistance() {
		return 1024.0;
	}

	@Override
	public boolean checkElasticInteractions(Entity holder, LeashData data) {
		return false;
	}

	@Override
	public void onElasticLeashPull() {
	}

	@Override
	public void leashTooFarBehaviour() {
	}

	@Override
	public boolean canHaveALeashAttachedTo(Entity entity) {
		return false;
	}

	@Override
	public void dropLeash() {
		releasing = true;
		if (level() instanceof ServerLevel serverLevel) {
			releaseSavedConnection(serverLevel);
		}
		removeLeash();
		discard();
	}

	/**
	 * Vanilla shears call {@link #dropLeash()} on everything tied to a knot.
	 * Drop the consumed lead and forget the saved span so the rope cannot reconnect.
	 */
	private void releaseSavedConnection(ServerLevel level) {
		var dimension = level.dimension().identifier();
		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		Optional<BlockPos> to = getTo();
		BlockPos from = getFrom();
		if (to.isPresent()) {
			if (data.removeLink(new FenceLeadLink(dimension, from, to.get()))) {
				Block.popResource(level, from, new ItemStack(Items.LEAD));
			}
		} else {
			releasePendingBundle(level, data, from);
		}
		FenceLeadVisuals.discardKnotsIfUnused(level, from, to.orElse(null));
		VpaNetworking.syncFenceLeads(level);
	}

	private void releasePendingBundle(ServerLevel level, FenceLeadSavedData data, BlockPos from) {
		getOwnerUuid().ifPresent(id -> {
			FenceLeadSavedData.PendingLink current = data.getPending(id);
			int extra = current == null ? 0 : Math.max(0, current.remaining() - 1);
			data.clearPending(id);
			FenceLeadVisuals.discardPendingExcept(level, id, getUUID());
			if (extra > 0) {
				Block.popResource(level, from, new ItemStack(Items.LEAD, extra));
			}
		});
		Block.popResource(level, from, new ItemStack(Items.LEAD));
	}

	/**
	 * A post disappeared without {@link FenceLeadHandler#onFenceBroken} refunding
	 * (piston, explosion). Completed spans pop one lead at {@code from} only if
	 * SavedData still held the link.
	 */
	private void releaseMissingFence(ServerLevel level) {
		releasing = true;
		var dimension = level.dimension().identifier();
		FenceLeadSavedData data = FenceLeadSavedData.get(level);
		Optional<BlockPos> to = getTo();
		BlockPos from = getFrom();
		if (to.isPresent()) {
			if (data.removeLink(new FenceLeadLink(dimension, from, to.get()))) {
				Block.popResource(level, from, new ItemStack(Items.LEAD));
			}
		} else {
			releasePendingBundle(level, data, from);
		}
		if (isLeashed()) {
			removeLeash();
		}
		FenceLeadVisuals.discardKnotsIfUnused(level, from, to.orElse(null));
		VpaNetworking.syncFenceLeads(level);
		discard();
	}

	@Override
	public Vec3 getLeashOffset(float partialTick) {
		return Vec3.ZERO;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_FROM, BlockPos.ZERO);
		builder.define(DATA_TO, Optional.empty());
		builder.define(DATA_HAS_OWNER, false);
		builder.define(DATA_OWNER_MSB, 0L);
		builder.define(DATA_OWNER_LSB, 0L);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean shouldRenderAtSqrDistance(double distance) {
		return distance < 96.0 * 96.0;
	}

	/** True when this marker will submit a completed span (DATA_TO present, in range, not culled). */
	public boolean isDrawingCompleted(Vec3 cameraPos) {
		if (!isPrimaryCompleted() || isRemoved()) {
			return false;
		}
		return shouldRenderAtSqrDistance(distanceToSqr(cameraPos));
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean shouldBeSaved() {
		return super.shouldBeSaved() && isPrimaryCompleted();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		BlockPos from = BlockPos.of(input.getLongOr("From", BlockPos.ZERO.asLong()));
		if (input.getBooleanOr("Completed", false)) {
			setCompleted(from, BlockPos.of(input.getLongOr("To", from.asLong())));
			return;
		}
		long ownerMsb = input.getLongOr("OwnerMsb", 0L);
		long ownerLsb = input.getLongOr("OwnerLsb", 0L);
		if (input.getBooleanOr("HasOwner", false)) {
			setPending(from, new UUID(ownerMsb, ownerLsb));
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putLong("From", getFrom().asLong());
		boolean completed = isPrimaryCompleted();
		output.putBoolean("Completed", completed);
		getTo().ifPresent(to -> output.putLong("To", to.asLong()));
		getOwnerUuid().ifPresent(owner -> {
			output.putBoolean("HasOwner", true);
			output.putLong("OwnerMsb", owner.getMostSignificantBits());
			output.putLong("OwnerLsb", owner.getLeastSignificantBits());
		});
	}

	@Override
	public void tick() {
		if (releasing || isRemoved() || level().isClientSide() || !(level() instanceof ServerLevel serverLevel)) {
			return;
		}

		if (isPending() && getOwnerUuid().isEmpty()) {
			discard();
			return;
		}

		if (isPending() && getOwnerUuid().isEmpty()) {
			discard();
			return;
		}

		BlockPos from = getFrom();
		Optional<BlockPos> to = getTo();
		if (!level().getBlockState(from).is(BlockTags.FENCES)
			|| (to.isPresent() && !level().getBlockState(to.get()).is(BlockTags.FENCES))) {
			releaseMissingFence(serverLevel);
			return;
		}

		if (tickCount % 20 == 0) {
			LeashFenceKnotEntity.getOrCreateKnot(serverLevel, from);
			to.ifPresent(pos -> LeashFenceKnotEntity.getOrCreateKnot(serverLevel, pos));
		}

		Vec3 expected = attachPoint(serverLevel, from);
		if (position().distanceToSqr(expected) > 0.0001) {
			setPos(expected);
		}

		if (!leashApplied && tickCount >= 1) {
			applyLeash(serverLevel);
		} else if (leashApplied && isPrimaryCompleted() && !isLeashed()) {
			// Re-attach if the destination knot was recreated.
			applyLeash(serverLevel);
		}

		Leashable.tickLeash(serverLevel, this);
	}

	public void forceApplyLeash(ServerLevel level) {
		if (isLeashed()) {
			removeLeash();
		}
		leashApplied = false;
		applyLeash(level);
	}

	private void applyLeash(ServerLevel level) {
		leashApplied = true;
		Optional<BlockPos> to = getTo();
		if (to.isPresent()) {
			LeashFenceKnotEntity.getOrCreateKnot(level, getFrom());
			LeashFenceKnotEntity knot = LeashFenceKnotEntity.getOrCreateKnot(level, to.get());
			setLeashedTo(knot, true);
			return;
		}
		Optional<UUID> ownerId = getOwnerUuid();
		if (ownerId.isEmpty()) {
			return;
		}
		net.minecraft.world.entity.player.Player player = level.getServer().getPlayerList().getPlayer(ownerId.get());
		if (player != null) {
			LeashFenceKnotEntity.getOrCreateKnot(level, getFrom());
			setLeashedTo(player, true);
		}
	}
}
