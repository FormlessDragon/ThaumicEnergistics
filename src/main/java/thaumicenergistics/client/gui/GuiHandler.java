package thaumicenergistics.client.gui;

import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.core.gui.locator.PartLocator;
import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.client.gui.item.GuiKnowledgeCore;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.part.*;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.part.*;
import thaumicenergistics.tile.TileArcaneAssembler;

import javax.annotation.Nullable;

/**
 * @author BrockWS
 */
public class GuiHandler implements IGuiHandler {

    public static void openGUI(ModGUIs gui, EntityPlayer player) {
        GuiHandler.openGUI(gui, player, null);
    }

    public static void openGUI(ModGUIs gui, EntityPlayer player, BlockPos pos) {
        GuiHandler.openGUI(gui, player, pos, null);
    }

    public static void openGUI(ModGUIs gui, EntityPlayer player, int slot) {
        if (gui == null)
            throw new IllegalArgumentException("gui cannot be null!");
        else if (player == null)
            throw new IllegalArgumentException("player cannot be null!");
        if (gui == ModGUIs.WIRELESS_ARCANE_TERMINAL) {
            throw rejectWirelessLegacyRoute(gui, "slot-only helper", player);
        }

        player.openGui(ThaumicEnergistics.INSTANCE, GuiHandler.calculateOrdinal(gui, EnumFacing.UP),
                player.getEntityWorld(), slot, 0, 0);
    }

    public static void openGUI(ModGUIs gui, EntityPlayer player, BlockPos pos, EnumFacing side) {
        if (gui == null)
            throw new IllegalArgumentException("gui cannot be null!");
        else if (player == null)
            throw new IllegalArgumentException("player cannot be null!");
        if (gui == ModGUIs.WIRELESS_ARCANE_TERMINAL) {
            throw rejectWirelessLegacyRoute(gui, "generic helper", player);
        }

        if (pos != null)
            player.openGui(ThaumicEnergistics.INSTANCE, GuiHandler.calculateOrdinal(gui, side), player.getEntityWorld(), pos.getX(), pos.getY(), pos.getZ());
        else
            player.openGui(ThaumicEnergistics.INSTANCE, GuiHandler.calculateOrdinal(gui, side), player.getEntityWorld(), 0, 0, 0);
    }

    public static int calculateOrdinal(ModGUIs gui, EnumFacing side) {
        if (side == null)
            side = EnumFacing.UP;
        return (gui.ordinal() << 4) | side.ordinal();
    }

    public static ModGUIs getGUIFromOrdinal(int ordinal) {
        return ModGUIs.values()[ordinal >> 4];
    }

    public static EnumFacing getSideFromOrdinal(int ordinal) {
        return EnumFacing.values()[ordinal & 7];
    }

    private <T extends AEBaseContainer> T initContainer(T container, GuiHostLocator locator) {
        container.setLocator(locator);
        return container;
    }

    private static IllegalArgumentException rejectWirelessLegacyRoute(ModGUIs gui, String route, EntityPlayer player) {
        return new IllegalArgumentException(gui.name()
                + " requires a locator-aware opener; legacy Forge route " + route
                + " cannot preserve locator context for player " + player);
    }

    private IArcaneTerminalHost locateArcanePartHost(ModGUIs guiID, EntityPlayer player, World world,
                                                     BlockPos pos, EnumFacing side, GuiHostLocator locator) {
        IArcaneTerminalHost host = locator.locate(player, IArcaneTerminalHost.class);
        if (host == null) {
            throw this.missingHost(guiID, pos, side, IArcaneTerminalHost.class, world.getTileEntity(pos));
        }
        return host;
    }

    private GuiHostLocator getArcanePartLocator(BlockPos pos, EnumFacing side) {
        return new PartLocator(pos, side);
    }

