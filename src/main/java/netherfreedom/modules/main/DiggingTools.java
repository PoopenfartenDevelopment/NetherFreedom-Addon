package netherfreedom.modules.main;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.AutoEatPlus;
import netherfreedom.modules.kmain.NFNuker;
import netherfreedom.modules.kmain.HotbarManager;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;

public class DiggingTools extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables DiggingTools when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> discordIntegrate = sgGeneral.add(new BoolSetting.Builder()
        .name("discord-integrate")
        .description("sends digging info to discord channel")
        .defaultValue(true)
        .build()
    );

    public DiggingTools() { super(NetherFreedom.MAIN, "digging-tools", "Automatically toggles the necessary modules to dig."); }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnDisconnect.get()) toggle();
    }

    @Override
    public void onActivate() {
        Modules modules = Modules.get();

        modules.get(AutoEatPlus.class).toggle();
        modules.get(AutoLog.class).toggle();
        modules.get(HandManager.class).toggle();
        modules.get(HotbarManager.class).toggle();
        modules.get(LiquidFiller.class).toggle();
        modules.get(NFNuker.class).toggle();
    }

    @Override
    public void onDeactivate() {
        Modules modules = Modules.get();

        if (modules.get(AutoEatPlus.class).isActive())
            modules.get(AutoEatPlus.class).toggle();
        if (modules.get(AutoLog.class).isActive())
            modules.get(AutoLog.class).toggle();
        if (modules.get(HandManager.class).isActive())
            modules.get(HandManager.class).toggle();
        if (modules.get(HotbarManager.class).isActive())
            modules.get(HotbarManager.class).toggle();
        if (modules.get(LiquidFiller.class).isActive())
            modules.get(LiquidFiller.class).toggle();
        if (modules.get(NFNuker.class).isActive())
            modules.get(NFNuker.class).toggle();
    }
}
