package thaumicenergistics.util.knowledgeCoreUtil;

import ae2.api.crafting.IPatternDetails;
import ae2.api.inventories.InternalInventory;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.core.AEConfig;
import ae2.util.inv.AppEngInternalInventory;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.items.ItemKnowledgeCore;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Alex811
 */
public abstract class KnowledgeCoreUtil {

    public static final int BASE_RECIPE_SLOTS = 9;
    public static final int RECIPE_SLOTS_PER_EXPANSION_CARD = 9;
    private static final KnowledgeCoreRecipeCodec RECIPE_CODEC = new KnowledgeCoreRecipeCodecImpl();

    public static int getMaxExpansionCards() {
        return Math.max(0, AEConfig.instance().getMolecularAssemblerPatternExpansionCardLimit());
    }

    public static int getRecipeSlotCount(ItemStack knowledgeCoreStack) {
        return BASE_RECIPE_SLOTS + getInstalledExpansionCards(knowledgeCoreStack) * RECIPE_SLOTS_PER_EXPANSION_CARD;
    }

    public static int getInstalledExpansionCards(ItemStack knowledgeCoreStack) {
        if (knowledgeCoreStack.isEmpty()) {
            return 0;
        }
        return getUpgradeInventory(knowledgeCoreStack)
            .getInstalledUpgrades(ThEItems.KNOWLEDGE_CORE_PATTERN_EXPANSION_CARD.item());
    }

    public static IUpgradeInventory getUpgradeInventory(ItemStack knowledgeCoreStack) {
        if (knowledgeCoreStack.isEmpty() || !(knowledgeCoreStack.getItem() instanceof ItemKnowledgeCore knowledgeCore)) {
            throw new IllegalArgumentException("Knowledge Core upgrade inventory requires a Knowledge Core stack, got "
                + knowledgeCoreStack);
        }
        return knowledgeCore.getUpgrades(knowledgeCoreStack);
    }

    /**
     * Returns the shared recipe serialization contract used by Knowledge Core storage and pattern projections.
     *
     * @return the singleton codec exposed only through its interface
     */
    public static KnowledgeCoreRecipeCodec getRecipeCodec() {
        return RECIPE_CODEC;
    }

    public static void setRecipe(ItemStack knowledgeCoreStack, int slot, Recipe recipe) {
        validateRecipeSlot(knowledgeCoreStack, slot);
        NBTTagCompound nbt = knowledgeCoreStack.getTagCompound();
        if (nbt == null) {
            if (recipe == null) {
                return;
            }
            nbt = new NBTTagCompound();
        }
        RECIPE_CODEC.setRecipe(nbt, slot, recipe);
        knowledgeCoreStack.setTagCompound(nbt.isEmpty() ? null : nbt);
    }

    /**
     * @param knowledgeCoreStack the Knowledge Core ItemStack
     * @param slot               recipe index
     * @return Recipe or null if no recipe exists in the specified slot
     */
    public static Recipe getRecipe(ItemStack knowledgeCoreStack, int slot) {
        validateRecipeSlot(knowledgeCoreStack, slot);
        NBTTagCompound nbt = knowledgeCoreStack.getTagCompound();
        return nbt == null ? null : RECIPE_CODEC.getRecipe(nbt, slot).orElse(null);
    }

    public static HashMap<ItemStack, InternalInventory> getRecipeMap(ItemStack knowledgeCoreStack) {
        HashMap<ItemStack, InternalInventory> recipeMap = new HashMap<>();
        for (int i = 0; i < getRecipeSlotCount(knowledgeCoreStack); i++) {
            Recipe recipe = getRecipe(knowledgeCoreStack, i);
            if (recipe != null) recipeMap.put(recipe.result, recipe.ingredients);
        }
        return recipeMap;
    }

    @Nullable
    public static Recipe getRecipe(ItemStack knowledgeCoreStack, ItemStack result) {
        for (int i = 0; i < getRecipeSlotCount(knowledgeCoreStack); i++) {
            Recipe recipe = getRecipe(knowledgeCoreStack, i);
            if (recipe != null && recipe.result().getItem().equals(result.getItem()))
                return recipe;
        }
        return null;
    }

    /**
     * @param knowledgeCore the Knowledge Core ItemStack
     * @return A clean stream of all the available recipes in the Knowledge Core, no nulls
     */
    public static Stream<Recipe> recipeStreamOf(ItemStack knowledgeCore) {
        return indexedRecipeStreamOf(knowledgeCore).map(KnowledgeCoreRecipeCodec.StoredRecipe::recipe);
    }

