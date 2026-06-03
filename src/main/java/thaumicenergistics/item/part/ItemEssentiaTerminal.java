package thaumicenergistics.item.part;

import ae2.api.parts.IPart;
import ae2.api.parts.PartModels;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.item.ItemPartBase;
import thaumicenergistics.part.PartEssentiaTerminal;

import javax.annotation.Nullable;

/**
 * @author BrockWS
 */
public class ItemEssentiaTerminal extends ItemPartBase {

    public ItemEssentiaTerminal(String id) {
        super(id);
    }

    @Nullable
    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return new PartEssentiaTerminal(this);
    }

    @Override
    public void initModel() {
        PartModels.registerModels(PartEssentiaTerminal.MODELS);
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(Reference.MOD_ID + ":part/essentia_terminal"));
    }
}
