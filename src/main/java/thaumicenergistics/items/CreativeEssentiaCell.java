package thaumicenergistics.items;

import ae2.api.config.FuzzyMode;
import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.ICellWorkbenchItem;
import ae2.api.storage.cells.IStackTooltipDataProvider;
import ae2.core.localization.GuiText;
import ae2.core.localization.Tooltips;
import ae2.items.AEBaseItem;
import ae2.items.contents.CellConfig;
import ae2.items.storage.StorageCellTooltipComponent;
import ae2.util.ConfigInventory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumicenergistics.me.cell.CreativeEssentiaCellHandler;
import thaumicenergistics.me.cell.CreativeEssentiaCellInventory;
import thaumicenergistics.me.key.AEEssentiaKeys;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author BrockWS
 */
public class CreativeEssentiaCell extends AEBaseItem implements ICellWorkbenchItem, IStackTooltipDataProvider {

    public CreativeEssentiaCell() {
        this.setMaxStackSize(1);
    }

    @Override
    public ConfigInventory getConfigInventory(ItemStack stack) {
        return CellConfig.create(Collections.singleton(AEEssentiaKeys.INSTANCE), stack);
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack) {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode fuzzyMode) {

    }

    @Override
    public Optional<StorageCellTooltipComponent> getStackTooltipData(ItemStack stack) {
        return CreativeEssentiaCellHandler.INSTANCE.getTooltipData(stack);
    }

    @Override
    protected void addCheckedInformation(final ItemStack stack, final World world, final List<String> lines,
                                         final ITooltipFlag advancedTooltips) {
        addToTooltip(stack, lines);
    }

    @Override
    public void addToTooltip(ItemStack stack, List<String> lines) {
        var inventory = StorageCells.getCellInventory(stack, null);
        if (!(inventory instanceof CreativeEssentiaCellInventory)) {
            return;
        }

        var cc = getConfigInventory(stack);
        if (cc.isEmpty()) {
            return;
        }

        if (GuiScreen.isShiftKeyDown()) {
            for (var key : cc.keySet()) {
                lines.add(key.getDisplayName().getFormattedText());
            }
        } else {
            lines.add(Tooltips.of(GuiText.PressShiftForFullList).getFormattedText());
        }
    }

}
