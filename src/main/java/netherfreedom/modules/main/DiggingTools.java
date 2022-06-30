package netherfreedom.modules.main;

import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.AutoEat;
import netherfreedom.modules.kmain.NetherBorer;
import netherfreedom.modules.kmain.InvManager;
import netherfreedom.modules.kmain.ScaffoldPlus;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import meteordevelopment.meteorclient.systems.modules.movement.SafeWalk;
import meteordevelopment.meteorclient.systems.modules.player.Rotation;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;

public class DiggingTools extends Module {

    public DiggingTools() { super(NetherFreedom.MAIN, "digging-tools", "Automatically toggles the necessary modules to dig."); }

    @Override
    public void onActivate() {
        Modules modules = Modules.get();

        modules.get(AutoEat.class).toggle();
        modules.get(AutoLog.class).toggle();
        modules.get(FreeLook.class).toggle();
        modules.get(HandManager.class).toggle();
        modules.get(InvManager.class).toggle();
        modules.get(LiquidFiller.class).toggle();
        modules.get(NetherBorer.class).toggle();
        modules.get(Rotation.class).toggle();
        modules.get(SafeWalk.class).toggle();
        modules.get(ScaffoldPlus.class).toggle();
    }

    @Override
    public void onDeactivate() {
        Modules modules = Modules.get();

        modules.get(AutoEat.class).toggle();
        modules.get(AutoLog.class).toggle();
        modules.get(FreeLook.class).toggle();
        modules.get(HandManager.class).toggle();
        modules.get(InvManager.class).toggle();
        modules.get(LiquidFiller.class).toggle();
        modules.get(NetherBorer.class).toggle();
        modules.get(Rotation.class).toggle();
        modules.get(SafeWalk.class).toggle();
        modules.get(ScaffoldPlus.class).toggle();
    }
}
