package thaumicenergistics.network.packets;

import ae2.api.stacks.AEKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.api.stacks.AEEssentiaKey;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStack;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStacks;
import thaumicenergistics.client.gui.part.GuiEssentiaTerminal;

import java.util.ArrayList;
import java.util.List;

/**
 * @author BrockWS
 */
public class PacketMEEssentiaUpdate implements IMessage {

    private final List<TerminalDisplayStack> list;

    public PacketMEEssentiaUpdate() {
        this.list = new ArrayList<>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        try {
            int rows = packetBuffer.readVarInt();
            for (int i = 0; i < rows; i++) {
                AEKey key = AEKey.readKey(packetBuffer);
                long amount = packetBuffer.readLong();
                boolean craftable = packetBuffer.readBoolean();
                if (key instanceof AEEssentiaKey) {
                    this.appendStack(key, amount, craftable);
                }
            }
        } catch (RuntimeException ignored) {
            this.list.clear();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        packetBuffer.writeVarInt(this.list.size());
        for (TerminalDisplayStack stack : this.list) {
            AEKey.writeKey(packetBuffer, stack.key());
            packetBuffer.writeLong(stack.stackSize());
            packetBuffer.writeBoolean(stack.craftable());
        }
    }

    public void appendStack(TerminalDisplayStack stack) {
        if (stack != null && stack.key() instanceof AEEssentiaKey) {
            this.list.add(stack.copy());
        }
    }

    public void appendStack(AEKey key, long amount, boolean craftable) {
        this.appendStack(TerminalDisplayStacks.of(key, amount, craftable));
    }

    public static class Handler implements IMessageHandler<PacketMEEssentiaUpdate, IMessage> {

        @Override
        public IMessage onMessage(PacketMEEssentiaUpdate message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                if (Minecraft.getMinecraft().currentScreen instanceof GuiEssentiaTerminal) {
                    GuiEssentiaTerminal gui = (GuiEssentiaTerminal) Minecraft.getMinecraft().currentScreen;
                    gui.onMEStorageUpdate(message.list);
                }
            });
            return null;
        }
    }
}
