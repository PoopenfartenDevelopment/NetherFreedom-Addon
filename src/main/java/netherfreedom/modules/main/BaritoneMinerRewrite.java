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

    private final Setting<Integer> shulkerSlot = sgGeneral.add(new IntSetting.Builder()
            .name("dedicated-shulker-slot")
            .description("put a shulker in this slot and it will place the shulker to refill")
            .defaultValue(0)
            .range(0,9)
            .sliderRange(0,9)
            .build()
    );

    private final Setting<Boolean> saveLineProgress = sgGeneral.add(new BoolSetting.Builder()
            .name("save line progress")
            .description("saves the progress on the current line you're digging")
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
            .name("disable on disconnect")
            .description("disables when you disconnect")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> getPickaxe = sgGeneral.add(new BoolSetting.Builder()
            .name("get-pickaxe-from-skulker")
            .description("if it runs out of pickaxes it will get pickaxes from a shulker")
            .defaultValue(true)
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

    private BlockPos endOfLinePos, barPos, offsetPos, currPlayerPos, shulkerPlacePos, savedPos = null;
    private Direction toEndOfLineDir, toAdvanceDir, shulkerPlaceDir = null;
    private boolean offsetting, refilling, placedShulker = false;
    private int length,dist = 0;

    @Override
    public void onActivate() {
        currPlayerPos = mc.player.getBlockPos();
        BlockPos extrapolatePos = new BlockPos(cornerOne.get().getX(),cornerOne.get().getY(),cornerTwo.get().getZ());
        toEndOfLineDir = findBlockDir(cornerOne.get(),extrapolatePos);
        length = findDistance(cornerOne.get(),extrapolatePos, toEndOfLineDir);
        toAdvanceDir = findBlockDir(extrapolatePos,cornerTwo.get());
        endOfLinePos = currPlayerPos.offset(toEndOfLineDir, length);
        barPos = currPlayerPos.offset(toEndOfLineDir,2);
        setGoal(barPos);

        if (enableDT.get()){
            if (!modules.isActive(DiggingTools.class)){
                modules.get(DiggingTools.class).toggle();
            }
        }

        shulkerPlaceDir = toEndOfLineDir.getOpposite();

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
        toAdvanceDir = null;
        offsetting = false;
        length = 0;
        shulkerPlaceDir = toEndOfLineDir.getOpposite();
        placedShulker = false;
        refilling = false;

        if (enableDT.get()){
            if (modules.isActive(DiggingTools.class)){
                modules.get(DiggingTools.class).toggle();
            }
        }

    }

    @EventHandler
     public void onTick(TickEvent.Pre event) throws InterruptedException {
        currPlayerPos = mc.player.getBlockPos();
        if (pauseBind.get().isPressed()) {
            if(baritone.getPathingBehavior().isPathing()){
                baritone.getCommandManager().execute("pause");
            }else{
                baritone.getCommandManager().execute("resume");
            }
        }
        if (notHavePickaxe() && !placedShulker && getPickaxe.get()) {
            baritone.getCommandManager().execute("pause");
            refilling = true;
            GoalBlock baritoneGoal = (GoalBlock) baritone.getCustomGoalProcess().getGoal();
            savedPos = new BlockPos(baritoneGoal.x,baritoneGoal.y,baritoneGoal.z);
            info("ran out of pickaxes... refilling");
            shulkerPlacePos = currPlayerPos.offset(shulkerPlaceDir, 2);
            if (modules.get(NFNuker.class).isActive()) modules.get(NFNuker.class).toggle();
            if (BlockUtils.place(shulkerPlacePos, Hand.MAIN_HAND, shulkerSlot.get(), true, 0, true, true, false)) {
                placedShulker = true;
            }else{
                info("unable to place... redirecting");
                shulkerPlaceDir = shulkerPlaceDir.rotateYClockwise();
                placedShulker = false;
                shulkerPlacePos = null;
                return;
            }
            return;
        }

        if (notHavePickaxe() && placedShulker) {
            BlockHitResult bhr = new BlockHitResult(Vec3d.ofCenter(shulkerPlacePos, 1), Direction.UP, shulkerPlacePos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, bhr);
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
            baritone.getCommandManager().execute("resume");
        }

        if (currPlayerPos.equals(shulkerPlacePos)) {
            setGoal(savedPos);
            shulkerPlacePos = null;
            savedPos = null;
            placedShulker = false;
            refilling = false;
            if (!modules.get(NFNuker.class).isActive()) modules.get(NFNuker.class).toggle();
        }

        if (!currPlayerPos.equals(barPos) && !offsetting && !refilling) {
            try {
                BlockPos preBarPos = new BlockPos(barPos.offset(toEndOfLineDir.getOpposite()));
                if (currPlayerPos.equals(preBarPos)) {
                    barPos = new BlockPos(barPos.offset(toEndOfLineDir));
                }
                setGoal(barPos);
                placeUnder(barPos);
                dist = findDistance(currPlayerPos,endOfLinePos,toEndOfLineDir);
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
            toEndOfLineDir = toEndOfLineDir.getOpposite();
            endOfLinePos = new BlockPos(offsetPos.offset(toEndOfLineDir, length));
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
            cornerTwo.set(cornerTwo.get().offset(toEndOfLineDir.getOpposite(), length*2));
        };
        return list;
    }

    @EventHandler
    private void onRender(Render3DEvent event){
        if (renderCorners.get()) {
            try {
                Color DARKRED = new Color(139,0,0);
                event.renderer.box(cornerOne.get(), Color.RED,Color.RED, ShapeMode.Both,0);
                event.renderer.box(cornerTwo.get(),DARKRED,DARKRED, ShapeMode.Both,0);
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

//pickaxe refilling stuff
    private boolean notHavePickaxe() {
        for (int i = 0; i < mc.player.getInventory().main.size(); i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.NETHERITE_PICKAXE) return false;
        }
        return true;
    }

    //double-clicks on slot if it has a pickaxe stops when it has only 1 slot available to leave room to pick up the shulker box
    private int grabAllPickaxes(){
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
                }
            }
        }
        info("picksMoved: " + picksMoved);
        return picksMoved;
    }
}
