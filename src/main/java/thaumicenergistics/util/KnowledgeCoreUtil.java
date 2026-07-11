package thaumicenergistics.util;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.IPatternDetailsDecoder;
import ae2.api.crafting.PatternDetailsHelper;
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
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import thaumicenergistics.common.me.key.ArcaneVisKey;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.items.ItemKnowledgeCore;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author Alex811
 */
public abstract class KnowledgeCoreUtil {

    public static final int BASE_RECIPE_SLOTS = 9;
    public static final int RECIPE_SLOTS_PER_EXPANSION_CARD = 9;
    private static final int TOTAL_INGREDIENT_SLOTS = BASE_RECIPE_SLOTS + 6;
    private static final String NBT_PATTERN_RECIPE = "thaumicenergistics:knowledgeCoreRecipe";
    private static final String NBT_RECIPE_INGREDIENTS = "ingredients";
    private static final String NBT_RECIPE_RESULT = "result";
    private static final String NBT_RECIPE_VIS_COST = "visCost";
    private static boolean patternDecoderRegistered;

    /**
     * Registers the decoder that lets AE2's Pattern Access Terminal inspect the read-only Knowledge Core projections.
     */
    public static synchronized void registerPatternDecoder() {
        if (patternDecoderRegistered) {
            return;
        }
        PatternDetailsHelper.registerDecoder(new KnowledgeCorePatternDecoder());
        patternDecoderRegistered = true;
    }

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

    public static void setRecipe(ItemStack knowledgeCoreStack, int slot, Recipe recipe) {
        validateRecipeSlot(knowledgeCoreStack, slot);
        String slotKey = String.valueOf(slot);
        NBTTagCompound nbt = knowledgeCoreStack.getTagCompound();
        if (nbt == null) nbt = new NBTTagCompound();
        if (recipe == null) {
            nbt.removeTag(slotKey);
            return;
        }
        NBTTagCompound nbtRecipe = new NBTTagCompound();
        nbtRecipe.setTag("ingredients", writeRecipeIngredients(recipe.ingredients()));
        nbtRecipe.setTag("result", recipe.result().serializeNBT());
        nbtRecipe.setFloat("visCost", recipe.visCost());
        nbt.setTag(slotKey, nbtRecipe);
        knowledgeCoreStack.setTagCompound(nbt);
    }

    /**
     * @param knowledgeCoreStack the Knowledge Core ItemStack
     * @param slot               recipe index
     * @return Recipe or null if no recipe exists in the specified slot
     */
    public static Recipe getRecipe(ItemStack knowledgeCoreStack, int slot) {
        validateRecipeSlot(knowledgeCoreStack, slot);
        NBTTagCompound nbtRecipe = getNBTRecipe(knowledgeCoreStack, slot);
        if (nbtRecipe == null) return null;
        AppEngInternalInventory ingredients = new AppEngInternalInventory(TOTAL_INGREDIENT_SLOTS);
        ItemStack result = new ItemStack(nbtRecipe.getCompoundTag("result"));
        readRecipeIngredients(ingredients, nbtRecipe.getTagList("ingredients", 10));
        return new Recipe(ingredients, result, nbtRecipe.getFloat("visCost"));
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
        final ArrayList<Recipe> recipeArrayList = new ArrayList<>();
        for (int i = 0; i < getRecipeSlotCount(knowledgeCore); i++)
            recipeArrayList.add(getRecipe(knowledgeCore, i));
        return recipeArrayList.stream().filter(Objects::nonNull);
    }

    public static boolean hasRecipe(ItemStack knowledgeCoreStack, Item result) {
        return getRecipeMap(knowledgeCoreStack).keySet().stream()
            .map(ItemStack::getItem)
            .anyMatch(item -> item.equals(result));
    }

    public static boolean hasRecipe(ItemStack knowledgeCoreStack, int slot) {
        validateRecipeSlot(knowledgeCoreStack, slot);
        return getNBTRecipe(knowledgeCoreStack, slot) != null;
    }

