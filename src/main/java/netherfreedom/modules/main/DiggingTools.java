package netherfreedom.modules.main;

import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.AutoEatPlus;
import netherfreedom.modules.kmain.NFNuker;
import netherfreedom.modules.kmain.InvManager;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;

public class DiggingTools extends Module {

    public DiggingTools() { super(NetherFreedom.MAIN, "digging-tools", "Automatically toggles the necessary modules to dig."); }

    @Override
    public void onActivate() {
        Modules modules = Modules.get();

        modules.get(AutoEatPlus.class).toggle();
        modules.get(AutoLog.class).toggle();
        modules.get(HandManager.class).toggle();
        modules.get(InvManager.class).toggle();
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
        if (modules.get(InvManager.class).isActive())
            modules.get(InvManager.class).toggle();
        if (modules.get(LiquidFiller.class).isActive())
            modules.get(LiquidFiller.class).toggle();
        if (modules.get(NFNuker.class).isActive())
            modules.get(NFNuker.class).toggle();
    }
}
