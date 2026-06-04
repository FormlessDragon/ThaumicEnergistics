package thaumicenergistics.core.definitions;

import ae2.core.definitions.ItemDefinition;
import ae2.items.materials.StorageComponentItem;
import ae2.items.storage.BasicStorageCell;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import thaumicenergistics.api.ids.ThEItemIds;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.me.key.AEEssentiaKeys;

public final class ThEItems {

    public static final ItemDefinition<BasicStorageCell> ESSENTIA_CELL_1K = new ItemDefinition<>(ThEItemIds.ESSENTIA_STORAGE_CELL_1K,
            new BasicStorageCell(1, 1, 8, 12, AEEssentiaKeys.INSTANCE), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<StorageComponentItem> ESSENTIA_COMPONENT_1K = new ItemDefinition<>(ThEItemIds.ESSENTIA_COMPONENT_1K,
            new StorageComponentItem(1), ModGlobals.CREATIVE_TAB);

    private static final ItemDefinition<?>[] ITEMS = {
            ESSENTIA_CELL_1K,
            ESSENTIA_COMPONENT_1K
    };

    public static void register(RegistryEvent.Register<Item> event) {
        for(ItemDefinition<?> definition : ITEMS) {
            event.getRegistry().register(definition.item());
        }
    }

    public static ItemDefinition<?>[] all() {
        return ITEMS.clone();
    }

}
