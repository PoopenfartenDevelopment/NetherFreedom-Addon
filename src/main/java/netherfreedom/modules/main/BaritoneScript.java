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
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.NFNuker;
import org.apache.commons.lang3.exception.ExceptionUtils;


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
    private BlockPos currGoal, barPos, offsetPos;
    private  Boolean offsetting = false;
    int ran,dist = 0;
    Direction dirToOpposite, goalDir;

    @Override
    public void onActivate(){
        //Bro this shit looks so fucking scuffed
        //todo: make this shit make more sense
        cornerOne = new BlockPos(corner1X.get(), yLevel.get(), corner1Z.get());
        cornerTwo = new BlockPos(corner2X.get(), yLevel.get(), corner2Z.get());
        cornerThree = new BlockPos(cornerOne.getX(), yLevel.get(), cornerTwo.getZ());
        cornerFour = new BlockPos(cornerTwo.getX(), yLevel.get(), cornerOne.getZ());


		currGoal = cornerThree;
        barPos = cornerOne;

        baritoneSettings.blockPlacementPenalty.value = 20.0;
        baritoneSettings.allowPlace.value = false;
    }

    @EventHandler
    public void onDisconnect(){
        if (modules.get(BaritoneScript.class).isActive()){
            toggle();
        }
    }

    @Override
    public void onDeactivate(){
        baritone.getPathingBehavior().cancelEverything();
        dirToOpposite = null;
		barPos = null;
		currGoal = null;
        offsetting = false;
        ran = 0;
        dist = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if(renderCorners.get()){
            try{
                event.renderer.box(cornerOne,Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerTwo,Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerThree,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(cornerFour,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(currGoal,Color.BLUE,Color.BLUE, ShapeMode.Both,0);
            }catch(Exception ignored){
            }
        }
    }


    @EventHandler
    public void onTick(TickEvent.Pre event){
        BlockPos currPlayerPos = mc.player.getBlockPos();
        int nukerOffset = nukerRange.get() * 2;

        if(currPlayerPos.equals(cornerOne)){
            goalDir = findBlockDir(currPlayerPos,currGoal);
            dist = findDistance(currPlayerPos,currGoal,goalDir);
            info(String.valueOf(currGoal));
            activateDiggingModules();
        }

        if(!currPlayerPos.equals(barPos) && !offsetting ){
            try{
                setGoal(barPos);
            } catch(Exception e){
                String stacktrace = ExceptionUtils.getStackTrace(e);
                info(stacktrace);
            }
        }

        if (currPlayerPos.equals(barPos)){
            barPos = new BlockPos(currPlayerPos.offset(goalDir,3));

        }

        if (currPlayerPos.equals(currGoal)){
            info("reached end of line");
            offsetPos = moveUpLine(currGoal,nukerOffset);
            setGoal(offsetPos);
            offsetting = true;
        }

        if(currPlayerPos.equals(offsetPos)){
            info("offsetting");
            try{
                currGoal = new BlockPos(offsetPos.offset(goalDir.getOpposite(),dist));
                info(String.valueOf(currGoal));
                goalDir = goalDir.getOpposite();
            }catch (Exception e){
                String stacktrace = ExceptionUtils.getStackTrace(e);
                info(stacktrace);
            }
            info(String.valueOf(currGoal));
            barPos = offsetPos;
            offsetting = false;
            offsetPos = null;
        }
    }

    private BlockPos moveUpLine(BlockPos Pos, int nukerOffset){
        dirToOpposite= findBlockDir(cornerThree,cornerTwo);
        return new BlockPos(Pos.offset(dirToOpposite,nukerOffset));
    }

    private int findDistance(BlockPos pos1, BlockPos pos2, Direction dir){
        int dist = 0;
        switch(dir){
            case EAST:
            case WEST:
                dist = Math.abs(pos1.getX() - pos2.getX());
            case SOUTH:
            case NORTH:
                dist = Math.abs(pos1.getZ() - pos2.getZ());
        }
        return dist;
    }

    private Direction findBlockDir(BlockPos originBlock, BlockPos goalBlock) {
        BlockPos vec = new BlockPos(Math.signum(goalBlock.getX() - originBlock.getX()),0, Math.signum(goalBlock.getZ() - originBlock.getZ()));
        Direction dir = Direction.fromVector(vec);
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
