package thaumicenergistics.items;

import net.minecraft.item.Item;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.thaumicenergistics.Reference;

/**
 * @author BrockWS
 */
public abstract class ItemBase extends Item {

    public ItemBase(String id) {
        this(id, true);
    }

    public ItemBase(String id, boolean setCreativeTab) {
        this.setTranslationKey(Reference.MOD_ID + "." + id);
        if (setCreativeTab)
            this.setCreativeTab(ModGlobals.CREATIVE_TAB);
    }
}
