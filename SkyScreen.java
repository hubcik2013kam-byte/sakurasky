package pl.sakurasky.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import pl.sakurasky.Config;
import pl.sakurasky.ParticleKind;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class SkyScreen extends Screen {
	private static final int W = 150, H = 20, GAP = 22;
	private static int lastTab = 0;
	private int tab = lastTab;

	/** Pole koloru: hex + 3 suwaki + podglad, zsynchronizowane. */
	private class ColorEditor {
		final IntSupplier get; final IntConsumer set;
		final EditBox hex; final ValueSlider[] sl = new ValueSlider[3];
		final int x, y; boolean syncing = false;

		ColorEditor(int x, int y, String name, IntSupplier get, IntConsumer set) {
			this.x = x; this.y = y; this.get = get; this.set = set;
			hex = new EditBox(font, x, y + GAP, W, H, Component.literal("hex"));
			hex.setMaxLength(7);
			hex.setValue(String.format("#%06X", get.getAsInt()));
			hex.setResponder(s -> {
				if (syncing) return;
				String t = s.startsWith("#") ? s.substring(1) : s;
				if (t.length() != 6) return;
				try {
					int v = Integer.parseInt(t, 16);
					set.accept(v);
					syncing = true;
					for (int i = 0; i < 3; i++) sl[i].sync((v >> (16 - 8 * i)) & 255);
					syncing = false;
				} catch (NumberFormatException ignored) { }
			});
			addRenderableWidget(hex);
			String[] ch = {"R", "G", "B"};
			for (int i = 0; i < 3; i++) {
				final int shift = 16 - 8 * i;
				sl[i] = new ValueSlider(x, y + GAP * (2 + i), W, H, name + " " + ch[i], 0, 255,
						(get.getAsInt() >> shift) & 255, v -> {
					int c = (get.getAsInt() & ~(255 << shift)) | ((int) v << shift);
					set.accept(c);
					if (!syncing) {
						syncing = true;
						hex.setValue(String.format("#%06X", c));
						syncing = false;
					}
				});
				addRenderableWidget(sl[i]);
			}
		}

		void renderSwatch(GuiGraphics g) {
			g.fill(x, y, x + W, y + H, 0xFF000000 | get.getAsInt());
			g.drawCenteredString(font, Component.literal("podglad"), x + W / 2, y + 6, 0xFFFFFFFF);
		}
	}

	private final List<ColorEditor> editors = new ArrayList<>();

	public SkyScreen() { super(Component.literal("Sakura Sky")); }

	@Override
	protected void init() {
		editors.clear();
		Config c = Config.I;
		int cx = this.width / 2;
		int tabW = 90;
		String[] names = {"Niebo", "Mgla", "Czasteczki"};
		for (int i = 0; i < 3; i++) {
			final int idx = i;
			Button b = Button.builder(Component.literal((i == tab ? "> " : "") + names[i]), x -> {
				lastTab = idx; tab = idx; rebuildWidgets();
			}).bounds(cx - (tabW * 3 + 8) / 2 + i * (tabW + 4), 14, tabW, H).build();
			addRenderableWidget(b);
		}

		int left = cx - W - 6, right = cx + 6, top = 44;

		if (tab == 0) {
			addRenderableWidget(toggle(cx - W / 2, top, "Kolor nieba", c.skyEnabled, v -> c.skyEnabled = v));
			editors.add(new ColorEditor(cx - W / 2, top + GAP, "Niebo", () -> c.skyColor, v -> c.skyColor = v));
		} else if (tab == 1) {
			addRenderableWidget(toggle(left, top, "Mgla", c.fogEnabled, v -> c.fogEnabled = v));
			addRenderableWidget(new ValueSlider(left, top + GAP, W, H, "Poczatek", 0, 200, c.fogStart, v -> {
				c.fogStart = (float) v;
				if (c.fogEnd < c.fogStart + 1) c.fogEnd = c.fogStart + 1;
			}));
			addRenderableWidget(new ValueSlider(left, top + GAP * 2, W, H, "Koniec", 1, 400, c.fogEnd, v ->
					c.fogEnd = (float) Math.max(v, c.fogStart + 1)));
			editors.add(new ColorEditor(right, top, "Mgla", () -> c.fogColor, v -> c.fogColor = v));
		} else {
			addRenderableWidget(toggle(left, top, "Czasteczki", c.particlesEnabled, v -> c.particlesEnabled = v));
			addRenderableWidget(Button.builder(Component.literal("Typ: " + ParticleKind.of(c.particleType).label), b -> {
				c.particleType = (c.particleType + 1) % ParticleKind.values().length;
				b.setMessage(Component.literal("Typ: " + ParticleKind.of(c.particleType).label));
			}).bounds(left, top + GAP, W, H).build());
			addRenderableWidget(new ValueSlider(left, top + GAP * 2, W, H, "Ilosc", 10, 1500, c.particleCount,
					v -> c.particleCount = (int) v));
			addRenderableWidget(new ValueSlider(left, top + GAP * 3, W, H, "Rozmiar %", 5, 150, c.particleSize * 100,
					v -> c.particleSize = (float) v / 100f));
			addRenderableWidget(new ValueSlider(left, top + GAP * 4, W, H, "Predkosc %", 0, 300, c.fallSpeed * 100,
					v -> c.fallSpeed = (float) v / 100f));
			editors.add(new ColorEditor(right, top, "Kolor", () -> c.particleColor, v -> c.particleColor = v));
		}

		int bottom = this.height - 30;
		addRenderableWidget(Button.builder(Component.literal("Reset do domyslnych"), b -> {
			Config.I.reset(); rebuildWidgets();
		}).bounds(left, bottom, W, H).build());
		addRenderableWidget(Button.builder(Component.literal("Gotowe"), b -> onClose())
				.bounds(right, bottom, W, H).build());
	}

	private Button toggle(int x, int y, String label, boolean on, java.util.function.Consumer<Boolean> set) {
		boolean[] st = {on};
		return Button.builder(txt(label, on), b -> {
			st[0] = !st[0]; set.accept(st[0]); b.setMessage(txt(label, st[0]));
		}).bounds(x, y, W, H).build();
	}

	private static Component txt(String l, boolean on) {
		return Component.literal(l + ": " + (on ? "WL" : "wyl"));
	}

	@Override
	public void render(GuiGraphics g, int mx, int my, float delta) {
		super.render(g, mx, my, delta);
		for (ColorEditor e : editors) e.renderSwatch(g);
	}

	@Override
	public void removed() { Config.save(); }

	private static class ValueSlider extends AbstractSliderButton {
		private final String label; private final double min, max; private final DoubleConsumer onChange;

		ValueSlider(int x, int y, int w, int h, String label, double min, double max, double cur, DoubleConsumer cb) {
			super(x, y, w, h, Component.empty(), (cur - min) / (max - min));
			this.label = label; this.min = min; this.max = max; this.onChange = cb;
			updateMessage();
		}

		void sync(double v) { this.value = (v - min) / (max - min); updateMessage(); }

		@Override protected void updateMessage() {
			if (label == null) return;
			setMessage(Component.literal(label + ": " + Math.round(min + (max - min) * this.value)));
		}

		@Override protected void applyValue() { onChange.accept(min + (max - min) * this.value); }
	}
}
