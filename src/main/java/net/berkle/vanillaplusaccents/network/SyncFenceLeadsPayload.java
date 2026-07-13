package net.berkle.vanillaplusaccents.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;
import net.berkle.vanillaplusaccents.fence.FenceLeadLink;

/** S2C: sync completed fence leads and the local player's pending anchor (if any). */
public record SyncFenceLeadsPayload(
	List<FenceLeadLink> links,
	boolean hasPending,
	UUID pendingPlayer,
	Identifier pendingDimension,
	BlockPos pendingPos
) implements CustomPacketPayload {

	public static final CustomPacketPayload.Type<SyncFenceLeadsPayload> TYPE = new CustomPacketPayload.Type<>(
		Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "sync_fence_leads")
	);

	private static final StreamCodec<RegistryFriendlyByteBuf, FenceLeadLink> LINK_CODEC = StreamCodec.composite(
		Identifier.STREAM_CODEC, FenceLeadLink::dimension,
		BlockPos.STREAM_CODEC, FenceLeadLink::from,
		BlockPos.STREAM_CODEC, FenceLeadLink::to,
		FenceLeadLink::new
	);

	private static final StreamCodec<RegistryFriendlyByteBuf, UUID> UUID_CODEC = StreamCodec.of(
		(buf, uuid) -> buf.writeUUID(uuid),
		buf -> buf.readUUID()
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, SyncFenceLeadsPayload> CODEC = StreamCodec.composite(
		LINK_CODEC.apply(ByteBufCodecs.list()), SyncFenceLeadsPayload::links,
		ByteBufCodecs.BOOL, SyncFenceLeadsPayload::hasPending,
		UUID_CODEC, SyncFenceLeadsPayload::pendingPlayer,
		Identifier.STREAM_CODEC, SyncFenceLeadsPayload::pendingDimension,
		BlockPos.STREAM_CODEC, SyncFenceLeadsPayload::pendingPos,
		SyncFenceLeadsPayload::new
	);

	public static SyncFenceLeadsPayload of(
		List<FenceLeadLink> links,
		UUID pendingPlayer,
		Identifier pendingDimension,
		BlockPos pendingPos
	) {
		if (pendingPlayer == null || pendingDimension == null || pendingPos == null) {
			return new SyncFenceLeadsPayload(links, false, new UUID(0L, 0L), Identifier.withDefaultNamespace("air"), BlockPos.ZERO);
		}
		return new SyncFenceLeadsPayload(links, true, pendingPlayer, pendingDimension, pendingPos);
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
