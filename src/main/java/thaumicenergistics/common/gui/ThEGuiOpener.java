package thaumicenergistics.common.gui;

import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.tile.TileArcaneAssembler;

/**
 * Opens ThE GUI routes that need AE2's full host locator payload.
 */
public final class ThEGuiOpener {

    private ThEGuiOpener() {
    }

    /**
     * Provides the server-side operations needed to open a locator-aware GUI.
     *
     * <p>The abstraction keeps GUI routing testable without constructing a real {@link EntityPlayerMP}. Production
     * implementations must preserve Minecraft's open order: close the current container, allocate the next window id,
     * send the open packet, then install the server container and add the player as listener.</p>
     */
    public interface ServerOpenContext {

        /**
         * Returns the player that owns the GUI route and supplies inventory/world context for host lookup.
         */
        EntityPlayer player();

        /**
         * Returns the currently open container before replacement; Knowledge Core routes use this to capture their
         * parent inscriber context before {@link #closeContainer()} mutates it.
         */
        Container openContainer();

        /**
         * Closes the currently open container before a new locator-aware container is installed.
         */
        void closeContainer();

        /**
         * Allocates and returns the window id for the new server/client container pair.
         */
        int nextWindowId();

        /**
         * Sends the client packet that carries the GUI id, locator and window id.
         */
        void send(PacketOpenLocatorGUI packet);

        /**
         * Installs the completed server container for the allocated window id.
         */
        void install(Container container, int windowId);
    }

    public static void openLocatorGui(EntityPlayer player, ModGUIs gui, GuiHostLocator locator,
                                      boolean returnedFromSubScreen) {
        validateLocatorRoute(player, gui, locator);
        if (!(player instanceof EntityPlayerMP serverPlayer)) {
            throw new IllegalArgumentException("Cannot open locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + " for non-server player " + player.getClass().getName()
                    + "; expected " + EntityPlayerMP.class.getName());
        }

        openLocatorGui(new ServerPlayerOpenContext(serverPlayer), gui, locator, returnedFromSubScreen);
    }

    static void openLocatorGui(ServerOpenContext context, ModGUIs gui, GuiHostLocator locator,
                               boolean returnedFromSubScreen) {
        validateLocatorRoute(context, gui, locator);

        GuiHostLocator packetLocator = locator;
        ContainerArcaneInscriber knowledgeCoreParent = null;
        if (isKnowledgeCoreGui(gui)) {
            knowledgeCoreParent = getKnowledgeCoreParent(context, gui);
            packetLocator = getKnowledgeCoreParentLocator(context, gui, knowledgeCoreParent);
        }

        context.closeContainer();
        int windowId = context.nextWindowId();
        Container container = createServerContainer(context.player(), gui, packetLocator, returnedFromSubScreen,
                windowId, knowledgeCoreParent);

        context.send(new PacketOpenLocatorGUI(gui, packetLocator, returnedFromSubScreen, windowId));
        context.install(container, windowId);
    }

