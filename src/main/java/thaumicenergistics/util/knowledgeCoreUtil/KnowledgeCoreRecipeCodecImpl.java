package thaumicenergistics.util.knowledgeCoreUtil;

import ae2.api.inventories.InternalInventory;
import ae2.util.inv.AppEngInternalInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import thaumicenergistics.core.ThELog;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Strict sparse-NBT implementation used by the Knowledge Core codec assembly point in {@link KnowledgeCoreUtil}.
 */
final class KnowledgeCoreRecipeCodecImpl implements KnowledgeCoreRecipeCodec {

    /**
     * Recipe field holding sparse ingredient item compounds.
     */
    private static final String INGREDIENTS_TAG = "ingredients";

    /**
     * Sparse ingredient field identifying its position in the fifteen-slot recipe inventory.
     */
    private static final String INGREDIENT_SLOT_TAG = "Slot";

    /**
     * Recipe field holding the non-empty crafted result.
     */
    private static final String RESULT_TAG = "result";

    /**
     * Recipe field holding the finite, non-negative Vis requirement.
     */
    private static final String VIS_COST_TAG = "visCost";

    @Override
    public void setRecipe(NBTTagCompound knowledgeCoreTag, int recipeSlot,
                          @Nullable KnowledgeCoreUtil.Recipe recipe) {
        Objects.requireNonNull(knowledgeCoreTag, "knowledgeCoreTag");
        validateNonNegativeRecipeSlot(recipeSlot);

        NBTTagCompound encodedRecipe = recipe == null ? null : this.encodeRecipe(recipe);
        NBTTagCompound recipes = this.getRecipesForWrite(knowledgeCoreTag, encodedRecipe != null);
        if (recipes == null) {
            return;
        }

        String recipeKey = Integer.toString(recipeSlot);
        if (encodedRecipe == null) {
            recipes.removeTag(recipeKey);
            if (recipes.isEmpty()) {
                knowledgeCoreTag.removeTag(RECIPES_TAG);
            }
            return;
        }

        recipes.setTag(recipeKey, encodedRecipe);
    }

    @Override
    public Optional<KnowledgeCoreUtil.Recipe> getRecipe(NBTTagCompound knowledgeCoreTag, int recipeSlot) {
        Objects.requireNonNull(knowledgeCoreTag, "knowledgeCoreTag");
        validateNonNegativeRecipeSlot(recipeSlot);

        Optional<NBTTagCompound> recipes = this.getRecipesForRead(knowledgeCoreTag);
        if (recipes.isEmpty()) {
            return Optional.empty();
        }

        String recipeKey = Integer.toString(recipeSlot);
        if (!recipes.get().hasKey(recipeKey)) {
            return Optional.empty();
        }
        if (!recipes.get().hasKey(recipeKey, Constants.NBT.TAG_COMPOUND)) {
            ThELog.error("Rejected malformed Knowledge Core recipe slot {}: entry is not a compound", recipeSlot);
            return Optional.empty();
        }

        return this.decodeRecipe(recipes.get().getCompoundTag(recipeKey),
            "Knowledge Core recipe slot " + recipeSlot);
    }

    @Override
    public List<StoredRecipe> getRecipes(NBTTagCompound knowledgeCoreTag, int recipeSlotCount) {
        Objects.requireNonNull(knowledgeCoreTag, "knowledgeCoreTag");
        if (recipeSlotCount < 0) {
            throw new IllegalArgumentException("Knowledge Core recipe slot count cannot be negative: "
                + recipeSlotCount);
        }

        Optional<NBTTagCompound> recipes = this.getRecipesForRead(knowledgeCoreTag);
        if (recipes.isEmpty()) {
            return List.of();
        }

        List<StoredRecipe> decodedRecipes = new ArrayList<>();
        for (String recipeKey : recipes.get().getKeySet()) {
            Optional<Integer> parsedSlot = this.parseRecipeSlot(recipeKey);
            if (parsedSlot.isEmpty()) {
                continue;
            }

            int recipeSlot = parsedSlot.get();
            if (recipeSlot >= recipeSlotCount) {
                ThELog.error("Rejected Knowledge Core recipe slot {} outside active capacity {}",
                    recipeSlot, recipeSlotCount);
                continue;
            }
            if (!recipes.get().hasKey(recipeKey, Constants.NBT.TAG_COMPOUND)) {
                ThELog.error("Rejected malformed Knowledge Core recipe slot {}: entry is not a compound",
                    recipeSlot);
                continue;
            }

            this.decodeRecipe(recipes.get().getCompoundTag(recipeKey),
                    "Knowledge Core recipe slot " + recipeSlot)
                .ifPresent(recipe -> decodedRecipes.add(new StoredRecipe(recipeSlot, recipe)));
        }

        decodedRecipes.sort(Comparator.comparingInt(StoredRecipe::slot));
        return List.copyOf(decodedRecipes);
    }

