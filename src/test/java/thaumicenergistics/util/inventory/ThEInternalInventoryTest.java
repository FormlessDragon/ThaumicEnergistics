package thaumicenergistics.util.inventory;

import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEInternalInventoryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void newInventoryIsEmptyUntilNonEmptyStackIsWritten() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 2, 64);

        assertTrue(inventory.isEmpty());

        inventory.setInventorySlotContents(1, new ItemStack(Items.APPLE));

        assertFalse(inventory.isEmpty());
    }

    @Test
    void clearKeepsSlotCountEmptiesSlotsAndAllowsFutureWrites() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 3, 64);
        inventory.setInventorySlotContents(0, new ItemStack(Items.APPLE, 4));
        inventory.setInventorySlotContents(2, new ItemStack(Items.DIAMOND, 2));

        inventory.clear();

        assertEquals(3, inventory.getSizeInventory());
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            assertTrue(inventory.getStackInSlot(i).isEmpty());
        }

        inventory.setInventorySlotContents(1, new ItemStack(Items.DIAMOND));

        assertEquals(Items.DIAMOND, inventory.getStackInSlot(1).getItem());
        assertEquals(1, inventory.getStackInSlot(1).getCount());
    }

    @Test
    void setInventorySlotContentsCopiesAndClampsToInventoryAndItemLimits() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 1, 8);
        ItemStack source = new ItemStack(Items.APPLE, 64);

        inventory.setInventorySlotContents(0, source);
        source.setCount(1);

        ItemStack stored = inventory.getStackInSlot(0);
        assertEquals(Items.APPLE, stored.getItem());
        assertEquals(8, stored.getCount());
        assertEquals(1, source.getCount());
    }

    @Test
    void setInventorySlotContentsClampsToItemMaxStackSize() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 1, 64);
        ItemStack source = new ItemStack(Items.IRON_CHESTPLATE, 4);

        inventory.setInventorySlotContents(0, source);

        ItemStack stored = inventory.getStackInSlot(0);
        assertEquals(Items.IRON_CHESTPLATE, stored.getItem());
        assertEquals(1, stored.getCount());
    }

    @Test
    void nullCustomNameUsesDefaultDisplayTextWithoutNpe() {
        ThEInternalInventory inventory = new ThEInternalInventory(null, 1, 64);

        assertFalse(inventory.hasCustomName());
        assertNotNull(inventory.getDisplayName().getFormattedText());
        assertEquals("container.inventory", inventory.getDisplayName().getUnformattedText());
    }

    @Test
    void serializeWithEmptyCompoundsKeepsSlotPositionsOnRoundtrip() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 3, 64);
        inventory.setInventorySlotContents(1, new ItemStack(Items.DIAMOND, 5));

        NBTTagList serialized = inventory.serializeNBT(true);

        assertEquals(3, serialized.tagCount());
        assertTrue(serialized.getCompoundTagAt(0).isEmpty());
        assertFalse(serialized.getCompoundTagAt(1).isEmpty());
        assertTrue(serialized.getCompoundTagAt(2).isEmpty());

        ThEInternalInventory restored = new ThEInternalInventory("test", 3, 64);
        restored.deserializeNBT(serialized);

        assertTrue(restored.getStackInSlot(0).isEmpty());
        assertEquals(Items.DIAMOND, restored.getStackInSlot(1).getItem());
        assertEquals(5, restored.getStackInSlot(1).getCount());
        assertTrue(restored.getStackInSlot(2).isEmpty());
    }

    @Test
    void deserializeShortListClearsOldTailContents() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 3, 64);
        inventory.setInventorySlotContents(0, new ItemStack(Items.APPLE, 2));
        inventory.setInventorySlotContents(1, new ItemStack(Items.DIAMOND, 3));
        inventory.setInventorySlotContents(2, new ItemStack(Items.EMERALD, 4));

        NBTTagList serialized = new NBTTagList();
        serialized.appendTag(new ItemStack(Items.APPLE, 1).serializeNBT());

        inventory.deserializeNBT(serialized);

        assertEquals(Items.APPLE, inventory.getStackInSlot(0).getItem());
        assertEquals(1, inventory.getStackInSlot(0).getCount());
        assertTrue(inventory.getStackInSlot(1).isEmpty());
        assertTrue(inventory.getStackInSlot(2).isEmpty());
    }

    @Test
    void deserializeLongListFailsExplicitly() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 1, 64);
        inventory.setInventorySlotContents(0, new ItemStack(Items.EMERALD, 7));
        NBTTagList serialized = new NBTTagList();
        serialized.appendTag(new ItemStack(Items.APPLE).serializeNBT());
        serialized.appendTag(new ItemStack(Items.DIAMOND).serializeNBT());

        assertThrows(IllegalArgumentException.class, () -> inventory.deserializeNBT(serialized));
        assertEquals(Items.EMERALD, inventory.getStackInSlot(0).getItem());
        assertEquals(7, inventory.getStackInSlot(0).getCount());
    }

    @Test
    void deserializeNonCompoundListFailsExplicitly() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 1, 64);
        inventory.setInventorySlotContents(0, new ItemStack(Items.EMERALD, 7));
        NBTTagList serialized = new NBTTagList();
        serialized.appendTag(new NBTTagString("not a stack compound"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> inventory.deserializeNBT(serialized));

        String message = exception.getMessage();
        assertTrue(message.contains("slot"));
        assertTrue(message.contains("compound") || message.contains("type"));
        assertEquals(Items.EMERALD, inventory.getStackInSlot(0).getItem());
        assertEquals(7, inventory.getStackInSlot(0).getCount());
    }

    @Test
    void iteratorReturnsOnlyNonEmptyStacksInSlotOrderAndCannotRemove() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 4, 64);
        inventory.setInventorySlotContents(2, new ItemStack(Items.DIAMOND, 3));
        inventory.setInventorySlotContents(0, new ItemStack(Items.APPLE, 2));

        Iterator<ItemStack> iterator = inventory.iterator();

        assertTrue(iterator.hasNext());
        ItemStack first = iterator.next();
        assertEquals(Items.APPLE, first.getItem());
        assertEquals(2, first.getCount());

        assertTrue(iterator.hasNext());
        ItemStack second = iterator.next();
        assertEquals(Items.DIAMOND, second.getItem());
        assertEquals(3, second.getCount());

        assertFalse(iterator.hasNext());
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }

    @Test
    void removeStackFromSlotReturnsOriginalStackClearsSlotAndMarksDirtyOnce() {
        DirtyCountingInventory inventory = new DirtyCountingInventory("test", 1, 64);
        ItemStack original = new ItemStack(Items.APPLE, 4);
        inventory.setInventorySlotContents(0, original);
        inventory.dirtyCount = 0;

        ItemStack removed = inventory.removeStackFromSlot(0);

        assertEquals(Items.APPLE, removed.getItem());
        assertEquals(4, removed.getCount());
        assertTrue(inventory.getStackInSlot(0).isEmpty());
        assertEquals(1, inventory.dirtyCount);
    }

    @Test
    void setClearAndDeserializeMarkDirty() {
        DirtyCountingInventory inventory = new DirtyCountingInventory("test", 2, 64);

        inventory.setInventorySlotContents(0, new ItemStack(Items.APPLE));
        assertEquals(1, inventory.dirtyCount);

        inventory.clear();
        assertEquals(2, inventory.dirtyCount);

        NBTTagList serialized = new NBTTagList();
        serialized.appendTag(new NBTTagCompound());
        serialized.appendTag(new ItemStack(Items.DIAMOND).serializeNBT());

        inventory.deserializeNBT(serialized);

        assertEquals(3, inventory.dirtyCount);
    }

    @Test
    void invWrapperKeepsInventoryIdentitySlotLimitAndDirtyCallbacks() {
        DirtyCountingInventory inventory = new DirtyCountingInventory("test", 2, 8);
        InvWrapper wrapper = new InvWrapper(inventory);

        assertEquals(2, wrapper.getSlots());
        assertSame(inventory, wrapper.getInv());
        assertEquals(8, wrapper.getSlotLimit(0));

        ItemStack insertRemainder = wrapper.insertItem(0, new ItemStack(Items.APPLE, 12), false);

        assertEquals(Items.APPLE, inventory.getStackInSlot(0).getItem());
        assertEquals(8, inventory.getStackInSlot(0).getCount());
        assertEquals(Items.APPLE, insertRemainder.getItem());
        assertEquals(4, insertRemainder.getCount());
        assertTrue(inventory.dirtyCount > 0);

        int dirtyCountAfterInsert = inventory.dirtyCount;

        ItemStack extracted = wrapper.extractItem(0, 3, false);

        assertEquals(Items.APPLE, extracted.getItem());
        assertEquals(3, extracted.getCount());
        assertEquals(Items.APPLE, inventory.getStackInSlot(0).getItem());
        assertEquals(5, inventory.getStackInSlot(0).getCount());
        assertTrue(inventory.dirtyCount > dirtyCountAfterInsert);
    }

    @Test
    void toItemHandlerCachesWrapperAndMutatesOnlyWhenNotSimulated() {
        ThEInternalInventory inventory = new ThEInternalInventory("test", 1, 10);
        IItemHandler handler = inventory.toItemHandler();

        assertSame(handler, inventory.toItemHandler());

        ItemStack simulatedInsertRemainder = handler.insertItem(0, new ItemStack(Items.DIAMOND, 6), true);

        assertTrue(simulatedInsertRemainder.isEmpty());
        assertTrue(inventory.getStackInSlot(0).isEmpty());

        ItemStack insertRemainder = handler.insertItem(0, new ItemStack(Items.DIAMOND, 6), false);

        assertTrue(insertRemainder.isEmpty());
        assertEquals(Items.DIAMOND, inventory.getStackInSlot(0).getItem());
        assertEquals(6, inventory.getStackInSlot(0).getCount());

        ItemStack simulatedExtract = handler.extractItem(0, 4, true);

        assertEquals(Items.DIAMOND, simulatedExtract.getItem());
        assertEquals(4, simulatedExtract.getCount());
        assertEquals(Items.DIAMOND, inventory.getStackInSlot(0).getItem());
        assertEquals(6, inventory.getStackInSlot(0).getCount());

        ItemStack extracted = handler.extractItem(0, 4, false);

        assertEquals(Items.DIAMOND, extracted.getItem());
        assertEquals(4, extracted.getCount());
        assertEquals(Items.DIAMOND, inventory.getStackInSlot(0).getItem());
        assertEquals(2, inventory.getStackInSlot(0).getCount());
    }

    @Test
    void subclassSlotValidityRuleIsSharedByInventoryInvWrapperAndCachedItemHandler() {
        DiamondOnlyInventory inventory = new DiamondOnlyInventory("test", 1, 8);
        InvWrapper directWrapper = new InvWrapper(inventory);
        IItemHandler cachedWrapper = inventory.toItemHandler();

        assertTrue(inventory.isItemValid(0, new ItemStack(Items.DIAMOND)));
        assertTrue(directWrapper.isItemValid(0, new ItemStack(Items.DIAMOND)));
        assertTrue(cachedWrapper.isItemValid(0, new ItemStack(Items.DIAMOND)));
        assertFalse(inventory.isItemValid(0, new ItemStack(Items.APPLE)));
        assertFalse(directWrapper.isItemValid(0, new ItemStack(Items.APPLE)));
        assertFalse(cachedWrapper.isItemValid(0, new ItemStack(Items.APPLE)));

        ItemStack illegalRemainder = cachedWrapper.insertItem(0, new ItemStack(Items.APPLE, 3), false);

        assertEquals(Items.APPLE, illegalRemainder.getItem());
        assertEquals(3, illegalRemainder.getCount());
        assertTrue(inventory.getStackInSlot(0).isEmpty());

        ItemStack legalRemainder = cachedWrapper.insertItem(0, new ItemStack(Items.DIAMOND, 4), false);

        assertTrue(legalRemainder.isEmpty());
        assertEquals(Items.DIAMOND, inventory.getStackInSlot(0).getItem());
        assertEquals(4, inventory.getStackInSlot(0).getCount());
    }

    private static final class DirtyCountingInventory extends ThEInternalInventory {
        private int dirtyCount;

        private DirtyCountingInventory(String customName, int size, int stackLimit) {
            super(customName, size, stackLimit);
        }

        @Override
        public void markDirty() {
            this.dirtyCount++;
        }
    }

    private static final class DiamondOnlyInventory extends ThEInternalInventory {
        private DiamondOnlyInventory(String customName, int size, int stackLimit) {
            super(customName, size, stackLimit);
        }

        @Override
        public boolean isItemValidForSlot(int index, ItemStack stack) {
            return stack.getItem() == Items.DIAMOND;
        }
    }
}
