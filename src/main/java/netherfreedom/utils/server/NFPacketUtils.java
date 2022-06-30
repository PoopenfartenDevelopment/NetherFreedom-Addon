package netherfreedom.utils.server;

import netherfreedom.utils.NFEntityUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class NFPacketUtils {

    public static Entity deadEntity;
    public static boolean isDeathPacket(PacketEvent.Receive event) {
        if (event.packet instanceof EntityStatusS2CPacket packet) {
            if (packet.getStatus() == NFEntityUtils.DeathPacket) {
                deadEntity = packet.getEntity(mc.world);
                if (deadEntity instanceof PlayerEntity) return true;
            }
        }

        return false;
    }
}
