package redcarlos.netherfreedomaddon;

import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.item.Items;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redcarlos.netherfreedomaddon.modules.hud.NFBindsHud;
import redcarlos.netherfreedomaddon.modules.hud.NFWelcomeHud;
import redcarlos.netherfreedomaddon.modules.main.*;

public class NFAddon extends MeteorAddon {
    public static final Logger LOG = LoggerFactory.getLogger("NFAddon");
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer("redcarlos.netherfreedomaddon").orElseThrow().getMetadata();
    public static final String VERSION = METADATA.getVersion().toString();
    public static final Category Main = new Category("NF Addon", Items.NETHERITE_PICKAXE.getDefaultStack());
    public static final HudGroup Hud = new HudGroup("NF Addon");

	@Override
	public void onInitialize() {
        LOG.info("Initializing NF Addon %s".formatted(NFAddon.VERSION));

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
        return "redcarlos.netherfreedomaddon";
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Main);
    }
}
