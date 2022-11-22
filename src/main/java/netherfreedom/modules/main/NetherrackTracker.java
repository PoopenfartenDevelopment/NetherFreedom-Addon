package netherfreedom.modules.main;

import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import netherfreedom.modules.NetherFreedom;

public class NetherrackTracker extends Module {

    public NetherrackTracker() {
        super(NetherFreedom.MAIN, "NetherrackTracker", "tracks the amount of netherrack mined");
    }
    int initialAmount, finalAmount;

    @Override
    public void onActivate(){
        initialAmount = getNetherrack();
    }

    @Override
    public void onDeactivate(){
        finalAmount = getNetherrack();
        int amount = initialAmount -finalAmount;
        info("mined" + amount + "obsidian");
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {

    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {

    }

    private int getNetherrack(){
        Parser.Result result = Parser.parse("{player.get_stat(\"netherrack\",\"mined\")}");
        return Integer.parseInt(String.valueOf(MeteorStarscript.ss.run(Compiler.compile(result))));
    }

}
