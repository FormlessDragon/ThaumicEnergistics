package thaumicenergistics.integration.jei;

import ae2.api.integrations.hei.IngredientConverter;
import ae2.api.stacks.GenericStack;
import com.google.common.primitives.Ints;
import mezz.jei.api.recipe.IIngredientType;
import org.jspecify.annotations.Nullable;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.api.stacks.EssentiaStack;
import thaumicenergistics.common.me.key.AEEssentiaKey;

import static com.buuz135.thaumicjei.ThaumcraftJEIPlugin.ASPECT_LIST;

public class TCJeiConverter implements IngredientConverter<AspectList> {

    @Override
    public IIngredientType<AspectList> getIngredientType() {
        return ASPECT_LIST;
    }

    @Override
    @Nullable
    public AspectList getIngredientFromStack(GenericStack stack) {
        if(stack == null || stack.amount() <= 0) {
            return null;
        }

        if (stack.what() instanceof AEEssentiaKey essentiaKey) {
            AspectList aspectList = new AspectList();
            EssentiaStack essentiaStack = essentiaKey.toStack(Math.max(1, Ints.saturatedCast(stack.amount())));
            return aspectList.add(essentiaStack.getAspect(), 1);
        }
        return null;
    }

    @Override
    @Nullable
    public GenericStack getStackFromIngredient(AspectList ingredient) {
        if(ingredient == null || ingredient.getAspects()[0] == null) {
            return null;
        }

        EssentiaStack essentiaStack = new EssentiaStack(ingredient.getAspects()[0], 1);
        AEEssentiaKey key = AEEssentiaKey.of(essentiaStack);
        if (key == null) {
            return null;
        }

        return new GenericStack(key, essentiaStack.getAmount());
    }
}
