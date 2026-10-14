package pl.sakurasky;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("sakurasky.json");
	public static Config I = new Config();

	public boolean skyEnabled = false;
	public int skyColor = 0xFFB7C5;

	public boolean fogEnabled = false;
	public int fogColor = 0xFFD9E4;
	public float fogStart = 8f;
	public float fogEnd = 96f;

	public boolean particlesEnabled = true;
	public int particleType = 0;          // indeks w ParticleKind
	public int particleColor = 0xFFB7C5;
	public int particleCount = 300;
	public float particleSize = 0.25f;    // bloki
	public float fallSpeed = 1.0f;        // mnoznik

	public void reset() {
		Config d = new Config();
		skyEnabled = d.skyEnabled; skyColor = d.skyColor;
		fogEnabled = d.fogEnabled; fogColor = d.fogColor; fogStart = d.fogStart; fogEnd = d.fogEnd;
		particlesEnabled = d.particlesEnabled; particleType = d.particleType; particleColor = d.particleColor;
		particleCount = d.particleCount; particleSize = d.particleSize; fallSpeed = d.fallSpeed;
	}

	public static void load() {
		try {
			if (Files.exists(FILE)) {
				Config c = GSON.fromJson(Files.readString(FILE), Config.class);
				if (c != null) I = c;
			}
		} catch (Exception e) {
			SakuraSkyClient.LOGGER.warn("Config load failed", e);
		}
	}

	public static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(I));
		} catch (Exception e) {
			SakuraSkyClient.LOGGER.warn("Config save failed", e);
		}
	}
}
