package me.redcarlos.netherfreedom.modules.main;

import me.redcarlos.netherfreedom.NFAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import meteordevelopment.meteorclient.systems.modules.movement.AutoWalk;
import meteordevelopment.meteorclient.systems.modules.movement.SafeWalk;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class DiggingTools extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScaffold = settings.createGroup("Scaffold");


    private final Setting<Boolean> disableOnDisconnect = sgGeneral.add(new BoolSetting.Builder()
        .name("disable-on-disconnect")
        .description("Disables DiggingTools when you leave a server.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> useBaritone = sgGeneral.add(new BoolSetting.Builder()
        .name("[BETA]-use-baritone")
        .description("Use baritone to automate the digging process.")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> scaffold = sgScaffold.add(new BoolSetting.Builder()
        .name("scaffold")
        .description("Scaffolds blocks under you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<ListMode> listMode = sgScaffold.add(new EnumSetting.Builder<ListMode>()
        .name("block-list-mode")
        .description("Block list selection mode.")
        .defaultValue(ListMode.Whitelist)
        .visible(scaffold::get)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgScaffold.add(new BlockListSetting.Builder()
        .name("whitelist")
        .description("Blocks allowed to scaffold.")
        .defaultValue(Blocks.NETHERRACK)
        .visible(() -> scaffold.get() && listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgScaffold.add(new BlockListSetting.Builder()
        .name("blacklist")
        .description("Blocks denied to scaffold.")
        .defaultValue(Blocks.OBSIDIAN)
        .visible(() -> scaffold.get() && listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<Integer> ext = sgScaffold.add(new IntSetting.Builder()
        .name("extend")
        .description("How much to place in front of you.")
        .defaultValue(3)
        .range(0, 5)
        .visible(scaffold::get)
        .build()
    );

    private final Setting<Boolean> keepY = sgScaffold.add(new BoolSetting.Builder()
        .name("keep-y")
        .description("Places blocks only at a specific Y value.")
        .defaultValue(true)
        .visible(scaffold::get)
        .build()
    );

    private final Setting<Integer> height = sgScaffold.add(new IntSetting.Builder()
        .name("height")
        .description("Y value to scaffold at.")
        .defaultValue(120)
        .range(-64, 320)
        .sliderRange(-64, 320)
        .visible(() -> scaffold.get() && keepY.get())
        .build()
    );

    private final List<Class<? extends Module>> commonClasses = List.of(
        AutoLog.class,
        HotbarManager.class,
        LiquidFiller.class,
        NetherBorer.class,
        OffhandManager.class
    );

    private final List<Class<? extends Module>> noBaritoneClasses = List.of(
        FreeLook.class,
        RotationNF.class,
        SafeWalk.class
    );

    private final List<Class<? extends Module>> onDeactivateClasses = List.of(
        AutoWalk.class,
        AutoWalkNF.class,
        BaritoneMiner.class
    );

    private boolean worked = false;

    public DiggingTools() {
        super(NFAddon.Main, "digging-tools", "The necessary tools to dig.");
    }

    @Override
    public void onActivate() {
        Modules modules = Modules.get();

        if (useBaritone.get()) {
            modules.get(BaritoneMiner.class).toggle();
        } else noBaritoneClasses.forEach(moduleClass -> modules.get(moduleClass).toggle());

        commonClasses.forEach(moduleClass -> modules.get(moduleClass).toggle());
    }

    @Override
    public void onDeactivate() {
        Modules modules = Modules.get();

        commonClasses.stream().filter(moduleClass -> modules.get(moduleClass).isActive()).forEach(moduleClass -> modules.get(moduleClass).toggle());
        noBaritoneClasses.stream().filter(moduleClass -> modules.get(moduleClass).isActive()).forEach(moduleClass -> modules.get(moduleClass).toggle());
        onDeactivateClasses.stream().filter(moduleClass -> modules.get(moduleClass).isActive()).forEach(moduleClass -> modules.get(moduleClass).toggle());
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        float f = MathHelper.sin(mc.player.getYaw() * 0.017453292f);
        float g = MathHelper.cos(mc.player.getYaw() * 0.017453292f);

        for (int i = 0; i <= (mc.player.getVelocity().x == 0.0 && mc.player.getVelocity().z == 0.0 ? 0 : ext.get()); i++) {
            // Loop body
            Vec3d pos = mc.player.getPos().add(-f * i, -0.5, g * i);
            if (keepY.get()) ((IVec3d) pos).meteor$setY(height.get() - 1.0);

            BlockPos bPos = BlockPos.ofFloored(pos);

            if (!mc.world.getBlockState(bPos).isReplaceable()) {
                worked = false;
                continue;
            }
            worked = true;

            // Find slot with a block
            FindItemResult item;
            if (listMode.get() == ListMode.Whitelist) {
                item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && whitelist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
            } else {
                item = InvUtils.findInHotbar(itemStack -> itemStack.getItem() instanceof BlockItem && !blacklist.get().contains(Block.getBlockFromItem(itemStack.getItem())));
            }
            if (!item.found()) {
                return;
            } else {
                InvUtils.swap(item.slot(), true);
            }

            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, new BlockHitResult(pos, Direction.getFacing(pos).getOpposite(), bPos, true), 0));

            InvUtils.swapBack();
        }
    }

    public boolean scaffoldPlaced() {
        return worked;
    }

    public enum ListMode {
        Blacklist,
        Whitelist
    }
}
