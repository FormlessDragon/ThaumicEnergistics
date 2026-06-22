package thaumicenergistics.init;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.thaumicenergistics.Reference;

/**
 * Contains useful constant values
 *
 * @author BrockWS
 */
public class ModGlobals {

    public static final String MOD_DEPENDENCIES =
            "required-after:ae2;" +
            "required-after:thaumcraft@[6.1.BETA26,);" +
            "after:thaumicjei;" +
            "after:inventorytweaks;" +
            "after:waila;" +
            "after:theoneprobe";

    /**
     * Creative tab.
     */
    public static CreativeTabs CREATIVE_TAB = new CreativeTabs("ThaumicEnergistics") {

        @Override
        public ItemStack createIcon() {
            ItemStack icon = ThEItems.ESSENTIA_CELL_1K.stack(1);
            if (icon.isEmpty())
                throw new NullPointerException("Unable to use essentiaCell1k for creative tab!");
            return icon;
        }
    };

    public static final String RESEARCH_CATEGORY = Reference.MOD_ID.toUpperCase();

    public static final String MOD_ID_AE2 = "ae2";

    public static final boolean DEBUG_MODE = System.getProperties().containsKey("thaumicenergisticsdebug");
}
