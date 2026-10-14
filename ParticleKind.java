package pl.sakurasky;

public enum ParticleKind {
	SAKURA("Sakura", "sakura"),
	SNOW("Snieg", "snow"),
	FIREFLY("Swietliki", "firefly"),
	SPARK("Iskry", "spark"),
	DUST("Kurz", "dust");

	public final String label;
	public final String texture;

	ParticleKind(String label, String texture) {
		this.label = label;
		this.texture = texture;
	}

	public static ParticleKind of(int i) {
		ParticleKind[] v = values();
		return v[Math.floorMod(i, v.length)];
	}
}
