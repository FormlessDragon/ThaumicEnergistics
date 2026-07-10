package thaumicenergistics.common.me.cell;

import ae2.api.stacks.GenericStack;
import ae2.api.storage.cells.ISaveProvider;
import ae2.api.storage.cells.StorageCell;
import ae2.core.AEConfig;
import ae2.items.contents.CellConfig;
import ae2.items.storage.StorageCellTooltipComponent;
import ae2.me.cells.CreativeCellHandler;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import thaumicenergistics.items.CreativeEssentiaCell;
import thaumicenergistics.common.me.key.AEEssentiaKeys;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author BrockWS
 */
public class CreativeEssentiaCellHandler extends CreativeCellHandler {

    public static final CreativeEssentiaCellHandler INSTANCE = new CreativeEssentiaCellHandler();

    @Override
    public boolean isCell(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof CreativeEssentiaCell;
    }

    @Override
    public StorageCell getCellInventory(ItemStack stack, ISaveProvider saveProvider) {
        return this.isCell(stack) ? new CreativeEssentiaCellInventory(stack) : null;
    }

    @Override
    public Optional<StorageCellTooltipComponent> getTooltipData(ItemStack stack) {
        if (!isCell(stack)) {
            return Optional.empty();
        }

        List<GenericStack> content;
        boolean hasMoreContent;
        if (AEConfig.instance().isTooltipShowCellContent()) {
            content = new ObjectArrayList<>();
            int maxCountShown = AEConfig.instance().getTooltipMaxCellContentShown();
            for (var key : CellConfig.create(Collections.singleton(AEEssentiaKeys.INSTANCE), stack).keySet()) {
                content.add(new GenericStack(key, 1));
            }
            hasMoreContent = content.size() > maxCountShown;
            if (hasMoreContent) {
                content = new ObjectArrayList<>(content.subList(0, maxCountShown));
            }
        } else {
            content = Collections.emptyList();
            hasMoreContent = false;
        }

        return Optional.of(new StorageCellTooltipComponent(Collections.emptyList(), content, hasMoreContent, false));
    }

}
