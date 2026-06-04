package thaumicenergistics.integration.appeng.cell;

import ae2.api.storage.cells.ICellHandler;
import ae2.api.storage.cells.ISaveProvider;
import ae2.api.storage.cells.StorageCell;
import net.minecraft.item.ItemStack;
import thaumicenergistics.items.ItemCreativeEssentiaCell;

/**
 * @author BrockWS
 */
public class CreativeEssentiaCellHandler implements ICellHandler {

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemCreativeEssentiaCell;
    }

    @Override
    public StorageCell getCellInventory(ItemStack stack, ISaveProvider saveProvider) {
        return this.isCell(stack) ? new CreativeEssentiaCellInventory(stack) : null;
    }
}