    private static Container createServerContainer(EntityPlayer player, ModGUIs gui, GuiHostLocator locator,
                                                   boolean returnedFromSubScreen, int windowId,
                                                   ContainerArcaneInscriber knowledgeCoreParent) {
        return switch (gui) {
            case ARCANE_TERMINAL -> createArcaneTerminalContainer(player, gui, locator, returnedFromSubScreen,
                    windowId);
            case ARCANE_INSCRIBER -> createArcaneInscriberContainer(player, gui, locator, windowId);
            case ARCANE_ASSEMBLER -> createArcaneAssemblerContainer(player, gui, locator, windowId);
            case KNOWLEDGE_CORE_ADD, KNOWLEDGE_CORE_DEL, KNOWLEDGE_CORE_VIEW ->
                    createKnowledgeCoreContainer(player, gui, locator, knowledgeCoreParent, windowId);
            case WIRELESS_ARCANE_TERMINAL -> createWirelessArcaneContainer(
                    player.inventory, locateWirelessArcaneHost(player, gui, locator), locator,
                    returnedFromSubScreen, windowId);
            default -> throw new IllegalArgumentException("Unsupported locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
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
                                                                          GuiHostLocator locator, int windowId) {
        ContainerArcaneInscriber container = new ContainerArcaneInscriber(player.inventory,
                locateArcaneTerminalHost(player, gui, locator));
        container.setLocator(locator);
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

    private static ContainerKnowledgeCore createKnowledgeCoreContainer(EntityPlayer player, ModGUIs gui,
                                                                       GuiHostLocator parentLocator,
                                                                       ContainerArcaneInscriber parent,
                                                                       int windowId) {
        if (parent == null) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent " + ContainerArcaneInscriber.class.getName()
                    + "; player " + playerDescription(player)
                    + "; locator " + locatorDescription(parentLocator));
        }
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(player, gui, parent, parentLocator);
        container.windowId = windowId;
        return container;
    }

    public static WirelessArcaneTerminalGuiHost locateWirelessArcaneHost(EntityPlayer player, ModGUIs gui,
                                                                          GuiHostLocator locator) {
        validateWirelessArcaneRoute(player, gui, locator);
        WirelessArcaneTerminalGuiHost host = locator.locate(player, WirelessArcaneTerminalGuiHost.class);
        if (host == null) {
            throw new IllegalStateException("Cannot locate host for locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + "; expected " + WirelessArcaneTerminalGuiHost.class.getName());
        }
        return host;
    }

    private static IArcaneTerminalHost locateArcaneTerminalHost(EntityPlayer player, ModGUIs gui,
                                                               GuiHostLocator locator) {
        IArcaneTerminalHost host = locator.locate(player, IArcaneTerminalHost.class);
        if (host == null) {
            throw new IllegalStateException("Cannot locate host for locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + "; expected " + IArcaneTerminalHost.class.getName());
        }
        return host;
    }

    private static TileArcaneAssembler locateArcaneAssembler(EntityPlayer player, ModGUIs gui,
                                                            GuiHostLocator locator) {
        TileArcaneAssembler host = locator.locate(player, TileArcaneAssembler.class);
        if (host == null) {
            throw new IllegalStateException("Cannot locate host for locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + "; expected " + TileArcaneAssembler.class.getName());
        }
        return host;
    }

    public static ContainerArcaneTerm createWirelessArcaneContainer(InventoryPlayer inventory,
                                                                     IArcaneTerminalHost host,
                                                                    GuiHostLocator locator,
                                                                    boolean returnedFromSubScreen,
                                                                    int windowId) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory cannot be null");
        }
        if (host == null) {
            throw new IllegalArgumentException("host cannot be null");
        }
        if (locator == null) {
            throw new IllegalArgumentException("locator cannot be null");
        }
        if (windowId < 0) {
            throw new IllegalArgumentException("windowId cannot be negative: " + windowId);
        }

        ContainerArcaneTerm container = new ContainerArcaneTerm(inventory, host);
        container.setLocator(locator);
        container.setReturnedFromSubScreen(returnedFromSubScreen);
        container.windowId = windowId;
        return container;
    }

    private static ContainerArcaneInscriber getKnowledgeCoreParent(ServerOpenContext context, ModGUIs gui) {
        Container openContainer = context.openContainer();
        if (!(openContainer instanceof ContainerArcaneInscriber parent)) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent " + ContainerArcaneInscriber.class.getName()
                    + "; player " + playerDescription(context.player())
                    + "; openContainer " + openContainer);
        }
        return parent;
    }

