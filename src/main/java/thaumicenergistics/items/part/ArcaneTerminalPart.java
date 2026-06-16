package thaumicenergistics.items.part;

import ae2.api.inventories.InternalInventory;
import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.container.GuiIds;
import ae2.items.parts.PartModels;
import ae2.parts.PartModel;
import ae2.parts.reporting.AbstractTerminalPart;
import ae2.util.inv.AppEngInternalInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.ThaumicEnergistics;

import java.util.List;

/**
 * @author BrockWS
 */
public class ArcaneTerminalPart extends AbstractTerminalPart {

    public static final ResourceLocation INV_CRAFTING = id("arcane_terminal_crafting");

    @PartModels
    public static final ResourceLocation MODEL_OFF = id("part/arcane_terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = id("part/arcane_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    private final AppEngInternalInventory craftingGrid = new AppEngInternalInventory(this, 15);

    public ArcaneTerminalPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        super.addAdditionalDrops(drops, wrenched);
        for(ItemStack is : this.craftingGrid) {
            if(!is.isEmpty()) {
                drops.add(is);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.craftingGrid.clear();
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.craftingGrid.readFromNBT(data, "craftingGrid");
    }

    @Override
    public void writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        this.craftingGrid.writeToNBT(data, "craftingGrid");
    }

    @Override
    public GuiIds.GuiKey getGuiKey(EntityPlayer player) {
        return GuiIds.GuiKey.CRAFTING_TERMINAL;
    }

    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if(id.equals(INV_CRAFTING)) {
            return this.craftingGrid;
        }else {
            return super.getSubInventory(id);
        }
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    private static ResourceLocation id(String id) {
        return ThaumicEnergistics.id(id);
    }

}
