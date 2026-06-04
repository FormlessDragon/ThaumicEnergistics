package thaumicenergistics.part;

import ae2.container.ISubGui;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.storage.ILinkStatus;
import ae2.api.storage.MEStorage;
import ae2.api.storage.ITerminalHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.integration.appeng.grid.GridUtil;
import thaumicenergistics.items.ItemPartBase;

/**
 * @author BrockWS
 */
public abstract class PartSharedTerminal extends PartBase implements ITerminalHost {

    protected final ModGUIs gui;    // the GUI that corresponds to this terminal, mainly used to know where to return to, from a different GUI

    public PartSharedTerminal(ItemPartBase item, ModGUIs gui) {
        super(item);
        this.gui = gui;
    }

    public ModGUIs getGui() {
        return this.gui;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.getConfigManager().readFromNBT(nbt);
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        this.getConfigManager().writeToNBT(nbt);
    }

    @Override
    public MEStorage getInventory() {
        try {
            return GridUtil.getStorageGrid(this).getInventory();
        } catch (thaumicenergistics.integration.appeng.compat.GridAccessException e) {
            // Ignored
        }
        return null;
    }

    @Override
    public ILinkStatus getLinkStatus() {
        return ILinkStatus.ofManagedNode(this.managedGridNode);
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        GuiHandler.openGUI(this.getGui(), player, this.getLocation().getPos(), this.side);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return this.getItemStack(thaumicenergistics.integration.appeng.compat.ThEPartItemStack.NETWORK);
    }

    @Override
    public double getIdlePowerUsage() {
        return 0.5d;
    }

    @Override
    public final void getBoxes(final IPartCollisionHelper bch) {
        bch.addBox(2, 2, 14, 14, 14, 16);
        bch.addBox(4, 4, 13, 12, 12, 14);
    }

    @Override
    public int getLightLevel() {
        return this.blockLight(this.isPowered() ? 9 : 0);
    }
}
