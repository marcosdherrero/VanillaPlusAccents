package net.berkle.vanillaplusaccents.flower;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import net.berkle.vanillaplusaccents.block.entity.FlowerPatchBlockEntity;

/** Block type backing stacked small-flower and mushroom patches. */
public class FlowerPatchBlock extends BaseEntityBlock implements BonemealableBlock {

	public static final MapCodec<FlowerPatchBlock> CODEC = simpleCodec(FlowerPatchBlock::new);
	private static final VoxelShape PLANT_SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 6.0, 14.0);
	private static final VoxelShape INTERACTION_SHAPE = Shapes.block();

	public FlowerPatchBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected MapCodec<? extends BaseEntityBlock> codec() {
		return CODEC;
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		// Chunk mesh is empty; flowers are drawn by FlowerPatchRenderer.
		return RenderShape.INVISIBLE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return PLANT_SHAPE;
	}

	@Override
	protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
		return INTERACTION_SHAPE;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new FlowerPatchBlockEntity(pos, state);
	}

	@Override
	public void playerDestroy(
		Level level,
		Player player,
		BlockPos pos,
		BlockState state,
		BlockEntity blockEntity,
		ItemStack tool
	) {
		if (blockEntity instanceof FlowerPatchBlockEntity patch) {
			Block flower = BuiltInRegistries.BLOCK.getValue(patch.getFlowerId());
			if (flower == null || !FlowerPatchSupport.canStackInPatch(flower)) {
				flower = null;
			}
			if (flower != null && level instanceof ServerLevel serverLevel) {
				Block.popResource(serverLevel, pos, new ItemStack(flower, patch.getCount()));
			}
		}
		super.playerDestroy(level, player, pos, state, blockEntity, tool);
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof FlowerPatchBlockEntity patch)) {
			return false;
		}
		return patch.getCount() < 4;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		FlowerPatchOperations.growPatch(level, pos);
	}

	/** Keep stored eyeblossom ids in sync with day/night open-close, like single flowers. */
	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof FlowerPatchBlockEntity patch)) {
			return;
		}
		Block flower = BuiltInRegistries.BLOCK.getValue(patch.getFlowerId());
		if (!FlowerPatchSupport.isEyeblossom(flower)) {
			return;
		}
		Identifier nextId = FlowerPatchSupport.resolveEyeblossomId(level, pos, patch.getFlowerId());
		if (!nextId.equals(patch.getFlowerId())) {
			patch.configure(nextId, patch.getCount());
		}
	}

	/** Wither-rose patches emit the same smoke particles as a single wither rose, per stem. */
	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (!(blockEntity instanceof FlowerPatchBlockEntity patch)) {
			return;
		}
		if (BuiltInRegistries.BLOCK.getValue(patch.getFlowerId()) != Blocks.WITHER_ROSE) {
			return;
		}

		for (FlowerPatchOffsets.Placement placement : FlowerPatchOffsets.forCount(patch.getCount(), pos).placements()) {
			for (int i = 0; i < 3; i++) {
				if (!random.nextBoolean()) {
					continue;
				}
				level.addParticle(
					ParticleTypes.SMOKE,
					pos.getX() + placement.x() + random.nextDouble() / 5.0,
					pos.getY() + 0.5 - random.nextDouble(),
					pos.getZ() + placement.z() + random.nextDouble() / 5.0,
					0.0,
					0.0,
					0.0
				);
			}
		}
	}

	@Override
	protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		if (blockEntity instanceof FlowerPatchBlockEntity patch) {
			Block flower = BuiltInRegistries.BLOCK.getValue(patch.getFlowerId());
			if (flower != null && FlowerPatchSupport.canStackInPatch(flower)) {
				return new ItemStack(flower);
			}
		}
		return super.getCloneItemStack(level, pos, state, includeData);
	}
}
