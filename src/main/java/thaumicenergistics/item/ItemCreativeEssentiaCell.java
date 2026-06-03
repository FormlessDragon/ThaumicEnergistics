package thaumicenergistics.item;

import ae2.api.config.FuzzyMode;
import ae2.api.storage.cells.ICellWorkbenchItem;
import ae2.items.contents.CellConfig;
import ae2.util.ConfigInventory;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import thaumicenergistics.api.stacks.AEEssentiaKeys;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.client.render.IThEModel;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

/**
 * @author BrockWS
 */
public class ItemCreativeEssentiaCell extends ItemBase implements ICellWorkbenchItem, IThEModel {

    public ItemCreativeEssentiaCell() {
        super("essentia_cell_creative");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        if (this.getConfigInventory(stack).keySet().isEmpty()) {
            tooltip.add("Contains all types of essentia");
        } else {
            tooltip.add("Contains configured essentia types");
        }
    }

    @Override
    public boolean isEditable(ItemStack itemStack) {
        return true;
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack itemStack) {
        return CellConfig.create(Collections.singleton(AEEssentiaKeys.INSTANCE), itemStack);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack itemStack) {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack itemStack, FuzzyMode fuzzyMode) {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void initModel() {
        ModelLoader.setCustomModelResourceLocation(this, 0, new ModelResourceLocation(Reference.MOD_ID + ":cell/essentia_cell_creative", "inventory"));
    }
}
