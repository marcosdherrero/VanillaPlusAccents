package net.berkle.vanillaplusaccents.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;
import net.berkle.vanillaplusaccents.block.entity.FlowerPatchBlockEntity;
import net.berkle.vanillaplusaccents.flower.FlowerPatchBlock;

/** Mod block registration. */
public final class VpaBlocks {

	public static final Identifier FLOWER_PATCH_ID = Identifier.fromNamespaceAndPath(
		VanillaPlusAccentsMain.MOD_ID,
		"flower_patch"
	);

	public static final ResourceKey<Block> FLOWER_PATCH_KEY = ResourceKey.create(Registries.BLOCK, FLOWER_PATCH_ID);

	public static final ResourceKey<Item> FLOWER_PATCH_ITEM_KEY = ResourceKey.create(Registries.ITEM, FLOWER_PATCH_ID);

	public static FlowerPatchBlock FLOWER_PATCH;
	public static BlockEntityType<FlowerPatchBlockEntity> FLOWER_PATCH_BE;

	private VpaBlocks() {
	}

	public static void register() {
		FLOWER_PATCH = Registry.register(
			BuiltInRegistries.BLOCK,
			FLOWER_PATCH_ID,
			new FlowerPatchBlock(BlockBehaviour.Properties.of()
				.noCollision()
				.instabreak()
				.sound(SoundType.GRASS)
				.noOcclusion()
				.randomTicks()
				.setId(FLOWER_PATCH_KEY))
		);

		Registry.register(
			BuiltInRegistries.ITEM,
			FLOWER_PATCH_ID,
			new BlockItem(FLOWER_PATCH, new Item.Properties().setId(FLOWER_PATCH_ITEM_KEY))
		);

		FLOWER_PATCH_BE = Registry.register(
			BuiltInRegistries.BLOCK_ENTITY_TYPE,
			FLOWER_PATCH_ID,
			FabricBlockEntityTypeBuilder.create(FlowerPatchBlockEntity::new, FLOWER_PATCH).build()
		);
	}
}
