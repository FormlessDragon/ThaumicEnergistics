package thaumicenergistics.util.inventory;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.Upgrades;
import ae2.items.materials.UpgradeCardItem;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEParts;
import thaumicenergistics.core.definitions.ThEBlocks;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEKnowledgeCoreInventoryTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }

        Upgrades.add(ThEItems.BLANK_KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item(), 1);
        Upgrades.add(ThEItems.KNOWLEDGE_CORE.item(), ThEParts.ARCANE_INSCRIBER.item(), 1);
    }

    @Test
    void acceptsOnlyKnowledgeCoreItemsWithoutRegistryCounts() {
        ThEKnowledgeCoreInventory inventory = new ThEKnowledgeCoreInventory(
                "knowledgeCore",
                1,
                1,
                ThEBlocks.ARCANE_ASSEMBLER.stack(1));
        ItemStack blankCore = ThEItems.BLANK_KNOWLEDGE_CORE.stack(1);
        ItemStack knowledgeCore = ThEItems.KNOWLEDGE_CORE.stack(1);

        assertAll(
                () -> assertTrue(inventory instanceof IUpgradeInventory),
                () -> assertTrue(inventory.isKnowledgeCoreSlot()),
                () -> assertTrue(inventory.isItemValidForSlot(0, blankCore)),
                () -> assertTrue(inventory.isItemValidForSlot(0, knowledgeCore)),
                () -> assertFalse(inventory.isItemValidForSlot(0, new ItemStack(new UpgradeCardItem()))),
                () -> assertFalse(inventory.isItemValidForSlot(0, ThEItems.UPGRADE_ARCANE.stack(1))),
                () -> assertEquals(0, Upgrades.getMaxInstallable(
                        ThEItems.KNOWLEDGE_CORE.item(),
                        ThEBlocks.ARCANE_ASSEMBLER.item())),
                () -> assertEquals(1, inventory.getMaxInstalled(ThEItems.KNOWLEDGE_CORE.item())),
                () -> assertEquals(0, inventory.getMaxInstalled(ThEItems.UPGRADE_ARCANE.item())));

        inventory.setInventorySlotContents(0, knowledgeCore);

        assertAll(
                () -> assertEquals(1, inventory.getInstalledUpgrades(ThEItems.KNOWLEDGE_CORE.item())),
                () -> assertEquals(0, inventory.getInstalledUpgrades(ThEItems.BLANK_KNOWLEDGE_CORE.item())),
                () -> assertEquals(0, inventory.getInstalledUpgrades(ThEItems.UPGRADE_ARCANE.item())));
    }
}
