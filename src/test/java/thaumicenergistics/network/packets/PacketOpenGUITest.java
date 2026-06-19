package thaumicenergistics.network.packets;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.init.ModGUIs;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    private static PacketOpenGUI roundTrip(PacketOpenGUI packet) {
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);

        PacketOpenGUI decoded = new PacketOpenGUI();
        decoded.fromBytes(buffer);

        assertEquals(0, buffer.readableBytes(), "PacketOpenGUI should consume its legacy payload");
        return decoded;
    }
}
