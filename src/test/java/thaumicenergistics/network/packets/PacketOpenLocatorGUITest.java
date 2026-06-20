package thaumicenergistics.network.packets;

import ae2.core.gui.locator.BaublesItemLocator;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.core.gui.locator.InventoryItemLocator;
import ae2.core.gui.locator.ItemGuiHostLocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.junit.jupiter.api.Test;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.core.CommonProxy;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.PacketHandler;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PacketOpenLocatorGUITest {

    @Test
    void roundTripsInventoryLocatorWirelessPayloadWithBlockHit() {
        RayTraceResult hitResult = GuiHostLocators.createItemUseHitResult(
                new BlockPos(11, 22, 33), EnumFacing.SOUTH, 0.25f, 0.5f, 0.75f);
        PacketOpenLocatorGUI decoded = roundTrip(new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                GuiHostLocators.forInventorySlot(17, hitResult),
                true,
                42));

        InventoryItemLocator locator = assertInstanceOf(InventoryItemLocator.class, decoded.locator());

        assertAll(
                () -> assertSame(ModGUIs.WIRELESS_ARCANE_TERMINAL, decoded.gui()),
                () -> assertTrue(decoded.returnedFromSubScreen()),
                () -> assertEquals(42, decoded.windowId()),
                () -> assertEquals(17, locator.getPlayerInventorySlot()),
                () -> assertBlockHitEquals(hitResult, locator.hitResult()));
    }

    @Test
    void roundTripsBaublesLocatorWirelessPayloadWithBlockHit() {
        RayTraceResult hitResult = GuiHostLocators.createItemUseHitResult(
                new BlockPos(44, 55, 66), EnumFacing.EAST, 0.125f, 0.375f, 0.625f);
        PacketOpenLocatorGUI decoded = roundTrip(new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                new BaublesItemLocator(3, hitResult),
                false,
                91));

        BaublesItemLocator locator = assertInstanceOf(BaublesItemLocator.class, decoded.locator());

        assertAll(
                () -> assertSame(ModGUIs.WIRELESS_ARCANE_TERMINAL, decoded.gui()),
                () -> assertEquals(3, locator.baubleSlot()),
                () -> assertBlockHitEquals(hitResult, locator.hitResult()));
    }

    @Test
    void rejectsInvalidGuiOrdinalWithDiagnosticMessage() {
        ByteBuf buffer = Unpooled.buffer();
        PacketBuffer packetBuffer = new PacketBuffer(buffer);
        packetBuffer.writeByte(ModGUIs.values().length);
        GuiHostLocators.writeToPacket(packetBuffer, GuiHostLocators.forInventorySlot(0));
        packetBuffer.writeBoolean(false);
        packetBuffer.writeVarInt(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PacketOpenLocatorGUI().fromBytes(buffer));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenLocatorGUI")),
                () -> assertTrue(exception.getMessage().contains("gui")),
                () -> assertTrue(exception.getMessage().contains(Integer.toString(ModGUIs.values().length))));
    }

    @Test
    void rejectsTrailingBytesWithDiagnosticMessage() {
        ByteBuf buffer = Unpooled.buffer();
        new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                GuiHostLocators.forInventorySlot(0),
                false,
                7).toBytes(buffer);
        buffer.writeByte(0x7F);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PacketOpenLocatorGUI().fromBytes(buffer));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("trailing")),
                () -> assertTrue(exception.getMessage().contains("1")));
    }

    @Test
    void rejectsUnsupportedGuiAtCreateTime() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PacketOpenLocatorGUI(
                ModGUIs.ARCANE_TERMINAL,
                GuiHostLocators.forInventorySlot(0),
                false,
                1));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(ModGUIs.ARCANE_TERMINAL.name())),
                () -> assertTrue(exception.getMessage().contains("PacketOpenLocatorGUI")));
    }

    @Test
    void rejectsNullGuiAtCreateTime() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PacketOpenLocatorGUI(
                null,
                GuiHostLocators.forInventorySlot(0),
                false,
                1));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenLocatorGUI")),
                () -> assertTrue(exception.getMessage().contains("gui")),
                () -> assertTrue(exception.getMessage().contains("null")));
    }

    @Test
    void rejectsNullLocatorAtCreateTime() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                null,
                false,
                1));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenLocatorGUI")),
                () -> assertTrue(exception.getMessage().contains("locator")),
                () -> assertTrue(exception.getMessage().contains(ModGUIs.WIRELESS_ARCANE_TERMINAL.name())));
    }

    @Test
    void rejectsNegativeWindowIdAtCreateTime() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                GuiHostLocators.forInventorySlot(0),
                false,
                -1));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenLocatorGUI")),
                () -> assertTrue(exception.getMessage().contains("windowId")),
                () -> assertTrue(exception.getMessage().contains("-1")));
    }

    @Test
    void rejectsNegativeWindowIdFromPayload() {
        ByteBuf buffer = Unpooled.buffer();
        PacketBuffer packetBuffer = new PacketBuffer(buffer);
        packetBuffer.writeByte(ModGUIs.WIRELESS_ARCANE_TERMINAL.ordinal());
        GuiHostLocators.writeToPacket(packetBuffer, GuiHostLocators.forInventorySlot(0));
        packetBuffer.writeBoolean(false);
        packetBuffer.writeVarInt(-1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PacketOpenLocatorGUI().fromBytes(buffer));

        assertAll(
                () -> assertTrue(exception.getMessage().contains("PacketOpenLocatorGUI")),
                () -> assertTrue(exception.getMessage().contains("windowId")),
                () -> assertTrue(exception.getMessage().contains("-1")));
    }

    @Test
    void handlerDelegatesClientOpenToProxy() {
        PacketOpenLocatorGUI packet = new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                GuiHostLocators.forInventorySlot(0),
                false,
                8);
        CommonProxy previousProxy = ThaumicEnergistics.proxy;
        RecordingProxy proxy = new RecordingProxy();
        ThaumicEnergistics.proxy = proxy;

        try {
            new PacketOpenLocatorGUI.Handler().onMessage(packet, null);
        } finally {
            ThaumicEnergistics.proxy = previousProxy;
        }

        assertAll(
                () -> assertSame(packet, proxy.message),
                () -> assertEquals(1, proxy.calls));
    }

    @Test
    void packetHandlerRegisterIsIdempotentAfterLocatorPayloadRouteSetup() {
        assertDoesNotThrow(PacketHandler::register);
        Object registeredWrapper = PacketHandler.INSTANCE;
        assertNotNull(registeredWrapper);

        assertDoesNotThrow(PacketHandler::register);
        assertSame(registeredWrapper, PacketHandler.INSTANCE);
    }

    private static PacketOpenLocatorGUI roundTrip(PacketOpenLocatorGUI packet) {
        ByteBuf buffer = Unpooled.buffer();
        packet.toBytes(buffer);

        PacketOpenLocatorGUI decoded = new PacketOpenLocatorGUI();
        decoded.fromBytes(buffer);

        assertEquals(0, buffer.readableBytes(), "PacketOpenLocatorGUI should consume its payload");
        assertInstanceOf(ItemGuiHostLocator.class, decoded.locator());
        return decoded;
    }

    private static void assertBlockHitEquals(RayTraceResult expected, RayTraceResult actual) {
        assertNotNull(actual);
        assertAll(
                () -> assertSame(RayTraceResult.Type.BLOCK, actual.typeOfHit),
                () -> assertEquals(expected.getBlockPos(), actual.getBlockPos()),
                () -> assertSame(expected.sideHit, actual.sideHit),
                () -> assertEquals(expected.hitVec.x, actual.hitVec.x),
                () -> assertEquals(expected.hitVec.y, actual.hitVec.y),
                () -> assertEquals(expected.hitVec.z, actual.hitVec.z));
    }

    private static final class RecordingProxy extends CommonProxy {

        private PacketOpenLocatorGUI message;
        private int calls;

        @Override
        public void openLocatorGui(PacketOpenLocatorGUI message, MessageContext ctx) {
            this.message = message;
            this.calls++;
        }
    }
}
