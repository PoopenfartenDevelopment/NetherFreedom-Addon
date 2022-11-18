package netherfreedom.modules;

import netherfreedom.commands.*;
import netherfreedom.modules.hud.*;
import netherfreedom.modules.main.*;
import netherfreedom.modules.kmain.*;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.commands.Commands;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static netherfreedom.AdapterKt.*;

public class NetherFreedom extends MeteorAddon {
    public static String VERSION = "1.2";
	public static final Logger LOG = LoggerFactory.getLogger("NF Client");
    public static final Category MAIN = new Category("NF Client", Items.NETHERITE_PICKAXE.getDefaultStack());
    public static final HudGroup HUD = new HudGroup("NF Client");

	@Override
	public void onInitialize() {
	    LOG.info("Initializing NF Client");

		MeteorClient.EVENT_BUS.registerLambdaFactory("netherfreedom", (lookupInMethod, klass) -> (MethodHandles.Lookup) lookupInMethod.invoke(null, klass, MethodHandles.lookup()));

        setCs2Ps();

        // Modules
        Modules modules = Modules.get();

        // Main (Java)
        modules.add(new AfkLogout());
        modules.add(new ArmorNotify());
        modules.add(new AutoWalkPlus());
        modules.add(new BaritoneScript());
        modules.add(new BaritoneMinerRewrite());
        modules.add(new ChatTweaks());
        modules.add(new DiggingTools());
        modules.add(new DiscordRPC());
        modules.add(new HandManager());
        modules.add(new RotationsPlus());
        // Main (Kotlin)
        modules.add(new HotbarManager());
        modules.add(new NoCaveCulling());
        modules.add(new NFNuker());
        modules.add(AutoEatPlus.INSTANCE);
        modules.add(ScaffoldPlus.INSTANCE);

        // Commands
        Commands commands = Commands.get();
        commands.add(new Disconnect());

        // HUD
        Hud hud = Systems.get(Hud.class);
        hud.register(BindsHud.INFO);
        hud.register(NFWelcomeHud.INFO);
	}

    @Override
    public String getPackage() {
        return "netherfreedom";
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(MAIN);
    }
}
