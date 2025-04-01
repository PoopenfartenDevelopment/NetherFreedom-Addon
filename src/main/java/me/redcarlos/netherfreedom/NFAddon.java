package me.redcarlos.netherfreedom;

import com.mojang.logging.LogUtils;
import me.redcarlos.netherfreedom.modules.hud.NFWelcomeHud;
import me.redcarlos.netherfreedom.modules.main.*;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.pathing.BaritoneUtils;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class NFAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final ModMetadata METADATA = FabricLoader.getInstance().getModContainer("nfaddon").orElseThrow().getMetadata();
    public static final String VERSION = METADATA.getVersion().toString();
    public static final Category Main = new Category("NF Addon", Items.NETHERITE_PICKAXE.getDefaultStack());
    public static final HudGroup Hud = new HudGroup("NF Addon");

	@Override
	public void onInitialize() {
        LOG.info("Initializing NF Addon " + NFAddon.VERSION);

        // Modules
        Modules modules = Modules.get();

        // Main
        modules.add(new AutoWalkNF());

        modules.add(new DiggingTools());
        modules.add(new HandManager());
        modules.add(new HotbarManager());
        //modules.add(new NetherrackTracker());
        modules.add(new NetherBorer());
        modules.add(new NFRotation());
        modules.add(new NFScaffold());

        // HUD
        Hud hud = Systems.get(Hud.class);
        hud.register(NFWelcomeHud.INFO);

        if (BaritoneUtils.IS_AVAILABLE) {
            modules.add(new BaritoneMiner());
        }
	}

    @Override
    public String getPackage() {
        return "me.redcarlos.netherfreedom";
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Main);
    }
}
