package thaumicenergistics.items;

import ae2.api.upgrades.Upgrades;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import thaumicenergistics.thaumicenergistics.Reference;

import javax.annotation.Nullable;
import java.util.List;

/**
 * @author BrockWS
 */
public class ItemMaterial extends Item {

    public ItemMaterial(String id) {
        this.setTranslationKey(Reference.MOD_ID + "." + id);
    }

    public ItemMaterial(String id, int stackSize) {
        this(id);
        setMaxStackSize(stackSize);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        List<ITextComponent> supported = Upgrades.getTooltipLinesForCard(stack.getItem());
        if (!supported.isEmpty()) {
            tooltip.add("Used in: " + String.join(", ", supported.stream().map(ITextComponent::getFormattedText).toList()));
        }

        super.addInformation(stack, worldIn, tooltip, flagIn);
    }

}
