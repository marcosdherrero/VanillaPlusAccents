package net.berkle.vanillaplusaccents.seat;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import net.berkle.vanillaplusaccents.entity.VpaEntityTypes;

/** Invisible seat marker for slab/stair sitting. */
public class SeatEntity extends Entity {

	private BlockPos seatBlock = BlockPos.ZERO;
	/** After mount while Shift is held, ignore sneak-dismount until Shift is released once. */
	private boolean allowSneakDismount;

	public SeatEntity(EntityType<?> type, Level level) {
		super(type, level);
		this.noPhysics = true;
		this.setInvisible(true);
	}

	public SeatEntity(Level level, BlockPos seatBlock, Vec3 seatPos) {
		this(VpaEntityTypes.SEAT, level);
		this.seatBlock = seatBlock.immutable();
		this.setPos(seatPos);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		return false;
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		seatBlock = BlockPos.of(input.getLongOr("SeatBlock", BlockPos.ZERO.asLong()));
		allowSneakDismount = input.getBooleanOr("AllowSneakDismount", true);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putLong("SeatBlock", seatBlock.asLong());
		output.putBoolean("AllowSneakDismount", allowSneakDismount);
	}

	public BlockPos getSeatBlock() {
		return seatBlock;
	}

	public boolean allowsSneakDismount() {
		return allowSneakDismount;
	}

	@Override
	public void tick() {
		if (level().isClientSide()) {
			updateSneakDismountGrace();
			return;
		}
		if (!SeatSupport.isSeatBlock(level().getBlockState(seatBlock))) {
			discard();
			return;
		}
		if (getPassengers().isEmpty()) {
			discard();
			return;
		}
		updateSneakDismountGrace();
	}

	private void updateSneakDismountGrace() {
		if (allowSneakDismount || getPassengers().isEmpty()) {
			return;
		}
		Entity passenger = getPassengers().getFirst();
		if (passenger instanceof Player player && !player.isShiftKeyDown()) {
			allowSneakDismount = true;
		}
	}

	@Override
	public void onPassengerTurned(Entity passenger) {
		setYRot(passenger.getYRot());
		yRotO = getYRot();
	}

	@Override
	public Vec3 getPassengerRidingPosition(Entity passenger) {
		return position();
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	public static SeatEntity spawn(ServerLevel level, BlockPos seatBlock, Vec3 seatPos) {
		SeatEntity seat = new SeatEntity(level, seatBlock, seatPos);
		level.addFreshEntity(seat);
		return seat;
	}
}
