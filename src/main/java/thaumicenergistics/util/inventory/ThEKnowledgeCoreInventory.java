package thaumicenergistics.util.inventory;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import thaumicenergistics.items.ItemKnowledgeCore;

/**
 * @author Alex811
 */
public class ThEKnowledgeCoreInventory extends ThEUpgradeInventory {

    public ThEKnowledgeCoreInventory(String customName, int size, int stackLimit, ItemStack upgradable) {
        super(customName, size, stackLimit, upgradable);
    }

    @Override
    public boolean isKnowledgeCoreSlot() {
        return true;
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemKnowledgeCore;
    }

    @Override
    public int getInstalledUpgrades(Item upgrade) {
        if (!(upgrade instanceof ItemKnowledgeCore)) {
            return 0;
        }

        int installed = 0;
        for (ItemStack stack : this) {
            if (!stack.isEmpty() && stack.getItem() == upgrade) {
                installed += stack.getCount();
            }
        }
        return Math.min(installed, this.getMaxInstalled(upgrade));
    }

    @Override
    public int getMaxInstalled(Item upgrade) {
        return upgrade instanceof ItemKnowledgeCore ? this.size() : 0;
    }
}
