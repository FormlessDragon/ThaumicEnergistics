package thaumicenergistics.part;

import ae2.api.inventories.InternalInventory;
import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.filter.IAEItemFilter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.api.storage.IArcaneInscriberHost;
import thaumicenergistics.core.ModGUIs;
import thaumicenergistics.core.ModGlobals;
import thaumicenergistics.items.ItemKnowledgeCore;
import thaumicenergistics.part.inventory.ArcaneInscriberMatrixInventory;
import thaumicenergistics.thaumicenergistics.Reference;

import javax.annotation.Nonnull;

/**
 * Arcane Inscriber part with a ghost recipe matrix and a dedicated knowledge-core inventory.
 *
 * @author Alex811
 */
public class PartArcaneInscriber extends AbstractArcaneTerminalPart implements IArcaneInscriberHost {

    private static final String TAG_KNOWLEDGE_CORE = "knowledgeCore";

    @PartModels
    public static final ResourceLocation MODEL_BASE =
            new ResourceLocation(Reference.MOD_ID, "part/arcane_inscriber/base");
    @PartModels
    public static final ResourceLocation MODEL_ON =
            new ResourceLocation(Reference.MOD_ID, "part/arcane_inscriber/on");
    @PartModels
    public static final ResourceLocation MODEL_OFF =
            new ResourceLocation(Reference.MOD_ID, "part/arcane_inscriber/off");

    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_on"));
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_off"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_has_channel"));

    private final AppEngInternalInventory knowledgeCoreInventory;

    public PartArcaneInscriber(IPartItem<?> item) {
        super(item, ModGUIs.ARCANE_INSCRIBER, ArcaneInscriberMatrixInventory::new);
        this.knowledgeCoreInventory =
                new AppEngInternalInventory(this, 1, 1, new KnowledgeCoreFilter());
    }

    @Override
    public final InternalInventory getKnowledgeCoreInventory() {
        return this.knowledgeCoreInventory;
    }

    @Override
    protected final InternalInventory getArcaneAuxiliaryInventory() {
        return this.knowledgeCoreInventory;
    }

    @Override
    protected final void readArcaneAuxiliaryInventory(NBTTagCompound tag) {
        this.knowledgeCoreInventory.readFromNBT(tag, TAG_KNOWLEDGE_CORE);
    }

    @Override
    protected final void writeArcaneAuxiliaryInventory(NBTTagCompound tag) {
        this.knowledgeCoreInventory.writeToNBT(tag, TAG_KNOWLEDGE_CORE);
    }

    @Override
    protected final boolean shouldDropCraftingInventory() {
        return false;
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    private static final class KnowledgeCoreFilter implements IAEItemFilter {

        @Override
        public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() instanceof ItemKnowledgeCore;
        }
    }
}
