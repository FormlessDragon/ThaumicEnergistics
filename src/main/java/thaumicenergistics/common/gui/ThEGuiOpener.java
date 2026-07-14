package thaumicenergistics.common.gui;

import ae2.api.parts.IPartHost;
import ae2.core.gui.locator.BaublesItemLocator;
import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.core.gui.locator.PartLocator;
import ae2.parts.AEBasePart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.api.storage.IArcaneInscriberHost;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.api.storage.IArcaneTerminalUpgradeHost;
import thaumicenergistics.client.gui.GuiIds;
import thaumicenergistics.client.gui.ModGUIs;
import thaumicenergistics.core.ThELog;

/**
 * Encodes ThE standard Forge GUI routes into tile, part, or item coordinate payloads.
 */
public final class ThEGuiOpener {

    private ThEGuiOpener() {
    }

    public static void openGui(EntityPlayer player, ModGUIs gui, TileEntity tile) {
        openGui(player, gui, tile, false);
    }

    public static void openGui(EntityPlayer player, ModGUIs gui, TileEntity tile, boolean returnedFromSubScreen) {
        validateTileOpenRoute(player, gui, tile);
        BlockPos pos = tile.getPos();
        openGui(player, gui, returnedFromSubScreen, tile.getWorld(), pos.getX(), pos.getY(), pos.getZ());
    }

    public static void openPartGui(EntityPlayer player, ModGUIs gui, AEBasePart part) {
        openPartGui(player, gui, part, false);
    }

    public static void openPartGui(EntityPlayer player, ModGUIs gui, AEBasePart part, boolean returnedFromSubScreen) {
        validatePartOpenRoute(player, gui);
        if (part == null) {
            throw rejectArgument("part cannot be null for part gui " + gui + " player " + playerDescription(player));
        }
        IPartHost host = part.getHost();
        if (host == null) {
            throw rejectState("Cannot open part gui " + gui + " without a part host for "
                    + part.getClass().getName() + " player " + playerDescription(player));
        }
        openPartGui(player, gui, host, part.getSide(), returnedFromSubScreen);
    }

    public static void openPartGui(EntityPlayer player, ModGUIs gui, IPartHost host, EnumFacing side) {
        openPartGui(player, gui, host, side, false);
    }

    public static void openPartGui(EntityPlayer player, ModGUIs gui, IPartHost host, EnumFacing side,
                                   boolean returnedFromSubScreen) {
        validatePartOpenRoute(player, gui);
        if (host == null) {
            throw rejectArgument("part host cannot be null for part gui " + gui + " player " + playerDescription(player));
        }
        if (host.getLocation() == null || host.getLocation().getPos() == null) {
            throw rejectState("Cannot open part gui " + gui + " because host " + host.getClass().getName()
                    + " has no location for player " + playerDescription(player));
        }
        BlockPos pos = host.getLocation().getPos();
        openPartGui(player, gui, pos, side, returnedFromSubScreen);
    }

    public static void openPartLocatorGui(EntityPlayer player, ModGUIs gui, GuiHostLocator locator,
                                          boolean returnedFromSubScreen) {
        validatePartOpenRoute(player, gui);
        if (!(locator instanceof PartLocator)) {
            throw rejectArgument("Cannot encode non-part locator " + locatorDescription(locator)
                    + " for part gui " + gui + " player " + playerDescription(player));
        }

        IArcaneTerminalHost host = locatePartHost(player, gui, locator);
        BlockPos pos = host.getReturnPos();
        if (pos == null) {
            throw rejectState("Cannot encode part locator " + locatorDescription(locator)
                    + " for part gui " + gui + " because host " + host.getClass().getName()
                    + " returned null position");
        }
        openPartGui(player, gui, pos, host.getReturnSide(), returnedFromSubScreen);
    }

    public static void openItemGui(EntityPlayer player, ModGUIs gui, ItemGuiHostLocator locator) {
        openItemGui(player, gui, locator, false);
    }

