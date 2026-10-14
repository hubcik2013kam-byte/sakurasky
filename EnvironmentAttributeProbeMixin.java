package pl.sakurasky.mixin;

import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeProbe;
import net.minecraft.world.attribute.EnvironmentAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pl.sakurasky.Config;

/** Klient czyta kolor nieba/mgly przez probe kamery - podmieniamy wynik. */
@Mixin(EnvironmentAttributeProbe.class)
public abstract class EnvironmentAttributeProbeMixin {

	@SuppressWarnings("unchecked")
	@Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
	private <Value> void sakurasky$override(EnvironmentAttribute<Value> attr, float partialTick,
											CallbackInfoReturnable<Value> cir) {
		Config c = Config.I;
		if (c.skyEnabled && attr == EnvironmentAttributes.SKY_COLOR) {
			cir.setReturnValue((Value) (Integer) (0xFF000000 | c.skyColor));
		} else if (c.fogEnabled) {
			if (attr == EnvironmentAttributes.FOG_COLOR) {
				cir.setReturnValue((Value) (Integer) (0xFF000000 | c.fogColor));
			} else if (attr == EnvironmentAttributes.FOG_START_DISTANCE) {
				cir.setReturnValue((Value) (Float) c.fogStart);
			} else if (attr == EnvironmentAttributes.FOG_END_DISTANCE) {
				cir.setReturnValue((Value) (Float) c.fogEnd);
			}
		}
	}
}
