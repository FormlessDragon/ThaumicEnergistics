package thaumicenergistics.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.junit.jupiter.api.Test;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.core.CommonProxy;
import thaumicenergistics.init.ModGUIs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketOpenGUITest {

    @Test
    void roundTripsLegacyOpenGuiPayload() {
        PacketOpenGUI decoded = roundTrip(new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL,
                new BlockPos(12, 34, 56), EnumFacing.WEST));

        assertAll(
                () -> assertEquals(ModGUIs.ARCANE_TERMINAL.ordinal(), decoded.gui),
                () -> assertEquals(new BlockPos(12, 34, 56), decoded.pos),
                () -> assertSame(EnumFacing.WEST, decoded.side));
    }

    @Test
    void roundTripDoesNotUsePackedForgeGuiOrdinal() {
        PacketOpenGUI packet = new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL, BlockPos.ORIGIN, EnumFacing.DOWN);
        ByteBuf buffer = Unpooled.buffer();

        packet.toBytes(buffer);
        int serializedGui = buffer.readUnsignedByte();

        assertAll(
                () -> assertEquals(ModGUIs.ARCANE_TERMINAL.ordinal(), serializedGui),
                () -> assertNotEquals(GuiHandler.calculateOrdinal(ModGUIs.ARCANE_TERMINAL, EnumFacing.DOWN),
                        serializedGui),
                () -> assertEquals(13, buffer.readableBytes(),
                        "legacy PacketOpenGUI payload remains BlockPos ints + side byte after gui byte"));
    }

    @Test
    void rejectsInvalidGuiOrdinalWithDiagnosticMessage() {
        int invalidGuiOrdinal = ModGUIs.values().length;
        ByteBuf buffer = legacyPayload(invalidGuiOrdinal, BlockPos.ORIGIN, EnumFacing.NORTH.ordinal());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PacketOpenGUI().fromBytes(buffer));

        assertTrue(exception.getMessage().contains(Integer.toString(invalidGuiOrdinal)));
    }

    @Test
    void rejectsInvalidSideOrdinalWithDiagnosticMessage() {
        int invalidSideOrdinal = EnumFacing.values().length;
        ByteBuf buffer = legacyPayload(ModGUIs.ARCANE_TERMINAL.ordinal(), BlockPos.ORIGIN, invalidSideOrdinal);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PacketOpenGUI().fromBytes(buffer));

        assertTrue(exception.getMessage().contains(Integer.toString(invalidSideOrdinal)));
    }

    @Test
    void rejectsTrailingBytesWithDiagnosticMessage() {
        ByteBuf buffer = legacyPayload(ModGUIs.ARCANE_TERMINAL.ordinal(), BlockPos.ORIGIN, EnumFacing.UP.ordinal());
        buffer.writeByte(0x7F);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PacketOpenGUI().fromBytes(buffer));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("trailing")),
                () -> assertTrue(exception.getMessage().contains("1")));
    }

    @Test
    void rejectsMutatedGuiOrdinalThroughProductionHandlerValidation() {
        int invalidGuiOrdinal = ModGUIs.values().length;
        PacketOpenGUI packet = new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL, BlockPos.ORIGIN, EnumFacing.UP);
        packet.gui = invalidGuiOrdinal;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, packet::validatedGui);

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenGUI")),
                () -> assertTrue(exception.getMessage().contains("gui")),
                () -> assertTrue(exception.getMessage().contains(Integer.toString(invalidGuiOrdinal))));
    }

    @Test
    void rejectsMutatedNullSideThroughProductionHandlerValidation() {
        PacketOpenGUI packet = new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL, BlockPos.ORIGIN, EnumFacing.UP);
        packet.side = null;

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, packet::validatedSide);

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenGUI")),
                () -> assertTrue(exception.getMessage().contains("side")),
                () -> assertTrue(exception.getMessage().contains("null")));
    }

    @Test
    void rejectsMutatedInvalidGuiOrdinalBeforeWritingPayload() {
        int invalidGuiOrdinal = ModGUIs.values().length;
        PacketOpenGUI packet = new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL, BlockPos.ORIGIN, EnumFacing.UP);
        packet.gui = invalidGuiOrdinal;
        ByteBuf buffer = Unpooled.buffer();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> packet.toBytes(buffer));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenGUI")),
                () -> assertTrue(exception.getMessage().contains("gui")),
                () -> assertTrue(exception.getMessage().contains(Integer.toString(invalidGuiOrdinal))),
                () -> assertEquals(0, buffer.writerIndex()));
    }

    @Test
    void rejectsMutatedNullPosBeforeWritingPayload() {
        PacketOpenGUI packet = new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL, BlockPos.ORIGIN, EnumFacing.UP);
        packet.pos = null;
        ByteBuf buffer = Unpooled.buffer();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> packet.toBytes(buffer));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenGUI")),
                () -> assertTrue(exception.getMessage().contains("pos")),
                () -> assertTrue(exception.getMessage().contains("null")),
                () -> assertEquals(0, buffer.writerIndex()));
    }

    @Test
    void rejectsMutatedNullSideBeforeWritingPayload() {
        PacketOpenGUI packet = new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL, BlockPos.ORIGIN, EnumFacing.UP);
        packet.side = null;
        ByteBuf buffer = Unpooled.buffer();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> packet.toBytes(buffer));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenGUI")),
                () -> assertTrue(exception.getMessage().contains("side")),
                () -> assertTrue(exception.getMessage().contains("null")),
                () -> assertEquals(0, buffer.writerIndex()));
    }

    @Test
    void handlerDelegatesServerOpenToProxy() {
        PacketOpenGUI packet = new PacketOpenGUI(ModGUIs.ARCANE_TERMINAL, new BlockPos(2, 3, 4), EnumFacing.NORTH);
        CommonProxy previousProxy = ThaumicEnergistics.proxy;
        RecordingProxy proxy = new RecordingProxy();
        ThaumicEnergistics.proxy = proxy;

        try {
            new PacketOpenGUI.Handler().onMessage(packet, null);
        } finally {
            ThaumicEnergistics.proxy = previousProxy;
        }

        assertAll(
                () -> assertSame(packet, proxy.message),
                () -> assertEquals(1, proxy.calls));
    }

    private static PacketOpenGUI roundTrip(PacketOpenGUI packet) {
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);

        PacketOpenGUI decoded = new PacketOpenGUI();
        decoded.fromBytes(buffer);

        assertEquals(0, buffer.readableBytes(), "PacketOpenGUI should consume its legacy payload");
        return decoded;
    }

    private static ByteBuf legacyPayload(int guiOrdinal, BlockPos pos, int sideOrdinal) {
        ByteBuf buffer = Unpooled.buffer();
        buffer.writeByte(guiOrdinal);
        buffer.writeInt(pos.getX());
        buffer.writeInt(pos.getY());
        buffer.writeInt(pos.getZ());
        buffer.writeByte(sideOrdinal);
        return buffer;
    }

    private static final class RecordingProxy extends CommonProxy {

        private PacketOpenGUI message;
        private int calls;

        @Override
        public void openGui(PacketOpenGUI message, MessageContext ctx) {
            this.message = message;
            this.calls++;
        }
    }
}
