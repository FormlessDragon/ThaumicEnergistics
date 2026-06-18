package thaumicenergistics.util.inventory;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.Upgrades;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.ThEBlocks;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEUpgradeInventoryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }

        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 1);
    }

    @Test
    void validityUsesSupergiantUpgradeRegistry() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(1);
        ItemStack supportedUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);
        ItemStack unsupportedUpgrade = ThEItems.DIFFUSION_CORE.stack(1);

        assertAll(
                () -> assertEquals(1, Upgrades.getMaxInstallable(
                        ThEItems.UPGRADE_ARCANE.item(),
                        ThEBlocks.ARCANE_ASSEMBLER.item())),
                () -> assertEquals(0, Upgrades.getMaxInstallable(
                        ThEItems.DIFFUSION_CORE.item(),
                        ThEBlocks.ARCANE_ASSEMBLER.item())),
                () -> assertTrue(inventory.isItemValidForSlot(0, supportedUpgrade)),
                () -> assertFalse(inventory.isItemValidForSlot(0, unsupportedUpgrade)));
    }

    @Test
    void implementsSupergiantUpgradeInventoryContract() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(1);

        assertAll(
                () -> assertTrue(inventory instanceof IUpgradeInventory),
                () -> assertEquals(ThEBlocks.ARCANE_ASSEMBLER.item(), inventory.getUpgradableItem()),
                () -> assertEquals(1, inventory.getMaxInstalled(ThEItems.UPGRADE_ARCANE.item())),
                () -> assertEquals(0, inventory.getMaxInstalled(ThEItems.DIFFUSION_CORE.item())));
    }

    @Test
    void sameUpgradeIsInvalidOnceInstalledCountReachesRegistryLimit() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(1);
        ItemStack arcaneUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);

        assertTrue(inventory.isItemValidForSlot(0, arcaneUpgrade));

        inventory.setInventorySlotContents(0, arcaneUpgrade);

        assertEquals(1, inventory.getInstalledUpgrades(ThEItems.UPGRADE_ARCANE.item()));
        assertFalse(inventory.isItemValidForSlot(0, arcaneUpgrade));
    }

    @Test
    void itemAndStackUpgradeCountsStayConsistentAndRefreshAfterInventoryChanges() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(2);
        ItemStack arcaneUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);

        assertCounts(inventory, arcaneUpgrade, 0);

        inventory.setInventorySlotContents(0, arcaneUpgrade);
        assertCounts(inventory, arcaneUpgrade, 1);

        inventory.removeStackFromSlot(0);
        assertCounts(inventory, arcaneUpgrade, 0);

        inventory.setInventorySlotContents(1, arcaneUpgrade.copy());
        assertCounts(inventory, arcaneUpgrade, 1);

        inventory.getStackInSlot(1).setCount(0);
        assertCounts(inventory, arcaneUpgrade, 1);
        inventory.markDirty();
        assertCounts(inventory, arcaneUpgrade, 0);
    }

    @Test
    void normalUpgradeSlotsRejectKnowledgeCores() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(1);

        assertFalse(inventory.isKnowledgeCoreSlot());
        assertFalse(inventory.isItemValidForSlot(0, ThEItems.BLANK_KNOWLEDGE_CORE.stack(1)));
        assertFalse(inventory.isItemValidForSlot(0, ThEItems.KNOWLEDGE_CORE.stack(1)));
    }

    @Test
    void nbtRoundtripPreservesSlotPositionsIncludingEmptySlots() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(3);
        inventory.setInventorySlotContents(1, new ItemStack(Items.DIAMOND));

        NBTTagList serialized = inventory.serializeNBT(true);

        assertEquals(3, serialized.tagCount());
        assertTrue(serialized.getCompoundTagAt(0).isEmpty());
        assertFalse(serialized.getCompoundTagAt(1).isEmpty());
        assertTrue(serialized.getCompoundTagAt(2).isEmpty());

        ThEUpgradeInventory restored = arcaneAssemblerUpgrades(3);
        restored.deserializeNBT(serialized);

        assertTrue(restored.getStackInSlot(0).isEmpty());
        assertEquals(Items.DIAMOND, restored.getStackInSlot(1).getItem());
        assertEquals(1, restored.getStackInSlot(1).getCount());
        assertTrue(restored.getStackInSlot(2).isEmpty());
    }

    @Test
    void compoundSubtagRoundtripUsesPositionListWithoutSparseSlotKeys() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(3);
        inventory.setInventorySlotContents(1, new ItemStack(Items.DIAMOND));

        NBTTagCompound compound = new NBTTagCompound();
        inventory.writeToNBT(compound, "upgrades");

        NBTTagList serialized = compound.getTagList("upgrades", 10);
        assertEquals(3, serialized.tagCount());
        assertTrue(serialized.getCompoundTagAt(0).isEmpty());
        assertFalse(serialized.getCompoundTagAt(1).isEmpty());
        assertFalse(serialized.getCompoundTagAt(1).hasKey("Slot"));
        assertTrue(serialized.getCompoundTagAt(2).isEmpty());

        ThEUpgradeInventory restored = arcaneAssemblerUpgrades(3);
        restored.readFromNBT(compound, "upgrades");

        assertAll(
                () -> assertTrue(restored.getStackInSlot(0).isEmpty()),
                () -> assertEquals(Items.DIAMOND, restored.getStackInSlot(1).getItem()),
                () -> assertEquals(1, restored.getStackInSlot(1).getCount()),
                () -> assertTrue(restored.getStackInSlot(2).isEmpty()));
    }

    @Test
    void readFromNBTRejectsNonListSubtag() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(1);
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("upgrades", "not a list");

        assertThrows(IllegalArgumentException.class, () -> inventory.readFromNBT(compound, "upgrades"));
    }

    @Test
    void readFromNBTRejectsListWithNonCompoundElement() {
        ThEUpgradeInventory inventory = arcaneAssemblerUpgrades(1);
        NBTTagCompound compound = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        list.appendTag(new NBTTagString("not a compound"));
        compound.setTag("upgrades", list);

        assertThrows(IllegalArgumentException.class, () -> inventory.readFromNBT(compound, "upgrades"));
    }

    @Test
    void inventoryWithoutUpgradableItemFailsFastWhenUpgradeContractIsUsed() {
        ThEUpgradeInventory inventory = new ThEUpgradeInventory("upgrades", 1, 1);
        ItemStack arcaneUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);

        assertAll(
                () -> assertThrows(IllegalStateException.class, inventory::getUpgradableItem),
                () -> assertThrows(IllegalStateException.class,
                        () -> inventory.isItemValidForSlot(0, arcaneUpgrade)));
    }

    private static ThEUpgradeInventory arcaneAssemblerUpgrades(int size) {
        return new ThEUpgradeInventory("upgrades", size, 1, ThEBlocks.ARCANE_ASSEMBLER.stack(1));
    }

    private static void assertCounts(ThEUpgradeInventory inventory, ItemStack stack, int expected) {
        assertEquals(expected, inventory.getInstalledUpgrades(stack.getItem()));
        assertEquals(expected > 0, inventory.isInstalled(stack.getItem()));
    }
}
