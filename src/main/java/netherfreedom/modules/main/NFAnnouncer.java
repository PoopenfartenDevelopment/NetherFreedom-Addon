package netherfreedom.modules.main;

import meteordevelopment.meteorclient.events.entity.player.BreakBlockEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import netherfreedom.modules.NetherFreedom;

public class NFAnnouncer extends Module {


    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("delay")
            .description("delay in seconds between message sends (assumes 20 tps)")
            .defaultValue(10)
            .range(1,100)
            .sliderRange(1,100)
            .build()
    );

    public NFAnnouncer() {
        super(NetherFreedom.MAIN, "NFAnnouncer", "sends the amount of netherrack blocks you've broken in chat");
    }

    private int count, ticks;
    private int delayTicks = delay.get() * 20;

    @Override
    public void onActivate(){
        count = 0;
        ticks = delayTicks;
    }

    @EventHandler
    public void onBreakBlock(BreakBlockEvent event){
        Block block = event.getBlockState(mc.world).getBlock();
        if (block == Blocks.NETHERRACK){
            count++;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event){
        if (ticks == 0 && count != 0) {
            mc.player.sendChatMessage("I just broke " + count + " blocks of netherrack With the power of NetherFreedom client", null);
            ticks = delayTicks;
            count = 0;
        }
        ticks--;
    }
}
