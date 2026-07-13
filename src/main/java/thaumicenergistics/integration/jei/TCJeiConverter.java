package thaumicenergistics.integration.jei;

import ae2.api.integrations.hei.IngredientConverter;
import ae2.api.stacks.GenericStack;
import com.google.common.primitives.Ints;
import mezz.jei.api.recipe.IIngredientType;
import org.jspecify.annotations.Nullable;
import thaumcraft.api.aspects.AspectList;
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
        if(stack == null) {
            return null;
        }

        if (!(stack.what() instanceof AEEssentiaKey essentiaKey)) {
            return null;
        }

        return essentiaKey.toTCJEIStack(Math.max(1, Ints.saturatedCast(stack.amount())));
    }

    @Override
    @Nullable
    public GenericStack getStackFromIngredient(AspectList ingredient) {
        if (ingredient == null || ingredient.getAspects().length != 1) {
            return null;
        }

        var aspect = ingredient.getAspects()[0];
        if (aspect == null) {
            return null;
        }

        return new GenericStack(AEEssentiaKey.of(aspect), ingredient.getAmount(aspect));
    }

}
