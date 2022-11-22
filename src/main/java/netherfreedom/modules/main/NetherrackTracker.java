package netherfreedom.modules.main;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.utils.WHhandler;

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
        int amount = initialAmount - finalAmount;
        WHhandler.sendMessage(mc.player.getEntityName() + " mined " + -amount + " blocks of netherrack this session");
    }

    private int getNetherrack(){
        Parser.Result result = Parser.parse("{player.get_stat(\"netherrack\",\"mined\")}");
        return Integer.parseInt(String.valueOf(MeteorStarscript.ss.run(Compiler.compile(result))));
    }

}
