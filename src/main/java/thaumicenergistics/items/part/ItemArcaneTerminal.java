package thaumicenergistics.items.part;

import ae2.api.parts.IPart;
import ae2.api.parts.PartModels;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.items.ItemPartBase;
import thaumicenergistics.part.PartArcaneTerminal;

import javax.annotation.Nullable;

/**
 * @author BrockWS
 */
public class ItemArcaneTerminal extends ItemPartBase {

    public ItemArcaneTerminal(String id) {
        super(id);
    }

    @Nullable
    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return new PartArcaneTerminal(this);
    }

    @Override
    public void initModel() {
        PartModels.registerModels(PartArcaneTerminal.MODELS);
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(Reference.MOD_ID + ":part/arcane_terminal"));
    }
}