    public static void openItemGui(EntityPlayer player, ModGUIs gui, ItemGuiHostLocator locator,
                                   boolean returnedFromSubScreen) {
        validateItemOpenRoute(player, gui, locator);
        int encodedSlot = encodeItemSlot(player, gui, locator);
        openGui(player, gui, returnedFromSubScreen, player.world, encodedSlot, 0, 0);
    }

    private static void openPartGui(EntityPlayer player, ModGUIs gui, BlockPos pos, EnumFacing side,
                                    boolean returnedFromSubScreen) {
        if (pos == null) {
            throw rejectArgument("part position cannot be null for part gui " + gui
                    + " player " + playerDescription(player));
        }
        openGui(player, gui, returnedFromSubScreen, player.world, pos.getX(), encodePartY(pos, side), pos.getZ());
    }

    private static void openGui(EntityPlayer player, ModGUIs gui, boolean returnedFromSubScreen, World world,
                                int x, int y, int z) {
        validateCommonOpenRoute(player, gui, world);
        player.openGui(ThaumicEnergistics.instance(), GuiIds.getGuiId(gui, returnedFromSubScreen), world, x, y, z);
    }

    private static int encodePartY(BlockPos pos, EnumFacing side) {
        return (encodePartSide(side) << 8) | (pos.getY() & 255);
    }

    private static int encodePartSide(EnumFacing side) {
        return side == null ? EnumFacing.VALUES.length : side.ordinal();
    }

    private static int encodeItemSlot(EntityPlayer player, ModGUIs gui, ItemGuiHostLocator locator) {
        Integer slot = locator.getPlayerInventorySlot();
        if (slot != null) {
            return encodeInventorySlot(player, gui, locator, slot);
        }
        if (locator instanceof BaublesItemLocator baublesLocator) {
            return encodeBaublesSlot(player, gui, baublesLocator);
        }
        throw rejectArgument("Cannot encode unknown item locator " + locatorDescription(locator)
                + " for item gui " + gui + " player " + playerDescription(player));
    }

    private static int encodeInventorySlot(EntityPlayer player, ModGUIs gui, ItemGuiHostLocator locator, int slot) {
        if (slot == Integer.MIN_VALUE) {
            throw rejectArgument("Cannot encode invalid inventory slot Integer.MIN_VALUE from locator "
                    + locatorDescription(locator) + " for item gui " + gui + " player " + playerDescription(player));
        }
        int inventorySize = player.inventory.getSizeInventory();
        if (slot < 0 || slot >= inventorySize) {
            throw rejectArgument("Cannot encode invalid inventory slot " + slot + " from locator "
                    + locatorDescription(locator) + " for item gui " + gui + " player " + playerDescription(player)
                    + "; valid range 0.." + (inventorySize - 1));
        }
        return slot;
    }

    private static int encodeBaublesSlot(EntityPlayer player, ModGUIs gui, BaublesItemLocator locator) {
        int baubleSlot = locator.baubleSlot();
        if (baubleSlot == Integer.MIN_VALUE) {
            throw rejectArgument("Cannot encode invalid Baubles slot Integer.MIN_VALUE from locator "
                    + locatorDescription(locator) + " for item gui " + gui + " player " + playerDescription(player));
        }
        if (baubleSlot < 0) {
            throw rejectArgument("Cannot encode negative Baubles slot " + baubleSlot + " from locator "
                    + locatorDescription(locator) + " for item gui " + gui + " player " + playerDescription(player));
        }
        int encodedSlot = -1 - baubleSlot;
        if (encodedSlot == Integer.MIN_VALUE) {
            throw rejectArgument("Cannot encode Baubles slot " + baubleSlot + " as Integer.MIN_VALUE from locator "
                    + locatorDescription(locator) + " for item gui " + gui + " player " + playerDescription(player));
        }
        return encodedSlot;
    }

