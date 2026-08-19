package net.berkle.vanillaplusaccents.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.vertex.PoseStack;

import net.berkle.vanillaplusaccents.client.render.state.FenceLeadRenderState;
import net.berkle.vanillaplusaccents.entity.VpaEntityTypes;
import net.berkle.vanillaplusaccents.fence.FenceLeadEntity;

/**
 * No model. Pending ropes use vanilla leashStates. Completed fence-to-fence
 * spans draw one 24-step catenary ribbon so rib spacing matches animal leads.
 */
public final class FenceLeadEntityRenderer extends EntityRenderer<FenceLeadEntity, FenceLeadRenderState> {

	public FenceLeadEntityRenderer(EntityRendererProvider.Context context) {
		super(context);
		this.shadowRadius = 0.0f;
	}

	@Override
	protected boolean affectedByCulling(FenceLeadEntity entity) {
		return false;
	}

	@Override
	protected AABB getBoundingBoxForCulling(FenceLeadEntity entity) {
		if (!entity.isPrimaryCompleted()) {
			return super.getBoundingBoxForCulling(entity);
		}
		return new AABB(
			FenceLeadEntity.attachPoint(entity.getFrom()),
			FenceLeadEntity.attachPoint(entity.getTo().orElse(entity.getFrom()))
		).inflate(0.5);
	}

	@Override
	public boolean shouldRender(FenceLeadEntity entity, Frustum frustum, double camX, double camY, double camZ) {
		if (!entity.shouldRender(camX, camY, camZ)) {
			return false;
		}
		if (entity.isPending() || entity.isPrimaryCompleted()) {
			return true;
		}
		return super.shouldRender(entity, frustum, camX, camY, camZ);
	}

	@Override
	public FenceLeadRenderState createRenderState() {
		return new FenceLeadRenderState();
	}

	@Override
	public void extractRenderState(FenceLeadEntity entity, FenceLeadRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);

		Optional<BlockPos> to = entity.getTo();
		Vec3 origin = entity.getPosition(partialTick);
		state.completed = false;
		state.start = Vec3.ZERO;
		state.end = Vec3.ZERO;
		if (!(entity.level() instanceof ClientLevel clientLevel)) {
			state.leashStates = null;
			return;
		}
		Vec3 start = FenceLeadEntity.attachPoint(clientLevel, entity.getFrom());
		state.start = start;
		if (to.isPresent()) {
			state.completed = true;
			state.end = FenceLeadEntity.attachPoint(clientLevel, to.get());
			state.leashStates = null;
			return;
		}

		Optional<UUID> owner = entity.getOwnerUuid();
		Player player = Minecraft.getInstance().player;
		if (owner.isEmpty() || player == null || !player.getUUID().equals(owner.get())) {
			state.leashStates = null;
			return;
		}

		List<EntityRenderState.LeashState> states = new ArrayList<>(1);
		states.add(FenceLeadRender.pendingState(
			clientLevel,
			start,
			player.getRopeHoldPosition(partialTick),
			origin
		));
		state.leashStates = states;
	}

	@Override
	public void submit(
		FenceLeadRenderState state,
		PoseStack poseStack,
		SubmitNodeCollector submitNodeCollector,
		CameraRenderState cameraRenderState
	) {
		if (state.completed && Minecraft.getInstance().level instanceof ClientLevel level) {
			FenceLeadRender.submitCompleted(
				level,
				poseStack,
				submitNodeCollector,
				new Vec3(state.x, state.y, state.z),
				state.start,
				state.end
			);
		}
		super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
	}

	public static void register() {
		EntityRendererRegistry.register(VpaEntityTypes.FENCE_LEAD, FenceLeadEntityRenderer::new);
	}
}
