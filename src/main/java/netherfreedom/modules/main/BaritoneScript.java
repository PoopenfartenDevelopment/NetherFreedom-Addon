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
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.NFNuker;


public class BaritoneScript extends Module {

    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final Settings baritoneSettings = BaritoneAPI.getSettings();
    Modules modules = Modules.get();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<BlockPos> cornerOne = sgGeneral.add(new BlockPosSetting.Builder()
        .name("CornerOne")
        .description("pos of corner one")
        .defaultValue(new BlockPos(0,120,0))
        .build()
    );

    private final Setting<BlockPos> cornerTwo = sgGeneral.add(new BlockPosSetting.Builder()
        .name("CornerTwo")
        .description("pos of corner two")
        .defaultValue(new BlockPos(0,120,0))
        .build()
    );

    private final Setting<Integer> nukerRange = sgGeneral.add(new IntSetting.Builder()
            .name("nukerRange")
            .description("the first corner of the square where it will mine")
            .defaultValue(4)
            .range(0,6)
            .sliderRange(0,6)
            .build()
    );

    private final Setting<Keybind> pauseBind = sgGeneral.add(new KeybindSetting.Builder()
        .name("Baritone pause")
        .description("pauses baritone ")
        .defaultValue(Keybind.none())
        .build()
    );

    private final Setting<Boolean> renderCorners = sgGeneral.add(new BoolSetting.Builder()
            .name("renderCorners")
            .description("renders the 2 corners")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("don't use this")
            .defaultValue(false)
            .build()
    );

    public BaritoneScript() {super(NetherFreedom.MAIN, "Baritone miner", "mines shit");}
    public BlockPos cornerThree, cornerFour;
    private BlockPos currGoal, barPos, offsetPos;
    private Boolean offsetting = false;
    private Boolean isPaused = false;
    private boolean bindPressed = false;
    int ran, dist = 0;
    Direction dirToOpposite, goalDir;

    @Override
    public void onActivate() {
        if(cornerOne.get().getY() != cornerTwo.get().getY()){
            info("Y-levels are not the same");
            toggle();
        }
        cornerThree = new BlockPos(cornerOne.get().getX(), cornerOne.get().getY(), cornerTwo.get().getZ());
        cornerFour = new BlockPos(cornerTwo.get().getX(), cornerOne.get().getY(), cornerOne.get().getZ());

        isPaused = false;
		currGoal = cornerThree;
        setGoal(cornerOne.get());

        baritoneSettings.blockPlacementPenalty.value = 0.0;
        baritoneSettings.assumeWalkOnLava.value = true;
        baritoneSettings.allowPlace.value = true;
    }

    @EventHandler
    public void onDisconnect() {
        if (modules.get(BaritoneScript.class).isActive()){
            toggle();
        }
    }

    @Override
    public void onDeactivate() {
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
                event.renderer.box(cornerOne.get(),Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerTwo.get(),Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerThree,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(cornerFour,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(currGoal,Color.BLUE,Color.BLUE, ShapeMode.Both,0);
            }catch(Exception e){
                if(debug.get()) info(String.valueOf(e));
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event){
        BlockPos currPlayerPos = mc.player.getBlockPos();
        int nukerOffset = nukerRange.get() * 2;

        if (currPlayerPos.equals(cornerOne.get())) {
            goalDir = findBlockDir(currPlayerPos,currGoal);
            barPos = new BlockPos(cornerOne.get().offset(goalDir));
            dist = findDistance(currPlayerPos,currGoal,goalDir);
            activateDiggingModules();
        }

        if(!currPlayerPos.equals(barPos) && !offsetting ){
            try{
                BlockPos preBarPos = new BlockPos(barPos.offset(goalDir.getOpposite(),1));
                if(currPlayerPos.equals(preBarPos)){
                    barPos = new BlockPos(barPos.offset(goalDir));
                }
                setGoal(barPos);
                placeUnder(barPos);
            } catch(Exception ignored){}
        }

        if(currPlayerPos.equals(barPos)){
            BlockPos whatever = new BlockPos(barPos.offset(goalDir,2));
            setGoal(whatever);
        }


        if (currPlayerPos.equals(currGoal)){
            offsetPos = moveUpLine(currGoal,nukerOffset);
            setGoal(offsetPos);
            offsetting = true;
        }

        if(currPlayerPos.equals(offsetPos)){
            try{
                goalDir = goalDir.getOpposite();
                currGoal = new BlockPos(offsetPos.offset(goalDir,dist));
            }catch (Exception ignored){}
            barPos = new BlockPos(offsetPos.offset(goalDir,2));
            offsetting = false;
            offsetPos = null;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (!pauseBind.get().isPressed()) bindPressed = false;

        if (pauseBind.get().isPressed() && !bindPressed && !isPaused) {
            baritone.getCommandManager().execute("pause");
            isPaused = true;
            bindPressed = true;
            return;
        }

        if (pauseBind.get().isPressed() && !bindPressed && isPaused) {
            baritone.getCommandManager().execute("resume");
            isPaused = false;
            bindPressed = true;
        }
    }

    private void placeUnder(BlockPos pos){
        BlockPos under = new BlockPos(pos.offset(Direction.DOWN));
        if(mc.world.getBlockState(under).getMaterial().isReplaceable()){
            BlockUtils.place(under, InvUtils.findInHotbar(Blocks.NETHERRACK.asItem()),false,0);
        }
    }

    private BlockPos moveUpLine(BlockPos Pos, int nukerOffset){
        dirToOpposite= findBlockDir(cornerThree,cornerTwo.get());
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
        return Direction.fromVector(vec);
    }

    private void setGoal(BlockPos goal){
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
    }

    private void activateDiggingModules (){
        if (!modules.get(NFNuker.class).isActive())
            modules.get(NFNuker.class).toggle();
    }


}
