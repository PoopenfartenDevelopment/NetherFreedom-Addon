package netherfreedom.modules;

import netherfreedom.commands.*;
import netherfreedom.modules.hud.*;
import netherfreedom.modules.main.*;
import netherfreedom.modules.kmain.*;
import netherfreedom.utils.NFDamageUtils;
import netherfreedom.utils.PacketFlyUtils;
import netherfreedom.utils.ServiceLoader;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.HUD;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.commands.Commands;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static netherfreedom.AdapterKt.*;

public class NetherFreedom extends MeteorAddon {
	public static final Logger LOG = LoggerFactory.getLogger("NF Client");
    public static final String VERSION = "1.0";
    public static final Category MAIN = new Category("NF Client", Items.NETHERITE_PICKAXE.getDefaultStack());

	@Override
	public void onInitialize() {
	    LOG.info("Initializing NF Client");

		MeteorClient.EVENT_BUS.registerLambdaFactory("netherfreedom", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        NFDamageUtils.init();
        ServiceLoader.load();
        PacketFlyUtils.init();
        setCs2Ps();

        // Modules
        Modules modules = Modules.get();

        // Main (Java)
        modules.add(new AfkLogout());
        modules.add(new ArmorNotify());
        modules.add(new Aura());
        modules.add(new AutoDisable());
        modules.add(new ChatTweaks());
        modules.add(new DiggingTools());
        modules.add(new DiscordRPC());
        modules.add(new HandManager());
        modules.add(new RotationsPlus());
        modules.add(new Strafe());
        modules.add(new TPSSync());
        // Main (Kotlin)
        modules.add(new NetherBorer());
        modules.add(new InvManager());
        modules.add(new NoCaveCulling());
        modules.add(AutoEat.INSTANCE);
        modules.add(ScaffoldPlus.INSTANCE);

        // Commands
        Commands commands = Commands.get();
        commands.add(new ClearChat());
        commands.add(new Disconnect());
        commands.add(new ToggleAll());

        // HUD
        HUD hud = Systems.get(HUD.class);
        hud.elements.add(new BaritoneHud(hud));
        hud.elements.add(new BindsHud(hud));
        hud.elements.add(new EchestHud(hud));
        hud.elements.add(new GapHud(hud));
        hud.elements.add(new NFWelcomeHud(hud));
        hud.elements.add(new ObbyHud(hud));
        hud.elements.add(new PickHud(hud));
        hud.elements.add(new SpotifyHud(hud));
        hud.elements.add(new XpHud(hud));
	}

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(MAIN);
    }
}
