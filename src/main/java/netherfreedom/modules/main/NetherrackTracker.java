package netherfreedom.modules.main;

import meteordevelopment.meteorclient.systems.modules.Module;
import netherfreedom.NetherFreedom;
import netherfreedom.utils.WHhandler;

import static netherfreedom.utils.NFUtils.getNetherrack;

public class NetherrackTracker extends Module {

    int initialAmount;

    public NetherrackTracker() {
        super(NetherFreedom.Main, "netherrack-tracker", "tracks the amount of netherrack mined and sends it to the discord chat.");
    }

    @Override
    public void onActivate(){
        initialAmount = getNetherrack();
    }

    @Override
    public void onDeactivate() {
        if (mc.player == null) return;

        int finalAmount = getNetherrack();
        int amount = finalAmount - initialAmount;
        if (amount > 1000){
            WHhandler.sendMessage("```" + mc.player.getEntityName() + " mined " + amount + " blocks of netherrack this session" + "```");
        }
    }
}