    @Override
    public boolean hasNoRecipes(NBTTagCompound knowledgeCoreTag, int recipeSlotCount) {
        return this.getRecipes(knowledgeCoreTag, recipeSlotCount).isEmpty();
    }

    @Override
    public NBTTagCompound encodeRecipe(KnowledgeCoreUtil.Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("Knowledge Core recipe cannot be null");
        }

        InternalInventory ingredients = recipe.ingredients();
        if (ingredients == null) {
            throw new IllegalArgumentException("Knowledge Core recipe ingredients cannot be null");
        }
        if (ingredients.size() != INGREDIENT_SLOT_COUNT) {
            throw new IllegalArgumentException("Knowledge Core recipe must contain exactly "
                + INGREDIENT_SLOT_COUNT + " ingredient slots, got " + ingredients.size());
        }

        ItemStack result = recipe.result();
        if (result == null || result.isEmpty()) {
            throw new IllegalArgumentException("Knowledge Core recipe result cannot be empty");
        }

        float visCost = recipe.visCost();
        if (!Float.isFinite(visCost) || visCost < 0.0F) {
            throw new IllegalArgumentException("Knowledge Core recipe Vis cost must be finite and non-negative: "
                + visCost);
        }

        NBTTagList serializedIngredients = new NBTTagList();
        for (int slot = 0; slot < ingredients.size(); slot++) {
            ItemStack ingredient = ingredients.getStackInSlot(slot);
            if (ingredient == null) {
                throw new IllegalArgumentException("Knowledge Core recipe ingredient slot " + slot
                    + " returned null instead of ItemStack.EMPTY");
            }
            if (ingredient.isEmpty()) {
                continue;
            }

            NBTTagCompound serializedIngredient = ingredient.serializeNBT();
            serializedIngredient.setByte(INGREDIENT_SLOT_TAG, (byte) slot);
            serializedIngredients.appendTag(serializedIngredient);
        }

