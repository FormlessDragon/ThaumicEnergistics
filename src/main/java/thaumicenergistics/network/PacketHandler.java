package thaumicenergistics.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.util.ThELog;

/**
 * @author BrockWS
 */
public class PacketHandler {

    public static SimpleNetworkWrapper INSTANCE = null;
    private static int PACKETID = 0;

    public static int nextID() {
        return PacketHandler.PACKETID++;
    }

    public static void register() {
        if (PacketHandler.INSTANCE != null)
            return;
        PacketHandler.INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);

        PacketHandler.INSTANCE.registerMessage(PacketOpenLocatorGUI.Handler.class, PacketOpenLocatorGUI.class, PacketHandler.nextID(), Side.CLIENT);
    }

    public static void sendToPlayer(EntityPlayerMP player, IMessage message) {
        PacketHandler.INSTANCE.sendTo(message, player);
    }

    public static void sendToAll(IMessage message) {
        ByteBuf buf = Unpooled.buffer();
        message.toBytes(buf);
        ThELog.trace("sendToAll readableBytes {} | read {} | write {} | message {}", buf.readableBytes(), buf.readerIndex(), buf.writerIndex(), message.getClass().getSimpleName());
        PacketHandler.INSTANCE.sendToAll(message);
    }
}
