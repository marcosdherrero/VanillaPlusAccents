package net.berkle.vanillaplusaccents.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;
import net.berkle.vanillaplusaccents.data.AccentSettingsSavedData;

/** Operator commands for grief toggles and ground-speed settings. */
public final class ModCommands {

	private ModCommands() {
	}

	public static void register(
		CommandDispatcher<CommandSourceStack> dispatcher,
		CommandBuildContext registryAccess,
		Commands.CommandSelection environment
	) {
		LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(VanillaPlusAccentsMain.COMMAND_ROOT)
			.then(help())
			.then(toggleBool("enderman_grief", AccentSettingsSavedData::isEndermanGrief, AccentSettingsSavedData::setEndermanGrief,
				"Enderman griefing"))
			.then(toggleBool("creeper_grief", AccentSettingsSavedData::isCreeperGrief, AccentSettingsSavedData::setCreeperGrief,
				"Creeper griefing"))
			.then(pathSpeed())
			.then(mudSpeed())
			.then(happyGhastSpeed());

		var registered = dispatcher.register(root);
		dispatcher.register(Commands.literal("vpa").redirect(registered));
	}

	private static LiteralArgumentBuilder<CommandSourceStack> help() {
		return Commands.literal("help").executes(ctx -> {
			ctx.getSource().sendSuccess(() -> Component.literal(
				"/" + VanillaPlusAccentsMain.COMMAND_ROOT + " (alias /vpa) — operator settings\n"
					+ "  enderman_grief [true|false] — omit to flip; false blocks pickup/place\n"
					+ "  creeper_grief [true|false] — omit to flip; false keeps damage, no block break\n"
					+ "  path_speed [" + AccentSettingsSavedData.MIN_SPEED_FACTOR + "-"
					+ AccentSettingsSavedData.MAX_SPEED_FACTOR + "] — dirt-path factor (1.0 = normal; default "
					+ AccentSettingsSavedData.DEFAULT_PATH_SPEED + "; also steps up full blocks)\n"
					+ "  mud_speed [" + AccentSettingsSavedData.MIN_SPEED_FACTOR + "-"
					+ AccentSettingsSavedData.MAX_SPEED_FACTOR + "] — mud factor (1.0 = normal; default "
					+ AccentSettingsSavedData.DEFAULT_MUD_SPEED + ")\n"
					+ "  happy_ghast_speed [" + AccentSettingsSavedData.MIN_SPEED_FACTOR + "-"
					+ AccentSettingsSavedData.MAX_SPEED_FACTOR + "] — ridden Happy Ghast factor (1.0 = normal; default "
					+ AccentSettingsSavedData.DEFAULT_HAPPY_GHAST_SPEED + ")"
			), false);
			return 1;
		});
	}

	@FunctionalInterface
	private interface BoolGetter {
		boolean get(AccentSettingsSavedData data);
	}

	@FunctionalInterface
	private interface BoolSetter {
		void set(AccentSettingsSavedData data, boolean value);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> toggleBool(String name, BoolGetter getter, BoolSetter setter, String label) {
		return Commands.literal(name)
			.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.executes(ctx -> {
				AccentSettingsSavedData data = AccentSettingsSavedData.get(ctx.getSource().getLevel());
				return setBool(ctx, setter, label, !getter.get(data));
			})
			.then(Commands.literal("true").executes(ctx -> setBool(ctx, setter, label, true)))
			.then(Commands.literal("false").executes(ctx -> setBool(ctx, setter, label, false)));
	}

	private static int setBool(CommandContext<CommandSourceStack> ctx, BoolSetter setter, String label, boolean value) {
		AccentSettingsSavedData data = AccentSettingsSavedData.get(ctx.getSource().getLevel());
		setter.set(data, value);
		ctx.getSource().sendSuccess(
			() -> Component.literal(label + ": " + (value ? "ON (vanilla)" : "OFF")),
			true
		);
		return 1;
	}

	private static LiteralArgumentBuilder<CommandSourceStack> pathSpeed() {
		return speedCommand(
			"path_speed",
			AccentSettingsSavedData::getPathSpeed,
			AccentSettingsSavedData::setPathSpeed,
			AccentSettingsSavedData.DEFAULT_PATH_SPEED,
			"Dirt path speed",
			" (also steps up full blocks like slabs while on path)"
		);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> mudSpeed() {
		return speedCommand(
			"mud_speed",
			AccentSettingsSavedData::getMudSpeed,
			AccentSettingsSavedData::setMudSpeed,
			AccentSettingsSavedData.DEFAULT_MUD_SPEED,
			"Mud speed",
			""
		);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> happyGhastSpeed() {
		return speedCommand(
			"happy_ghast_speed",
			AccentSettingsSavedData::getHappyGhastSpeed,
			AccentSettingsSavedData::setHappyGhastSpeed,
			AccentSettingsSavedData.DEFAULT_HAPPY_GHAST_SPEED,
			"Happy Ghast ride speed",
			""
		);
	}

	@FunctionalInterface
	private interface SpeedGetter {
		double get(AccentSettingsSavedData data);
	}

	@FunctionalInterface
	private interface SpeedSetter {
		void set(AccentSettingsSavedData data, double value);
	}

	private static LiteralArgumentBuilder<CommandSourceStack> speedCommand(
		String name,
		SpeedGetter getter,
		SpeedSetter setter,
		double defaultFactor,
		String label,
		String extraNote
	) {
		return Commands.literal(name)
			.requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
			.executes(ctx -> {
				AccentSettingsSavedData data = AccentSettingsSavedData.get(ctx.getSource().getLevel());
				double factor = getter.get(data);
				ctx.getSource().sendSuccess(
					() -> Component.literal(
						label + ": " + formatFactor(factor)
							+ " (1.0 = normal; default " + formatFactor(defaultFactor)
							+ "; clamp " + AccentSettingsSavedData.MIN_SPEED_FACTOR
							+ "-" + AccentSettingsSavedData.MAX_SPEED_FACTOR + ")"
							+ extraNote
					),
					false
				);
				return (int) Math.round(factor * 100.0);
			})
			.then(
				Commands.argument(
					"factor",
					DoubleArgumentType.doubleArg(
						AccentSettingsSavedData.MIN_SPEED_FACTOR,
						AccentSettingsSavedData.MAX_SPEED_FACTOR
					)
				).executes(ctx -> {
					double factor = DoubleArgumentType.getDouble(ctx, "factor");
					AccentSettingsSavedData data = AccentSettingsSavedData.get(ctx.getSource().getLevel());
					setter.set(data, factor);
					ctx.getSource().sendSuccess(
						() -> Component.literal(label + " set to " + formatFactor(getter.get(data)) + "."),
						true
					);
					return 1;
				})
			);
	}

	private static String formatFactor(double factor) {
		if (Math.abs(factor - Math.rint(factor)) < 1.0e-6) {
			return Integer.toString((int) Math.rint(factor));
		}
		return String.format(java.util.Locale.ROOT, "%.2f", factor);
	}
}
