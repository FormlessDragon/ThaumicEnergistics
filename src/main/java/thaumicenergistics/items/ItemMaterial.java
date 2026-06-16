package thaumicenergistics.items;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.api.IThEUpgrade;
import thaumicenergistics.api.ThEApi;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        Optional<IThEUpgrade> optional = ThEApi.instance().upgrades().getUpgrade(stack);
        optional.ifPresent(upgrade -> {
            String supported = upgrade.getSupported().keySet().stream().map(ItemStack::getDisplayName).collect(Collectors.joining(", "));
            if (!supported.isEmpty()) tooltip.add("Used in: " + supported);
        });

        super.addInformation(stack, worldIn, tooltip, flagIn);
    }

}