    private static GuiHostLocator getKnowledgeCoreParentLocator(ServerOpenContext context, ModGUIs gui,
                                                                ContainerArcaneInscriber parent) {
        GuiHostLocator parentLocator = parent.getLocator();
        if (parentLocator == null) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent locator for player " + playerDescription(context.player())
                    + "; parent " + parent);
        }

        IArcaneTerminalHost parentHost = parent.getHost();
        if (parentHost == null) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + gui.name()
                    + " without parent host for player " + playerDescription(context.player())
                    + "; locator " + locatorDescription(parentLocator)
                    + "; parent " + parent);
        }

        IArcaneTerminalHost locatedHost = parentLocator.locate(context.player(), IArcaneTerminalHost.class);
        if (locatedHost != parentHost) {
            throw new IllegalStateException("Knowledge Core parent locator mismatch for gui " + gui.name()
                    + " player " + playerDescription(context.player())
                    + " locator " + locatorDescription(parentLocator)
                    + " parent " + parent
                    + " parentHost " + parentHost
                    + " locatedHost " + locatedHost);
        }
        return parentLocator;
    }

    private static void validateLocatorRoute(ServerOpenContext context, ModGUIs gui, GuiHostLocator locator) {
        if (context == null) {
            throw new IllegalArgumentException("server open context cannot be null for locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator));
        }
        validateLocatorRoute(context.player(), gui, locator);
    }

    private static void validateLocatorRoute(EntityPlayer player, ModGUIs gui, GuiHostLocator locator) {
        if (gui == null) {
            throw new IllegalArgumentException("gui cannot be null for locator-aware opener"
                    + " with locator " + locatorDescription(locator)
                    + " for player " + playerDescription(player));
        }
        if (locator == null) {
            throw new IllegalArgumentException("locator cannot be null for locator-aware gui " + gui
                    + " for player " + playerDescription(player));
        }
        if (player == null) {
            throw new IllegalArgumentException("player cannot be null for locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator));
        }
        if (!isSupportedLocatorGui(gui)) {
            throw new IllegalArgumentException("Unsupported locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + " for player " + player.getClass().getName());
        }
    }

    private static void validateWirelessArcaneRoute(EntityPlayer player, ModGUIs gui, GuiHostLocator locator) {
        validateLocatorRoute(player, gui, locator);
        if (gui != ModGUIs.WIRELESS_ARCANE_TERMINAL) {
            throw new IllegalArgumentException("Unsupported locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + " for player " + player.getClass().getName());
        }
    }

    private static boolean isSupportedLocatorGui(ModGUIs gui) {
        return gui == ModGUIs.ARCANE_TERMINAL
                || gui == ModGUIs.ARCANE_INSCRIBER
                || gui == ModGUIs.ARCANE_ASSEMBLER
                || gui == ModGUIs.WIRELESS_ARCANE_TERMINAL
                || isKnowledgeCoreGui(gui);
    }

    private static boolean isKnowledgeCoreGui(ModGUIs gui) {
        return gui == ModGUIs.KNOWLEDGE_CORE_ADD
                || gui == ModGUIs.KNOWLEDGE_CORE_DEL
                || gui == ModGUIs.KNOWLEDGE_CORE_VIEW;
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

    private static final class ServerPlayerOpenContext implements ServerOpenContext {

        private final EntityPlayerMP player;

        private ServerPlayerOpenContext(EntityPlayerMP player) {
            this.player = player;
        }

        @Override
        public EntityPlayer player() {
            return this.player;
        }

        @Override
        public Container openContainer() {
            return this.player.openContainer;
        }

        @Override
        public void closeContainer() {
            this.player.closeContainer();
        }

        @Override
        public int nextWindowId() {
            this.player.getNextWindowId();
            return this.player.currentWindowId;
        }

        @Override
        public void send(PacketOpenLocatorGUI packet) {
            PacketHandler.sendToPlayer(this.player, packet);
        }

        @Override
        public void install(Container container, int windowId) {
            this.player.openContainer = container;
            this.player.openContainer.windowId = windowId;
            this.player.openContainer.addListener(this.player);
        }
    }
}
