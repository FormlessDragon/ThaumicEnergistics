package thaumicenergistics.client.gui;

import ae2.core.gui.locator.GuiHostLocators;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import org.junit.jupiter.api.Test;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEClientGuiOpenerTest {

    @Test
    void containerCreationFailureResetsContainerClosesScreenAndSendsClosePacket() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.createContainerFailure = new IllegalStateException("container failed");
        PacketOpenLocatorGUI packet = packet(37);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.CONTAINER, result.stage()),
                () -> assertEquals(List.of(37), client.closeWindowIds),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertTrue(client.screenClosed),
                () -> assertTrue(result.closePacketSent()),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    @Test
    void screenCreationFailureDoesNotInstallNewContainerAndRestoresInventoryContainer() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.createdContainer = new TestContainer();
        client.createScreenFailure = new IllegalStateException("screen failed");
        PacketOpenLocatorGUI packet = packet(41);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.SCREEN, result.stage()),
                () -> assertEquals(41, client.createdContainer.windowId),
                () -> assertFalse(client.installedContainers.contains(client.createdContainer)),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertEquals(List.of(41), client.closeWindowIds),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    @Test
    void displayFailureRestoresInventoryContainerAfterInstallingNewContainer() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.createdContainer = new TestContainer();
        client.displayFailure = new IllegalStateException("display failed");
        PacketOpenLocatorGUI packet = packet(53);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.DISPLAY, result.stage()),
                () -> assertEquals(53, client.createdContainer.windowId),
                () -> assertSame(client.createdContainer, client.installedContainers.get(0)),
                () -> assertSame(client.inventoryContainer, client.installedContainers.get(1)),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertEquals(List.of(53), client.closeWindowIds),
                () -> assertTrue(client.screenClosed),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    @Test
    void missingConnectionReportsDiagnosticAndStillResetsLocalState() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.connectionPresent = false;
        client.createContainerFailure = new IllegalStateException("container failed");
        PacketOpenLocatorGUI packet = packet(79);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.CONTAINER, result.stage()),
                () -> assertEquals(1, client.closeWindowAttempts),
                () -> assertTrue(client.closeWindowIds.isEmpty()),
                () -> assertFalse(result.closePacketSent()),
                () -> assertTrue(result.connectionMissing()),
                () -> assertTrue(result.diagnostic().contains("connection")),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    private static PacketOpenLocatorGUI packet(int windowId) {
        return new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                GuiHostLocators.forInventorySlot(0),
                false,
                windowId);
    }

    private static final class TestClientOpenContext implements ThEClientGuiOpener.ClientOpenContext {

        private final TestContainer inventoryContainer = new TestContainer();
        private final List<Integer> closeWindowIds = new ArrayList<>();
        private final List<Container> installedContainers = new ArrayList<>();
        private Container openContainer = new TestContainer();
        private Container createdContainer = new TestContainer();
        private RuntimeException createContainerFailure;
        private RuntimeException createScreenFailure;
        private RuntimeException displayFailure;
        private boolean connectionPresent = true;
        private boolean screenClosed;
        private int closeWindowAttempts;

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String describePlayer() {
            return "test-client-player";
        }

        @Override
        public Container inventoryContainer() {
            return this.inventoryContainer;
        }

        @Override
        public void setOpenContainer(Container container) {
            this.openContainer = container;
            this.installedContainers.add(container);
        }

        @Override
        public Container createContainer(PacketOpenLocatorGUI message) {
            if (this.createContainerFailure != null) {
                throw this.createContainerFailure;
            }
            return this.createdContainer;
        }

        @Override
        public GuiScreen createScreen(Container container, PacketOpenLocatorGUI message) {
            if (this.createScreenFailure != null) {
                throw this.createScreenFailure;
            }
            return new GuiScreen() {
            };
        }

        @Override
        public void displayScreen(GuiScreen screen) {
            if (this.displayFailure != null) {
                throw this.displayFailure;
            }
        }

        @Override
        public boolean sendCloseWindow(int windowId) {
            this.closeWindowAttempts++;
            if (!this.connectionPresent) {
                return false;
            }
            this.closeWindowIds.add(windowId);
            return true;
        }

        @Override
        public void closeScreen() {
            this.screenClosed = true;
        }
    }

    private static final class TestContainer extends Container {

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return true;
        }
    }
}
