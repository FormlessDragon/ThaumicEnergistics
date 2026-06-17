package thaumicenergistics.integration.jei;

import ae2.api.integrations.hei.IngredientConverters;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.core.definitions.ThEParts;

import java.util.Optional;


/**
 * @author BrockWS
 * @author Alex811
 */
@JEIPlugin
public class ThEHeiPlugin implements IModPlugin {

    @Override
    public void register(IModRegistry registry) {
        IRecipeTransferHandlerHelper rthh = registry.getJeiHelpers().recipeTransferHandlerHelper();

        Optional.of(ThEParts.ARCANE_TERMINAL.stack(1)).ifPresent(stack -> registerWorkbenchCatalyst(registry, new ACTRecipeTransferHandler<>(rthh), stack));
        Optional.of(ThEParts.ARCANE_INSCRIBER.stack(1)).ifPresent(stack -> registerWorkbenchCatalyst(registry, new ACIRecipeTransferHandler<>(rthh), stack));

        Optional.of(ThEParts.ARCANE_INSCRIBER.stack(1))
                .ifPresent(_ -> registry.addGhostIngredientHandler(
                        GuiArcaneInscriber.class,
                        new GhostInscriberHandler()));

        if(Loader.isModLoaded("thaumicjei")) {
            IngredientConverters.register(new TCJeiConverter());
        }
    }

    public void registerWorkbenchCatalyst(IModRegistry registry, IRecipeTransferHandler<? extends Container> handler, ItemStack stack) {
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, VanillaRecipeCategoryUid.CRAFTING);
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, "THAUMCRAFT_ARCANE_WORKBENCH");
        registry.addRecipeCatalyst(stack, "THAUMCRAFT_ARCANE_WORKBENCH");
    }

}
