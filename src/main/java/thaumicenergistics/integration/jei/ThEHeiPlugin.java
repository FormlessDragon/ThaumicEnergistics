package thaumicenergistics.integration.jei;

import ae2.api.integrations.hei.IngredientConverters;
import com.buuz135.thaumicjei.config.ThaumicConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IIngredientType;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.block.Block;
import net.minecraft.inventory.Container;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import org.jetbrains.annotations.NotNull;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.api.stacks.EssentiaStack;
import thaumicenergistics.client.gui.part.GuiArcaneInscriber;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.core.definitions.ThEParts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;


/**
 * @author BrockWS
 * @author Alex811
 */
@JEIPlugin
public class ThEHeiPlugin implements IModPlugin {

    public static EssentiaSmeltCategory essentiaSmeltCategory;

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registry) {
        essentiaSmeltCategory = new EssentiaSmeltCategory();
        registry.addRecipeCategories(essentiaSmeltCategory);
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

        if(Loader.isModLoaded("thaumicjei")) {
            IngredientConverters.register(new TCJeiConverter());

            long time = System.currentTimeMillis();
            List<EssentiaSmeltCategory.EssentiaSmeltWrapper> wrappers = new ArrayList<>();
            for(ItemStack item : new ArrayList<>(registry.getIngredientRegistry().getAllIngredients(VanillaTypes.ITEM))) {
                AspectList list = new AspectList(item);
                List<AspectList> aspects = new ArrayList<>();
                for(Aspect aspect : list.getAspects()) {
                    AspectList singleAspect = new AspectList();
                    singleAspect.add(aspect, list.getAmount(aspect));
                    aspects.add(singleAspect);
                }
                wrappers.add(new EssentiaSmeltCategory.EssentiaSmeltWrapper(item, aspects));
            }
            registry.addRecipes(wrappers, essentiaSmeltCategory.getUid());
            registry.addRecipeCatalyst(new ItemStack(Block.getBlockFromName(new ResourceLocation("thaumcraft", "smelter_basic").toString())), essentiaSmeltCategory.getUid());
            registry.addRecipeCatalyst(new ItemStack(Block.getBlockFromName(new ResourceLocation("thaumcraft", "smelter_thaumium").toString())), essentiaSmeltCategory.getUid());
            registry.addRecipeCatalyst(new ItemStack(Block.getBlockFromName(new ResourceLocation("thaumcraft", "smelter_void").toString())), essentiaSmeltCategory.getUid());
            ThELog.info("essentiaSmelt category loaded in {} ms", System.currentTimeMillis() - time);
        }
    }

    public void registerWorkbenchCatalyst(IModRegistry registry, IRecipeTransferHandler<? extends Container> handler, ItemStack stack) {
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, VanillaRecipeCategoryUid.CRAFTING);
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, "THAUMCRAFT_ARCANE_WORKBENCH");
        registry.addRecipeCatalyst(stack, "THAUMCRAFT_ARCANE_WORKBENCH");
    }

}