    /**
     * Streams valid recipes in physical Knowledge Core slot order while retaining their source indices.
     *
     * <p>Pattern projections use this view to compress unused capacity without losing deterministic ordering.</p>
     *
     * @param knowledgeCore Knowledge Core whose canonical recipes compound should be read
     * @return valid indexed recipes; malformed entries have already been logged and excluded
     */
    public static Stream<KnowledgeCoreRecipeCodec.StoredRecipe> indexedRecipeStreamOf(ItemStack knowledgeCore) {
        int recipeSlotCount = getRecipeSlotCount(knowledgeCore);
        NBTTagCompound nbt = knowledgeCore.getTagCompound();
        if (nbt == null) {
            return Stream.empty();
        }
        return RECIPE_CODEC.getRecipes(nbt, recipeSlotCount).stream();
    }

    public static boolean hasRecipe(ItemStack knowledgeCoreStack, Item result) {
        return getRecipeMap(knowledgeCoreStack).keySet().stream()
            .map(ItemStack::getItem)
            .anyMatch(item -> item.equals(result));
    }

    public static boolean hasRecipe(ItemStack knowledgeCoreStack, int slot) {
        validateRecipeSlot(knowledgeCoreStack, slot);
        return getRecipe(knowledgeCoreStack, slot) != null;
    }

    /**
     * @param knowledgeCoreStack the Knowledge Core ItemStack
     * @return True if the Knowledge Core has no recipes stored.
     */
    public static boolean hasNoRecipes(ItemStack knowledgeCoreStack) {
        int recipeSlotCount = getRecipeSlotCount(knowledgeCoreStack);
        NBTTagCompound nbt = knowledgeCoreStack.getTagCompound();
        return nbt == null || RECIPE_CODEC.hasNoRecipes(nbt, recipeSlotCount);
    }

    /**
     * Copies all non-recipe NBT while changing between blank and encoded Knowledge Core item types.
     *
     * @param source source Knowledge Core whose upgrades and extension data must survive
     * @param target newly allocated target Knowledge Core stack
     * @return {@code target} with a detached copy of non-recipe root data; canonical {@code recipes} and obsolete
     * root-level non-negative numeric recipe keys are discarded without decoding or migration
     */
    public static ItemStack copyNonRecipeData(ItemStack source, ItemStack target) {
        validateKnowledgeCoreStack(source, "source");
        validateKnowledgeCoreStack(target, "target");
        if (source == target) {
            throw new IllegalArgumentException("Knowledge Core conversion source and target must be distinct stacks");
        }

        NBTTagCompound copiedData = RECIPE_CODEC.copyNonRecipeData(source.getTagCompound());
        target.setTagCompound(copiedData.isEmpty() ? null : copiedData);
        return target;
    }

    private static void validateRecipeSlot(ItemStack knowledgeCoreStack, int slot) {
        if (slot < 0 || slot >= getRecipeSlotCount(knowledgeCoreStack)) {
            throw new IllegalArgumentException("Knowledge Core recipe index " + slot + " is outside active capacity "
                + getRecipeSlotCount(knowledgeCoreStack));
        }
    }

