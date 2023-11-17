package redcarlos.netherfreedomaddon.modules.main;

import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import meteordevelopment.meteorclient.systems.modules.movement.SafeWalk;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.Items;
import redcarlos.netherfreedomaddon.NFAddon;

import java.util.List;

public class DiggingTools extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables DiggingTools when you leave a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("use-baritone")
        .description("Use baritone to automate the digging process (EXPERIMENTAL).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pickToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("pickaxe-toggle")
        .description("Automatically disables DiggingTools when you run out of pickaxes.")
        .defaultValue(true)
        .visible(() -> !useBaritone.get())
        .build()
    );

    private final List<Class<? extends Module>> commonClasses = List.of(
        AutoLog.class,
        HandManager.class,
        HotbarManager.class,
        LiquidFiller.class,
        NFBorer.class,
        NFScaffold.class
    );

    private final List<Class<? extends Module>> noBaritoneClasses = List.of(
        FreeLook.class,
        NFRotation.class,
        SafeWalk.class
    );

    public DiggingTools() {
        super(NFAddon.Main, "digging-tools", "Automatically toggles the necessary modules to dig.");
    }

    @Override
    public void onActivate() {
        Modules modules = Modules.get();

        if (useBaritone.get()) {
            modules.get(BaritoneMiner.class).toggle();
        } else noBaritoneClasses.forEach(moduleClass -> modules.get(moduleClass).toggle());
        commonClasses.forEach(moduleClass -> modules.get(moduleClass).toggle());
    }

    @Override
    public void onDeactivate() {
        Modules modules = Modules.get();

        if (modules.get(BaritoneMiner.class).isActive()) modules.get(BaritoneMiner.class).toggle();
        noBaritoneClasses.stream().filter(moduleClass -> modules.get(moduleClass).isActive());
        commonClasses.stream().filter(moduleClass -> modules.get(moduleClass).isActive());
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (pickToggle.get() && !useBaritone.get()) {
            FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);

            if (!pickaxe.found()) {
                error("No pickaxe found, disabling.");
                toggle();
            }
        }
    }
}
