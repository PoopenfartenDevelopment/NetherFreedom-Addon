package me.redcarlos.netherfreedom.modules.main;

import me.redcarlos.netherfreedom.NFAddon;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoLog;
import meteordevelopment.meteorclient.systems.modules.movement.SafeWalk;
import meteordevelopment.meteorclient.systems.modules.render.FreeLook;
import meteordevelopment.meteorclient.systems.modules.world.LiquidFiller;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
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
        .name("use-baritone")
        .description("Use baritone to automate the digging process (EXPERIMENTAL).")
        .defaultValue(false)
        .build()
    );

    private final Setting<Boolean> pickToggle = sgGeneral.add(new BoolSetting.Builder()
        .name("pickaxe-toggle")
        .description("Automatically disables DiggingTools when you run out of pickaxes.")
        .defaultValue(true)
        .visible(() -> !useBaritone.get())
        .build()
    );

    private final Setting<Boolean> scaffold = sgScaffold.add(new BoolSetting.Builder()
        .name("scaffold")
        .description("Scaffolds blocks under you.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> ext = sgScaffold.add(new IntSetting.Builder()
        .name("extend")
        .description("How much to place in front of you.")
        .defaultValue(2)
        .range(0, 5)
        .build()
    );

    private final Setting<Integer> keepY = sgScaffold.add(new IntSetting.Builder()
        .name("keepY")
        .description("Keeps a specific Y level when digging.")
        .defaultValue(120)
        .range(-1, 255)
        .sliderRange(-1, 255)
        .visible(scaffold::get)
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

    private int slot = -1;
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

        if (modules.get(BaritoneMiner.class).isActive()) modules.get(BaritoneMiner.class).toggle();
        noBaritoneClasses.stream().filter(moduleClass -> modules.get(moduleClass).isActive());
        commonClasses.stream().filter(moduleClass -> modules.get(moduleClass).isActive());
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (disableOnDisconnect.get() && event.screen instanceof DisconnectedScreen) toggle();
    }

    @EventHandler
    public void tick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        float f = MathHelper.sin(mc.player.getYaw() * 0.017453292f);
        float g = MathHelper.cos(mc.player.getYaw() * 0.017453292f);
        int prevSlot = mc.player.getInventory().selectedSlot;

        if (mc.player.getY() <= keepY.get()) return;

        for (int i = 0; i <= (mc.player.getVelocity().x == 0.0 && mc.player.getVelocity().z == 0.0 ? 0 : ext.get()); i++) {
            // Loop body
            Vec3d pos = mc.player.getPos().add(-f * i, -1.0, g * i);
            if (keepY.get() != -1) ((IVec3d) pos).meteor$setY(keepY.get() - 1.0);
            BlockPos bpos = BlockPos.ofFloored(pos);
            if (!mc.world.getBlockState(bpos).isReplaceable()) {
                worked = false;
                continue;
            }
            worked = true;

            boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
            boolean mainHand = mc.player.getMainHandStack().getItem() instanceof BlockItem;
            if (!offHand && !mainHand) {
                for (int j = 0; j <= 8; j++) {
                    if (mc.player.getInventory().getStack(j).getItem() instanceof BlockItem) {
                        slot = j;
                        break;
                    }
                }
                if (slot == -1) return;
                mc.player.getInventory().selectedSlot = slot;
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
            }

            mc.player.networkHandler.sendPacket(
                new PlayerInteractBlockC2SPacket(
                    offHand ? Hand.OFF_HAND : Hand.MAIN_HAND,
                    new BlockHitResult(pos, Direction.DOWN, bpos, false),
                    0
                )
            );
            slot = -1;
        }

        if (mc.player.getInventory().selectedSlot != prevSlot) {
            mc.player.getInventory().selectedSlot = prevSlot;
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(prevSlot));
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (pickToggle.get() && !useBaritone.get()) {
            FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() == Items.DIAMOND_PICKAXE || itemStack.getItem() == Items.NETHERITE_PICKAXE);

            if (!pickaxe.found()) {
                error("No pickaxe found, disabling.");
                toggle();
            }
        }
    }

    public boolean scaffoldPlaced() {
        return worked;
    }
}
