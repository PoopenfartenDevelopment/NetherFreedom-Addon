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
import net.minecraft.util.math.Direction;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.NFNuker;




public class BaritoneScript extends Module {

    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final Settings baritoneSettings = BaritoneAPI.getSettings();
    Modules modules = Modules.get();

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
    private BlockPos iterativePos;
    private boolean startPosReached, reachedEndOfLIne = false;
    int ran = 0;
    Direction dirToGoal;

    @Override
    public void onActivate(){
        //Bro this shit looks so fucking scuffed
        //todo: make this shit make more sense
        cornerOne = new BlockPos(corner1X.get(), yLevel.get(), corner1Z.get());
        cornerTwo = new BlockPos(corner2X.get(), yLevel.get(), corner2Z.get());
        cornerThree = new BlockPos(cornerOne.getX(), yLevel.get(), cornerTwo.getZ());
        cornerFour = new BlockPos(cornerTwo.getX(), yLevel.get(), cornerOne.getZ());

         dirToGoal = findBlockDir(cornerOne,cornerTwo);

        setGoal(cornerOne);

        baritoneSettings.blockPlacementPenalty.value = 0.5;
        baritoneSettings.allowPlace.value = false;
    }

    @Override
    public void onDeactivate(){
        baritone.getPathingBehavior().cancelEverything();
        iterativePos = null;
        dirToGoal = null;
        startPosReached = false;
        ran = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if(renderCorners.get()){
            try{
                event.renderer.box(cornerOne,Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerTwo,Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerThree,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(cornerFour,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(iterativePos,Color.BLUE,Color.BLUE, ShapeMode.Both,0);
            }catch(Exception ignored){
            }
        }
    }


    @EventHandler
    public void onTick(TickEvent.Pre event){
        BlockPos currPlayerPos = mc.player.getBlockPos();
        int nukerOffset = nukerRange.get() * 2;

        //checks if the starting position has been reached and turns on the necessary modules
        if(cornerOne.equals(currPlayerPos)) {
            startPosReached = true;
            activateDiggingModules();
            iterativePos = cornerThree;
            setGoal(iterativePos);
            if(debug.get() != debugs.None) info("activated digging modules and startpos reached");
        }

        if(currPlayerPos.equals(iterativePos)) reachedEndOfLIne = true;

        if(startPosReached && reachedEndOfLIne){
            BlockPos offsetStart = moveUpLine(iterativePos,nukerOffset);
            setGoal(offsetStart);
            if(currPlayerPos.equals(offsetStart)){
                reachedEndOfLIne = false;
                setGoal(iterativePos.offset(findBlockDir(iterativePos,cornerOne)));
            }
        }


        if(startPosReached){
            setGoal(iterativePos);
        }


    }

    private BlockPos moveUpLine(BlockPos currPos, int nukerOffset){
        return new BlockPos(currPos.offset(dirToGoal,nukerOffset));
    }

    private Direction findBlockDir(BlockPos originBlock , BlockPos goalBlock) {
        BlockPos vec = new BlockPos(goalBlock.getX() - originBlock.getX(),0, goalBlock.getZ() - originBlock.getZ());
        Direction dir = Direction.fromVector((int) Math.signum(vec.getX()),0, (int) Math.signum(vec.getY()));
        info(String.valueOf(dir));
        return dir;
    }

    private void setGoal(BlockPos goal){
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
    }


    private void activateDiggingModules (){
        if (!modules.get(LiquidFiller.class).isActive())
            modules.get(LiquidFiller.class).toggle();
        if (!modules.get(NFNuker.class).isActive())
            modules.get(NFNuker.class).toggle();
    }


}
