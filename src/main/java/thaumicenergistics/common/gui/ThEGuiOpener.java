package thaumicenergistics.common.gui;

import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.PacketHandler;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;

/**
 * Opens ThE GUI routes that need AE2's full host locator payload.
 */
public final class ThEGuiOpener {

    private ThEGuiOpener() {
    }

    public static void openLocatorGui(EntityPlayer player, ModGUIs gui, GuiHostLocator locator,
                                      boolean returnedFromSubScreen) {
        validateWirelessArcaneRoute(player, gui, locator);
        if (!(player instanceof EntityPlayerMP serverPlayer)) {
            throw new IllegalArgumentException("Cannot open locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + " for non-server player " + player.getClass().getName()
                    + "; expected " + EntityPlayerMP.class.getName());
        }

        WirelessArcaneTerminalGuiHost host = locateWirelessArcaneHost(serverPlayer, gui, locator);

        serverPlayer.closeContainer();
        serverPlayer.getNextWindowId();
        int windowId = serverPlayer.currentWindowId;

        ContainerArcaneTerm container = createWirelessArcaneContainer(
                serverPlayer.inventory, host, locator, returnedFromSubScreen, windowId);

        PacketHandler.sendToPlayer(serverPlayer,
                new PacketOpenLocatorGUI(gui, locator, returnedFromSubScreen, windowId));

        serverPlayer.openContainer = container;
        serverPlayer.openContainer.windowId = windowId;
        serverPlayer.openContainer.addListener(serverPlayer);
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

    private static void validateWirelessArcaneRoute(EntityPlayer player, ModGUIs gui, GuiHostLocator locator) {
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
        if (gui != ModGUIs.WIRELESS_ARCANE_TERMINAL) {
            throw new IllegalArgumentException("Unsupported locator-aware gui " + gui
                    + " with locator " + locatorDescription(locator)
                    + " for player " + player.getClass().getName());
        }
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
}
