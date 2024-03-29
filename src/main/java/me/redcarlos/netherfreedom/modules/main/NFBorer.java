package me.redcarlos.netherfreedom.modules.main;

import me.redcarlos.netherfreedom.NFAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

import java.util.stream.IntStream;

import static me.redcarlos.netherfreedom.utils.NFUtils.*;

public class NFBorer extends Module {
    /**
     * Last time packets were sent
     */
    private long lastUpdateTime = 0;
    /**
     * Floored block position of player
     */
    private BlockPos playerPos = BlockPos.ORIGIN;
    private int packets = 0;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> keepY = sgGeneral.add(new IntSetting.Builder()
        .name("KeepY")
        .description("Keeps a specific Y level when digging.")
        .defaultValue(120)
        .range(-1, 255)
        .sliderRange(-1, 255)
        .build()
    );

    public NFBorer() {
        super(NFAddon.Main, "NF-borer", "does the funni");
    }

    @EventHandler
    public void tick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;
        // previous floored block position of player
        BlockPos prevBlockPos = playerPos;
        playerPos = new BlockPos(
            MathHelper.floor(mc.player.getX()),
            keepY.get() != -1 ? keepY.get() : MathHelper.floor(mc.player.getY()),
            MathHelper.floor(mc.player.getZ())
        );

        if (playerPos != prevBlockPos || Util.getMeasuringTimeMs() - lastUpdateTime > 800) {
            mineArea(playerPos.add(0, 0, 0));
        }

        lastUpdateTime = Util.getMeasuringTimeMs();
        packets = 0;
    }

    protected void mineArea(BlockPos playerPos) {
        IntStream.rangeClosed(-4, 4).forEach(i -> {
            breakBlock(forward(playerPos, i));
            breakBlock(forward(playerPos, i).up());
            breakBlock(forward(playerPos, i).up(2));
            breakBlock(forward(playerPos, i).up(3));

            breakBlock(right(forward(playerPos, i), 1));
            breakBlock(right(forward(playerPos, i), 1).up());
            breakBlock(right(forward(playerPos, i), 1).up(2));
            breakBlock(right(forward(playerPos, i), 1).up(3));

            breakBlock(right(forward(playerPos, i), 2));
            breakBlock(right(forward(playerPos, i), 2).up());
            breakBlock(right(forward(playerPos, i), 2).up(2));
            breakBlock(right(forward(playerPos, i), 2).up(3));

            breakBlock(left(forward(playerPos, i), 1));
            breakBlock(left(forward(playerPos, i), 1).up());
            breakBlock(left(forward(playerPos, i), 1).up(2));
            breakBlock(left(forward(playerPos, i), 1).up(3));

            breakBlock(left(forward(playerPos, i), 2));
            breakBlock(left(forward(playerPos, i), 2).up());
            breakBlock(left(forward(playerPos, i), 2).up(2));
            breakBlock(left(forward(playerPos, i), 2).up(3));

            breakBlock(left(forward(playerPos, i), 3));
            breakBlock(left(forward(playerPos, i), 3).up());
            breakBlock(left(forward(playerPos, i), 3).up(2));
            breakBlock(left(forward(playerPos, i), 3).up(3));
        });
    }

    protected void breakBlock(BlockPos blockPos) {
        if (mc.player == null || mc.world == null) return;

        if (packets >= 130 || mc.world.getBlockState(blockPos).isReplaceable()) {
            return;
        }

        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.UP));
        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.UP));
        packets += 2;
    }
}