    /**
     * @param knowledgeCoreStack the Knowledge Core ItemStack
     * @return True if the Knowledge Core has no recipes stored.
     */
    public static boolean isEmpty(ItemStack knowledgeCoreStack) {
        NBTTagCompound nbt = knowledgeCoreStack.getTagCompound();
        if (nbt == null) return true;
        return nbt.isEmpty();
    }

    /**
     * @param knowledgeCoreStack The Knowledge Core ItemStack
     * @param slot               The Knowledge Core's recipe slot index
     * @return NBTTagCompound that represents the recipe, or null if no recipe exists in the specified slot
     */
    private static NBTTagCompound getNBTRecipe(ItemStack knowledgeCoreStack, int slot) {
        String slotKey = String.valueOf(slot);
        NBTTagCompound nbt = knowledgeCoreStack.getTagCompound();
        if (nbt == null || !nbt.hasKey(slotKey)) return null;
        return nbt.getCompoundTag(slotKey);
    }

    private static void validateRecipeSlot(ItemStack knowledgeCoreStack, int slot) {
        if (slot < 0 || slot >= getRecipeSlotCount(knowledgeCoreStack)) {
            throw new IllegalArgumentException("Knowledge Core recipe index " + slot + " is outside active capacity "
                + getRecipeSlotCount(knowledgeCoreStack));
        }
    }

    /**
     * Keeps the pre-migration dense ingredient-list format. Existing Knowledge Core stacks encode one compound per
     * recipe slot, including empty slots, rather than AE2's sparse inventory representation.
     */
    private static NBTTagList writeRecipeIngredients(InternalInventory ingredients) {
        NBTTagList serializedIngredients = new NBTTagList();
        for (int slot = 0; slot < ingredients.size(); slot++) {
            serializedIngredients.appendTag(ingredients.getStackInSlot(slot).serializeNBT());
        }
        return serializedIngredients;
    }

