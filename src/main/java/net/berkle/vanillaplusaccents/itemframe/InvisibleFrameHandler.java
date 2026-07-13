package net.berkle.vanillaplusaccents.itemframe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

import net.berkle.vanillaplusaccents.accessor.ItemFrameEntityAccess;

/** Shears toggle item frame backing visibility. */
public final class InvisibleFrameHandler {

	private InvisibleFrameHandler() {
	}

	public static InteractionResult onUseEntity(
		Player player,
		Level level,
		InteractionHand hand,
		net.minecraft.world.entity.Entity entity,
		EntityHitResult hitResult
	) {
		if (!(level instanceof ServerLevel) || hand != InteractionHand.MAIN_HAND) {
			return InteractionResult.PASS;
		}
		if (!(entity instanceof ItemFrame frame)) {
			return InteractionResult.PASS;
		}
		if (!player.getItemInHand(hand).is(Items.SHEARS)) {
			return InteractionResult.PASS;
		}

		ItemFrameEntityAccess access = (ItemFrameEntityAccess) frame;
		boolean next = !access.vpa$isFrameInvisible();
		access.vpa$setFrameInvisible(next);

		level.playSound(
			null,
			frame.blockPosition(),
			SoundEvents.SHEEP_SHEAR,
			SoundSource.BLOCKS,
			1.0f,
			1.0f
		);

		if (!player.getAbilities().instabuild) {
			player.getItemInHand(hand).hurtAndBreak(1, player, hand.asEquipmentSlot());
		}

		return InteractionResult.SUCCESS;
	}
}
