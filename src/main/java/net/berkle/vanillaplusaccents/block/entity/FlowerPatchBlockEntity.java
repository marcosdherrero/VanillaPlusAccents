package net.berkle.vanillaplusaccents.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.berkle.vanillaplusaccents.block.VpaBlocks;
import net.berkle.vanillaplusaccents.flower.FlowerPatchOffsets;
import net.berkle.vanillaplusaccents.flower.FlowerPatchSync;

/** Stores flower type and count for a flower patch. */
public class FlowerPatchBlockEntity extends BlockEntity {

	private Identifier flowerId = Identifier.withDefaultNamespace("dandelion");
	private int count = 1;

	public FlowerPatchBlockEntity(BlockPos pos, BlockState state) {
		super(VpaBlocks.FLOWER_PATCH_BE, pos, state);
	}

	public Identifier getFlowerId() {
		return flowerId;
	}

	public int getCount() {
		return count;
	}

	public FlowerPatchOffsets getOffsets() {
		return FlowerPatchOffsets.forCount(count, worldPosition);
	}

	public void configure(Identifier flowerId, int count) {
		configure(flowerId, count, true);
	}

	public void configure(Identifier flowerId, int count, boolean sync) {
		this.flowerId = flowerId;
		this.count = Math.clamp(count, 1, 4);
		setChanged();
		if (sync && level instanceof ServerLevel) {
			FlowerPatchSync.broadcast(this);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		flowerId = input.read("flower", Identifier.CODEC).orElse(Identifier.withDefaultNamespace("dandelion"));
		count = Math.clamp(input.getIntOr("count", 1), 1, 4);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.store("flower", Identifier.CODEC, flowerId);
		output.putInt("count", count);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
		// Vanilla default is empty; without this clients never receive flower/count and BER draws nothing useful.
		return saveWithoutMetadata(registries);
	}
}
