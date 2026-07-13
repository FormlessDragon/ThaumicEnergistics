package thaumicenergistics.container.item;

import ae2.api.inventories.InternalInventory;
import ae2.api.storage.ILinkStatus;
import ae2.api.util.DimensionalBlockPos;
import ae2.container.ISubGui;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.helpers.WirelessTerminalGuiHost;
import ae2.items.tools.powered.WirelessTerminalItem;
import ae2.items.tools.powered.WirelessTerminals;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.util.inv.AppEngInternalInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumicenergistics.api.storage.IArcaneTerminalUpgradeHost;
import thaumicenergistics.client.gui.ModGUIs;

import java.util.function.BiConsumer;

public class WirelessArcaneTerminalGuiHost extends WirelessTerminalGuiHost<WirelessTerminalItem>
        implements IArcaneTerminalUpgradeHost {

    private static final String TAG_ARCANE_MATRIX = "arcaneMatrix";
    private static final String TAG_ARCANE_UPGRADES = "arcaneUpgrades";

    private final WirelessTerminalItem terminalItem;
    private final AppEngInternalInventory craftingInventory;
    private final IUpgradeInventory upgradeInventory;

    public WirelessArcaneTerminalGuiHost(WirelessTerminalItem stackItem,
                                         WirelessTerminalItem terminalItem,
                                         EntityPlayer player,
                                         ItemGuiHostLocator locator,
                                         BiConsumer<EntityPlayer, ISubGui> returnToMainContainer) {
        super(stackItem, terminalItem, player, locator, returnToMainContainer);
        this.terminalItem = terminalItem;
        this.craftingInventory = new AppEngInternalInventory(null, 15, 64) {
            @Override
            protected void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                WirelessArcaneTerminalGuiHost.this.saveArcaneInventories();
            }
        };
        this.upgradeInventory = UpgradeInventories.forMachine(this.terminalItem, 1, this::saveArcaneInventories);
        this.loadArcaneInventories();
    }

    private NBTTagCompound getTerminalData() {
        return WirelessTerminals.getTerminalData(getItemStack(), this.terminalItem);
    }

    private DimensionalBlockPos getLinkedPosition() {
        return this.terminalItem.getLinkedPosition(getItemStack(), this.terminalItem);
    }

    private void loadArcaneInventories() {
        NBTTagCompound data = WirelessTerminals.getExistingTerminalData(getItemStack(), this.terminalItem);
        if (data == null) {
            return;
        }
        this.craftingInventory.readFromNBT(data, TAG_ARCANE_MATRIX);
        this.upgradeInventory.readFromNBT(data, TAG_ARCANE_UPGRADES);
    }

    private void saveArcaneInventories() {
        NBTTagCompound data = this.getTerminalData();
        this.craftingInventory.writeToNBT(data, TAG_ARCANE_MATRIX);
        this.upgradeInventory.writeToNBT(data, TAG_ARCANE_UPGRADES);
    }

    @Override
    public ModGUIs getGui() {
        return ModGUIs.WIRELESS_ARCANE_TERMINAL;
    }

    @Override
    public InternalInventory getArcaneCraftingInventory() {
        return this.craftingInventory;
    }

    @Override
    public IUpgradeInventory getArcaneUpgradeInventory() {
        return this.upgradeInventory;
    }

    @Override
    public boolean hasVisSource() {
        return this.getLinkedPosition() != null && this.getLinkStatus().connected();
    }

    @Override
    public World getVisWorld() {
        DimensionalBlockPos linkedPosition = this.getLinkedPosition();
        return linkedPosition != null ? linkedPosition.getLevel() : this.getPlayer().world;
    }

    @Override
    public BlockPos getVisPos() {
        DimensionalBlockPos linkedPosition = this.getLinkedPosition();
        return linkedPosition != null ? linkedPosition.getPos() : this.getPlayer().getPosition();
    }

    @Override
    public BlockPos getReturnPos() {
        return this.getPlayer().getPosition();
    }

    @Override
    public EnumFacing getReturnSide() {
        return EnumFacing.UP;
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        super.returnToMainContainer(player, subGui);
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return getItemStack();
    }

    @Override
    public ILinkStatus getLinkStatus() {
        return super.getLinkStatus();
    }
}
