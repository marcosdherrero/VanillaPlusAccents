package net.berkle.vanillaplusaccents.flower;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Pushes flower patch block and block-entity changes to watching clients. */
public final class FlowerPatchSync {

	private FlowerPatchSync() {
	}

	public static void broadcast(BlockEntity blockEntity) {
		if (!(blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
			return;
		}

		syncChange(serverLevel, blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity, null);
	}

	/** Sends the block type change; required when replacing a vanilla plant with a flower patch. */
	public static void broadcastBlockChange(ServerLevel level, BlockPos pos, BlockState state) {
		sendToWatchingPlayers(level, pos, new ClientboundBlockUpdatePacket(pos, state));
	}

	public static void broadcastConversion(ServerLevel level, BlockPos pos, BlockState patchState, BlockEntity blockEntity) {
		syncChange(level, pos, patchState, blockEntity, null);
	}

	public static void syncChange(
		ServerLevel level,
		BlockPos pos,
		BlockState state,
		BlockEntity blockEntity,
		ServerPlayer player
	) {
		broadcastBlockChange(level, pos, state);
		if (blockEntity != null) {
			Packet<?> packet = blockEntity.getUpdatePacket();
			if (packet instanceof ClientboundBlockEntityDataPacket dataPacket) {
				sendToWatchingPlayers(level, pos, dataPacket);
			}
		}

		if (player != null) {
			player.connection.send(new ClientboundBlockUpdatePacket(pos, state));
			if (blockEntity != null) {
				Packet<?> packet = blockEntity.getUpdatePacket();
				if (packet instanceof ClientboundBlockEntityDataPacket dataPacket) {
					player.connection.send(dataPacket);
				}
			}
		}
	}

	private static void sendToWatchingPlayers(ServerLevel level, BlockPos pos, Packet<?> packet) {
		ChunkPos chunkPos = ChunkPos.containing(pos);
		for (ServerPlayer watcher : level.getPlayers(p -> p.getChunkTrackingView().contains(chunkPos))) {
			watcher.connection.send(packet);
		}
	}
}
