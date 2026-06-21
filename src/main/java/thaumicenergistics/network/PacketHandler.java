package thaumicenergistics.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import thaumicenergistics.network.packets.PacketAssemblerGUIUpdate;
import thaumicenergistics.network.packets.PacketAssemblerGUIUpdateRequest;
import thaumicenergistics.network.packets.PacketEssentiaFilter;
import thaumicenergistics.network.packets.PacketInvHeldUpdate;
import thaumicenergistics.network.packets.PacketIsArcaneUpdate;
import thaumicenergistics.network.packets.PacketOpenGUI;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.network.packets.PacketPlaySound;
import thaumicenergistics.network.packets.PacketSubscribe;
import thaumicenergistics.network.packets.PacketVisUpdate;
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

        PacketHandler.INSTANCE.registerMessage(PacketEssentiaFilter.Handler.class, PacketEssentiaFilter.class, PacketHandler.nextID(), Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(PacketInvHeldUpdate.Handler.class, PacketInvHeldUpdate.class, PacketHandler.nextID(), Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(PacketVisUpdate.Handler.class, PacketVisUpdate.class, PacketHandler.nextID(), Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(PacketIsArcaneUpdate.Handler.class, PacketIsArcaneUpdate.class, PacketHandler.nextID(), Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(PacketPlaySound.Handler.class, PacketPlaySound.class, PacketHandler.nextID(), Side.CLIENT);
        PacketHandler.INSTANCE.registerMessage(PacketAssemblerGUIUpdate.Handler.class, PacketAssemblerGUIUpdate.class, PacketHandler.nextID(), Side.CLIENT);

        PacketHandler.INSTANCE.registerMessage(PacketOpenGUI.Handler.class, PacketOpenGUI.class, PacketHandler.nextID(), Side.SERVER);
        PacketHandler.INSTANCE.registerMessage(PacketSubscribe.Handler.class, PacketSubscribe.class, PacketHandler.nextID(), Side.SERVER);
        PacketHandler.INSTANCE.registerMessage(PacketAssemblerGUIUpdateRequest.Handler.class, PacketAssemblerGUIUpdateRequest.class, PacketHandler.nextID(), Side.SERVER);
        PacketHandler.INSTANCE.registerMessage(PacketOpenLocatorGUI.Handler.class, PacketOpenLocatorGUI.class, PacketHandler.nextID(), Side.CLIENT);
    }

    public static void sendToPlayer(EntityPlayerMP player, IMessage message) {
        PacketHandler.INSTANCE.sendTo(message, player);
    }

    public static void sendToServer(IMessage message) {
        if (!(message instanceof PacketVisUpdate)) {
            ByteBuf buf = Unpooled.buffer();
            message.toBytes(buf);
            ThELog.trace("sendToServer readableBytes {} | read {} | write {} | message {}", buf.readableBytes(), buf.readerIndex(), buf.writerIndex(), message.getClass().getSimpleName());
        }
        PacketHandler.INSTANCE.sendToServer(message);
    }

    public static void sendToAll(IMessage message) {
        if (!(message instanceof PacketVisUpdate)) {
            ByteBuf buf = Unpooled.buffer();
            message.toBytes(buf);
            ThELog.trace("sendToAll readableBytes {} | read {} | write {} | message {}", buf.readableBytes(), buf.readerIndex(), buf.writerIndex(), message.getClass().getSimpleName());
        }
        PacketHandler.INSTANCE.sendToAll(message);
    }
}
