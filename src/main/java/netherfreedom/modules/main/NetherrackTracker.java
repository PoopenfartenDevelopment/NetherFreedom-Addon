package netherfreedom.modules.main;

import meteordevelopment.meteorclient.systems.modules.Module;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.utils.WHhandler;

import static netherfreedom.utils.NFUtils.getNetherrack;

public class NetherrackTracker extends Module {

    public NetherrackTracker() {
        super(NetherFreedom.MAIN, "NetherrackTracker", "tracks the amount of netherrack mined and sends it to the discord chat");
    }
    int initialAmount, finalAmount;


    @Override
    public void onActivate(){
        initialAmount = getNetherrack();
    }

    @Override
    public void onDeactivate(){
        finalAmount = getNetherrack();
        int amount = finalAmount - initialAmount;
        WHhandler.sendMessage("'''"+mc.player.getEntityName() + " mined " + amount + " blocks of netherrack this session" + "'''");
    }



}
