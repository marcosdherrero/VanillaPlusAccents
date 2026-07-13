package net.berkle.vanillaplusaccents.client.render.state;

import java.util.List;

import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;

import net.berkle.vanillaplusaccents.flower.FlowerPatchOffsets;

/** Render state for stacked flower patches. */
public class FlowerPatchRenderState extends BlockEntityRenderState {

	public static final int MAX_COUNT = 4;

	public Identifier flowerId = Identifier.withDefaultNamespace("dandelion");
	public int count = 1;
	public List<FlowerPatchOffsets.Placement> placements = List.of();
	/** Terrain CUTOUT_BLOCK path — same shading as planted flowers. */
	public final MovingBlockRenderState movingBlock = new MovingBlockRenderState();
}
