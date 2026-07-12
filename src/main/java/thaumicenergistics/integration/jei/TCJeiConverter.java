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
            int amount = Ints.saturatedCast(stack.amount());
            if (amount <= 0) {
                return null;
            }
            EssentiaStack essentiaStack = essentiaKey.toStack(amount);
            return aspectList.add(essentiaStack.getAspect(), amount);
        }
        return null;
    }

    @Override
    @Nullable
    public GenericStack getStackFromIngredient(AspectList ingredient) {
        if (ingredient == null || ingredient.getAspects().length != 1) {
            return null;
        }

        var aspect = ingredient.getAspects()[0];
        if (aspect == null || ingredient.getAmount(aspect) <= 0) {
            return null;
        }

        return new GenericStack(AEEssentiaKey.of(aspect), ingredient.getAmount(aspect));
    }
}
