package thaumicenergistics.part;

import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.util.ItemHandlerUtil;
import thaumicenergistics.util.inventory.ThEKnowledgeCoreInventory;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author Alex811
 */
public class PartArcaneInscriber extends PartArcaneTerminal {

    @PartModels
    public static final ResourceLocation MODEL_BASE = new ResourceLocation(Reference.MOD_ID, "part/arcane_inscriber/base");
    @PartModels
    public static final ResourceLocation MODEL_ON = new ResourceLocation(Reference.MOD_ID, "part/arcane_inscriber/on");
    @PartModels
    public static final ResourceLocation MODEL_OFF = new ResourceLocation(Reference.MOD_ID, "part/arcane_inscriber/off");

    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_on"));
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_off"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_has_channel"));

    public PartArcaneInscriber(IPartItem<?> item) {
        super(item, ModGUIs.ARCANE_INSCRIBER);
        this.upgradeInventory = new ThEKnowledgeCoreInventory("upgrades", 1, 1, this.getPartItem().asItemStack()) {
            @Override
            public void markDirty() {
                super.markDirty();
                PartArcaneInscriber.this.saveChanges();
            }
        };
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        if (this.isPowered())
            if (this.isActive())
                return MODELS_HAS_CHANNEL;
            else
                return MODELS_ON;
        return MODELS_OFF;
    }

    @Override
    protected void addArcaneDrops(List<ItemStack> list) {
        list.addAll(ItemHandlerUtil.getInventoryAsList(this.getInventoryByName("upgrades")));
    }
}
