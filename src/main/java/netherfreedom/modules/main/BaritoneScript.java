package netherfreedom.modules.main;

import baritone.api.BaritoneAPI;
import baritone.api.IBaritone;
import baritone.api.Settings;
import baritone.api.pathing.goals.GoalBlock;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
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


public class BaritoneScript extends Module {

    /*
    todo:future features list
        1. option for it to stop at certain goal position
        2. option to disconnect on running out of pickaxes
        3. designated shulker hotbar slot for refilling pickaxes
        4. swarm websocket integration(some day)
        5. saves progress(probably not tho)
    */


    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final Settings baritoneSettings = BaritoneAPI.getSettings();
    Modules modules = Modules.get();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<BlockPos> cornerOne = sgGeneral.add(new BlockPosSetting.Builder()
            .name("corner-1")
            .description("Pos of corner one.")
            .defaultValue(new BlockPos(10,120,10))
            .build()
    );

    private final Setting<BlockPos> cornerTwo = sgGeneral.add(new BlockPosSetting.Builder()
            .name("corner-2")
            .description("Pos of corner two.")
            .defaultValue(new BlockPos(-10,120,-10))
            .build()
    );

    private final Setting<Integer> nukerRange = sgGeneral.add(new IntSetting.Builder()
            .name("nuker-range")
            .description("The first corner of the square where it will mine.")
            .defaultValue(4)
            .range(0,6)
            .sliderRange(0,6)
            .build()
    );