    private ArcaneAssemblerRoute locateArcaneAssembler(ModGUIs guiID, EntityPlayer player, World world,
                                                       BlockPos pos, EnumFacing side) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile == null) {
            throw this.missingHost(guiID, pos, side, TileArcaneAssembler.class, null);
        }

        GuiHostLocator locator = GuiHostLocators.forTile(tile);
        TileArcaneAssembler host = locator.locate(player, TileArcaneAssembler.class);
        if (host == null) {
            throw this.missingHost(guiID, pos, side, TileArcaneAssembler.class, tile);
        }
        return new ArcaneAssemblerRoute(host, locator);
    }

    private Object createClientGuiElement(ModGUIs guiID, EntityPlayer player, GuiHostLocator locator) {
        PacketOpenLocatorGUI message = new PacketOpenLocatorGUI(guiID, locator, false, 0);
        Container container = ThEClientGuiOpener.createClientContainer(player, player.openContainer, message);
        return ThEClientGuiOpener.createClientScreen(player, container, message);
    }

    private ContainerKnowledgeCore createKnowledgeCoreContainer(ModGUIs guiID, EntityPlayer player, World world,
                                                               BlockPos pos, EnumFacing side) {
        ContainerArcaneInscriber parent = this.getKnowledgeCoreParent(guiID, player);
        GuiHostLocator parentLocator = parent.getLocator();
        if (parentLocator == null) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + guiID.name()
                    + " without parent locator for player " + player
                    + "; parent " + parent);
        }

        IArcaneTerminalHost parentHost = parent.getHost();
        if (parentHost == null) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + guiID.name()
                    + " without parent host for player " + player
                    + "; locator " + parentLocator
                    + "; parent " + parent);
        }

        IArcaneTerminalHost parentLocatedHost = parentLocator.locate(player, IArcaneTerminalHost.class);
        if (parentLocatedHost != parentHost) {
            throw new IllegalStateException("Knowledge Core parent locator mismatch for gui " + guiID.name()
                    + " player " + player
                    + " locator " + parentLocator
                    + " parent " + parent
                    + " parentHost " + parentHost
                    + " locatedHost " + parentLocatedHost);
        }

        GuiHostLocator routeLocator = this.getArcanePartLocator(pos, side);
        IArcaneTerminalHost routeHost = this.locateArcanePartHost(guiID, player, world, pos, side, routeLocator);
        if (routeHost != parentHost) {
            throw new IllegalStateException("Knowledge Core route host mismatch for gui " + guiID.name()
                    + " at " + pos
                    + " side " + side.name()
                    + " player " + player
                    + "; parent " + parent
                    + "; parent locator " + parentLocator
                    + "; route locator " + routeLocator
                    + "; parent host " + parentHost
                    + "; route host " + routeHost);
        }

        return new ContainerKnowledgeCore(player, guiID, parent, parentLocator);
    }

    private ContainerArcaneInscriber getKnowledgeCoreParent(ModGUIs guiID, EntityPlayer player) {
        Container openContainer = player.openContainer;
        if (!(openContainer instanceof ContainerArcaneInscriber parent)) {
            throw new IllegalStateException("Cannot open Knowledge Core gui " + guiID.name()
                    + " without parent " + ContainerArcaneInscriber.class.getName()
                    + "; player " + player
                    + "; openContainer " + openContainer);
        }
        return parent;
    }

    private IllegalStateException missingHost(ModGUIs guiID, BlockPos pos, EnumFacing side,
                                               Class<?> hostType, @Nullable Object actualHost) {
        return new IllegalStateException("Cannot locate host for " + guiID.name()
                + " at " + pos
                + " side " + side.name()
                + "; expected " + hostType.getName()
                + "; actual " + actualHost);
    }

    private record ArcaneAssemblerRoute(TileArcaneAssembler host, GuiHostLocator locator) {
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
        ModGUIs guiID = GuiHandler.getGUIFromOrdinal(ordinal);
        EnumFacing side = GuiHandler.getSideFromOrdinal(ordinal);
        BlockPos pos = new BlockPos(x, y, z);

        switch (guiID) {
            case ARCANE_ASSEMBLER: {
                ArcaneAssemblerRoute route = this.locateArcaneAssembler(guiID, player, world, pos, side);
                return this.initContainer(new ContainerArcaneAssembler(player, route.host()), route.locator());
            }
            case ARCANE_TERMINAL: {
                GuiHostLocator arcaneLocator = this.getArcanePartLocator(pos, side);
                IArcaneTerminalHost arcaneHost = this.locateArcanePartHost(guiID, player, world, pos, side, arcaneLocator);
                return this.initContainer(new ContainerArcaneTerm(player.inventory, arcaneHost), arcaneLocator);
            }
            case WIRELESS_ARCANE_TERMINAL:
                throw rejectWirelessLegacyRoute(guiID, "server IGuiHandler", player);
            case ARCANE_INSCRIBER: {
                GuiHostLocator arcaneLocator = this.getArcanePartLocator(pos, side);
                IArcaneTerminalHost arcaneHost = this.locateArcanePartHost(guiID, player, world, pos, side, arcaneLocator);
                return this.initContainer(new ContainerArcaneInscriber(player.inventory, arcaneHost), arcaneLocator);
            }
            case AE2_CRAFT_AMOUNT:
            case AE2_CRAFT_CONFIRM:
            case AE2_CRAFT_STATUS:
                return null;
            case KNOWLEDGE_CORE_ADD:
            case KNOWLEDGE_CORE_DEL:
            case KNOWLEDGE_CORE_VIEW:
                return this.createKnowledgeCoreContainer(guiID, player, world, pos, side);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
        ModGUIs guiID = GuiHandler.getGUIFromOrdinal(ordinal);
        EnumFacing side = GuiHandler.getSideFromOrdinal(ordinal);
        BlockPos pos = new BlockPos(x, y, z);

        switch (guiID) {
            case ARCANE_ASSEMBLER: {
                ArcaneAssemblerRoute route = this.locateArcaneAssembler(guiID, player, world, pos, side);
                return this.createClientGuiElement(guiID, player, route.locator());
            }
            case ARCANE_TERMINAL: {
                GuiHostLocator arcaneLocator = this.getArcanePartLocator(pos, side);
                this.locateArcanePartHost(guiID, player, world, pos, side, arcaneLocator);
                return this.createClientGuiElement(guiID, player, arcaneLocator);
            }
            case WIRELESS_ARCANE_TERMINAL:
                throw rejectWirelessLegacyRoute(guiID, "client IGuiHandler", player);
            case ARCANE_INSCRIBER: {
                GuiHostLocator arcaneLocator = this.getArcanePartLocator(pos, side);
                this.locateArcanePartHost(guiID, player, world, pos, side, arcaneLocator);
                return this.createClientGuiElement(guiID, player, arcaneLocator);
            }
            case AE2_CRAFT_AMOUNT:
            case AE2_CRAFT_CONFIRM:
            case AE2_CRAFT_STATUS:
                return null;
            case KNOWLEDGE_CORE_ADD:
            case KNOWLEDGE_CORE_DEL:
            case KNOWLEDGE_CORE_VIEW:
                return new GuiKnowledgeCore(this.createKnowledgeCoreContainer(guiID, player, world, pos, side));
            default:
                return null;
        }
    }
}