    /**
     * Validates the item boundary used by Knowledge Core type conversion.
     */
    private static void validateKnowledgeCoreStack(ItemStack stack, String role) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof ItemKnowledgeCore)) {
            throw new IllegalArgumentException("Knowledge Core conversion " + role
                + " must be a non-empty Knowledge Core stack, got " + stack);
        }
    }

    /**
     * Similar to {@link KnowledgeCoreUtil#getAEPattern(Recipe)},
     * for when you don't have the actual recipe yet
     * and you want to extract it from a Knowledge Core
     *
     * @param knowledgeCore Knowledge Core to extract from
     * @param slot          The Knowledge Core slot to read from
     * @return IPatternDetails instance to send to AE2.
     */
    public static IPatternDetails getAEPattern(ItemStack knowledgeCore, int slot) {
        Recipe recipe = getRecipe(knowledgeCore, slot);
        return getAEPattern(recipe);
    }

    /**
     * Method to extract an IPatternDetails instance from a Knowledge Core recipe, to use with AE2
     *
     * @param recipe Recipe to extract from
     * @return IPatternDetails instance to send to AE2.
     * @throws IllegalArgumentException If the recipe slot is empty, it lets AE2 deal with it, which currently means you'll get an exception indirectly
     */
    public static IPatternDetails getAEPattern(Recipe recipe) {
        if (recipe == null) {
            throw new IllegalArgumentException("Knowledge core recipe cannot be null");
        }
        return new KnowledgeCorePatternDetails(recipe);
    }

    public static Recipe getRecipe(ItemStack knowledgeCoreStack, IPatternDetails patternDetails) {
        GenericStack output = patternDetails == null ? null : patternDetails.getPrimaryOutput();
        if (output == null || !(output.what() instanceof AEItemKey)) {
            return null;
        }
        return getRecipe(knowledgeCoreStack, ((AEItemKey) output.what()).toStack((int) Math.min(output.amount(), Integer.MAX_VALUE)));
    }

    public record Recipe(InternalInventory ingredients, ItemStack result, float visCost) {
        public Recipe(InternalInventory ingredients, ItemStack result, float visCost) {
            this.ingredients = ingredients;
            this.result = result.copy();
            this.visCost = visCost;
        }

        /**
         * @return The recipe ingredients, aspect crystals included as the last 6 elements
         */
        @Override
        public InternalInventory ingredients() {
            return this.ingredients;
        }

        /**
         * Get the part of the ingredients that excludes aspects, or the part that only has the aspects
         *
         * @param aspect true to get the aspect part
         * @return the ingredients
         */
        public AppEngInternalInventory getIngredientPart(boolean aspect) {
            AppEngInternalInventory ingredients;
            InternalInventory ingredientsWithAspect = ingredients();
            if (aspect) {
                ingredients = new AppEngInternalInventory(6);
                for (int i = 0; i < 6; i++)
                    ingredients.insertItem(i, ingredientsWithAspect.getStackInSlot(i + 9), false);
            } else {
                ingredients = new AppEngInternalInventory(9);
                for (int i = 0; i < 9; i++)
                    ingredients.insertItem(i, ingredientsWithAspect.getStackInSlot(i), false);
            }
            return ingredients;
        }

    }

    public static class KnowledgeCorePatternDetails implements IPatternDetails {
        private final Recipe recipe;
        private final AEItemKey definition;
        private final IInput[] inputs;
        private final List<GenericStack> outputs;

        public KnowledgeCorePatternDetails(Recipe recipe) {
            this.recipe = Objects.requireNonNull(recipe, "recipe");
            this.definition = Objects.requireNonNull(
                AEItemKey.of(KnowledgeCorePatternProjection.INSTANCE.encode(recipe)));
            this.inputs = buildInputs(recipe);
            this.outputs = Collections.singletonList(new GenericStack(Objects.requireNonNull(AEItemKey.of(recipe.result())), recipe.result().getCount()));
        }

        public Recipe getRecipe() {
            return this.recipe;
        }

        @Override
        public AEItemKey getDefinition() {
            return this.definition;
        }

        @Override
        public IInput[] getInputs() {
            return this.inputs.clone();
        }

        @Override
        public List<GenericStack> getOutputs() {
            return this.outputs;
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                || other instanceof KnowledgeCorePatternDetails that
                && this.definition.equals(that.definition);
        }

        @Override
        public int hashCode() {
            return this.definition.hashCode();
        }

        private static IInput[] buildInputs(Recipe recipe) {
            List<IInput> inputs = new ArrayList<>();
            InternalInventory normalInputs = recipe.getIngredientPart(false);
            for (int slot = 0; slot < normalInputs.size(); slot++) {
                ItemStack stack = normalInputs.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    inputs.add(new ItemPatternInput(stack));
                }
            }
            return inputs.toArray(new IInput[0]);
        }
    }

    private static class ItemPatternInput implements IPatternDetails.IInput {
        private final AEItemKey key;
        private final long multiplier;
        private final GenericStack[] possibleInputs;

        private ItemPatternInput(ItemStack stack) {
            this.key = Objects.requireNonNull(AEItemKey.of(stack));
            this.multiplier = stack.getCount();
            this.possibleInputs = new GenericStack[]{new GenericStack(this.key, 1)};
        }

        @Override
        public GenericStack[] possibleInputs() {
            return this.possibleInputs.clone();
        }

        @Override
        public long getMultiplier() {
            return this.multiplier;
        }

        @Override
        public boolean isValid(AEKey key, World world) {
            return this.key.equals(key);
        }

        @Override
        public AEKey getRemainingKey(AEKey key) {
            return null;
        }
    }

}
