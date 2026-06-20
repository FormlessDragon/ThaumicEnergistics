package thaumicenergistics.network.packets;

import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.GuiHostLocators;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.init.ModGUIs;

/**
 * Opens a ThE GUI on the client with AE2's full host locator payload.
 */
public class PacketOpenLocatorGUI implements IMessage {

    private ModGUIs gui;
    private GuiHostLocator locator;
    private boolean returnedFromSubScreen;
    private int windowId;

    public PacketOpenLocatorGUI() {
    }

    public PacketOpenLocatorGUI(ModGUIs gui, GuiHostLocator locator, boolean returnedFromSubScreen, int windowId) {
        validateSupportedGui(gui);
        if (locator == null) {
            throw new IllegalArgumentException("PacketOpenLocatorGUI locator cannot be null for gui " + gui);
        }
        validateWindowId(windowId);
        this.gui = gui;
        this.locator = locator;
        this.returnedFromSubScreen = returnedFromSubScreen;
        this.windowId = windowId;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        int guiOrdinal = packetBuffer.readUnsignedByte();
        this.gui = validateGuiOrdinal(guiOrdinal);
        this.locator = GuiHostLocators.readFromPacket(packetBuffer);
        if (this.locator == null) {
            throw new IllegalArgumentException("PacketOpenLocatorGUI locator cannot be null for gui " + this.gui);
        }
        this.returnedFromSubScreen = packetBuffer.readBoolean();
        this.windowId = packetBuffer.readVarInt();
        validateWindowId(this.windowId);

        int trailingBytes = packetBuffer.readableBytes();
        if (trailingBytes != 0) {
            throw new IllegalArgumentException("Invalid PacketOpenLocatorGUI trailing byte count: " + trailingBytes);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        PacketBuffer packetBuffer = new PacketBuffer(buf);
        ModGUIs validatedGui = validateSupportedGui(this.gui);
        if (this.locator == null) {
            throw new IllegalArgumentException("PacketOpenLocatorGUI locator cannot be null for gui " + validatedGui);
        }
        packetBuffer.writeByte(validatedGui.ordinal());
        GuiHostLocators.writeToPacket(packetBuffer, this.locator);
        packetBuffer.writeBoolean(this.returnedFromSubScreen);
        packetBuffer.writeVarInt(validateWindowId(this.windowId));
    }

    public ModGUIs gui() {
        return validateSupportedGui(this.gui);
    }

    public GuiHostLocator locator() {
        if (this.locator == null) {
            throw new IllegalArgumentException("PacketOpenLocatorGUI locator cannot be null for gui " + this.gui);
        }
        return this.locator;
    }

    public boolean returnedFromSubScreen() {
        return this.returnedFromSubScreen;
    }

    public int windowId() {
        return validateWindowId(this.windowId);
    }

    static ModGUIs validateGuiOrdinal(int guiOrdinal) {
        ModGUIs[] guis = ModGUIs.values();
        if (guiOrdinal < 0 || guiOrdinal >= guis.length) {
            throw new IllegalArgumentException("Invalid PacketOpenLocatorGUI gui ordinal: " + guiOrdinal);
        }
        return validateSupportedGui(guis[guiOrdinal]);
    }

    static ModGUIs validateSupportedGui(ModGUIs gui) {
        if (gui == null) {
            throw new IllegalArgumentException("PacketOpenLocatorGUI gui cannot be null");
        }
        switch (gui) {
            case ARCANE_TERMINAL:
            case ARCANE_INSCRIBER:
            case ARCANE_ASSEMBLER:
            case KNOWLEDGE_CORE_ADD:
            case KNOWLEDGE_CORE_DEL:
            case KNOWLEDGE_CORE_VIEW:
            case WIRELESS_ARCANE_TERMINAL:
                return gui;
            default:
                throw new IllegalArgumentException("Unsupported PacketOpenLocatorGUI gui: " + gui);
        }
    }

    static int validateWindowId(int windowId) {
        if (windowId < 0) {
            throw new IllegalArgumentException("PacketOpenLocatorGUI windowId cannot be negative: " + windowId);
        }
        return windowId;
    }

    public static class Handler implements IMessageHandler<PacketOpenLocatorGUI, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenLocatorGUI message, MessageContext ctx) {
            ThaumicEnergistics.proxy.openLocatorGui(message, ctx);
            return null;
        }
    }
}
