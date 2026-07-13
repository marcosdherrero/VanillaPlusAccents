package net.berkle.vanillaplusaccents.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;
import net.berkle.vanillaplusaccents.fence.FenceLeadEntity;
import net.berkle.vanillaplusaccents.seat.SeatEntity;

/** Mod entity types. */
public final class VpaEntityTypes {

	public static final Identifier SEAT_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "seat");
	public static final Identifier FENCE_LEAD_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "fence_lead");

	public static final ResourceKey<EntityType<?>> SEAT_KEY = ResourceKey.create(Registries.ENTITY_TYPE, SEAT_ID);
	public static final ResourceKey<EntityType<?>> FENCE_LEAD_KEY = ResourceKey.create(Registries.ENTITY_TYPE, FENCE_LEAD_ID);

	public static EntityType<SeatEntity> SEAT;
	public static EntityType<FenceLeadEntity> FENCE_LEAD;

	private VpaEntityTypes() {
	}

	public static void register() {
		SEAT = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			SEAT_ID,
			EntityType.Builder.<SeatEntity>of(SeatEntity::new, MobCategory.MISC)
				.sized(0.0f, 0.0f)
				.clientTrackingRange(8)
				.updateInterval(20)
				.build(SEAT_KEY)
		);
		FENCE_LEAD = Registry.register(
			BuiltInRegistries.ENTITY_TYPE,
			FENCE_LEAD_ID,
			EntityType.Builder.<FenceLeadEntity>of(FenceLeadEntity::new, MobCategory.MISC)
				.sized(0.5f, 0.5f)
				.noSave()
				.clientTrackingRange(64)
				.updateInterval(10)
				.build(FENCE_LEAD_KEY)
		);
	}
}
