package thaumicenergistics.part;

import ae2.api.inventories.InternalInventory;
import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2.util.inv.AppEngInternalInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.api.storage.IArcaneTerminalUpgradeHost;
import thaumicenergistics.client.gui.ModGUIs;
import thaumicenergistics.core.ModGlobals;
import thaumicenergistics.thaumicenergistics.Reference;

import javax.annotation.Nonnull;

/**
 * Arcane Terminal part with a real crafting matrix and a dedicated AE2 upgrade inventory.
 *
 * @author BrockWS
 * @author Alex811
 */
public class PartArcaneTerminal extends AbstractArcaneTerminalPart implements IArcaneTerminalUpgradeHost {

    private static final String TAG_UPGRADES = "upgrades";

    @PartModels
    public static final ResourceLocation MODEL_BASE =
            new ResourceLocation(Reference.MOD_ID, "part/arcane_terminal/base");
    @PartModels
    public static final ResourceLocation MODEL_ON =
            new ResourceLocation(Reference.MOD_ID, "part/arcane_terminal/on");
    @PartModels
    public static final ResourceLocation MODEL_OFF =
            new ResourceLocation(Reference.MOD_ID, "part/arcane_terminal/off");

    private static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_on"));
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_off"));
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON,
            new ResourceLocation(ModGlobals.MOD_ID_AE2, "part/display_status_has_channel"));

    private final IUpgradeInventory upgradeInventory;

    public PartArcaneTerminal(IPartItem<?> item) {
        super(item, ModGUIs.ARCANE_TERMINAL, host -> new AppEngInternalInventory(host, 15, 64));
        this.upgradeInventory =
                UpgradeInventories.forMachine(this.getPartItem().asItem(), 1, this::saveChanges);
    }

    @Override
    public final IUpgradeInventory getArcaneUpgradeInventory() {
        return this.upgradeInventory;
    }

    @Override
    protected final InternalInventory getArcaneAuxiliaryInventory() {
        return this.upgradeInventory;
    }

    @Override
    protected final void readArcaneAuxiliaryInventory(NBTTagCompound tag) {
        this.upgradeInventory.readFromNBT(tag, TAG_UPGRADES);
    }

    @Override
    protected final void writeArcaneAuxiliaryInventory(NBTTagCompound tag) {
        this.upgradeInventory.writeToNBT(tag, TAG_UPGRADES);
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }
}
