package thaumicenergistics.util.inventory;

import ae2.api.inventories.BaseInternalInventory;
import net.minecraft.nbt.NBTBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.util.INBTSerializable;

/**
 * Manages an internal inventory
 *
 * @author BrockWS
 */
public class ThEInternalInventory extends BaseInternalInventory
        implements IInventory, INBTSerializable<NBTTagList>, Iterable<ItemStack> {

    private static final String DEFAULT_NAME = "container.inventory";

    /**
     * Stack size limit.
     */
    private final int stackLimit;

    /**
     * The inventory slots.
     */
    private final NonNullList<ItemStack> slots;

    /**
     * Name of the inventory.
     */
    private final String customName;

    public ThEInternalInventory(String customName, int size, int stackLimit) {
        this.slots = NonNullList.withSize(size, ItemStack.EMPTY);
        this.customName = customName;
        this.stackLimit = stackLimit;
    }

    @Override
    public int size() {
        return this.slots.size();
    }

    @Override
    public int getSizeInventory() {
        return this.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.slots) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return this.slots.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        ItemStack stack = this.getStackInSlot(index);
        if (stack.isEmpty())
            return stack;
        ItemStack toReturn = stack.splitStack(count);
        this.setInventorySlotContents(index, stack);
        return toReturn;
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        ItemStack stack = this.getStackInSlot(index);
        this.setInventorySlotContents(index, ItemStack.EMPTY);
        return stack != null ? stack : ItemStack.EMPTY;
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.setItemDirect(index, stack);
    }

    @Override
    public void setItemDirect(int slotIndex, ItemStack stack) {
        this.slots.set(slotIndex, this.normalizeStack(stack));
        this.markDirty();
    }

    @Override
    public int getInventoryStackLimit() {
        return this.stackLimit;
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.stackLimit;
    }

    @Override
    public void markDirty() {

    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return true;
    }

    @Override
    public void openInventory(EntityPlayer player) {

    }

    @Override
    public void closeInventory(EntityPlayer player) {

    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.isItemValidForSlot(slot, stack);
    }

    @Override
    public int getField(int id) {
        return 0; // ?
    }

    @Override
    public void setField(int id, int value) {
        // ?
    }

    @Override
    public int getFieldCount() {
        return 0; // ?
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.slots.size(); i++) {
            this.slots.set(i, ItemStack.EMPTY);
        }
        this.markDirty();
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : DEFAULT_NAME;
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null;
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString(this.getName());
    }

    public NBTTagList serializeNBT(boolean noAir) {
        NBTTagList nbt = new NBTTagList();
        this.slots.forEach(slot -> {
            if (noAir && slot.isEmpty())
                nbt.appendTag(new NBTTagCompound());
            else
                nbt.appendTag(slot.serializeNBT());
        });
        return nbt;
    }

    @Override
    public NBTTagList serializeNBT() {
        return this.serializeNBT(false);
    }

    @Override
    public void deserializeNBT(NBTTagList nbt) {
        NonNullList<ItemStack> deserializedSlots = this.readSlots(nbt);
        for (int i = 0; i < this.slots.size(); i++) {
            this.slots.set(i, deserializedSlots.get(i));
        }
        this.markDirty();
    }

    private NonNullList<ItemStack> readSlots(NBTTagList nbt) {
        if (nbt.tagCount() > this.slots.size()) {
            throw new IllegalArgumentException("Inventory NBT list has " + nbt.tagCount()
                    + " slot entries but inventory size is " + this.slots.size());
        }

        NonNullList<ItemStack> deserializedSlots = NonNullList.withSize(this.slots.size(), ItemStack.EMPTY);
        for (int i = 0; i < nbt.tagCount(); i++) {
            NBTBase slotTag = nbt.get(i);
            if (!(slotTag instanceof NBTTagCompound)) {
                throw new IllegalArgumentException("Inventory slot " + i
                        + " must be a compound tag, got type " + slotTag.getId());
            }
            deserializedSlots.set(i, this.normalizeStack(new ItemStack((NBTTagCompound) slotTag)));
        }
        return deserializedSlots;
    }

    private ItemStack normalizeStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();
        int stackSizeLimit = Math.min(this.stackLimit, copy.getMaxStackSize());
        if (stackSizeLimit <= 0) {
            return ItemStack.EMPTY;
        }
        if (copy.getCount() > stackSizeLimit) {
            copy.setCount(stackSizeLimit);
        }
        return copy.isEmpty() ? ItemStack.EMPTY : copy;
    }
}
