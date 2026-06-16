package thaumicenergistics.integration.jei;

import com.google.common.base.Strings;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.core.definitions.ThEParts;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;


/**
 * @author BrockWS
 * @author Alex811
 */
@JEIPlugin
public class ThEJEI implements IModPlugin {
    private static IJeiRuntime runtime;

    @Override
    @ParametersAreNonnullByDefault
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    public static String getSearchText() {
        return Strings.nullToEmpty(runtime.getIngredientFilter().getFilterText());
    }

    public static void setSearchText(String searchText) {
        runtime.getIngredientFilter().setFilterText(Strings.nullToEmpty(searchText));
    }

    @Override
    public void register(IModRegistry registry) {
        IRecipeTransferHandlerHelper rthh = registry.getJeiHelpers().recipeTransferHandlerHelper();

        Optional.of(ThEParts.ARCANE_TERMINAL.stack(1)).ifPresent(stack -> registerWorkbenchCatalyst(registry, new ACTRecipeTransferHandler<>(rthh), stack));
        Optional.of(ThEParts.ARCANE_INSCRIBER.stack(1)).ifPresent(stack -> registerWorkbenchCatalyst(registry, new ACIRecipeTransferHandler<>(rthh), stack));

        Optional.of(ThEParts.ARCANE_INSCRIBER.stack(1))
                .ifPresent(_ -> registry.addGhostIngredientHandler(
                        GuiArcaneInscriber.class,
                        new GhostInscriberHandler()));
    }

    public void registerWorkbenchCatalyst(IModRegistry registry, IRecipeTransferHandler<? extends Container> handler, ItemStack stack) {
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, VanillaRecipeCategoryUid.CRAFTING);
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, "THAUMCRAFT_ARCANE_WORKBENCH");
        registry.addRecipeCatalyst(stack, "THAUMCRAFT_ARCANE_WORKBENCH");
    }

}
