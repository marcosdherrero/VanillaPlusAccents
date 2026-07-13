package net.berkle.vanillaplusaccents.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;

/** C2S: ctrl+shift accent action on a block or entity. */
public record AccentActionPayload(
	AccentActionKind kind,
	BlockPos blockPos,
	int entityId
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<AccentActionPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "accent_action")
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, AccentActionPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeEnum(payload.kind);
			buf.writeBlockPos(payload.blockPos);
			buf.writeVarInt(payload.entityId);
		},
		buf -> new AccentActionPayload(
			buf.readEnum(AccentActionKind.class),
			buf.readBlockPos(),
			buf.readVarInt()
		)
	);

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
