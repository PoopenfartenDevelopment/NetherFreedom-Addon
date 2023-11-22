package me.redcarlos.netherfreedom.modules.main;

import me.redcarlos.netherfreedom.NFAddon;
import me.redcarlos.netherfreedom.utils.WHhandler;
import meteordevelopment.meteorclient.systems.modules.Module;

import static me.redcarlos.netherfreedom.utils.NFUtils.getNetherrack;

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