    private final Setting<Boolean> DTCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("digging-tools-check")
            .description("Disables itself if DiggingTools isn't on.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables DiggingTools when you disconnect from a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> renderCorners = sgGeneral.add(new BoolSetting.Builder()
            .name("render-corners")
            .description("renders the 2 corners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Keybind> pauseBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("pause")
            .description("Pauses baritone.")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<Boolean> getPickaxe = sgGeneral.add(new BoolSetting.Builder()
        .name("get-pickaxe-from-skulker")
        .description("if it runs out of pickaxes it will get pickaxes from a shulker")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> shulkerSlot = sgGeneral.add(new IntSetting.Builder()
            .name("dedicated-shulker-slot")
            .description("put a shulker in this slot and it will place the shulker to refill")
            .defaultValue(0)
            .range(0,9)
            .sliderRange(0,9)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("don't use this")
            .defaultValue(false)
            .build()
    );


    public BaritoneScript() {super(NetherFreedom.MAIN, "baritone-miner", "Allows you to mine while automatically. Use with DiggingTools.");}


    public BlockPos cornerThree, cornerFour;
    private BlockPos currGoal, barPos, offsetPos, placePos, savedPos;
    private boolean offsetting, bindPressed, refilling, isPaused, placedShulker = false;
    int dist = 0;
    Direction dirToOpposite, goalDir;

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (disableOnDisconnect.get()) toggle();
    }

    @Override
    public void onActivate() {
        // Makes sure the corners are at the same y-level
        if (cornerOne.get().getY() != cornerTwo.get().getY()) {
            scriptError("Corners Y levels are not the same, disabling.");
            return;
        }

        if (cornerOne.get().equals(cornerTwo.get())){
            scriptError("The corners are the same you monkey ");
            return;
        }

        if (DTCheck.get() && !Modules.get().isActive(DiggingTools.class)) {
            scriptError("DiggingTools isn't active, disabling.");
            return;
        }

        // Defines the 2 other corners to create a square based on the 2 positions the user defined
        cornerThree = new BlockPos(cornerOne.get().getX(), cornerOne.get().getY(), cornerTwo.get().getZ());
        cornerFour = new BlockPos(cornerTwo.get().getX(), cornerOne.get().getY(), cornerOne.get().getZ());

        dirToOpposite = findBlockDir(cornerThree,cornerTwo.get());

        isPaused = false;
		currGoal = cornerThree;
        setGoal(cornerOne.get());

        baritoneSettings.blockPlacementPenalty.value = 0.0;
        baritoneSettings.assumeWalkOnLava.value = true;
        baritoneSettings.allowPlace.value = true;
    }

    @Override
    public void onDeactivate() {
        baritone.getPathingBehavior().cancelEverything();
        dirToOpposite = null;
		barPos = null;
		currGoal = null;
        placePos = null;
        savedPos = null;
        offsetting = false;
        placedShulker = false;
        refilling = false;
        dist = 0;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (renderCorners.get()) {
            try {
                event.renderer.box(cornerOne.get(),Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerTwo.get(),Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerThree,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(cornerFour,Color.GREEN,Color.GREEN, ShapeMode.Both,0);
                event.renderer.box(currGoal,Color.BLUE,Color.BLUE, ShapeMode.Both,0);
            } catch(Exception ignored){}
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) throws InterruptedException {
        BlockPos currPlayerPos = mc.player.getBlockPos();
        int nukerOffset = nukerRange.get() * 2;

        if (notHavePickaxe() && !placedShulker && getPickaxe.get()) {
            refilling = true;
            //saves the position of current baritone pathing goal to allow for regular pathing after it is done refilling
            GoalBlock baritoneGoal = (GoalBlock) baritone.getCustomGoalProcess().getGoal();
            savedPos = new BlockPos(baritoneGoal.x,baritoneGoal.y,baritoneGoal.z);
            info("ran out of pickaxes... refilling");
            baritone.getCommandManager().execute("pause");

            //places 2 blocks away to make sure the player isn't in the way
            placePos = currPlayerPos.offset(goalDir.getOpposite(), 2);

            //disables nuker to keep it from mining the shulker
            if (modules.get(NFNuker.class).isActive()) modules.get(NFNuker.class).toggle();

            //places shulker
            if (BlockUtils.place(placePos, Hand.MAIN_HAND, shulkerSlot.get(), true, 0, true, true, false)) {
                placedShulker = true;
            }
            return;
        }

        if (notHavePickaxe() && placedShulker) {
            try {
                //opens placed shulker box
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(Vec3d.ofCenter(placePos, 1), Direction.UP, placePos, false));
                //grabs all pickaxes from shulker box
                if (mc.currentScreen instanceof ShulkerBoxScreen) {
                    grabAllPickaxes();
                }
                //sets baritone goal to position of shulker to pick it up
                setGoal(placePos);
                baritone.getCommandManager().execute("resume");
            } catch (Exception e) {
                info(String.valueOf(e));
            }
        }

        if (currPlayerPos.equals(placePos)) {
            //waits to allow time for the shulker box to be picked up
            baritone.getCommandManager().execute("pause");
            Thread.sleep(1000);
            baritone.getCommandManager().execute("resume");
            //resumes regular function
            setGoal(savedPos);
            refilling = false;
            placedShulker = false;
            placePos = null;
            if (!modules.get(NFNuker.class).isActive()) modules.get(NFNuker.class).toggle();
        }

        if (currPlayerPos.equals(cornerOne.get())) {
            goalDir = findBlockDir(currPlayerPos,currGoal);
            barPos = new BlockPos(cornerOne.get().offset(goalDir));
        }

        if (!currPlayerPos.equals(barPos) && !offsetting && !refilling) {
            try {
                BlockPos preBarPos = new BlockPos(barPos.offset(goalDir.getOpposite(),1));
                if (currPlayerPos.equals(preBarPos)) {
                    barPos = new BlockPos(barPos.offset(goalDir));
                }
                setGoal(barPos);
                placeUnder(barPos);
            } catch (Exception ignored) {}
        }

        if (currPlayerPos.equals(barPos)) {
            BlockPos whatever = new BlockPos(barPos.offset(goalDir.getOpposite()));
            setGoal(whatever);
        }

        if (currPlayerPos.equals(currGoal)) {
            try{
                offsetPos = new BlockPos(currGoal.offset(dirToOpposite,nukerOffset));
                setGoal(offsetPos);
            }catch (Exception ignored){}
            offsetting = true;
        }

        if (currPlayerPos.equals(offsetPos)) {
            dist = findDistance(cornerOne.get(),cornerThree,goalDir);
            try {
                goalDir = goalDir.getOpposite();
                currGoal = new BlockPos(offsetPos.offset(goalDir,dist));
            } catch (Exception ignored) {}
            barPos = new BlockPos(offsetPos.offset(goalDir,2));
            offsetting = false;
            offsetPos = null;
        }
    }

    //don't ask why there are 2 onTicks
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

    //places block underneath wear baritone is pathing
    private void placeUnder(BlockPos pos) {
        BlockPos under = new BlockPos(pos.offset(Direction.DOWN));
        if (mc.world.getBlockState(under).getMaterial().isReplaceable()) {
            BlockUtils.place(under, InvUtils.findInHotbar(Blocks.NETHERRACK.asItem()),false,0);
        }
    }

    //bot pathing logic
     private Direction findBlockDir(BlockPos originBlock, BlockPos goalBlock) {
        //very bad this can very easily break if the 2 blocks positions are not inline with each other
        BlockPos vec = new BlockPos(Math.signum(goalBlock.getX() - originBlock.getX()),0, Math.signum(goalBlock.getZ() - originBlock.getZ()));
        return Direction.fromVector(vec);
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

    //reduce a bit of code rewriting
    private void scriptError(String error){
        info(error);
        baritone.getPathingBehavior().cancelEverything();
        toggle();
    }

    //pickaxe refilling stuff
    private boolean notHavePickaxe() {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.NETHERITE_PICKAXE) return false;
        }
        return true;
    }

    //double-clicks on slot if it has a pickaxe stops when it has only 1 slot available to leave room to pick up the shulker box
    private void grabAllPickaxes(){
        int picksMoved = 0;
        int availableSlots = 0;

        for (int i = 27; i < mc.player.currentScreenHandler.slots.size(); i++ ){
            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();
            if(item.equals(Items.AIR)){
                availableSlots++;
            }
        }
        info("availableSlots: " + availableSlots);

        for (int i = 0; i < mc.player.currentScreenHandler.slots.size() - 36; i++) {
            Item item = mc.player.currentScreenHandler.getSlot(i).getStack().getItem();
            if(item.equals(Items.NETHERITE_PICKAXE)){
                //do not fucking ask why
                if(availableSlots - 2 > picksMoved){
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, i, 1, SlotActionType.QUICK_MOVE, mc.player);
                    picksMoved++;
                }else return;
            }
        }
        info("picksMoved: " + picksMoved);
    }


}