        NBTTagCompound encodedRecipe = new NBTTagCompound();
        encodedRecipe.setTag(INGREDIENTS_TAG, serializedIngredients);
        encodedRecipe.setTag(RESULT_TAG, result.serializeNBT());
        encodedRecipe.setFloat(VIS_COST_TAG, visCost);
        return encodedRecipe;
    }

    @Override
    public Optional<KnowledgeCoreUtil.Recipe> decodeRecipe(NBTTagCompound recipeTag, String sourceDescription) {
        Objects.requireNonNull(recipeTag, "recipeTag");
        if (sourceDescription == null || sourceDescription.isBlank()) {
            throw new IllegalArgumentException("Knowledge Core recipe source description cannot be blank");
        }

        try {
            return Optional.of(this.decodeRecipeStrict(recipeTag));
        } catch (RuntimeException exception) {
            ThELog.error("Rejected malformed " + sourceDescription, exception);
            return Optional.empty();
        }
    }

    @Override
    public NBTTagCompound copyNonRecipeData(@Nullable NBTTagCompound sourceTag) {
        if (sourceTag == null) {
            return new NBTTagCompound();
        }

        NBTTagCompound copy = sourceTag.copy();
        copy.removeTag(RECIPES_TAG);
        for (String rootKey : new ArrayList<>(copy.getKeySet())) {
            if (isLegacyRecipeRootKey(rootKey)) {
                copy.removeTag(rootKey);
            }
        }
        return copy;
    }

    /**
     * Decodes one recipe after the public corruption boundary has established logging and exception containment.
     */
    private KnowledgeCoreUtil.Recipe decodeRecipeStrict(NBTTagCompound recipeTag) {
        if (!recipeTag.hasKey(INGREDIENTS_TAG, Constants.NBT.TAG_LIST)) {
            throw new IllegalArgumentException("missing compound ingredient list");
        }

        NBTBase ingredientsBase = recipeTag.getTag(INGREDIENTS_TAG);
        if (!(ingredientsBase instanceof NBTTagList serializedIngredients)) {
            throw new IllegalArgumentException("ingredient data is not a list");
        }
        int ingredientTagType = serializedIngredients.getTagType();
        if (ingredientTagType != Constants.NBT.TAG_END
            && ingredientTagType != Constants.NBT.TAG_COMPOUND) {
            throw new IllegalArgumentException("ingredient list elements are not compounds");
        }

        AppEngInternalInventory ingredients = new AppEngInternalInventory(INGREDIENT_SLOT_COUNT);
        boolean[] occupiedSlots = new boolean[INGREDIENT_SLOT_COUNT];
        for (int index = 0; index < serializedIngredients.tagCount(); index++) {
            NBTBase ingredientBase = serializedIngredients.get(index);
            if (!(ingredientBase instanceof NBTTagCompound serializedIngredient)) {
                throw new IllegalArgumentException("ingredient entry " + index + " is not a compound");
            }
            if (!serializedIngredient.hasKey(INGREDIENT_SLOT_TAG, Constants.NBT.TAG_BYTE)) {
                throw new IllegalArgumentException("ingredient entry " + index + " has no byte Slot");
            }

            int ingredientSlot = Byte.toUnsignedInt(serializedIngredient.getByte(INGREDIENT_SLOT_TAG));
            if (ingredientSlot >= INGREDIENT_SLOT_COUNT) {
                throw new IllegalArgumentException("ingredient entry " + index + " has out-of-range Slot "
                    + ingredientSlot);
            }
            if (occupiedSlots[ingredientSlot]) {
                throw new IllegalArgumentException("ingredient Slot " + ingredientSlot + " occurs more than once");
            }

            ItemStack ingredient = new ItemStack(serializedIngredient);
            if (ingredient.isEmpty()) {
                throw new IllegalArgumentException("ingredient Slot " + ingredientSlot + " encodes an empty stack");
            }

            ingredients.setItemDirect(ingredientSlot, ingredient);
            occupiedSlots[ingredientSlot] = true;
        }

        if (!recipeTag.hasKey(RESULT_TAG, Constants.NBT.TAG_COMPOUND)) {
            throw new IllegalArgumentException("missing compound result");
        }
        ItemStack result = new ItemStack(recipeTag.getCompoundTag(RESULT_TAG));
        if (result.isEmpty()) {
            throw new IllegalArgumentException("result encodes an empty stack");
        }

        if (!recipeTag.hasKey(VIS_COST_TAG, Constants.NBT.TAG_FLOAT)) {
            throw new IllegalArgumentException("missing float Vis cost");
        }
        float visCost = recipeTag.getFloat(VIS_COST_TAG);
        if (!Float.isFinite(visCost) || visCost < 0.0F) {
            throw new IllegalArgumentException("Vis cost must be finite and non-negative: " + visCost);
        }

        return new KnowledgeCoreUtil.Recipe(ingredients, result, visCost);
    }

    /**
     * Resolves the canonical recipes compound for mutation without silently replacing malformed persisted data.
     */
    @Nullable
    private NBTTagCompound getRecipesForWrite(NBTTagCompound knowledgeCoreTag, boolean create) {
        if (!knowledgeCoreTag.hasKey(RECIPES_TAG)) {
            if (!create) {
                return null;
            }
            NBTTagCompound recipes = new NBTTagCompound();
            knowledgeCoreTag.setTag(RECIPES_TAG, recipes);
            return recipes;
        }
        if (!knowledgeCoreTag.hasKey(RECIPES_TAG, Constants.NBT.TAG_COMPOUND)) {
            IllegalArgumentException exception = new IllegalArgumentException(
                "Knowledge Core recipes root is not a compound");
            ThELog.error("Cannot write Knowledge Core recipe data", exception);
            throw exception;
        }
        return knowledgeCoreTag.getCompoundTag(RECIPES_TAG);
    }

    /**
     * Resolves the canonical recipes compound at the external-data boundary and excludes a malformed root.
     */
    private Optional<NBTTagCompound> getRecipesForRead(NBTTagCompound knowledgeCoreTag) {
        if (!knowledgeCoreTag.hasKey(RECIPES_TAG)) {
            return Optional.empty();
        }
        if (!knowledgeCoreTag.hasKey(RECIPES_TAG, Constants.NBT.TAG_COMPOUND)) {
            ThELog.error("Rejected malformed Knowledge Core recipes root: value is not a compound");
            return Optional.empty();
        }
        return Optional.of(knowledgeCoreTag.getCompoundTag(RECIPES_TAG));
    }

    /**
     * Parses only canonical non-negative decimal recipe keys; no legacy root-key interpretation is performed.
     */
    private Optional<Integer> parseRecipeSlot(String recipeKey) {
        final int recipeSlot;
        try {
            recipeSlot = Integer.parseInt(recipeKey);
        } catch (NumberFormatException exception) {
            ThELog.error("Rejected malformed Knowledge Core recipe key " + recipeKey, exception);
            return Optional.empty();
        }

        if (recipeSlot < 0 || !Integer.toString(recipeSlot).equals(recipeKey)) {
            ThELog.error("Rejected non-canonical Knowledge Core recipe key {}", recipeKey);
            return Optional.empty();
        }
        return Optional.of(recipeSlot);
    }

    /**
     * Fails immediately when a caller attempts to address a recipe outside the codec's non-negative key domain.
     */
    private static void validateNonNegativeRecipeSlot(int recipeSlot) {
        if (recipeSlot < 0) {
            throw new IllegalArgumentException("Knowledge Core recipe slot cannot be negative: " + recipeSlot);
        }
    }

    /**
     * Identifies obsolete canonical root-level recipe indices for deletion without interpreting their payload.
     */
    private static boolean isLegacyRecipeRootKey(String rootKey) {
        final int slot;
        try {
            slot = Integer.parseInt(rootKey);
        } catch (NumberFormatException exception) {
            return false;
        }
        return slot >= 0 && Integer.toString(slot).equals(rootKey);
    }
}
