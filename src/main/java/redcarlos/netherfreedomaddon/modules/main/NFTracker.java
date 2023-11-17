package redcarlos.netherfreedomaddon.modules.main;

import meteordevelopment.meteorclient.systems.modules.Module;
import redcarlos.netherfreedomaddon.NFAddon;
import redcarlos.netherfreedomaddon.utils.WHhandler;

import static redcarlos.netherfreedomaddon.utils.NFUtils.getNetherrack;

public class NFTracker extends Module {

    int initialAmount;

    public NFTracker() {
        super(NFAddon.Main, "NF-tracker", "tracks the amount of netherrack mined and sends it to the discord chat.");
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
