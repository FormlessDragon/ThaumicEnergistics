package thaumicenergistics.network;

import ae2.core.network.AppEngPayloadHandler;
import ae2.core.network.InitNetwork;
import net.minecraft.entity.player.EntityPlayerMP;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.util.ThELog;

/**
 * Registers Thaumic Energistics packets on Supergiant's AE2 network channel.
 */
public final class ThENetwork {

    private static boolean registered;

    private ThENetwork() {
    }

    public static synchronized void register() {
        if (registered) {
            return;
        }

        InitNetwork.registerClientbound(AppEngPayloadHandler.Client.class, PacketOpenLocatorGUI.class);
        registered = true;
    }

    public static void sendOpenLocatorGui(EntityPlayerMP player, PacketOpenLocatorGUI packet) {
        if (player == null) {
            String diagnostic = "Cannot send locator-aware GUI packet without a server player";
            ThELog.error(diagnostic);
            throw new IllegalArgumentException(diagnostic);
        }
        if (packet == null) {
            String diagnostic = "Cannot send null locator-aware GUI packet to " + player.getName();
            ThELog.error(diagnostic);
            throw new IllegalArgumentException(diagnostic);
        }

        InitNetwork.sendToClient(player, packet);
    }
}
