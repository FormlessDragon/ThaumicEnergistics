package thaumicenergistics.init.internal;

import ae2.api.client.StorageCellModels;
import ae2.core.definitions.AEItems;
import ae2.recipes.game.StorageCellDisassemblyRecipe;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.core.definitions.ThEItems;

import java.util.List;
import java.util.Objects;

public final class InitStorageCells {

    // TODO: 绘制源质元件的驱动器模型
    private static final ResourceLocation MODEL_CELL_CREATIVE_ESSENTIA = id("block/drive/cells/1k_essentia_cells");
    private static final ResourceLocation MODEL_CELL_ESSENTIA_1K = id("block/drive/cells/1k_essentia_cells");
    private static final ResourceLocation MODEL_CELL_ESSENTIA_4K = id("block/drive/cells/1k_essentia_cells");
    private static final ResourceLocation MODEL_CELL_ESSENTIA_16K = id("block/drive/cells/1k_essentia_cells");
    private static final ResourceLocation MODEL_CELL_ESSENTIA_64K = id("block/drive/cells/1k_essentia_cells");

    private static boolean initialized;

    public static void init() {
        if(initialized) {
            return;
        }
        initialized = true;

        registerEssentiaCellDisassembly();

        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.CREATIVE_ESSENTIA_CELL.item()), MODEL_CELL_CREATIVE_ESSENTIA);
        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_1K.item()), MODEL_CELL_ESSENTIA_1K);
        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_4K.item()), MODEL_CELL_ESSENTIA_4K);
        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_16K.item()), MODEL_CELL_ESSENTIA_16K);
        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_64K.item()), MODEL_CELL_ESSENTIA_64K);
    }

    static void registerEssentiaCellDisassembly() {
        registerStorageCellDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_1K.item(), "1k essentia storage cell"),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_1K.item(), "1k essentia storage component"));
        registerStorageCellDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_4K.item(), "4k essentia storage cell"),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_4K.item(), "4k essentia storage component"));
        registerStorageCellDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_16K.item(), "16k essentia storage cell"),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_16K.item(), "16k essentia storage component"));
        registerStorageCellDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_64K.item(), "64k essentia storage cell"),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_64K.item(), "64k essentia storage component"));
    }

    private static void registerStorageCellDisassembly(Item cell, Item component) {
        Item housing = Objects.requireNonNull(AEItems.ITEM_CELL_HOUSING.item(), "item cell housing");
        StorageCellDisassemblyRecipe.register(new StorageCellDisassemblyRecipe(
                cell,
                List.of(new ItemStack(housing), new ItemStack(component))));
    }

    @SuppressWarnings("SameParameterValue")
    private static ResourceLocation id(String id)  {
        return ThaumicEnergistics.id(id);
    }

}
