package thaumicenergistics.api;

import net.minecraft.item.ItemStack;
import thaumicenergistics.api.definitions.IThEItemDefinition;

import java.util.Map;

/**
 * @author BrockWS
 */
public interface IThEUpgrade {

    void registerItem(IThEItemDefinition item, int max);

    void registerItem(ItemStack item, int max);

    IThEItemDefinition getDefinition();

    Map<ItemStack, Integer> getSupported();

    int getSupported(ItemStack stack);

    boolean isSupported(ItemStack stack);
}
