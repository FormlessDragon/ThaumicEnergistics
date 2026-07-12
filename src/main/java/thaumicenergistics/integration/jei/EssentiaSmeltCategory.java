package thaumicenergistics.integration.jei;

import com.buuz135.thaumicjei.ThaumcraftJEIPlugin;
import com.buuz135.thaumicjei.ThaumicJEI;
import com.buuz135.thaumicjei.drawable.AlphaDrawable;
import com.buuz135.thaumicjei.ingredient.AspectIngredientRender;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.opengl.GL11;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.core.definitions.GuiText;

import java.util.List;

public class EssentiaSmeltCategory implements IRecipeCategory<EssentiaSmeltCategory.EssentiaSmeltWrapper> {

    public static final String UUID = "EssentiaSmelt";

    @Override
    @NotNull
    public String getUid() {
        return UUID;
    }

    @Override
    @NotNull
    public String getTitle() {
        return GuiText.essentia_smelt.getLocal();
    }

    @Override
    @NotNull
    public String getModName() {
        return ThaumicJEI.MOD_NAME;
    }

    @Override
    @NotNull
    public IDrawable getBackground() {
        return new AlphaDrawable(new ResourceLocation("thaumcraft", "textures/gui/gui_researchbook_overlay.png"), 40, 6, 32, 32, 0, 18 * 4 + 5, 72, 72);
    }

    @Override
    public void drawExtras(Minecraft minecraft) {
        minecraft.renderEngine.bindTexture(new ResourceLocation(ThaumicJEI.MOD_ID, "textures/gui/gui.png"));
        GL11.glEnable(3042);
        Gui.drawModalRectWithCustomSizedTexture(-66 + 81 - 9, 31, 0, 0, 163, 74, 256, 256);
        GL11.glDisable(3042);
    }

    @Override
    public void setRecipe(@NotNull IRecipeLayout iRecipeLayout, @NotNull EssentiaSmeltWrapper essentiaSmeltWrapper, @NotNull IIngredients iIngredients) {
        iRecipeLayout.getItemStacks().init(0, true, 79, 8);
        iRecipeLayout.getItemStacks().set(0, iIngredients.getInputs(VanillaTypes.ITEM).getFirst());
        int slot = 0;
        int row = 9;
        for(List<AspectList> aspect : iIngredients.getOutputs(ThaumcraftJEIPlugin.ASPECT_LIST)) {
            iRecipeLayout.getIngredientsGroup(ThaumcraftJEIPlugin.ASPECT_LIST).init(slot + 1, false, new AspectIngredientRender(), (slot % row) * 18 - 18 * 3 - 21 + 81, (slot / row) * 18 + 32, 16, 16, 0, 0);
            iRecipeLayout.getIngredientsGroup(ThaumcraftJEIPlugin.ASPECT_LIST).set(slot + 1, aspect);
            ++slot;
        }
    }

    public static class EssentiaSmeltWrapper implements IRecipeWrapper {

        private final ItemStack stack;
        private final List<AspectList> aspects;

        public EssentiaSmeltWrapper(ItemStack stack, List<AspectList> aspects) {
            this.stack = stack;
            this.aspects = aspects;
        }


        @Override
        public void getIngredients(IIngredients ingredients) {
            ingredients.setInput(VanillaTypes.ITEM, stack);
            ingredients.setOutputs(ThaumcraftJEIPlugin.ASPECT_LIST, aspects);
        }
    }

}
