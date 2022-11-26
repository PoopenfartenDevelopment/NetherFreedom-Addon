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
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import netherfreedom.modules.NetherFreedom;
import netherfreedom.modules.kmain.NFNuker;
import netherfreedom.utils.NFUtils;

public class BaritoneMiner extends Module {

    /*
    todo:future features list
        1. option for it to stop at certain goal position
        2. option to disconnect on running out of pickaxes
        3. swarm websocket integration(some day)
    */

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

    private final Setting<Boolean> pathStart = sgGeneral.add(new BoolSetting.Builder()
            .name("path-to-start")
            .description("when activated baritone paths to corner one before starting")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderCorners = sgGeneral.add(new BoolSetting.Builder()
            .name("render-corners")
            .description("renders the 2 corners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
            .name("disable-on-disconnect")
            .description("Disables when you disconnect.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> getPickaxe = sgGeneral.add(new BoolSetting.Builder()
            .name("get-pickaxe-from-skulker")
            .description("if it runs out of pickaxes it will get pickaxes from a shulker.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> enableDT = sgGeneral.add(new BoolSetting.Builder()
            .name("enable-digging-tools")
            .description("Enables digging tools at the same time.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Keybind> pauseBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("pause")
            .description("Pauses baritone.")
            .defaultValue(Keybind.none())
            .build()
    );
    public BaritoneMiner() {
        super(NetherFreedom.MAIN, "BaritoneMiner", "does the funni");
    }

    //a global variable or two
    private BlockPos endOfLinePos, barPos, offsetPos, currPlayerPos, shulkerPlacePos, savedPos = null;
    private Direction toEndOfLineDir, toAdvanceDir, shulkerPlaceDir = null;
    private boolean offsetting, bindPressed, isPaused, refilling, placedShulker, defined= false;
    private int length, initialNetherrack, initialPicksBroken = 0;

    @Override
    public void onActivate() {
        if (!defined && !pathStart.get()){
            start();
        } else if (!defined && pathStart.get()) {
            setGoal(cornerOne.get());
        }else {
            setGoal(barPos);
        }


        if (enableDT.get()){
            if (!modules.isActive(DiggingTools.class)){
                modules.get(DiggingTools.class).toggle();
            }
        }

        isPaused = false;

        initialPicksBroken = NFUtils.getPickaxesBroken();
        initialNetherrack = NFUtils.getNetherrack();

        baritoneSettings.blockPlacementPenalty.value = 0.0;
        baritoneSettings.assumeWalkOnLava.value = true;
        baritoneSettings.allowPlace.value = true;
        baritoneSettings.mineScanDroppedItems.value = true;
    }

    @Override
    public void onDeactivate(){
        baritone.getPathingBehavior().cancelEverything();

        int finalNetherrack = NFUtils.getNetherrack();
        int finalPickaxesBroken = NFUtils.getPickaxesBroken();
        info("Blocks Broken: %d", (finalNetherrack - initialNetherrack));
        info("Pickaxes used: %d", (finalPickaxesBroken - initialPicksBroken));

        if (modules.get(NetherrackTracker.class).isActive())
            modules.get(NetherrackTracker.class).toggle();

        if (enableDT.get()){
            if (modules.isActive(DiggingTools.class)){
                modules.get(DiggingTools.class).toggle();
            }
        }
    }

    @EventHandler
     public void onTick(TickEvent.Pre event) throws InterruptedException {
        currPlayerPos = mc.player.getBlockPos();

        if (pathStart.get() && currPlayerPos.equals(cornerOne.get())){
            start();
        }

        if (getPickAmount() == 0 && !placedShulker && getPickaxe.get()) {
            if (baritone.getPathingBehavior().isPathing()) baritone.getCommandManager().execute("pause");
            refilling = true;
            //saves bots current goal, so it can later resume after refilling on pickaxes
            GoalBlock baritoneGoal = (GoalBlock) baritone.getCustomGoalProcess().getGoal();
            savedPos = new BlockPos(baritoneGoal.x,baritoneGoal.y,baritoneGoal.z);

            //places shulker and toggles nuker rotating and placing another shulker if the spot is invalid
            shulkerPlacePos = currPlayerPos.offset(shulkerPlaceDir,2);
            if (modules.get(NFNuker.class).isActive()) modules.get(NFNuker.class).toggle();
            if (!BlockUtils.place(shulkerPlacePos, findShulkerBox(), true, 0, true, true, false)) {
                info("trying to place at" + shulkerPlacePos.getX() + " " + shulkerPlacePos.getZ());
                shulkerPlaceDir = shulkerPlaceDir.rotateYClockwise();
                placedShulker = false;
                shulkerPlacePos = null;
                return;
            }
            placedShulker = true;
            return;
        }

        if (getPickAmount() == 0 && placedShulker) {
            openShulker(shulkerPlacePos);
            //if no pickaxes were moved then it will reset and place another shulker 90 degrees clockwise to the player
            if (mc.currentScreen instanceof ShulkerBoxScreen) {
                if (grabAllPickaxes() == 0){
                    mc.currentScreen.close();
                    shulkerPlaceDir = shulkerPlaceDir.rotateYClockwise();
                    placedShulker = false;
                    shulkerPlacePos = null;
                    return;
                }
                mc.currentScreen.close();
            }
            setGoal(shulkerPlacePos);
            if (!baritone.getPathingBehavior().isPathing()) baritone.getCommandManager().execute("resume");
        }

        //breaks, paths, then pauses where the shulker was placed then resets to continue mining
        if (currPlayerPos.equals(shulkerPlacePos)) {
            //very monkey fix for right now
            if (!baritone.getPathingBehavior().isPathing()) baritone.getCommandManager().execute("pause");
            Thread.sleep(1000);
            if (!baritone.getPathingBehavior().isPathing()) baritone.getCommandManager().execute("resume");

            setGoal(savedPos);
            shulkerPlacePos = null;
            savedPos = null;
            placedShulker = false;
            refilling = false;
            if (!modules.get(NFNuker.class).isActive()) modules.get(NFNuker.class).toggle();
        }

        //the mode it will be working in most of the time.
        if (!currPlayerPos.equals(barPos) && !offsetting && !refilling) {
            try {
                //if the player is one block before the goal then it will set the goal one further. this is to keep if from stuttering
                BlockPos preBarPos = new BlockPos(barPos.offset(toEndOfLineDir.getOpposite()));
                if (currPlayerPos.equals(preBarPos)) {
                    barPos = new BlockPos(barPos.offset(toEndOfLineDir));
                }
                setGoal(barPos);
                //places a block under the goal to keep baritone from pulling any funny business
                placeUnder(barPos);
            } catch (Exception ignored) {}
        }

        if (currPlayerPos.equals(barPos)) {
            BlockPos whatever = new BlockPos(barPos.offset(toEndOfLineDir.getOpposite()));
            setGoal(whatever);
        }


        if (currPlayerPos.equals(endOfLinePos)) {
            offsetPos = new BlockPos(endOfLinePos.offset(toAdvanceDir, nukerOffset.get()));
            setGoal(offsetPos);
            offsetting = true;
        }
        if (currPlayerPos.equals(offsetPos)) {
            toEndOfLineDir = toEndOfLineDir.getOpposite();
            shulkerPlaceDir = toEndOfLineDir.getOpposite();
            endOfLinePos = new BlockPos(offsetPos.offset(toEndOfLineDir, length));
            barPos = new BlockPos(offsetPos.offset(toEndOfLineDir,2));
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

    //defines the buttons swap direction and hard reset button
    @Override
    public WWidget getWidget(GuiTheme theme) {
        WVerticalList list = theme.verticalList();
        WHorizontalList b = list.add(theme.horizontalList()).expandX().widget();
        WButton start = b.add(theme.button("swap direction")).expandX().widget();
        start.action = () -> {
            cornerTwo.set(cornerTwo.get().offset(toEndOfLineDir.getOpposite(), length*2));
        };

        WButton reset = b.add(theme.button("reset progress")).expandX().widget();
        reset.action = () -> {
            placedShulker = false;
            refilling = false;
            defined = false;
            offsetting = false;
            endOfLinePos = null;
            barPos = null;
            offsetPos = null;
            toAdvanceDir = null;
            length = 0;
        };

        return list;
    }

    //renders the 3 blocks
    @EventHandler
    private void onRender(Render3DEvent event) {
        if (renderCorners.get()) {
            try {
                Color DARKRED = new Color(139,0,0);
                event.renderer.box(cornerOne.get(), Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerTwo.get(),DARKRED,DARKRED, ShapeMode.Both,0);
                event.renderer.box(endOfLinePos,Color.BLUE,Color.BLUE,ShapeMode.Both,0);
            } catch (Exception ignored) {}
        }
    }

    //these toggle the module if you disconnect or leave the server
    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();}
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {if (disableOnDisconnect.get()) toggle();}

     //bot pathing logic
    private void start(){
        //ain't this just lovely to read
        currPlayerPos = mc.player.getBlockPos();
        BlockPos extrapolatePos = new BlockPos(cornerOne.get().getX(),cornerOne.get().getY(),cornerTwo.get().getZ());
        toEndOfLineDir = findBlockDir(cornerOne.get(),extrapolatePos);
        length = findDistance(cornerOne.get(),extrapolatePos, toEndOfLineDir);
        toAdvanceDir = findBlockDir(extrapolatePos,cornerTwo.get());
        endOfLinePos = currPlayerPos.offset(toEndOfLineDir, length);
        barPos = currPlayerPos.offset(toEndOfLineDir,2);
        shulkerPlaceDir = toEndOfLineDir.getOpposite();
        defined = true;

        setGoal(barPos);
    }

    //finds the direction for one block to get to the other
     private Direction findBlockDir(BlockPos originBlock, BlockPos goalBlock) {
        //very bad this can very easily break if the 2 blocks positions are not inline with each other
        BlockPos vec = new BlockPos(Math.signum(goalBlock.getX() - originBlock.getX()),0, Math.signum(goalBlock.getZ() - originBlock.getZ()));
        return Direction.fromVector(vec);
    }

    //places a block below the input
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
    private int findDistance(BlockPos pos1, BlockPos pos2, Direction dir) {
        int dist = 0;
        switch(dir) {
            case EAST:
            case WEST:
                dist = Math.abs(pos1.getX() - pos2.getX());
            case SOUTH:
            case NORTH:
                dist = Math.abs(pos1.getZ() - pos2.getZ());
        }
        return dist;
    }

    //pickaxe refilling stuff
    public FindItemResult findShulkerBox() {
        return InvUtils.find(itemStack -> NFUtils.shulkers.contains(itemStack.getItem()));
    }

    //opens shulker
    private void openShulker(BlockPos sulkerPos) {
        Vec3d shulkerVec = new Vec3d(sulkerPos.getX(), sulkerPos.getY(), sulkerPos.getZ());
        BlockHitResult table = new BlockHitResult(shulkerVec, Direction.UP, sulkerPos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, table);
    }

    //finds number of pickaxes in player inventory
    private int getPickAmount(){
        return NFUtils.haveItem(Items.NETHERITE_PICKAXE);
    }

    //grabs pickaxes stops when it has only 1 slot available to leave room to pick up the shulker box
    private int grabAllPickaxes() {
        int picksMoved = 0;
        int availableSlots = 0;
        //checks player's inventory for available slots
        for (int i = 27; i < mc.player.currentScreenHandler.slots.size(); i++ ) {
            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();
            if (item.equals(Items.AIR)) {
                availableSlots++;
            }
        }
        info("availableSlots: " + availableSlots);

        //checks shulker box for pickaxes and shift-clicks them stopping when there's 2 slots available
        for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();
            if (item.equals(Items.NETHERITE_PICKAXE)) {
                if (availableSlots - 2 > picksMoved) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.QUICK_MOVE, mc.player);
                    picksMoved++;
                }
            }
        }
        info("picksMoved: " + picksMoved);
        return picksMoved;
    }


}