    private static void readRecipeIngredients(AppEngInternalInventory ingredients, NBTTagList serializedIngredients) {
        if (serializedIngredients.tagCount() > ingredients.size()) {
            throw new IllegalArgumentException("Knowledge Core recipe has " + serializedIngredients.tagCount()
                + " ingredient entries but supports only " + ingredients.size());
        }
        for (int slot = 0; slot < serializedIngredients.tagCount(); slot++) {
            ingredients.insertItem(slot, new ItemStack(serializedIngredients.getCompoundTagAt(slot)), false);
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
        return getAEPattern(recipe, true);
    }

    /**
     * Creates a pattern details snapshot for one assembler candidate. If the candidate does not currently have enough
     * Vis, the pattern exposes the diagnostic Arcane Vis input so AE2 cannot select it as a craft provider.
     *
     * @param recipe       the stored arcane recipe
     * @param visAvailable whether this assembler had enough Vis when the snapshot was created
     * @return immutable Vis-availability pattern snapshot
     */
    public static IPatternDetails getAEPattern(Recipe recipe, boolean visAvailable) {
        if (recipe == null) {
            throw new IllegalArgumentException("Knowledge core recipe cannot be null");
        }
        return new KnowledgeCorePatternDetails(recipe, visAvailable);
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

        /**
         * Transforms the recipe to an ItemStack that includes tags like the ones a normal AE2 pattern would have.
         * Mainly for internal use, you're probably looking for {@link #getAEPattern(ItemStack, int)}
         *
         * @return AE2 pattern ItemStack
         */
        public ItemStack toAEPatternStack() {
            if (this.ingredients().size() != TOTAL_INGREDIENT_SLOTS) {
                throw new IllegalArgumentException("Knowledge Core pattern must contain " + TOTAL_INGREDIENT_SLOTS
                    + " ingredient slots");
            }
            if (this.result().isEmpty()) {
                throw new IllegalArgumentException("Knowledge Core pattern result cannot be empty");
            }
            if (!Float.isFinite(this.visCost()) || this.visCost() < 0.0F) {
                throw new IllegalArgumentException("Knowledge Core pattern Vis cost must be finite and non-negative");
            }
            ItemStack stack = Optional.of(ThEItems.KNOWLEDGE_CORE.stack(1)).orElseThrow(RuntimeException::new);
            NBTTagCompound nbt = new NBTTagCompound();
            nbt.setTag("in", writeRecipeIngredients(this.getIngredientPart(false)));
            NBTTagList out = new NBTTagList();
            out.appendTag(this.result().serializeNBT());
            nbt.setTag("out", out);
            nbt.setBoolean("crafting", false);
            nbt.setBoolean("substitute", false);
            NBTTagCompound encodedRecipe = new NBTTagCompound();
            encodedRecipe.setTag(NBT_RECIPE_INGREDIENTS, writeRecipeIngredients(this.ingredients()));
            encodedRecipe.setTag(NBT_RECIPE_RESULT, this.result().serializeNBT());
            encodedRecipe.setFloat(NBT_RECIPE_VIS_COST, this.visCost());
            nbt.setTag(NBT_PATTERN_RECIPE, encodedRecipe);
            stack.setTagCompound(nbt);
            return stack;
        }
    }

    public static class KnowledgeCorePatternDetails implements IPatternDetails {
        private final Recipe recipe;
        private final AEItemKey definition;
        private final IInput[] inputs;
        private final List<GenericStack> outputs;

        public KnowledgeCorePatternDetails(Recipe recipe) {
            this(recipe, true);
        }

        /**
         * Builds a pattern with a fixed Vis-availability observation. Later world changes do not mutate this detail;
         * the assembler publishes a replacement snapshot when its availability vector changes.
         */
        public KnowledgeCorePatternDetails(Recipe recipe, boolean visAvailable) {
            this.recipe = recipe;
            this.definition = AEItemKey.of(recipe.toAEPatternStack());
            this.inputs = buildInputs(recipe, visAvailable);
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

        private static IInput[] buildInputs(Recipe recipe, boolean visAvailable) {
            List<IInput> inputs = new ArrayList<>();
            InternalInventory normalInputs = recipe.getIngredientPart(false);
            for (int slot = 0; slot < normalInputs.size(); slot++) {
                ItemStack stack = normalInputs.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    inputs.add(new ItemPatternInput(stack));
                }
            }
            if (!visAvailable && recipe.visCost() > 0.0F) {
                inputs.add(new ArcaneVisPatternInput(getVisMultiplier(recipe.visCost())));
            }
            return inputs.toArray(new IInput[0]);
        }

        private static long getVisMultiplier(float visCost) {
            if (!Float.isFinite(visCost) || visCost <= 0.0F) {
                throw new IllegalArgumentException("Arcane recipe Vis cost must be finite and positive");
            }
            return (long) Math.ceil(visCost);
        }
    }

    private static class ItemPatternInput implements IPatternDetails.IInput {
        private final AEItemKey key;
        private final long amount;
        private final GenericStack[] possibleInputs;

        private ItemPatternInput(ItemStack stack) {
            this.key = Objects.requireNonNull(AEItemKey.of(stack));
            this.amount = stack.getCount();
            this.possibleInputs = new GenericStack[]{new GenericStack(this.key, this.amount)};
        }

        @Override
        public GenericStack[] possibleInputs() {
            return this.possibleInputs.clone();
        }

        @Override
        public long getMultiplier() {
            return this.amount;
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

    private static class ArcaneVisPatternInput implements IPatternDetails.IInput {
        private final GenericStack[] possibleInputs = {new GenericStack(ArcaneVisKey.INSTANCE, 1)};
        private final long multiplier;

        private ArcaneVisPatternInput(long multiplier) {
            this.multiplier = multiplier;
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
            return ArcaneVisKey.INSTANCE.equals(key);
        }

        @Override
        public AEKey getRemainingKey(AEKey key) {
            return null;
        }
    }

    private static final class KnowledgeCorePatternDecoder implements IPatternDetailsDecoder {
        private static final String NBT_INPUTS = "in";
        private static final String NBT_OUTPUTS = "out";

        @Override
        public boolean isEncodedPattern(ItemStack stack) {
            return decodeRecipe(stack) != null;
        }

        @Override
        public IPatternDetails decodePattern(AEItemKey what, World level) {
            if (what == null) {
                return null;
            }
            Recipe recipe = decodeRecipe(what.toStack(1));
            return recipe == null ? null : new KnowledgeCorePatternDetails(recipe);
        }

        @Nullable
        private static Recipe decodeRecipe(ItemStack stack) {
            if (stack.isEmpty() || stack.getItem() != ThEItems.KNOWLEDGE_CORE.item()) {
                return null;
            }
            NBTTagCompound tag = stack.getTagCompound();
            if (tag == null) {
                return null;
            }
            if (tag.hasKey(NBT_PATTERN_RECIPE)) {
                if (!tag.hasKey(NBT_PATTERN_RECIPE, Constants.NBT.TAG_COMPOUND)) {
                    return null;
                }
                return decodeCompleteRecipe(tag.getCompoundTag(NBT_PATTERN_RECIPE));
            }
            return decodeLegacyRecipe(tag);
        }

        @Nullable
        private static Recipe decodeCompleteRecipe(NBTTagCompound encodedRecipe) {
            if (!encodedRecipe.hasKey(NBT_RECIPE_INGREDIENTS, Constants.NBT.TAG_LIST)
                || !encodedRecipe.hasKey(NBT_RECIPE_RESULT, Constants.NBT.TAG_COMPOUND)
                || !encodedRecipe.hasKey(NBT_RECIPE_VIS_COST, Constants.NBT.TAG_FLOAT)) {
                return null;
            }
            NBTTagList ingredientsTag = encodedRecipe.getTagList(NBT_RECIPE_INGREDIENTS, Constants.NBT.TAG_COMPOUND);
            if (ingredientsTag.tagCount() != TOTAL_INGREDIENT_SLOTS) {
                return null;
            }
            ItemStack result = new ItemStack(encodedRecipe.getCompoundTag(NBT_RECIPE_RESULT));
            float visCost = encodedRecipe.getFloat(NBT_RECIPE_VIS_COST);
            if (result.isEmpty() || !Float.isFinite(visCost) || visCost < 0.0F) {
                return null;
            }
            AppEngInternalInventory ingredients = new AppEngInternalInventory(TOTAL_INGREDIENT_SLOTS);
            for (int slot = 0; slot < TOTAL_INGREDIENT_SLOTS; slot++) {
                ingredients.setItemDirect(slot, new ItemStack(ingredientsTag.getCompoundTagAt(slot)));
            }
            return new Recipe(ingredients, result, visCost);
        }

        @Nullable
        private static Recipe decodeLegacyRecipe(NBTTagCompound tag) {
            if (!tag.hasKey(NBT_INPUTS, Constants.NBT.TAG_LIST)
                || !tag.hasKey(NBT_OUTPUTS, Constants.NBT.TAG_LIST)) {
                return null;
            }
            NBTTagList inputs = tag.getTagList(NBT_INPUTS, Constants.NBT.TAG_COMPOUND);
            NBTTagList outputs = tag.getTagList(NBT_OUTPUTS, Constants.NBT.TAG_COMPOUND);
            if (inputs.tagCount() != BASE_RECIPE_SLOTS || outputs.tagCount() != 1) {
                return null;
            }
            ItemStack result = new ItemStack(outputs.getCompoundTagAt(0));
            if (result.isEmpty()) {
                return null;
            }
            AppEngInternalInventory ingredients = new AppEngInternalInventory(TOTAL_INGREDIENT_SLOTS);
            for (int slot = 0; slot < BASE_RECIPE_SLOTS; slot++) {
                ingredients.setItemDirect(slot, new ItemStack(inputs.getCompoundTagAt(slot)));
            }
            return new Recipe(ingredients, result, 0.0F);
        }
    }
}
