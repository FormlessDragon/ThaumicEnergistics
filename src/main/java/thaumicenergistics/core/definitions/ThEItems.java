package thaumicenergistics.core.definitions;

import ae2.core.AEConfig;
import ae2.core.definitions.AEBlocks;
import ae2.core.definitions.BlockDefinition;
import ae2.core.definitions.ItemDefinition;
import ae2.items.materials.StorageComponentItem;
import ae2.items.storage.BasicStorageCell;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import thaumicenergistics.api.ids.ThEItemIds;
import thaumicenergistics.api.storage.EssentiaStorageCell;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.items.CreativeEssentiaCell;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.items.tools.powerd.WirelessArcaneTermItem;

import java.util.function.IntSupplier;

public final class ThEItems {

    public static final ItemDefinition<CreativeEssentiaCell> CREATIVE_ESSENTIA_CELL = new ItemDefinition<>(ThEItemIds.CREATIVE_ESSENTIA_CELL,
            new CreativeEssentiaCell(), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<BasicStorageCell> ESSENTIA_CELL_1K = new ItemDefinition<>(ThEItemIds.ESSENTIA_STORAGE_CELL_1K,
            new EssentiaStorageCell(1), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<BasicStorageCell> ESSENTIA_CELL_4K = new ItemDefinition<>(ThEItemIds.ESSENTIA_STORAGE_CELL_4K,
            new EssentiaStorageCell(4), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<BasicStorageCell> ESSENTIA_CELL_16K = new ItemDefinition<>(ThEItemIds.ESSENTIA_STORAGE_CELL_16K,
            new EssentiaStorageCell(16), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<BasicStorageCell> ESSENTIA_CELL_64K = new ItemDefinition<>(ThEItemIds.ESSENTIA_STORAGE_CELL_64K,
            new EssentiaStorageCell(64), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<StorageComponentItem> ESSENTIA_COMPONENT_1K = new ItemDefinition<>(ThEItemIds.ESSENTIA_COMPONENT_1K,
            new StorageComponentItem(1), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<StorageComponentItem> ESSENTIA_COMPONENT_4K = new ItemDefinition<>(ThEItemIds.ESSENTIA_COMPONENT_4K,
            new StorageComponentItem(4), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<StorageComponentItem> ESSENTIA_COMPONENT_16K = new ItemDefinition<>(ThEItemIds.ESSENTIA_COMPONENT_16K,
            new StorageComponentItem(16), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<StorageComponentItem> ESSENTIA_COMPONENT_64K = new ItemDefinition<>(ThEItemIds.ESSENTIA_COMPONENT_64K,
            new StorageComponentItem(64), ModGlobals.CREATIVE_TAB);

    public static final ItemDefinition<WirelessArcaneTermItem> WIRELESS_ARCANE_TERMINAL = new ItemDefinition<>(ThEItemIds.WIRELESS_ARCANE_TERMINAL,
            new WirelessArcaneTermItem(getWirelessTerminalBattery()), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<ItemKnowledgeCore> BLANK_KNOWLEDGE_CORE = new ItemDefinition<>(ThEItemIds.BLANK_KNOWLEDGE_CORE,
            new ItemKnowledgeCore(true), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<ItemKnowledgeCore> KNOWLEDGE_CORE = new ItemDefinition<>(ThEItemIds.KNOWLEDGE_CORE,
            new ItemKnowledgeCore(false), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<Item> UPGRADE_ARCANE = new ItemDefinition<>(ThEItemIds.UPGRADE_ARCANE,
            new Item(), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<Item> DIFFUSION_CORE = new ItemDefinition<>(ThEItemIds.DIFFUSION_CORE,
            new Item(), ModGlobals.CREATIVE_TAB);
    public static final ItemDefinition<Item> COALESCENCE_CORE = new ItemDefinition<>(ThEItemIds.COALESCENCE_CORE,
            new Item(), ModGlobals.CREATIVE_TAB);

    private static final ItemDefinition<?>[] ITEMS = {
            CREATIVE_ESSENTIA_CELL,
            ESSENTIA_CELL_1K,
            ESSENTIA_CELL_4K,
            ESSENTIA_CELL_16K,
            ESSENTIA_CELL_64K,
            ESSENTIA_COMPONENT_1K,
            ESSENTIA_COMPONENT_4K,
            ESSENTIA_COMPONENT_16K,
            ESSENTIA_COMPONENT_64K,

            WIRELESS_ARCANE_TERMINAL,
            BLANK_KNOWLEDGE_CORE,
            KNOWLEDGE_CORE,
            UPGRADE_ARCANE,
            DIFFUSION_CORE,
            COALESCENCE_CORE
    };

    private ThEItems() {}

    private static double getWirelessTerminalBattery() {
        return getConfiguredBattery(AEConfig.instance()::getWirelessTerminalBattery, 1600000);
    }

    private static double getConfiguredBattery(IntSupplier supplier, double fallback) {
        try {
            return supplier.getAsInt();
        } catch (IllegalStateException ignored) {
            return fallback;
        }
    }

    public static ItemDefinition<?>[] all() {
        return ITEMS.clone();
    }

    public static void register(RegistryEvent.Register<Item> event) {
        for(BlockDefinition<?> definition : ThEBlocks.all()) {
            ItemBlock itemBlock = definition.item();
            if(itemBlock != null) {
                event.getRegistry().register(itemBlock);
            }
        }
        for(ItemDefinition<?> definition : ITEMS) {
            event.getRegistry().register(definition.item());
        }
        ThEParts.register(event);
    }

}
