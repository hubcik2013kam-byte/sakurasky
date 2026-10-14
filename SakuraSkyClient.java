package pl.sakurasky;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.sakurasky.gui.SkyScreen;

public class SakuraSkyClient implements ClientModInitializer {
	public static final String MOD_ID = "sakurasky";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static KeyMapping openKey;

	@Override
	public void onInitializeClient() {
		Config.load();

		KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
		openKey = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.sakurasky.open_menu", InputConstants.Type.KEYSYM, InputConstants.KEY_K, cat));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (openKey.consumeClick()) {
				if (client.screen == null) client.setScreen(new SkyScreen());
			}
		});

		ParticleLayer layer = new ParticleLayer();
		WorldRenderEvents.BEFORE_TRANSLUCENT.register(layer::render);
		ClientLifecycleEvents.CLIENT_STOPPING.register(c -> layer.close());
	}
}
