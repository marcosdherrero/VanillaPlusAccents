package net.berkle.vanillaplusaccents.client.render.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

/** Completed fence-to-fence ropes draw a catenary; pending ropes use vanilla leashStates. */
public class FenceLeadRenderState extends EntityRenderState {

	public boolean completed;
	public Vec3 start = Vec3.ZERO;
	public Vec3 end = Vec3.ZERO;
}
