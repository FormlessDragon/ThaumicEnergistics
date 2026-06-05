package thaumicenergistics.init.internal;

import ae2.api.client.StorageCellModels;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.core.definitions.ThEItems;

import java.util.Objects;

public final class InitStorageCells {

    // TODO: 绘制源质元件的驱动器模型
    private static final ResourceLocation MODEL_CELL_ESSENTIA_1K = ThaumicEnergistics.id("block/drive/cells/1k_essentia_cells");
    private static final ResourceLocation MODEL_CELL_ESSENTIA_4K = ThaumicEnergistics.id("block/drive/cells/1k_essentia_cells");
    private static final ResourceLocation MODEL_CELL_ESSENTIA_16K = ThaumicEnergistics.id("block/drive/cells/1k_essentia_cells");
    private static final ResourceLocation MODEL_CELL_ESSENTIA_64K = ThaumicEnergistics.id("block/drive/cells/1k_essentia_cells");

    private static boolean initialized;

    public static void init() {
        if(initialized) {
            return;
        }
        initialized = true;

        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_1K.item()), MODEL_CELL_ESSENTIA_1K);
        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_4K.item()), MODEL_CELL_ESSENTIA_4K);
        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_16K.item()), MODEL_CELL_ESSENTIA_16K);
        StorageCellModels.registerModel(Objects.requireNonNull(ThEItems.ESSENTIA_CELL_64K.item()), MODEL_CELL_ESSENTIA_64K);
    }

}
