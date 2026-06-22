package thaumicenergistics.client.gui;

import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.network.play.client.CPacketCloseWindow;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.client.gui.block.GuiArcaneAssembler;
import thaumicenergistics.client.gui.item.GuiKnowledgeCore;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.client.gui.part.GuiArcaneTerm;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.ThELog;

/**
 * Client-only GUI construction for locator-aware ThE screens.
 */
public final class ThEClientGuiOpener {

    private ThEClientGuiOpener() {
    }

    public static void openLocatorGui(Minecraft minecraft, PacketOpenLocatorGUI message) {
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

    static Container createClientContainer(EntityPlayer player, Container openContainer, PacketOpenLocatorGUI message) {
        if (player == null) {
            throw new IllegalArgumentException("Cannot create locator-aware client container without player");
        }
        ModGUIs gui = message.gui();
        GuiHostLocator locator = message.locator();
        return switch (gui) {
            case ARCANE_TERMINAL -> createArcaneTerminalContainer(player, gui, locator,
                    message.returnedFromSubScreen(), message.windowId());
            case ARCANE_INSCRIBER -> createArcaneInscriberContainer(player, gui, locator,
                    message.returnedFromSubScreen(), message.windowId());
            case ARCANE_ASSEMBLER -> createArcaneAssemblerContainer(player, gui, locator, message.windowId());
            case KNOWLEDGE_CORE_ADD, KNOWLEDGE_CORE_DEL, KNOWLEDGE_CORE_VIEW ->
                    createKnowledgeCoreContainer(player, openContainer, gui, locator, message.windowId());
            case WIRELESS_ARCANE_TERMINAL -> ThEGuiOpener.createWirelessArcaneContainer(
                    player.inventory, locateWirelessArcaneHost(player, gui, locator), locator,
                    message.returnedFromSubScreen(), message.windowId());
            default -> throw new IllegalArgumentException("Unsupported locator-aware client gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + " for player " + playerDescription(player));
        };
    }

    static GuiScreen createClientScreen(EntityPlayer player, Container container, PacketOpenLocatorGUI message) {
        if (player == null) {
            throw new IllegalArgumentException("Cannot create locator-aware client screen without player");
        }
        if (container == null) {
            throw new IllegalArgumentException("Cannot create locator-aware client screen without container for gui "
                    + message.gui());
        }
        return switch (message.gui()) {
            case ARCANE_TERMINAL, WIRELESS_ARCANE_TERMINAL ->
                    new GuiArcaneTerm((ContainerArcaneTerm) container, player.inventory);
            case ARCANE_INSCRIBER -> new GuiArcaneInscriber((ContainerArcaneInscriber) container, player.inventory);
            case ARCANE_ASSEMBLER -> new GuiArcaneAssembler((ContainerArcaneAssembler) container, player.inventory);
            case KNOWLEDGE_CORE_ADD, KNOWLEDGE_CORE_DEL, KNOWLEDGE_CORE_VIEW ->
                    new GuiKnowledgeCore((ContainerKnowledgeCore) container);
            default -> throw new IllegalArgumentException("Unsupported locator-aware client gui " + message.gui()
                    + " with locator " + locatorDescription(message.locator())
                    + " for player " + playerDescription(player));
        };
    }

    private static ContainerArcaneTerm createArcaneTerminalContainer(EntityPlayer player, ModGUIs gui,
                                                                     GuiHostLocator locator,
                                                                     boolean returnedFromSubScreen,
                                                                     int windowId) {
        ContainerArcaneTerm container = new ContainerArcaneTerm(player.inventory,
                locateArcaneTerminalHost(player, gui, locator));
        container.setLocator(locator);
        container.setReturnedFromSubScreen(returnedFromSubScreen);
        container.windowId = windowId;
        return container;
    }

    private static ContainerArcaneInscriber createArcaneInscriberContainer(EntityPlayer player, ModGUIs gui,
                                                                           GuiHostLocator locator,
                                                                           boolean returnedFromSubScreen,
                                                                           int windowId) {
        ContainerArcaneInscriber container = new ContainerArcaneInscriber(player.inventory,
                locateArcaneTerminalHost(player, gui, locator));
        container.setLocator(locator);
        container.setReturnedFromSubScreen(returnedFromSubScreen);
        container.windowId = windowId;
        return container;
    }

    private static ContainerArcaneAssembler createArcaneAssemblerContainer(EntityPlayer player, ModGUIs gui,
                                                                           GuiHostLocator locator, int windowId) {
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player,
                locateArcaneAssembler(player, gui, locator));
        container.setLocator(locator);
        container.windowId = windowId;
        return container;
    }

    private static ContainerKnowledgeCore createKnowledgeCoreContainer(EntityPlayer player, Container openContainer,
                                                                       ModGUIs gui, GuiHostLocator packetLocator,
                                                                       int windowId) {
        ContainerArcaneInscriber parent = getKnowledgeCoreParent(player, gui, openContainer);
        GuiHostLocator parentLocator = getKnowledgeCoreParentLocator(player, gui, parent);
        IArcaneTerminalHost parentHost = parent.getHost();
        IArcaneTerminalHost packetLocatedHost = locateArcaneTerminalHost(player, gui, packetLocator);
        if (packetLocatedHost != parentHost) {
            throw new IllegalStateException("Knowledge Core packet locator mismatch for gui " + gui.name()
                    + " player " + playerDescription(player)
                    + " packet locator " + locatorDescription(packetLocator)
                    + " parent locator " + locatorDescription(parentLocator)
                    + " parent " + parent
                    + " parentHost " + parentHost
                    + " packetLocatedHost " + packetLocatedHost);
        }

        ContainerKnowledgeCore container = new ContainerKnowledgeCore(player, gui, parent, parentLocator);
        container.windowId = windowId;
        return container;
    }

    private static ContainerArcaneInscriber getKnowledgeCoreParent(EntityPlayer player, ModGUIs gui,
                                                                   Container openContainer) {
        if (!(openContainer instanceof ContainerArcaneInscriber parent)) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent " + ContainerArcaneInscriber.class.getName()
                    + "; player " + playerDescription(player)
                    + "; openContainer " + openContainer);
        }
        return parent;
    }

    private static GuiHostLocator getKnowledgeCoreParentLocator(EntityPlayer player, ModGUIs gui,
                                                                ContainerArcaneInscriber parent) {
        GuiHostLocator parentLocator = parent.getLocator();
        if (parentLocator == null) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent locator for player " + playerDescription(player)
                    + "; parent " + parent);
        }

        IArcaneTerminalHost parentHost = parent.getHost();
        if (parentHost == null) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent host for player " + playerDescription(player)
                    + "; locator " + locatorDescription(parentLocator)
                    + "; parent " + parent);
        }

        IArcaneTerminalHost locatedHost = parentLocator.locate(player, IArcaneTerminalHost.class);
        if (locatedHost != parentHost) {
            throw new IllegalStateException("Knowledge Core parent locator mismatch for gui " + gui.name()
                    + " player " + playerDescription(player)
                    + " locator " + locatorDescription(parentLocator)
                    + " parent " + parent
                    + " parentHost " + parentHost
                    + " locatedHost " + locatedHost);
        }
        return parentLocator;
    }

    private static WirelessArcaneTerminalGuiHost locateWirelessArcaneHost(EntityPlayer player, ModGUIs gui,
                                                                          GuiHostLocator locator) {
        WirelessArcaneTerminalGuiHost host = locator.locate(player, WirelessArcaneTerminalGuiHost.class);
        if (host == null) {
            throw new IllegalStateException("Cannot locate host for locator-aware client gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + "; expected " + WirelessArcaneTerminalGuiHost.class.getName());
        }
        return host;
    }

    private static IArcaneTerminalHost locateArcaneTerminalHost(EntityPlayer player, ModGUIs gui,
                                                                GuiHostLocator locator) {
        IArcaneTerminalHost host = locator.locate(player, IArcaneTerminalHost.class);
        if (host == null) {
            throw new IllegalStateException("Cannot locate host for locator-aware client gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + "; expected " + IArcaneTerminalHost.class.getName());
        }
        return host;
    }

    private static TileArcaneAssembler locateArcaneAssembler(EntityPlayer player, ModGUIs gui,
                                                             GuiHostLocator locator) {
        TileArcaneAssembler host = locator.locate(player, TileArcaneAssembler.class);
        if (host == null) {
            throw new IllegalStateException("Cannot locate host for locator-aware client gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + "; expected " + TileArcaneAssembler.class.getName());
        }
        return host;
    }

    private static String locatorDescription(GuiHostLocator locator) {
        if (locator == null) {
            return "null";
        }
        return locator.getClass().getName() + " " + locator;
    }

    private static String playerDescription(EntityPlayer player) {
        return player == null ? "null" : player.getClass().getName();
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
            return createClientContainer(this.player, this.player.openContainer, message);
        }

        @Override
        public GuiScreen createScreen(Container container, PacketOpenLocatorGUI message) {
            return createClientScreen(this.player, container, message);
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
