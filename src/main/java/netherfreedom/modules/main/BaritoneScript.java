package netherfreedom.modules.main;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.NFNuker;


public class BaritoneScript extends Module {

    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final Settings baritoneSettings = BaritoneAPI.getSettings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum debugs{
        None,
        baritonePathing,
        whatItDO
    }

    //todo: find a better fucking way to get coords as settings
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

    private final Setting<Boolean> renderCorners = sgGeneral.add(new BoolSetting.Builder()
            .name("renderCorners")
            .description("renders the 2 corners")
            .defaultValue(true)
            .build()
    );

    private final Setting<debugs> debug = sgGeneral.add(new EnumSetting.Builder<debugs>()
            .name("debug")
            .description("don't use this")
            .defaultValue(debugs.None)
            .build()
    );

    public BaritoneScript() {super(NetherFreedom.MAIN, "Baritone miner", "mines shit");}
    //ask carlos for the y pos
    public BlockPos cornerOne, cornerTwo, cornerThree, cornerFour;
    private BlockPos iterativePos, endOfLine;
    private boolean startPosReached = false;
    Modules modules = Modules.get();


    @Override
    public void onActivate(){
        //Bro this shit looks so fucking scuffed
        //todo: make this shit make more sense
        cornerOne = new BlockPos(corner1X.get(), yLevel.get(), corner1Z.get());
        cornerTwo = new BlockPos(corner2X.get(), yLevel.get(), corner2Z.get());
        cornerThree = new BlockPos(cornerOne.getX(), yLevel.get(), cornerTwo.getZ());
        cornerFour = new BlockPos(cornerTwo.getX(), yLevel.get(), cornerOne.getZ());

        //heading to the starting position
        if (!debug.get().equals(debugs.None)){
            info("cornerOne: " + cornerOne);
            info("cornerTwo: " + cornerTwo);

        }
        setGoal(cornerOne);

        baritoneSettings.blockPlacementPenalty.value = 0.5;
        baritoneSettings.allowPlace.value = false;
    }

    @Override
    public void onDeactivate(){
        baritone.getPathingBehavior().cancelEverything();
        iterativePos = null;
        endOfLine = null;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        Color color1 = new Color(255, 0, 0, 75);
        Color color2 = new Color(255, 0, 0, 255);
        if(renderCorners.get()){
            event.renderer.box(cornerOne,color1,color2, ShapeMode.Both,0);
            event.renderer.box(cornerTwo,color1,color2, ShapeMode.Both,0);

        }
    }


    @EventHandler
    public void onTick(TickEvent.Pre event){
        BlockPos currPlayerPos = mc.player.getBlockPos();
        int nukerOffset = nukerRange.get() > 0 ? nukerRange.get() *2: -nukerRange.get()*2;
        //checks if the starting position has been reached and turns on the necessary modules

       if(debug.get().equals(debugs.baritonePathing)){
           String currGoal = String.valueOf(baritone.getCustomGoalProcess().getGoal());
           if(currGoal != null) info(currGoal);
       }

        if(cornerOne == currPlayerPos) {
            startPosReached = true;
            activateDiggingModules();
        }

        endOfLine = new BlockPos(cornerOne.getX(),yLevel.get(),cornerTwo.getZ());
        iterativePos = new BlockPos(endOfLine.add(0,0,nukerOffset));

        if (!endOfLine.equals(currPlayerPos) && startPosReached){
            setGoal(endOfLine);
        } else if (endOfLine.equals(currPlayerPos) && startPosReached) {
            setGoal(iterativePos);
        }


        if(currPlayerPos.equals(iterativePos)){
            endOfLine = new BlockPos(cornerOne.getX(),yLevel.get(),iterativePos.getZ());
        }
    }

    private void setGoal(BlockPos goal){
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
    }
    private void activateDiggingModules (){
        if (modules.get(LiquidFiller.class).isActive())
            modules.get(LiquidFiller.class).toggle();
        if (modules.get(NFNuker.class).isActive())
            modules.get(NFNuker.class).toggle();
    }


}
