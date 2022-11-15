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
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import netherfreedom.modules.NetherFreedom;

import java.util.function.Predicate;


public class BaritoneScript extends Module {

    private final IBaritone baritone = BaritoneAPI.getProvider().getPrimaryBaritone();
    private final Settings baritoneSettings = BaritoneAPI.getSettings();

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

    private final Setting<Keybind> pauseBind = sgGeneral.add(new KeybindSetting.Builder()
            .name("pause")
            .description("Pauses baritone.")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<Boolean> DTCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("digging-tools-check")
            .description("Disables itself if DiggingTools isn't on.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderCorners = sgGeneral.add(new BoolSetting.Builder()
            .name("render-corners")
            .description("renders the 2 corners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> getPickaxe = sgGeneral.add(new BoolSetting.Builder()
        .name("get-pickaxe-from-skulker")
        .description("if it runs out of pickaxes it will get pickaxes from a shulker")
        .defaultValue(true)
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

    @Override
    public void onActivate() {
        // Makes sure the corners are at the same y-level
        if (cornerOne.get().getY() != cornerTwo.get().getY()) {
            info("Corners Y levels are not the same, disabling.");
            toggle();
        }
        if (cornerOne.get().equals(cornerTwo.get())){
            info("The corners are the same you monkey ");
            baritone.getPathingBehavior().cancelEverything();
            toggle();
        }

        if (DTCheck.get() && !Modules.get().isActive(DiggingTools.class)) {
            info("DiggingTools isn't active, disabling.");
            baritone.getPathingBehavior().cancelEverything();
            toggle();
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

        if (!hasItem() && !placedShulker) {
            refilling = true;
            BlockPos PlacePos = currPlayerPos.offset(goalDir);
            placeRefillShulker(PlacePos);
            Vec3d lookVec = Vec3d.ofCenter(PlacePos, 1);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, new BlockHitResult(lookVec, Direction.UP, PlacePos, false));
            placedShulker = true;
            return;
        }

        if (currPlayerPos.equals(cornerOne.get())) {
            goalDir = findBlockDir(currPlayerPos,currGoal);
            barPos = new BlockPos(cornerOne.get().offset(goalDir));
            dist = findDistance(currPlayerPos,currGoal,goalDir);
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
            offsetPos = moveUpLine(currGoal,nukerOffset);
            setGoal(offsetPos);
            offsetting = true;
        }

        if (currPlayerPos.equals(offsetPos)) {
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
        int slot = findAndMoveToHotbar(itemStack -> itemStack.getItem() == Items.SHULKER_BOX);
        BlockUtils.place(shulkerPlacePos, Hand.MAIN_HAND, slot, true, 0, true, true, false);

    }

    int findAndMoveToHotbar(Predicate<ItemStack> predicate) {
        int slot = findSlot( predicate, true);
        if (slot != -1) return slot;

        int hotbarSlot = findHotbarSlot();

        slot = findSlot(predicate, false);

        // Move items from inventory to hotbar
        InvUtils.move().from(slot).toHotbar(hotbarSlot);
        InvUtils.dropHand();

        return hotbarSlot;
    }

    private int findSlot(Predicate<ItemStack> predicate, boolean hotbar) {
        for (int i = hotbar ? 0 : 9; i < (hotbar ? 9 : mc.player.getInventory().main.size()); i++) {
            if (predicate.test(mc.player.getInventory().getStack(i))) return i;
        }
        return -1;
    }

    private int findHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            // Return if the slot is empty
            if (itemStack.isEmpty()) return i;
            // Return if the slot contains a tool and replacing tools is enabled
        }
        return -1;
    }

    private boolean hasItem() {
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
