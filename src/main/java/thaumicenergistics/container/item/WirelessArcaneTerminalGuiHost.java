package thaumicenergistics.container.item;

import ae2.api.storage.ILinkStatus;
import ae2.api.util.DimensionalBlockPos;
import ae2.container.ISubGui;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.helpers.WirelessTerminalGuiHost;
import ae2.items.tools.powered.WirelessTerminalItem;
import ae2.items.tools.powered.WirelessTerminals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import java.util.function.BiConsumer;

public class WirelessArcaneTerminalGuiHost extends WirelessTerminalGuiHost<WirelessTerminalItem>
        implements IArcaneTerminalHost {

    private static final String TAG_ARCANE_MATRIX = "arcaneMatrix";
    private static final String TAG_ARCANE_UPGRADES = "arcaneUpgrades";

    private final WirelessTerminalItem terminalItem;
    private final ThEInternalInventory craftingInventory;
    private final ThEUpgradeInventory upgradeInventory;

    public WirelessArcaneTerminalGuiHost(WirelessTerminalItem stackItem,
                                         WirelessTerminalItem terminalItem,
                                         EntityPlayer player,
                                         ItemGuiHostLocator locator,
                                         BiConsumer<EntityPlayer, ISubGui> returnToMainContainer) {
        super(stackItem, terminalItem, player, locator, returnToMainContainer);
        this.terminalItem = terminalItem;
        this.craftingInventory = new ThEInternalInventory("matrix", 15, 64) {
            @Override
            public void markDirty() {
                super.markDirty();
                WirelessArcaneTerminalGuiHost.this.saveArcaneInventories();
            }
        };
        this.upgradeInventory = new ThEUpgradeInventory("upgrades", 1, 1, getItemStack()) {
            @Override
            public void markDirty() {
                super.markDirty();
                WirelessArcaneTerminalGuiHost.this.saveArcaneInventories();
            }
        };
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
        if (data.hasKey(TAG_ARCANE_MATRIX)) {
            this.craftingInventory.deserializeNBT(data.getTagList(TAG_ARCANE_MATRIX, 10));
        }
        if (data.hasKey(TAG_ARCANE_UPGRADES)) {
            this.upgradeInventory.deserializeNBT(data.getTagList(TAG_ARCANE_UPGRADES, 10));
        }
    }

    private void saveArcaneInventories() {
        NBTTagCompound data = this.getTerminalData();
        data.setTag(TAG_ARCANE_MATRIX, this.craftingInventory.serializeNBT());
        data.setTag(TAG_ARCANE_UPGRADES, this.upgradeInventory.serializeNBT());
    }

    @Override
    public ModGUIs getGui() {
        return ModGUIs.WIRELESS_ARCANE_TERMINAL;
    }

    @Override
    public IItemHandler getInventoryByName(String name) {
        if (name.equalsIgnoreCase("crafting")) {
            return new InvWrapper(this.craftingInventory);
        }
        if (name.equalsIgnoreCase("upgrades")) {
            return new InvWrapper(this.upgradeInventory);
        }
        return null;
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
