package thaumicenergistics.part;

import ae2.api.inventories.InternalInventory;
import ae2.api.parts.IPartItem;
import ae2.container.ISubGui;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.parts.reporting.AbstractTerminalPart;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.core.ModGUIs;
import thaumicenergistics.util.AEUtil;
import thaumicenergistics.util.ForgeUtil;

import java.util.List;
import java.util.function.Function;

/**
 * Shared AE2 part implementation for the Arcane Terminal and Arcane Inscriber.
 * <p>
 * It owns the common fifteen-slot matrix and centralizes GUI navigation, Vis context, native AE2 NBT persistence,
 * clearing and drop collection. Subclasses supply their semantically distinct auxiliary inventory.
 */
public abstract class AbstractArcaneTerminalPart extends AbstractTerminalPart implements IArcaneTerminalHost {

    private static final String TAG_CRAFTING = "crafting";

    private final ModGUIs gui;
    private final AppEngInternalInventory craftingInventory;

    protected AbstractArcaneTerminalPart(IPartItem<?> item,
                                         ModGUIs gui,
                                         Function<InternalInventoryHost, AppEngInternalInventory> matrixFactory) {
        super(item);
        this.gui = gui;
        this.craftingInventory = matrixFactory.apply(this);
        if (this.craftingInventory == null || this.craftingInventory.size() != 15) {
            int actualSize = this.craftingInventory == null ? -1 : this.craftingInventory.size();
            ThELog.error("Arcane part {} created an invalid crafting matrix with {} slots",
                    this.getClass().getName(), actualSize);
            throw new IllegalStateException("Arcane crafting matrix must contain exactly 15 slots");
        }
    }

    @Override
    public final ModGUIs getGui() {
        return this.gui;
    }

    @Override
    public final InternalInventory getArcaneCraftingInventory() {
        return this.craftingInventory;
    }

    /**
     * Returns the subclass-owned inventory that participates in NBT, clearing and drop lifecycle handling.
     */
    protected abstract InternalInventory getArcaneAuxiliaryInventory();

    /**
     * Reads the subclass-owned inventory from its dedicated native AE2 NBT tag.
     */
    protected abstract void readArcaneAuxiliaryInventory(NBTTagCompound tag);

    /**
     * Writes the subclass-owned inventory to its dedicated native AE2 NBT tag.
     */
    protected abstract void writeArcaneAuxiliaryInventory(NBTTagCompound tag);

    /**
     * Indicates whether the matrix contains real items that must be dropped when the part is removed.
     */
    protected boolean shouldDropCraftingInventory() {
        return true;
    }

    @Override
    public final void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.craftingInventory.clear();
        this.getArcaneAuxiliaryInventory().clear();
        this.craftingInventory.readFromNBT(tag, TAG_CRAFTING);
        this.readArcaneAuxiliaryInventory(tag);
    }

    @Override
    public final void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        this.craftingInventory.writeToNBT(tag, TAG_CRAFTING);
        this.writeArcaneAuxiliaryInventory(tag);
    }

    @Override
    public final void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        if (this.shouldDropCraftingInventory()) {
            this.addInventoryDrops(drops, this.craftingInventory);
        }
        this.addInventoryDrops(drops, this.getArcaneAuxiliaryInventory());
    }

    @Override
    public final void clearContent() {
        super.clearContent();
        this.craftingInventory.clear();
        this.getArcaneAuxiliaryInventory().clear();
    }

    protected final void addInventoryDrops(List<ItemStack> drops, InternalInventory inventory) {
        for (ItemStack stack : inventory) {
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Override
    public void saveChanges() {
        if (this.getHost() != null) {
            super.saveChanges();
        }
    }

    public final TileEntity getTile() {
        return this.getTileEntity();
    }

    @Override
    public final boolean hasVisSource() {
        return this.getTile() != null && this.getTile().hasWorld();
    }

    @Override
    public final World getVisWorld() {
        return this.getTile().getWorld();
    }

    @Override
    public final BlockPos getVisPos() {
        return this.getTile().getPos();
    }

    @Override
    public final BlockPos getReturnPos() {
        return this.getTile().getPos();
    }

    @Override
    public final EnumFacing getReturnSide() {
        return this.getSide();
    }

    @Override
    public final void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        ThEGuiOpener.openLocatorGui(player, this.getGui(), GuiHostLocators.forPart(this), true);
    }

    @Override
    public final ItemStack getMainContainerIcon() {
        return this.getPartItem().asItemStack();
    }

    @Override
    public final boolean onUseItemOn(ItemStack itemStack, EntityPlayer player, EnumHand hand, Vec3d pos) {
        if (super.onUseItemOn(itemStack, player, hand, pos)) {
            return true;
        }
        if (player.isSneaking() && AEUtil.isWrench(player.getHeldItem(hand), player, this.getTile().getPos())) {
            return false;
        }
        return this.openTerminalGui(player);
    }

    @Override
    public final boolean onUseWithoutItem(EntityPlayer player, Vec3d pos) {
        return this.openTerminalGui(player);
    }

    private boolean openTerminalGui(EntityPlayer player) {
        if (ForgeUtil.isServer()) {
            ThEGuiOpener.openLocatorGui(player, this.getGui(), GuiHostLocators.forPart(this), false);
        }
        this.getHost().markForUpdate();
        return true;
    }
}
