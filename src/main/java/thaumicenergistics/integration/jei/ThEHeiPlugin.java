package thaumicenergistics.integration.jei;

import ae2.api.integrations.hei.IngredientConverters;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.block.Block;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.Loader;
import org.jetbrains.annotations.NotNull;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.core.definitions.ThEParts;

import java.util.ArrayList;
import java.util.List;


/**
 * @author BrockWS
 * @author Alex811
 */
@JEIPlugin
public class ThEHeiPlugin implements IModPlugin {

    public static EssentiaSmeltCategory essentiaSmeltCategory;
    private static final String[] SMELTER_BLOCKS = {"smelter_basic", "smelter_thaumium", "smelter_void"};

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registry) {
        if (!Loader.isModLoaded("thaumicjei")) {
            return;
        }
        essentiaSmeltCategory = new EssentiaSmeltCategory();
        registry.addRecipeCategories(essentiaSmeltCategory);
    }

    @Override
    public void register(IModRegistry registry) {
        IRecipeTransferHandlerHelper rthh = registry.getJeiHelpers().recipeTransferHandlerHelper();

        registerWorkbenchCatalyst(registry, new ACTRecipeTransferHandler<>(ContainerArcaneTerm.class, rthh), ThEParts.ARCANE_TERMINAL.stack());
        registerWorkbenchCatalyst(registry, new ACIRecipeTransferHandler<>(ContainerArcaneInscriber.class, rthh), ThEParts.ARCANE_INSCRIBER.stack());

        if (!Loader.isModLoaded("thaumicjei")) {
            return;
        }

        String categoryUid = EssentiaSmeltCategory.UUID;
        for (String smelter : SMELTER_BLOCKS) {
            ItemStack catalyst = smelterStack(smelter);
            if (!catalyst.isEmpty()) {
                registry.addRecipeCatalyst(catalyst, categoryUid);
            }
        }

        IngredientConverters.register(new TCJeiConverter());

        long time = System.currentTimeMillis();
        List<EssentiaSmeltCategory.EssentiaSmeltWrapper> wrappers = new ArrayList<>();
        for (ItemStack item : new ArrayList<>(registry.getIngredientRegistry().getAllIngredients(VanillaTypes.ITEM))) {
            AspectList list = new AspectList(item);
            if (list.size() == 0) {
                continue;
            }
            List<AspectList> aspects = new ArrayList<>();
            for (Aspect aspect : list.getAspects()) {
                AspectList singleAspect = new AspectList();
                singleAspect.add(aspect, list.getAmount(aspect));
                aspects.add(singleAspect);
            }
            wrappers.add(new EssentiaSmeltCategory.EssentiaSmeltWrapper(item, aspects));
        }
        registry.addRecipes(wrappers, categoryUid);
        ThELog.info("Registered essentiaSmelt recipes in {} ms", System.currentTimeMillis() - time);
    }

    private static ItemStack smelterStack(String name) {
        Block block = Block.getBlockFromName("thaumcraft:" + name);
        return block == null ? ItemStack.EMPTY : new ItemStack(block);
    }

    public void registerWorkbenchCatalyst(IModRegistry registry, IRecipeTransferHandler<? extends Container> handler, ItemStack stack) {
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, VanillaRecipeCategoryUid.CRAFTING);
        registry.getRecipeTransferRegistry().addRecipeTransferHandler(handler, "THAUMCRAFT_ARCANE_WORKBENCH");
        registry.addRecipeCatalyst(stack, "THAUMCRAFT_ARCANE_WORKBENCH");
    }

}
