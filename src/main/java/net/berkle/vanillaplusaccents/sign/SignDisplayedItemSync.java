package net.berkle.vanillaplusaccents.sign;

import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;

/** Pushes sign displayed-item changes to watching clients. */
public final class SignDisplayedItemSync {

	private SignDisplayedItemSync() {
	}

	public static void broadcast(SignBlockEntity sign) {
		Level level = sign.getLevel();
		if (!(level instanceof ServerLevel serverLevel)) {
			return;
		}

		ClientboundBlockEntityDataPacket packet = sign.getUpdatePacket();
		if (packet == null) {
			return;
		}

		ChunkPos chunkPos = ChunkPos.containing(sign.getBlockPos());
		for (ServerPlayer player : serverLevel.getPlayers(p -> p.getChunkTrackingView().contains(chunkPos))) {
			player.connection.send(packet);
		}
	}
}
