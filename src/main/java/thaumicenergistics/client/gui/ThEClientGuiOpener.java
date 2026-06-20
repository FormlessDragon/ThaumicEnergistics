package thaumicenergistics.client.gui;

import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.client.CPacketCloseWindow;
import thaumicenergistics.client.gui.part.GuiArcaneTerm;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.util.ThELog;

/**
 * Client-only GUI construction for locator-aware ThE screens.
 */
public final class ThEClientGuiOpener {

    private ThEClientGuiOpener() {
    }

    public static void openLocatorGui(PacketOpenLocatorGUI message) {
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> openLocatorGui(new ClientOpenContextImpl(minecraft), message));
    }

    static void openLocatorGui(Minecraft minecraft, PacketOpenLocatorGUI message) {
        openLocatorGui(new ClientOpenContextImpl(minecraft), message);
    }

    static OpenResult openLocatorGui(ClientOpenContext client, PacketOpenLocatorGUI message) {
        if (!client.hasPlayer()) {
            String diagnostic = "Cannot open locator-aware gui " + message.gui() + " without a client player";
            ThELog.warn(diagnostic);
            return OpenResult.failed(OpenStage.PLAYER, false, false, false, false, diagnostic);
        }

        Container container;
        try {
            container = client.createContainer(message);
            if (container == null) {
                throw new IllegalStateException("Client container factory returned null for locator-aware gui "
                        + message.gui() + " window " + message.windowId());
            }
            container.windowId = message.windowId();
        } catch (RuntimeException e) {
            String diagnostic = "Cannot create client container for locator-aware gui " + message.gui()
                    + " window " + message.windowId();
            ThELog.error(diagnostic, e);
            return failAndClose(client, message, OpenStage.CONTAINER, diagnostic);
        }

        GuiScreen screen;
        try {
            screen = client.createScreen(container, message);
            if (screen == null) {
                throw new IllegalStateException("Client screen factory returned null for locator-aware gui "
                        + message.gui() + " window " + message.windowId());
            }
        } catch (RuntimeException e) {
            String diagnostic = "Cannot create client screen for locator-aware gui " + message.gui()
                    + " window " + message.windowId();
            ThELog.error(diagnostic, e);
            return failAndClose(client, message, OpenStage.SCREEN, diagnostic);
        }

        try {
            client.setOpenContainer(container);
        } catch (RuntimeException e) {
            String diagnostic = "Cannot install client container for locator-aware gui " + message.gui()
                    + " window " + message.windowId();
            ThELog.error(diagnostic, e);
            return failAndClose(client, message, OpenStage.INSTALL, diagnostic);
        }

        try {
            client.displayScreen(screen);
        } catch (RuntimeException e) {
            String diagnostic = "Cannot display client screen for locator-aware gui " + message.gui()
                    + " window " + message.windowId();
            ThELog.error(diagnostic, e);
            return failAndClose(client, message, OpenStage.DISPLAY, diagnostic);
        }

        return OpenResult.success();
    }

    public static ContainerArcaneTerm createWirelessArcaneContainer(EntityPlayerSP player,
                                                                    PacketOpenLocatorGUI message) {
        if (player == null) {
            throw new IllegalArgumentException("Cannot create locator-aware client container without player");
        }
        ModGUIs gui = message.gui();
        GuiHostLocator locator = message.locator();
        WirelessArcaneTerminalGuiHost host = ThEGuiOpener.locateWirelessArcaneHost(player, gui, locator);
        return ThEGuiOpener.createWirelessArcaneContainer(
                player.inventory, host, locator, message.returnedFromSubScreen(), message.windowId());
    }

    private static OpenResult failAndClose(ClientOpenContext client, PacketOpenLocatorGUI message, OpenStage stage,
                                           String diagnostic) {
        CleanupResult cleanup = closeWindowAndReset(client, message);
        String fullDiagnostic = cleanup.diagnostic().isEmpty()
                ? diagnostic
                : diagnostic + "; " + cleanup.diagnostic();
        return OpenResult.failed(stage, cleanup.closePacketSent(), cleanup.connectionMissing(),
                cleanup.openContainerReset(), cleanup.screenClosed(), fullDiagnostic);
    }

    private static CleanupResult closeWindowAndReset(ClientOpenContext client, PacketOpenLocatorGUI message) {
        boolean closePacketSent = false;
        boolean connectionMissing = false;
        boolean screenClosed = false;
        boolean openContainerReset = false;
        StringBuilder diagnostic = new StringBuilder();

        try {
            closePacketSent = client.sendCloseWindow(message.windowId());
            if (!closePacketSent) {
                connectionMissing = true;
                appendDiagnostic(diagnostic, "client connection is unavailable for close-window packet");
                ThELog.error("Cannot send close-window packet for locator-aware gui {} window {} because client connection is unavailable",
                        message.gui(), message.windowId());
            }
        } catch (RuntimeException e) {
            appendDiagnostic(diagnostic, "close-window packet failed");
            ThELog.error("Failed to send close-window packet for locator-aware gui " + message.gui()
                    + " window " + message.windowId(), e);
        }

        try {
            client.closeScreen();
            screenClosed = true;
        } catch (RuntimeException e) {
            appendDiagnostic(diagnostic, "local screen close failed");
            ThELog.error("Failed to close local screen for locator-aware gui " + message.gui()
                    + " window " + message.windowId(), e);
        }

        try {
            Container inventoryContainer = client.inventoryContainer();
            if (inventoryContainer == null) {
                appendDiagnostic(diagnostic, "inventory container is unavailable for reset");
                ThELog.error("Cannot reset openContainer for locator-aware gui {} window {} because inventoryContainer is null for {}",
                        message.gui(), message.windowId(), client.describePlayer());
            } else {
                client.setOpenContainer(inventoryContainer);
                openContainerReset = true;
            }
        } catch (RuntimeException e) {
            appendDiagnostic(diagnostic, "openContainer reset failed");
            ThELog.error("Failed to reset openContainer for locator-aware gui " + message.gui()
                    + " window " + message.windowId() + " for " + client.describePlayer(), e);
        }

        return new CleanupResult(closePacketSent, connectionMissing, openContainerReset, screenClosed,
                diagnostic.toString());
    }

    private static void appendDiagnostic(StringBuilder diagnostic, String message) {
        if (!diagnostic.isEmpty()) {
            diagnostic.append("; ");
        }
        diagnostic.append(message);
    }

    enum OpenStatus {
        SUCCESS,
        FAILED
    }

    enum OpenStage {
        PLAYER,
        CONTAINER,
        SCREEN,
        INSTALL,
        DISPLAY,
        COMPLETE
    }

    static final class OpenResult {

        private final OpenStatus status;
        private final OpenStage stage;
        private final boolean closePacketSent;
        private final boolean connectionMissing;
        private final boolean openContainerReset;
        private final boolean screenClosed;
        private final String diagnostic;

        private OpenResult(OpenStatus status, OpenStage stage, boolean closePacketSent,
                           boolean connectionMissing, boolean openContainerReset, boolean screenClosed,
                           String diagnostic) {
            this.status = status;
            this.stage = stage;
            this.closePacketSent = closePacketSent;
            this.connectionMissing = connectionMissing;
            this.openContainerReset = openContainerReset;
            this.screenClosed = screenClosed;
            this.diagnostic = diagnostic;
        }

        static OpenResult success() {
            return new OpenResult(OpenStatus.SUCCESS, OpenStage.COMPLETE, false, false, false, false, "");
        }

        static OpenResult failed(OpenStage stage, boolean closePacketSent, boolean connectionMissing,
                                 boolean openContainerReset, boolean screenClosed, String diagnostic) {
            return new OpenResult(OpenStatus.FAILED, stage, closePacketSent, connectionMissing,
                    openContainerReset, screenClosed, diagnostic);
        }

        OpenStatus status() {
            return this.status;
        }

        OpenStage stage() {
            return this.stage;
        }

        boolean closePacketSent() {
            return this.closePacketSent;
        }

        boolean connectionMissing() {
            return this.connectionMissing;
        }

        boolean openContainerReset() {
            return this.openContainerReset;
        }

        boolean screenClosed() {
            return this.screenClosed;
        }

        String diagnostic() {
            return this.diagnostic;
        }
    }

    /**
     * Provides the client-side Minecraft operations required to open a locator-aware GUI.
     *
     * <p>The interface exists so packet handling can validate and clean up GUI open failures through behavior tests
     * without constructing a real Minecraft client.</p>
     */
    interface ClientOpenContext {

        /**
         * Reports whether a client player is available before any GUI construction touches player state.
         */
        boolean hasPlayer();

        /**
         * Describes the current player for diagnostics emitted when cleanup cannot fully reset client state.
         */
        String describePlayer();

        /**
         * Supplies the fallback inventory container used when a failed open must restore the player's container.
         */
        Container inventoryContainer();

        /**
         * Installs the active client container after construction succeeds or during cleanup reset.
         */
        void setOpenContainer(Container container);

        /**
         * Builds the logical container for the locator-aware packet being opened.
         */
        Container createContainer(PacketOpenLocatorGUI message);

        /**
         * Builds the visible screen around the newly created container.
         */
        GuiScreen createScreen(Container container, PacketOpenLocatorGUI message);

        /**
         * Displays the completed GUI screen on the client.
         */
        void displayScreen(GuiScreen screen);

        /**
         * Sends the server close-window packet when a partial client open must be rolled back.
         */
        boolean sendCloseWindow(int windowId);

        /**
         * Closes the local screen during rollback after a failed locator-aware GUI open.
         */
        void closeScreen();
    }

    private record CleanupResult(boolean closePacketSent, boolean connectionMissing, boolean openContainerReset,
                                 boolean screenClosed, String diagnostic) {
    }

    private static final class ClientOpenContextImpl implements ClientOpenContext {

        private final Minecraft minecraft;
        private final EntityPlayerSP player;

        private ClientOpenContextImpl(Minecraft minecraft) {
            this.minecraft = minecraft;
            this.player = minecraft.player;
        }

        @Override
        public boolean hasPlayer() {
            return this.player != null;
        }

        @Override
        public String describePlayer() {
            return this.player == null ? "null" : this.player.getClass().getName();
        }

        @Override
        public Container inventoryContainer() {
            return this.player.inventoryContainer;
        }

        @Override
        public void setOpenContainer(Container container) {
            this.player.openContainer = container;
        }

        @Override
        public Container createContainer(PacketOpenLocatorGUI message) {
            return createWirelessArcaneContainer(this.player, message);
        }

        @Override
        public GuiScreen createScreen(Container container, PacketOpenLocatorGUI message) {
            return new GuiArcaneTerm((ContainerArcaneTerm) container, this.player.inventory);
        }

        @Override
        public void displayScreen(GuiScreen screen) {
            this.minecraft.displayGuiScreen(screen);
        }

        @Override
        public boolean sendCloseWindow(int windowId) {
            if (this.player.connection == null) {
                return false;
            }
            this.player.connection.sendPacket(new CPacketCloseWindow(windowId));
            return true;
        }

        @Override
        public void closeScreen() {
            this.minecraft.displayGuiScreen(null);
        }
    }
}
