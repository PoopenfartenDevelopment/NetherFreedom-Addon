package me.redcarlos.netherfreedom.modules.main;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IVec3d;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.BlockItem;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import me.redcarlos.netherfreedom.NFAddon;

public class NFScaffold extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> ext = sgGeneral.add(new IntSetting.Builder()
        .name("extend")
        .description("How much to place in front of you.")
        .defaultValue(3)
        .range(0, 5)
        .build()
    );

    private final Setting<Integer> keepY = sgGeneral.add(new IntSetting.Builder()
        .name("keepY")
        .description("Keeps the Y value of the block.")
        .defaultValue(120)
        .range(-1, 255)
        .sliderRange(-1, 255)
        .build()
    );

    private int slot = -1;
    private boolean worked = false;

    public NFScaffold() {
        super(NFAddon.Main, "NF-scaffold", "Scaffolds blocks under you.");
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
            if (keepY.get() != -1) ((IVec3d) pos).setY(keepY.get() - 1.0);
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

    public boolean hasWorked() {
        return worked;
    }
}
