package thaumicenergistics.network.packets;

import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStack;
import thaumicenergistics.client.gui.helpers.TerminalDisplayStacks;
import thaumicenergistics.client.gui.part.GuiArcaneTerminal;
import thaumicenergistics.util.ThELog;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author BrockWS
 */
public class PacketMEItemUpdate implements IMessage {

    private static final int UNCOMPRESSED_PACKET_BYTE_LIMIT = 16 * 1024 * 1024;
    private static final int OPERATION_BYTE_LIMIT = 2 * 1024;
    private static final int TEMP_BUFFER_SIZE = 1024;

    private final List<TerminalDisplayStack> list;

    private int writtenBytes = 0;
    private boolean empty = true;
    private boolean clearExisting = true;

    public PacketMEItemUpdate() throws IOException {
        this.list = new ArrayList<>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (!buf.isReadable()) {
            return;
        }
        byte[] compressed = new byte[buf.readableBytes()];
        buf.readBytes(compressed);
        try (GZIPInputStream gzReader = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            ByteArrayOutputStream uncompressed = new ByteArrayOutputStream();
            byte[] tmp = new byte[TEMP_BUFFER_SIZE];
            while (true) {
                int bytes = gzReader.read(tmp);
                if (bytes > 0) {
                    uncompressed.write(tmp, 0, bytes);
                } else if (bytes < 0) {
                    break;
                }
            }
            PacketBuffer packetBuffer = new PacketBuffer(io.netty.buffer.Unpooled.wrappedBuffer(uncompressed.toByteArray()));
            this.clearExisting = packetBuffer.readBoolean();
            int rows = packetBuffer.readVarInt();
            for (int i = 0; i < rows; i++) {
                AEKey key = AEKey.readKey(packetBuffer);
                long amount = packetBuffer.readLong();
                boolean craftable = packetBuffer.readBoolean();
                if (key instanceof AEItemKey) {
                    this.list.add(TerminalDisplayStacks.of(key, amount, craftable));
                }
            }
        } catch (IOException | RuntimeException e) {
            ThELog.error("fromBytes IOException", e);
            this.list.clear();
            this.clearExisting = true;
        }
        this.empty = this.list.isEmpty();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        try {
            PacketBuffer packetBuffer = new PacketBuffer(io.netty.buffer.Unpooled.buffer(Math.max(OPERATION_BYTE_LIMIT, this.list.size() * 32)));
            packetBuffer.writeBoolean(this.clearExisting);
            packetBuffer.writeVarInt(this.list.size());
            for (TerminalDisplayStack stack : this.list) {
                AEKey.writeKey(packetBuffer, stack.key());
                packetBuffer.writeLong(stack.stackSize());
                packetBuffer.writeBoolean(stack.craftable());
            }
            byte[] uncompressed = new byte[packetBuffer.readableBytes()];
            packetBuffer.readBytes(uncompressed);
            ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            try (GZIPOutputStream gzWriter = new GZIPOutputStream(compressed)) {
                gzWriter.write(uncompressed);
            }
            if (compressed.size() > 2 * 1024 * 1024) {
                throw new IllegalArgumentException("Sorry, ThE made a " + compressed.size() + " byte packet by accident!");
            }
            buf.writeBytes(compressed.toByteArray());
        } catch (IOException e) {
            ThELog.error("toBytes IOException", e);
        }
    }

    public void appendStack(TerminalDisplayStack stack) throws IOException, BufferOverflowException {
        if (stack == null || !(stack.key() instanceof AEItemKey)) {
            return;
        }

        int estimatedBytes = OPERATION_BYTE_LIMIT;
        if (writtenBytes + estimatedBytes > UNCOMPRESSED_PACKET_BYTE_LIMIT) {
            throw new BufferOverflowException();
        } else {
            writtenBytes += estimatedBytes;
            this.list.add(stack.copy());
            this.empty = false;
        }
    }

    public void appendStack(AEKey key, long amount, boolean craftable) throws IOException, BufferOverflowException {
        this.appendStack(TerminalDisplayStacks.of(key, amount, craftable));
    }

    public void setClearExisting(boolean clearExisting) {
        this.clearExisting = clearExisting;
    }

    public boolean shouldClearExisting() {
        return this.clearExisting;
    }

    public static class Handler implements IMessageHandler<PacketMEItemUpdate, IMessage> {

        @Override
        public IMessage onMessage(PacketMEItemUpdate message, MessageContext ctx) {
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> {
                if (Minecraft.getMinecraft().currentScreen instanceof GuiArcaneTerminal) {
                    GuiArcaneTerminal gui = (GuiArcaneTerminal) Minecraft.getMinecraft().currentScreen;
                    gui.onMEStorageUpdate(message.list, message.clearExisting);
                }
            });
            return null;
        }
    }

    public boolean isEmpty() {
        return this.empty;
    }
}
