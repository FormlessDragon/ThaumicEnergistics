package thaumicenergistics.integration.jei;

import mcp.MethodsReturnNonnullByDefault;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import thaumicenergistics.container.part.ContainerArcaneInscriber;

import javax.annotation.Nullable;

/**
 * @author Alex811
 */
public class ACIRecipeTransferHandler<C extends ContainerArcaneInscriber> extends ACTRecipeTransferHandler<C> implements IRecipeTransferHandler<C> {

    public ACIRecipeTransferHandler(Class<C> containerClass, IRecipeTransferHandlerHelper helper) {
        super(containerClass, helper);
    }

    @Override
    @MethodsReturnNonnullByDefault
    public Class<C> getContainerClass() {
        return this.containerClass;
    }

    @Nullable
    @Override
    protected IRecipeTransferError preflightTransfer(C container, ExtractedArcaneRecipe recipe) {
        return null;
    }

}
