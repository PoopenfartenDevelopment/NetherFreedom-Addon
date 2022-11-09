package netherfreedom.modules.main;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.process.ICustomGoalProcess;
import baritone.api.process.IMineProcess;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.NFNuker;


public class BaritoneTest extends Module {

    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final Settings baritoneSettings = BaritoneAPI.getSettings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> corner1X = sgGeneral.add(new IntSetting.Builder()
            .name("corner1X")
            .description("the first corner of the square where it will mine")
            .defaultValue(0)
            .range(-29999983, 29999983)
            .sliderRange(-29999983, 29999983)
            .build()
    );


    private final Setting<Integer> corner1Z = sgGeneral.add(new IntSetting.Builder()
            .name("corner1Z")
            .description("the first corner of the square where it will mine")
            .defaultValue(0)
            .range(-29999983, 29999983)
            .sliderRange(-29999983, 29999983)
            .build()
    );

    private final Setting<Integer> corner2X = sgGeneral.add(new IntSetting.Builder()
            .name("corner2X")
            .description("the second corner of the square where it will mine")
            .defaultValue(0)
            .range(-29999983, 29999983)
            .sliderRange(-29999983, 29999983)
            .build()
    );

    private final Setting<Integer> corner2Z = sgGeneral.add(new IntSetting.Builder()
            .name("corner2Z")
            .description("the second corner of the square where it will mine")
            .defaultValue(0)
            .range(-29999983, 29999983)
            .sliderRange(-29999983, 29999983)
            .build()
    );
     private final Setting<Integer> nukerRange = sgGeneral.add(new IntSetting.Builder()
            .name("nukerRange")
            .description("the first corner of the square where it will mine")
            .defaultValue(0)
            .range(0,6)
            .sliderRange(0,6)
            .build()
    );

    private final Setting<Integer> yLevel = sgGeneral.add(new IntSetting.Builder()
            .name("ylevel")
            .description("the first corner of the square where it will mine")
            .defaultValue(0)
            .range(-256,256)
            .sliderRange(-256,256)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("don't use this")
            .defaultValue(true)
            .build()
    );

    public BaritoneTest() {super(NetherFreedom.MAIN, "Baritone miner", "mines shit");}
    //ask carlos for the y pos
    public BlockPos cornerOne, cornerTwo;
    private BlockPos iterativePos;
    Modules modules = Modules.get();


    @Override
    public void onActivate(){
        cornerOne = new BlockPos(corner1X.get(), yLevel.get(), corner1Z.get());
        cornerTwo = new BlockPos(corner2X.get(), yLevel.get(), corner2Z.get());

        //heading to the starting position
        info("cornerOne: " + cornerOne);
        info("cornerTwo: " + cornerTwo);
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(cornerOne));

        baritoneSettings.blockPlacementPenalty.value = 0.5;
        baritoneSettings.allowPlace.value = false;
    }

    @Override
    public void onDeactivate(){
        baritone.getPathingBehavior().cancelEverything();

    }

    @EventHandler
    public void onTick(){
        BlockPos currPlayerPos = mc.player.getBlockPos();
        //checks if the starting position has been reached and turns on the necessary modules

        if(cornerOne == currPlayerPos) {
            activateDiggingModules();
        }

        BlockPos endOfLine = new BlockPos(cornerOne.getX(),yLevel.get(),cornerTwo.getZ());
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(endOfLine));

    }

    private void activateDiggingModules (){
        if (modules.get(LiquidFiller.class).isActive())
            modules.get(LiquidFiller.class).toggle();
        if (modules.get(NFNuker.class).isActive())
            modules.get(NFNuker.class).toggle();
    }


}
