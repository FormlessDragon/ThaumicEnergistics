package thaumicenergistics.api.ids;

import net.minecraft.util.ResourceLocation;
import thaumicenergistics.thaumicenergistics.Reference;

public final class ThEItemIds {

    ///
    /// STORAGE CELLS
    ///
    public static final ResourceLocation ESSENTIA_STORAGE_CELL_1K = id("essentia_storage_cell_1k");

    ///
    /// The following items were previously part of ApiItems
    ///
    public static final ResourceLocation ESSENTIA_COMPONENT_1K = id("essentia_component_1k");

    private static ResourceLocation id(String id) {
        return new ResourceLocation(Reference.MOD_ID, id);
    }

}
