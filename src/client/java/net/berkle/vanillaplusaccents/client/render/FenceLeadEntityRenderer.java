package net.berkle.vanillaplusaccents.client.render;

import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import net.berkle.vanillaplusaccents.entity.VpaEntityTypes;
import net.berkle.vanillaplusaccents.fence.FenceLeadEntity;

/**
 * No model. Vanilla {@link EntityRenderer#extractRenderState} fills {@code leashStates}
 * from the Leashable holder (pending → player, completed → dest knot) and
 * {@link EntityRenderer#submit} calls {@code submitLeash} — same mesh as a pig on a knot.
 */
public final class FenceLeadEntityRenderer extends EntityRenderer<FenceLeadEntity, EntityRenderState> {

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
	public EntityRenderState createRenderState() {
		return new EntityRenderState();
	}

	/**
	 * Vanilla {@code LeashState} defaults slack to true, but same-Y chords have
	 * {@code dy == 0} so {@code submitLeash} still draws a straight line. Force
	 * slack and, for level fence spans, dip a midpoint so the striped mesh droops
	 * like a pig lead. Does not touch mob renderers.
	 */
	@Override
	public void extractRenderState(FenceLeadEntity entity, EntityRenderState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		List<EntityRenderState.LeashState> leashes = state.leashStates;
		if (leashes == null || leashes.isEmpty()) {
			return;
		}
		for (EntityRenderState.LeashState leash : leashes) {
			leash.slack = true;
		}
		if (!entity.isPrimaryCompleted() && !entity.isPending()) {
			return;
		}
		if (leashes.size() != 1) {
			return;
		}
		applySameYSlack(entity, leashes.getFirst(), leashes);
	}

	private static void applySameYSlack(
		FenceLeadEntity entity,
		EntityRenderState.LeashState leash,
		List<EntityRenderState.LeashState> leashes
	) {
		double dy = leash.end.y - leash.start.y;
		if (Math.abs(dy) > 0.05) {
			return;
		}
		double dx = leash.end.x - leash.start.x;
		double dz = leash.end.z - leash.start.z;
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal < 0.25) {
			return;
		}
		double sag = Math.min(0.42, 0.10 + horizontal * 0.06);
		Vec3 mid = new Vec3(
			(leash.start.x + leash.end.x) * 0.5,
			(leash.start.y + leash.end.y) * 0.5 - sag,
			(leash.start.z + leash.end.z) * 0.5
		);
		EntityRenderState.LeashState second = new EntityRenderState.LeashState();
		second.offset = mid.subtract(entity.position());
		second.start = mid;
		second.end = leash.end;
		second.startBlockLight = (leash.startBlockLight + leash.endBlockLight) / 2;
		second.endBlockLight = leash.endBlockLight;
		second.startSkyLight = (leash.startSkyLight + leash.endSkyLight) / 2;
		second.endSkyLight = leash.endSkyLight;
		second.slack = true;

		leash.end = mid;
		leash.endBlockLight = second.startBlockLight;
		leash.endSkyLight = second.startSkyLight;
		leash.slack = true;
		leashes.add(second);
	}

	public static void register() {
		EntityRendererRegistry.register(VpaEntityTypes.FENCE_LEAD, FenceLeadEntityRenderer::new);
	}
}
