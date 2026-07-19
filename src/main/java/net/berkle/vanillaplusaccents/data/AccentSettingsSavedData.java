package net.berkle.vanillaplusaccents.data;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import net.berkle.vanillaplusaccents.VanillaPlusAccentsMain;

/** World-wide operator toggles: mob grief and speed factors. */
public final class AccentSettingsSavedData extends SavedData {

	public static final Identifier DATA_ID = Identifier.fromNamespaceAndPath(VanillaPlusAccentsMain.MOD_ID, "accent_settings");

	/** Shared clamp for path, mud, and Happy Ghast speed factors (`1.0` = normal). */
	public static final double MIN_SPEED_FACTOR = 0.5;
	public static final double MAX_SPEED_FACTOR = 2.0;

	public static final double DEFAULT_PATH_SPEED = 1.5;
	public static final double DEFAULT_MUD_SPEED = 0.9;
	public static final double DEFAULT_HAPPY_GHAST_SPEED = 1.5;

	private static final Codec<AccentSettingsSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		Codec.BOOL.optionalFieldOf("EndermanGrief", true).forGetter(AccentSettingsSavedData::isEndermanGrief),
		Codec.BOOL.optionalFieldOf("CreeperGrief", true).forGetter(AccentSettingsSavedData::isCreeperGrief),
		Codec.DOUBLE.optionalFieldOf("PathSpeed").forGetter(data -> Optional.of(data.getPathSpeed())),
		// Legacy percent field (20 → 1.20); written as empty so new saves only keep PathSpeed.
		Codec.INT.optionalFieldOf("PathSpeedPercent").forGetter(data -> Optional.empty()),
		Codec.DOUBLE.optionalFieldOf("MudSpeed", DEFAULT_MUD_SPEED).forGetter(AccentSettingsSavedData::getMudSpeed),
		Codec.DOUBLE.optionalFieldOf("HappyGhastSpeed", DEFAULT_HAPPY_GHAST_SPEED).forGetter(AccentSettingsSavedData::getHappyGhastSpeed)
	).apply(instance, AccentSettingsSavedData::fromCodec));

	public static final SavedDataType<AccentSettingsSavedData> TYPE = new SavedDataType<>(
		DATA_ID,
		AccentSettingsSavedData::new,
		CODEC,
		DataFixTypes.SAVED_DATA_COMMAND_STORAGE
	);

	private boolean endermanGrief = true;
	private boolean creeperGrief = true;
	private double pathSpeed = DEFAULT_PATH_SPEED;
	private double mudSpeed = DEFAULT_MUD_SPEED;
	private double happyGhastSpeed = DEFAULT_HAPPY_GHAST_SPEED;

	private AccentSettingsSavedData() {
	}

	private AccentSettingsSavedData(
		boolean endermanGrief,
		boolean creeperGrief,
		double pathSpeed,
		double mudSpeed,
		double happyGhastSpeed
	) {
		this.endermanGrief = endermanGrief;
		this.creeperGrief = creeperGrief;
		this.pathSpeed = clampSpeedFactor(pathSpeed);
		this.mudSpeed = clampSpeedFactor(mudSpeed);
		this.happyGhastSpeed = clampSpeedFactor(happyGhastSpeed);
	}

	private static AccentSettingsSavedData fromCodec(
		boolean endermanGrief,
		boolean creeperGrief,
		Optional<Double> pathSpeed,
		Optional<Integer> legacyPathPercent,
		double mudSpeed,
		double happyGhastSpeed
	) {
		double resolvedPath = pathSpeed.orElseGet(() -> legacyPathPercent
			.map(percent -> 1.0 + percent / 100.0)
			.orElse(DEFAULT_PATH_SPEED));
		return new AccentSettingsSavedData(endermanGrief, creeperGrief, resolvedPath, mudSpeed, happyGhastSpeed);
	}

	public static AccentSettingsSavedData get(ServerLevel level) {
		ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			overworld = level;
		}
		return overworld.getDataStorage().computeIfAbsent(TYPE);
	}

	public static AccentSettingsSavedData get(Level level) {
		if (!(level instanceof ServerLevel serverLevel)) {
			throw new IllegalStateException("AccentSettingsSavedData only on server");
		}
		return get(serverLevel);
	}

	public boolean isEndermanGrief() {
		return endermanGrief;
	}

	public void setEndermanGrief(boolean endermanGrief) {
		this.endermanGrief = endermanGrief;
		setDirty();
	}

	public boolean isCreeperGrief() {
		return creeperGrief;
	}

	public void setCreeperGrief(boolean creeperGrief) {
		this.creeperGrief = creeperGrief;
		setDirty();
	}

	/** Absolute dirt-path move-speed factor (1.0 = unchanged). */
	public double getPathSpeed() {
		return pathSpeed;
	}

	public void setPathSpeed(double pathSpeed) {
		this.pathSpeed = clampSpeedFactor(pathSpeed);
		setDirty();
	}

	/** Absolute move-speed factor on mud (1.0 = unchanged). */
	public double getMudSpeed() {
		return mudSpeed;
	}

	public void setMudSpeed(double mudSpeed) {
		this.mudSpeed = clampSpeedFactor(mudSpeed);
		setDirty();
	}

	/** Absolute Happy Ghast ride-speed factor (1.0 = unchanged). */
	public double getHappyGhastSpeed() {
		return happyGhastSpeed;
	}

	public void setHappyGhastSpeed(double happyGhastSpeed) {
		this.happyGhastSpeed = clampSpeedFactor(happyGhastSpeed);
		setDirty();
	}

	public static double clampSpeedFactor(double value) {
		return Math.max(MIN_SPEED_FACTOR, Math.min(MAX_SPEED_FACTOR, value));
	}
}
