package thaumicenergistics.api.definitions;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface IThEItemDefinition {

    @Nonnull
    String identifier();

    Optional<Item> maybeItem();

    Optional<ItemStack> maybeStack(int stackSize);

    Optional<ItemStack> maybeStack(int stackSize, int damage);

    boolean isEnabled();

    boolean isSameAs(ItemStack comparableStack);
}
