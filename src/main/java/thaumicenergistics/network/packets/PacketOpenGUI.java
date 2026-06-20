package thaumicenergistics.network.packets;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.init.ModGUIs;

/**
 * @author BrockWS
 */
public class PacketOpenGUI implements IMessage {

    public int gui;
    public BlockPos pos;
    public EnumFacing side;

    public PacketOpenGUI() {
    }

    public PacketOpenGUI(ModGUIs gui, BlockPos pos, EnumFacing side) {
        this.gui = gui.ordinal();
        this.pos = pos;
        this.side = side;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int guiOrdinal = buf.readUnsignedByte();
        this.gui = validateGuiOrdinal(guiOrdinal).ordinal();
        this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
        int sideOrdinal = buf.readUnsignedByte();
        this.side = validateSideOrdinal(sideOrdinal);
        int trailingBytes = buf.readableBytes();
        if (trailingBytes != 0) {
            throw new IllegalArgumentException("Invalid PacketOpenGUI trailing byte count: " + trailingBytes);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ModGUIs gui = this.validatedGui();
        BlockPos pos = this.validatedPos();
        EnumFacing side = this.validatedSide();
        buf.writeByte(gui.ordinal());
        buf.writeInt(pos.getX());
        buf.writeInt(pos.getY());
        buf.writeInt(pos.getZ());
        buf.writeByte(side.ordinal());
    }

    public ModGUIs validatedGui() {
        return validateGuiOrdinal(this.gui);
    }

    public EnumFacing validatedSide() {
        if (this.side == null) {
            throw new IllegalArgumentException("Invalid PacketOpenGUI side value: null");
        }
        return validateSideOrdinal(this.side.ordinal());
    }

    public BlockPos validatedPos() {
        if (this.pos == null) {
            throw new IllegalArgumentException("Invalid PacketOpenGUI pos value: null");
        }
        return this.pos;
    }

    private static ModGUIs validateGuiOrdinal(int guiOrdinal) {
        ModGUIs[] guis = ModGUIs.values();
        if (guiOrdinal < 0 || guiOrdinal >= guis.length) {
            throw new IllegalArgumentException("Invalid PacketOpenGUI gui ordinal: " + guiOrdinal);
        }
        return guis[guiOrdinal];
    }

    private static EnumFacing validateSideOrdinal(int sideOrdinal) {
        EnumFacing[] sides = EnumFacing.values();
        if (sideOrdinal < 0 || sideOrdinal >= sides.length) {
            throw new IllegalArgumentException("Invalid PacketOpenGUI side ordinal: " + sideOrdinal);
        }
        return sides[sideOrdinal];
    }

    public static class Handler implements IMessageHandler<PacketOpenGUI, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenGUI message, MessageContext ctx) {
            ThaumicEnergistics.proxy.openGui(message, ctx);
            return null;
        }
    }
}
