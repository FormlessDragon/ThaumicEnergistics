package thaumicenergistics.items;

import net.minecraft.item.Item;
import thaumicenergistics.util.KnowledgeCoreUtil;

/**
 * If you're looking for methods to operate on a
 * Knowledge Core ItemStack and its recipes, check out {@link KnowledgeCoreUtil}
 *
 * @author Alex811
 */
public class ItemKnowledgeCore extends Item {

    boolean isBlank;

    public ItemKnowledgeCore(boolean isBlank) {
        this.isBlank = isBlank;
    }

    public boolean isBlank() {
        return this.isBlank;
    }
}
