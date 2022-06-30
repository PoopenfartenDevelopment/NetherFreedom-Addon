package netherfreedom.mixins;

import netherfreedom.modules.NetherFreedom;
import meteordevelopment.meteorclient.mixininterface.IMinecraftClient;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.util.Window;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

@Mixin(
    value = {MinecraftClient.class},
    priority = 1001
)
public abstract class MinecraftClientMixin2 implements IMinecraftClient {

    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    public void getWindowTitle(CallbackInfoReturnable<String> ci){
        String title = "NetherFreedom " + NetherFreedom.VERSION;
        ci.setReturnValue(title);
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Ljava/io/InputStream;Ljava/io/InputStream;)V"))
    public void setAlternativeWindowIcon(Window window, InputStream inputStream1, InputStream inputStream2) throws IOException {
        window.setIcon(
            NetherFreedom.class.getResourceAsStream("/assets/netherfreedom/16x.png"),
            NetherFreedom.class.getResourceAsStream("/assets/netherfreedom/32x.png")
        );
    }
}
