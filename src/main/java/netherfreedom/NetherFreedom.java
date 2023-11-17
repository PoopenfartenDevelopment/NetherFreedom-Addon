package netherfreedom;

import netherfreedom.modules.main.AfkLogout;
import netherfreedom.modules.main.ArmorNotify;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import netherfreedom.modules.hud.*;
import netherfreedom.modules.main.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.Category;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetherFreedom extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("NF Client");
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer("netherfreedom").orElseThrow().getMetadata();
    public static final String VERSION = METADATA.getVersion().toString();
    public static final Category Main = new Category("NF Client", Items.NETHERITE_PICKAXE.getDefaultStack());
    public static final HudGroup Hud = new HudGroup("NF Client");

	@Override
	public void onInitialize() {
        LOG.info("Initializing NF Client %s".formatted(NetherFreedom.VERSION));

        // Modules
        Modules modules = Modules.get();

        // Main
        modules.add(new AfkLogout());
        modules.add(new ArmorNotify());
        modules.add(new AutoWalkPlus());
        modules.add(new BaritoneMiner());
        modules.add(new DiggingTools());
        modules.add(new DiscordRPC());
        modules.add(new HandManager());
        modules.add(new HotbarManager());
        //modules.add(new NetherrackTracker());
        modules.add(new NFAnnouncer());
        modules.add(new NFBorer());
        modules.add(new NFRotation());
        modules.add(new NFScaffold());

        // HUD
        Hud hud = Systems.get(Hud.class);
        hud.register(NFBindsHud.INFO);
        hud.register(NFWelcomeHud.INFO);
	}

    @Override
    public String getPackage() {
        return "netherfreedom";
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Main);
    }
}
