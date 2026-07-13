package net.berkle.vanillaplusaccents.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.berkle.vanillaplusaccents.entity.VpaEntityTypes;
import net.berkle.vanillaplusaccents.fence.FenceLeadEntity;

/**
 * No model. Builds {@code leashStates} from synched endpoints so completed ropes
 * render even when the vanilla leash-holder link packet is missing/late.
 */
public final class FenceLeadEntityRenderer extends EntityRenderer<FenceLeadEntity, EntityRenderState> {

	public FenceLeadEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0f;
	}

	@Override
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	@Override
	public void extractRenderState(FenceLeadEntity entity, EntityRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);

		Optional<BlockPos> to = entity.getTo();
		Vec3 start = FenceLeadEntity.attachPoint(entity.getFrom());
		Vec3 end;
		if (to.isPresent()) {
			end = FenceLeadEntity.attachPoint(to.get());
		} else {
			Optional<UUID> owner = entity.getOwnerUuid();
			Player player = Minecraft.getInstance().player;
			if (owner.isEmpty() || player == null || !player.getUUID().equals(owner.get())) {
				state.leashStates = null;
				return;
			}
			end = player.getRopeHoldPosition(partialTick);
		}

		EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
		leash.offset = start.subtract(entity.getPosition(partialTick));
		leash.start = start;
		leash.end = end;
		leash.slack = true;

		int startPacked = LevelRenderer.getLightCoords(entity.level(), BlockPos.containing(start));
		int endPacked = LevelRenderer.getLightCoords(entity.level(), BlockPos.containing(end));
		leash.startBlockLight = LightCoordsUtil.block(startPacked);
		leash.startSkyLight = LightCoordsUtil.sky(startPacked);
		leash.endBlockLight = LightCoordsUtil.block(endPacked);
		leash.endSkyLight = LightCoordsUtil.sky(endPacked);

		List<EntityRenderState.LeashState> states = new ArrayList<>(1);
		states.add(leash);
		state.leashStates = states;
	}

	public static void register() {
		EntityRendererRegistry.register(VpaEntityTypes.FENCE_LEAD, FenceLeadEntityRenderer::new);
	}
}
