package me.redcarlos.netherfreedom.mixins;

import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import me.redcarlos.netherfreedom.NFAddon;

@Mixin(
    value = {MinecraftClient.class},
    priority = 1001
)
public abstract class NFWindowTitleMixin implements IMinecraftClient {

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void getWindowTitle(CallbackInfoReturnable<String> ci) {
        String title = "NetherFreedom " + NFAddon.VERSION;
        ci.setReturnValue(title);
    }
}
