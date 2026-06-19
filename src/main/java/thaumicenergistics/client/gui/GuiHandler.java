package thaumicenergistics.client.gui;

import ae2.api.parts.IPart;
import ae2.api.parts.IPartHost;
import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.core.gui.locator.PartLocator;
import ae2.container.AEBaseContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.client.gui.block.GuiArcaneAssembler;
import thaumicenergistics.client.gui.item.GuiKnowledgeCore;
import thaumicenergistics.client.gui.part.*;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.*;
import thaumicenergistics.init.ModGUIs;
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

        player.openGui(ThaumicEnergistics.INSTANCE, GuiHandler.calculateOrdinal(gui, EnumFacing.UP),
                player.getEntityWorld(), slot, 0, 0);
    }

    public static void openGUI(ModGUIs gui, EntityPlayer player, BlockPos pos, EnumFacing side) {
        if (gui == null)
            throw new IllegalArgumentException("gui cannot be null!");
        else if (player == null)
            throw new IllegalArgumentException("player cannot be null!");

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

    public static IPart getPartFromWorld(World world, BlockPos pos, EnumFacing side) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof IPartHost) {
            return ((IPartHost) te).getPart(side);
        }
        return null;
    }

    private GuiHostLocator getArcaneLocator(IPart part, BlockPos pos, EnumFacing side, int inventorySlot) {
        if (part != null) {
            return new PartLocator(pos, side);
        }
        return GuiHostLocators.forInventorySlot(inventorySlot);
    }

    private <T extends AEBaseContainer> T initContainer(T container, GuiHostLocator locator) {
        container.setLocator(locator);
        return container;
    }

    @Nullable
    @Override
    public Object getServerGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = null;
        ModGUIs guiID = GuiHandler.getGUIFromOrdinal(ordinal);
        EnumFacing side = GuiHandler.getSideFromOrdinal(ordinal);
        BlockPos pos = new BlockPos(x, y, z);
        IPart part = GuiHandler.getPartFromWorld(world, pos, side);
        if (part == null) te = world.getTileEntity(pos);
        IArcaneTerminalHost arcaneHost = part instanceof IArcaneTerminalHost ? (IArcaneTerminalHost) part : null;
        GuiHostLocator arcaneLocator = this.getArcaneLocator(part, pos, side, x);

        switch (guiID) {
            case ARCANE_ASSEMBLER:
                return new ContainerArcaneAssembler(player, (TileArcaneAssembler) te);
            case ARCANE_TERMINAL:
                if (arcaneHost == null) {
                    return null;
                }
                return this.initContainer(new ContainerArcaneTerm(player.inventory, arcaneHost), arcaneLocator);
            case WIRELESS_ARCANE_TERMINAL: {
                arcaneLocator = GuiHostLocators.forInventorySlot(x);
                IArcaneTerminalHost wirelessHost = arcaneLocator.locate(player, WirelessArcaneTerminalGuiHost.class);
                if (wirelessHost == null) {
                    return null;
                }
                return this.initContainer(new ContainerArcaneTerm(player.inventory, wirelessHost), arcaneLocator);
            }
            case ARCANE_INSCRIBER:
                if (arcaneHost == null) {
                    return null;
                }
                return this.initContainer(new ContainerArcaneInscriber(player.inventory, arcaneHost), arcaneLocator);
            case AE2_CRAFT_AMOUNT:
            case AE2_CRAFT_CONFIRM:
            case AE2_CRAFT_STATUS:
                return null;
            case KNOWLEDGE_CORE_ADD:
            case KNOWLEDGE_CORE_DEL:
            case KNOWLEDGE_CORE_VIEW:
                return new ContainerKnowledgeCore(player, guiID, player.openContainer);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ordinal, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity te = null;
        ModGUIs guiID = GuiHandler.getGUIFromOrdinal(ordinal);
        EnumFacing side = GuiHandler.getSideFromOrdinal(ordinal);
        BlockPos pos = new BlockPos(x, y, z);
        IPart part = GuiHandler.getPartFromWorld(world, pos, side);
        if (part == null) te = world.getTileEntity(pos);
        IArcaneTerminalHost arcaneHost = part instanceof IArcaneTerminalHost ? (IArcaneTerminalHost) part : null;
        GuiHostLocator arcaneLocator = this.getArcaneLocator(part, pos, side, x);

        switch (guiID) {
            case ARCANE_ASSEMBLER:
                return new GuiArcaneAssembler(new ContainerArcaneAssembler(player, (TileArcaneAssembler) te));
            case ARCANE_TERMINAL:
                if (arcaneHost == null) {
                    return null;
                }
                return new GuiArcaneTerm(
                        this.initContainer(new ContainerArcaneTerm(player.inventory, arcaneHost), arcaneLocator),
                        player.inventory);
            case WIRELESS_ARCANE_TERMINAL: {
                arcaneLocator = GuiHostLocators.forInventorySlot(x);
                IArcaneTerminalHost wirelessHost = arcaneLocator.locate(player, WirelessArcaneTerminalGuiHost.class);
                if (wirelessHost == null) {
                    return null;
                }
                return new GuiArcaneTerm(
                        this.initContainer(new ContainerArcaneTerm(player.inventory, wirelessHost), arcaneLocator),
                        player.inventory);
            }
            case ARCANE_INSCRIBER:
                if (arcaneHost == null) {
                    return null;
                }
                return new GuiArcaneInscriber(
                        this.initContainer(new ContainerArcaneInscriber(player.inventory, arcaneHost), arcaneLocator),
                        player.inventory);
            case AE2_CRAFT_AMOUNT:
            case AE2_CRAFT_CONFIRM:
            case AE2_CRAFT_STATUS:
                return null;
            case KNOWLEDGE_CORE_ADD:
            case KNOWLEDGE_CORE_DEL:
            case KNOWLEDGE_CORE_VIEW:
                return new GuiKnowledgeCore(new ContainerKnowledgeCore(player, guiID, player.openContainer));
            default:
                return null;
        }
    }
}
