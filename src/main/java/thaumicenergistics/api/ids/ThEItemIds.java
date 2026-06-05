package thaumicenergistics.api.ids;

import net.minecraft.util.ResourceLocation;
import thaumicenergistics.thaumicenergistics.Reference;

public final class ThEItemIds {

    ///
    /// STORAGE CELLS
    ///
    public static final ResourceLocation ESSENTIA_STORAGE_CELL_1K = id("essentia_storage_cell_1k");
    public static final ResourceLocation ESSENTIA_STORAGE_CELL_4K = id("essentia_storage_cell_4k");
    public static final ResourceLocation ESSENTIA_STORAGE_CELL_16K = id("essentia_storage_cell_16k");
    public static final ResourceLocation ESSENTIA_STORAGE_CELL_64K = id("essentia_storage_cell_64k");

    ///
    /// The following items were previously part of ApiItems
    ///
    public static final ResourceLocation ESSENTIA_COMPONENT_1K = id("essentia_component_1k");
    public static final ResourceLocation ESSENTIA_COMPONENT_4K = id("essentia_component_4k");
    public static final ResourceLocation ESSENTIA_COMPONENT_16K = id("essentia_component_16k");
    public static final ResourceLocation ESSENTIA_COMPONENT_64K = id("essentia_component_64k");


    private static ResourceLocation id(String id) {
        return new ResourceLocation(Reference.MOD_ID, id);
    }

}
