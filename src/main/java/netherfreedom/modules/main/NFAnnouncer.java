package netherfreedom.modules.main;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;
import netherfreedom.NetherFreedom;
import netherfreedom.utils.NFUtils;

public class NFAnnouncer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("delay in ticks between message sends")
            .defaultValue(10)
            .range(1,100)
            .sliderRange(1,100)
            .build()
    );

    private int initialCount;
    private int ticks;

    public NFAnnouncer() {
        super(NetherFreedom.Main, "NF-announcer", "Sends the amount of netherrack blocks you've broken in chat.");
    }

    @Override
    public void onActivate(){
        initialCount = NFUtils.getNetherrack();
        ticks = delay.get();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        int count = NFUtils.getNetherrack() - initialCount;
        if (ticks == 0 && count != 0) {
            mc.player.sendMessage(Text.of("I just broke " + count + " blocks of netherrack with the power of NetherFreedom client!"));
            initialCount = count;
        }
        ticks--;
    }
}
