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
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
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
        1.option for it to stop at certain goal position
        2.option to disconnect on running out of pickaxes
        3.designated shulker hotbar slot for refilling pickaxes
        4(some day). swarm websocket integration
        5.saves progress(probably not tho)
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
    private BlockPos currGoal, barPos, offsetPos;
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
            info("Corners Y levels are not the same, disabling.");
            baritone.getPathingBehavior().cancelEverything();
            toggle();
            return;
        }
        if (cornerOne.get().equals(cornerTwo.get())){
            info("The corners are the same you monkey ");
            baritone.getPathingBehavior().cancelEverything();
            toggle();
            return;
        }

        if (!hasPickaxes()){
            info("you ain't got no pickaxes dumbass");
            baritone.getPathingBehavior().cancelEverything();
            return;
        }

        if (DTCheck.get() && !Modules.get().isActive(DiggingTools.class)) {
            info("DiggingTools isn't active, disabling.");
            baritone.getPathingBehavior().cancelEverything();
            toggle();
            return;
        }

        // Defines the 2 other corners to create a square based on the 2 positions the user defined
        cornerThree = new BlockPos(cornerOne.get().getX(), cornerOne.get().getY(), cornerTwo.get().getZ());
        cornerFour = new BlockPos(cornerTwo.get().getX(), cornerOne.get().getY(), cornerOne.get().getZ());


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
    public void onTick(TickEvent.Pre event){
        BlockPos currPlayerPos = mc.player.getBlockPos();
        int nukerOffset = nukerRange.get() * 2;

        if (!hasPickaxes() && !placedShulker && getPickaxe.get()) {
            refilling = true;
            info("ran out of pickaxes... refilling");
            baritone.getCommandManager().execute("pause");
            BlockPos placePos = currPlayerPos.offset(goalDir.getOpposite());
            if(modules.get(NFNuker.class).isActive())modules.get(NFNuker.class).toggle();
            placeRefillShulker(placePos);
            Vec3d lookVec = Vec3d.ofCenter(placePos, 1);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(lookVec, Direction.UP, placePos, false));
            placedShulker = true;
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
                offsetPos = moveUpLine(currGoal,nukerOffset);
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

    private void placeUnder(BlockPos pos) {
        BlockPos under = new BlockPos(pos.offset(Direction.DOWN));
        if (mc.world.getBlockState(under).getMaterial().isReplaceable()) {
            BlockUtils.place(under, InvUtils.findInHotbar(Blocks.NETHERRACK.asItem()),false,0);
        }
    }

    private BlockPos moveUpLine(BlockPos Pos, int nukerOffset) {
        dirToOpposite= findBlockDir(cornerThree,cornerTwo.get());
        return new BlockPos(Pos.offset(dirToOpposite,nukerOffset));
    }


    private void placeRefillShulker(BlockPos shulkerPlacePos){
        BlockUtils.place(shulkerPlacePos, Hand.MAIN_HAND, shulkerSlot.get(), true, 0, true, true, false);
    }

    private boolean hasPickaxes() {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.NETHERITE_PICKAXE) return true;
        }
        return false;
    }


    private void setGoal(BlockPos goal){
        baritone.getCustomGoalProcess().setGoalAndPath(new GoalBlock(goal));
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


    public static int getRows(ScreenHandler handler) {
        return (handler instanceof GenericContainerScreenHandler ? ((GenericContainerScreenHandler) handler).getRows() : 3);
    }

    public static void moveSlots(ScreenHandler handler, int end) {
        for (int i = 0; i < end; i++) {
            if (!handler.getSlot(i).hasStack()) continue;
            InvUtils.quickMove().slotId(i);
        }
    }

    public static void steal(ScreenHandler handler) {
        MeteorExecutor.execute(() -> moveSlots(handler, getRows(handler) * 9));
    }
}
