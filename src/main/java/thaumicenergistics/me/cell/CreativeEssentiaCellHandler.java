package thaumicenergistics.me.cell;

import ae2.api.storage.cells.ISaveProvider;
import ae2.api.storage.cells.StorageCell;
import ae2.me.cells.CreativeCellHandler;
import net.minecraft.item.ItemStack;
import thaumicenergistics.items.CreativeEssentiaCell;

/**
 * @author BrockWS
 */
public class CreativeEssentiaCellHandler extends CreativeCellHandler {

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CreativeEssentiaCell;
    }

    @Override
    public StorageCell getCellInventory(ItemStack stack, ISaveProvider saveProvider) {
        return this.isCell(stack) ? new CreativeEssentiaCellInventory(stack) : null;
    }

}
