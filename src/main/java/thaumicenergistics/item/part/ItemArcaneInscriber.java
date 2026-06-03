package thaumicenergistics.item.part;

import ae2.api.parts.IPart;
import ae2.api.parts.PartModels;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.ItemStack;
import net.minecraftforge.client.model.ModelLoader;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.part.PartArcaneInscriber;

import javax.annotation.Nullable;

/**
 * @author Alex811
 */
public class ItemArcaneInscriber extends ItemArcaneTerminal {
    public ItemArcaneInscriber(String id) {
        super(id);
    }

    @Nullable
    @Override
    public IPart createPartFromItemStack(ItemStack stack) {
        return new PartArcaneInscriber(this);
    }

    @Override
    public void initModel() {
        PartModels.registerModels(PartArcaneInscriber.MODELS);
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(Reference.MOD_ID + ":part/arcane_inscriber"));
    }
}
