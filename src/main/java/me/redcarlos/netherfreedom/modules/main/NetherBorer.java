package me.redcarlos.netherfreedom.modules.main;

import me.redcarlos.netherfreedom.NFAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.stream.IntStream;

import static me.redcarlos.netherfreedom.utils.NFUtils.*;

public class NetherBorer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> keepY = sgGeneral.add(new IntSetting.Builder()
        .name("keepY")
        .description("Keeps a specific Y level when digging.")
        .defaultValue(120)
        .range(-1, 255)
        .sliderRange(-1, 255)
        .build()
    );

    private int packets = 0;
    private long lastUpdateTime = 0; // Last time packets were sent
    private BlockPos playerPos = BlockPos.ORIGIN; // Floored block position of player

    public NetherBorer() {
        super(NFAddon.Main, "NF-borer", "Digs netherrack. Optimized for NetherFreedom.");
    }

    @EventHandler
    public void tick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        // Previous floored block position of player
        BlockPos prevBlockPos = playerPos;
        playerPos = new BlockPos(
            MathHelper.floor(mc.player.getX()),
            keepY.get() != -1 ? keepY.get() : MathHelper.floor(mc.player.getY()),
            MathHelper.floor(mc.player.getZ())
        );

        if (playerPos != prevBlockPos || Util.getMeasuringTimeMs() - lastUpdateTime > 800) {
            mineArea(playerPos.add(0, 0, 0));
            lastUpdateTime = Util.getMeasuringTimeMs();
        }
        packets = 0;
    }

    protected void mineArea(BlockPos playerPos) {
        IntStream.rangeClosed(-4, 4).forEach(i -> {
            breakBlock(forward(playerPos, i));
            breakBlock(forward(playerPos, i).up());
            breakBlock(forward(playerPos, i).up(2));

            breakBlock(right(forward(playerPos, i), 1));
            breakBlock(right(forward(playerPos, i), 1).up());
            breakBlock(right(forward(playerPos, i), 1).up(2));

            breakBlock(right(forward(playerPos, i), 2));
            breakBlock(right(forward(playerPos, i), 2).up());
            breakBlock(right(forward(playerPos, i), 2).up(2));

            breakBlock(right(forward(playerPos, i), 3));
            breakBlock(right(forward(playerPos, i), 3).up());
            breakBlock(right(forward(playerPos, i), 3).up(2));

            breakBlock(right(forward(playerPos, i), 4));
            breakBlock(right(forward(playerPos, i), 4).up());
            breakBlock(right(forward(playerPos, i), 4).up(2));

            breakBlock(left(forward(playerPos, i), 1));
            breakBlock(left(forward(playerPos, i), 1).up());
            breakBlock(left(forward(playerPos, i), 1).up(2));

            breakBlock(left(forward(playerPos, i), 2));
            breakBlock(left(forward(playerPos, i), 2).up());
            breakBlock(left(forward(playerPos, i), 2).up(2));

            breakBlock(left(forward(playerPos, i), 3));
            breakBlock(left(forward(playerPos, i), 3).up());
            breakBlock(left(forward(playerPos, i), 3).up(2));

            breakBlock(left(forward(playerPos, i), 4));
            breakBlock(left(forward(playerPos, i), 4).up());
            breakBlock(left(forward(playerPos, i), 4).up(2));

            breakBlock(backward(playerPos, i));
            breakBlock(backward(playerPos, i).up());
            breakBlock(backward(playerPos, i).up(2));
        });
    }

    protected void breakBlock(BlockPos blockPos) {
        if (mc.player == null || mc.world == null) return;

        if (!(mc.player.getMainHandStack().getItem() instanceof PickaxeItem) || packets >= 130 || mc.world.getBlockState(blockPos).isReplaceable()) {
            return;
        }

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        packets += 2;

        mc.player.getInventory().updateItems();
    }
}