    private static IArcaneTerminalHost locatePartHost(EntityPlayer player, ModGUIs gui, GuiHostLocator locator) {
        Class<? extends IArcaneTerminalHost> hostType = getPartLocatorHostType(gui);
        IArcaneTerminalHost host = locator.locate(player, hostType);
        if (host == null) {
            throw rejectState("Cannot locate " + hostType.getName() + " from part locator "
                    + locatorDescription(locator) + " for part gui " + gui + " player " + playerDescription(player));
        }
        return host;
    }

    private static Class<? extends IArcaneTerminalHost> getPartLocatorHostType(ModGUIs gui) {
        return switch (gui) {
            case ARCANE_TERMINAL -> IArcaneTerminalUpgradeHost.class;
            case ARCANE_INSCRIBER, KNOWLEDGE_CORE_ADD, KNOWLEDGE_CORE_DEL, KNOWLEDGE_CORE_VIEW ->
                    IArcaneInscriberHost.class;
            default -> throw rejectArgument("Unsupported part locator gui " + gui);
        };
    }

    private static void validateTileOpenRoute(EntityPlayer player, ModGUIs gui, TileEntity tile) {
        if (tile == null) {
            throw rejectArgument("tile cannot be null for tile gui " + gui + " player " + playerDescription(player));
        }
        if (gui != ModGUIs.ARCANE_ASSEMBLER) {
            throw rejectArgument("Unsupported tile gui " + gui + " for tile " + tile.getClass().getName()
                    + " player " + playerDescription(player));
        }
        if (tile.getWorld() == null) {
            throw rejectState("Cannot open tile gui " + gui + " for tile " + tile.getClass().getName()
                    + " at " + tile.getPos() + " because the tile has no world");
        }
    }

    private static void validatePartOpenRoute(EntityPlayer player, ModGUIs gui) {
        validateOpenPlayerAndGui(player, gui, "part");
        if (!isPartGui(gui)) {
            throw rejectArgument("Unsupported part gui " + gui + " for player " + playerDescription(player));
        }
    }

    private static void validateItemOpenRoute(EntityPlayer player, ModGUIs gui, ItemGuiHostLocator locator) {
        validateOpenPlayerAndGui(player, gui, "item");
        if (!isItemGui(gui)) {
            throw rejectArgument("Unsupported item gui " + gui + " for player " + playerDescription(player)
                    + " locator " + locatorDescription(locator));
        }
        if (locator == null) {
            throw rejectArgument("item locator cannot be null for item gui " + gui
                    + " player " + playerDescription(player));
        }
    }

    private static void validateCommonOpenRoute(EntityPlayer player, ModGUIs gui, World world) {
        validateOpenPlayerAndGui(player, gui, "Forge");
        if (world == null) {
            throw rejectState("Cannot open gui " + gui + " for player " + playerDescription(player)
                    + " because the target world is null");
        }
        if (ThaumicEnergistics.instance() == null) {
            throw rejectState("Cannot open gui " + gui + " because the Thaumic Energistics mod instance is null");
        }
    }

    private static void validateOpenPlayerAndGui(EntityPlayer player, ModGUIs gui, String route) {
        if (gui == null) {
            throw rejectArgument(route + " gui cannot be null for player " + playerDescription(player));
        }
        if (player == null) {
            throw rejectArgument("player cannot be null for " + route + " gui " + gui);
        }
    }

    private static boolean isPartGui(ModGUIs gui) {
        return gui == ModGUIs.ARCANE_TERMINAL
                || gui == ModGUIs.ARCANE_INSCRIBER
                || isKnowledgeCoreGui(gui);
    }

    private static boolean isItemGui(ModGUIs gui) {
        return gui == ModGUIs.KNOWLEDGE_CORE_MANAGE
                || gui == ModGUIs.WIRELESS_ARCANE_TERMINAL;
    }

    private static IllegalArgumentException rejectArgument(String diagnostic) {
        ThELog.error(diagnostic);
        return new IllegalArgumentException(diagnostic);
    }

    private static IllegalStateException rejectState(String diagnostic) {
        ThELog.error(diagnostic);
        return new IllegalStateException(diagnostic);
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
}
