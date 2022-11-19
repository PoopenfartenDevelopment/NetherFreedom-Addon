package netherfreedom.modules.main;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import netherfreedom.modules.NetherFreedom;

public class BaritoneMinerRewrite extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgShape = settings.createGroup("Shape");

    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final Settings baritoneSettings = BaritoneAPI.getSettings();
    Modules modules = Modules.get();

    private final Setting<BlockPos> cornerOne = sgShape.add(new BlockPosSetting.Builder()
            .name("corner-1")
            .description("Pos of corner one.")
            .defaultValue(new BlockPos(10,120,10))
            .build()
    );

    private final Setting<BlockPos> cornerTwo = sgShape.add(new BlockPosSetting.Builder()
            .name("corner-2")
            .description("Pos of corner two.")
            .defaultValue(new BlockPos(-10,120,-10))
            .build()
    );

    private final Setting<Integer> nukerOffset = sgGeneral.add(new IntSetting.Builder()
            .name("nuker-offset")
            .description("distance for bot to offset after reaching end of line")
            .defaultValue(8)
            .range(0,15)
            .sliderRange(1,15)
            .build()
    );

    private final Setting<Boolean> renderCorners = sgGeneral.add(new BoolSetting.Builder()
            .name("render-corners")
            .description("renders the 2 corners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
            .name("disable on disconnect")
            .description("disables when you disconnect")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> pathToStart = sgGeneral.add(new BoolSetting.Builder()
            .name("path to start")
            .description("paths to corner 1 before beginning")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> enableDT = sgGeneral.add(new BoolSetting.Builder()
            .name("enable digging tools")
            .description("enables digging tools at the same time")
            .defaultValue(true)
            .build()
    );

    private final Setting<Keybind> pauseBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("pause")
            .description("Pauses baritone.")
            .defaultValue(Keybind.none())
            .build()
    );
    public BaritoneMinerRewrite() {
        super(NetherFreedom.MAIN, "BaritoneMinerRewrite", "this shit bugs me");
    }

    private BlockPos endOfLinePos, barPos, offsetPos, currPlayerPos = null;
    private Direction toEndOfLineDir, toAdvanceDir = null;
    private boolean offsetting = false;
    private boolean reachedStart = true;
    private int dist = 0;

    @Override
    public void onActivate() {
        currPlayerPos = mc.player.getBlockPos();
        BlockPos extrapolatePos = new BlockPos(cornerOne.get().getX(),cornerOne.get().getY(),cornerTwo.get().getZ());
        toEndOfLineDir = findBlockDir(cornerOne.get(),extrapolatePos);
        dist = findDistance(cornerOne.get(),extrapolatePos, toEndOfLineDir);
        toAdvanceDir = findBlockDir(extrapolatePos,cornerTwo.get());
        endOfLinePos = currPlayerPos.offset(toEndOfLineDir,dist);
        barPos = currPlayerPos.offset(toEndOfLineDir,2);
        setGoal(barPos);
        if (enableDT.get()){
            if (!modules.isActive(DiggingTools.class)){
                modules.get(DiggingTools.class).toggle();
            }
        }

        if (pathToStart.get()){
            reachedStart = false;
            setGoal(cornerOne.get());
        }

        baritoneSettings.blockPlacementPenalty.value = 0.0;
        baritoneSettings.assumeWalkOnLava.value = true;
        baritoneSettings.allowPlace.value = true;
        baritoneSettings.mineScanDroppedItems.value = true;
    }

    @Override
    public void onDeactivate(){
        baritone.getPathingBehavior().cancelEverything();
        endOfLinePos = null;
        barPos = null;
        offsetPos = null;
        toEndOfLineDir = null;
        toAdvanceDir = null;
        offsetting = false;
        dist = 0;
        reachedStart = false;

        if (enableDT.get()){
            if (modules.isActive(DiggingTools.class)){
                modules.get(DiggingTools.class).toggle();
            }
        }

    }

    @EventHandler
     public void onTick(TickEvent.Pre event){
        currPlayerPos = mc.player.getBlockPos();

        if (pauseBind.get().isPressed()) {
            if(baritone.getPathingBehavior().isPathing()){
                baritone.getCommandManager().execute("pause");
            }else{
                baritone.getCommandManager().execute("resume");
            }
        }

        if (currPlayerPos.equals(cornerOne)){
            reachedStart = true;
        }

        if (!currPlayerPos.equals(barPos) && !offsetting && reachedStart) {
            try {
                BlockPos preBarPos = new BlockPos(barPos.offset(toEndOfLineDir.getOpposite(),1));
                if (currPlayerPos.equals(preBarPos)) {
                    barPos = new BlockPos(barPos.offset(toEndOfLineDir));
                }
                setGoal(barPos);
                placeUnder(barPos);
            } catch (Exception ignored) {}
        }

        if (currPlayerPos.equals(barPos)) {
            BlockPos whatever = new BlockPos(barPos.offset(toEndOfLineDir.getOpposite()));
            setGoal(whatever);
        }


        if (currPlayerPos.equals(endOfLinePos)) {
            try {
                offsetPos = new BlockPos(endOfLinePos.offset(toAdvanceDir, nukerOffset.get()));
                setGoal(offsetPos);
            } catch (Exception ignored) {
            }
            offsetting = true;
        }

        if (currPlayerPos.equals(offsetPos)) {
            info(String.valueOf(toEndOfLineDir));
            toEndOfLineDir = toEndOfLineDir.getOpposite();
            endOfLinePos = new BlockPos(offsetPos.offset(toEndOfLineDir,dist));
            barPos = new BlockPos(offsetPos.offset(toEndOfLineDir,2));
            offsetting = false;
            offsetPos = null;
        }
    }


    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();

        WHorizontalList b = list.add(theme.horizontalList()).expandX().widget();
        WButton start = b.add(theme.button("swap direction")).expandX().widget();
        start.action = () -> {
            cornerOne.set(cornerOne.get().offset(toEndOfLineDir.getOpposite(),dist));
            cornerTwo.set(cornerTwo.get().offset(toEndOfLineDir.getOpposite(),dist));
        };
        return list;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (renderCorners.get()) {
            try {
                event.renderer.box(cornerOne.get(), Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerTwo.get(),Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(endOfLinePos,Color.BLUE,Color.BLUE,ShapeMode.Both,0);
            } catch(Exception ignored){}
        }
    }
     @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();}

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {if (disableOnDisconnect.get()) toggle();}

     //bot pathing logic
     private Direction findBlockDir(BlockPos originBlock, BlockPos goalBlock) {
        //very bad this can very easily break if the 2 blocks positions are not inline with each other
        BlockPos vec = new BlockPos(Math.signum(goalBlock.getX() - originBlock.getX()),0, Math.signum(goalBlock.getZ() - originBlock.getZ()));
        return Direction.fromVector(vec);
    }

    private void placeUnder(BlockPos pos) {
        BlockPos under = new BlockPos(pos.offset(Direction.DOWN));
        if (mc.world.getBlockState(under).getMaterial().isReplaceable()) {
            BlockUtils.place(under, InvUtils.findInHotbar(Blocks.NETHERRACK.asItem()),false,0);
        }
    }
    //do I really have to explain this
    private void setGoal(BlockPos goal){
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
    }

    //extremely monkey way of finding distance between 2 blocks
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

}
