package thaumicenergistics.init.internal;

import ae2.api.client.StorageCellModels;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.thaumicenergistics.Reference;

public final class InitStorageCells {

    // TODO: 绘制源质元件的驱动器模型
    private static final ResourceLocation MODEL_CELL_ESSENTIA_1K = new ResourceLocation(Reference.MOD_ID, "block/drive/cells/1k_essentia_cells");
    private static boolean initialized;

    public static void init() {
        if(initialized) {
            return;
        }
        initialized = true;

        StorageCellModels.registerModel(ThEItems.ESSENTIA_CELL_1K.item(), MODEL_CELL_ESSENTIA_1K);
    }

}
