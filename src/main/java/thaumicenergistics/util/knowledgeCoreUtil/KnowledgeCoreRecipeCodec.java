package thaumicenergistics.util.knowledgeCoreUtil;

import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Defines the single NBT contract used to persist and project Knowledge Core recipes.
 *
 * <p>The codec exists so Knowledge Core item storage and read-only AE2 pattern projections cannot drift into
 * separate formats. Implementations must write only the strict sparse format and must report malformed external
 * data before excluding it from reads.</p>
 */
public interface KnowledgeCoreRecipeCodec {

    /**
     * The fixed number of ingredient positions in an arcane recipe: nine matrix slots followed by six crystal
     * slots.
     */
    int INGREDIENT_SLOT_COUNT = 15;

    /**
     * The root compound that exclusively owns all encoded Knowledge Core recipes.
     */
    String RECIPES_TAG = "recipes";

    /**
     * Writes or removes one recipe in the supplied Knowledge Core root tag.
     *
     * @param knowledgeCoreTag mutable root tag owned by a Knowledge Core stack
     * @param recipeSlot       non-negative storage slot represented inside {@link #RECIPES_TAG}
     * @param recipe           recipe to encode, or {@code null} to remove the slot
     * @throws IllegalArgumentException when the slot, recipe, or existing recipes container violates the codec
     *                                  contract
     */
    void setRecipe(NBTTagCompound knowledgeCoreTag, int recipeSlot,
                   @Nullable KnowledgeCoreUtil.Recipe recipe);

    /**
     * Reads one recipe from a Knowledge Core root tag.
     *
     * <p>Malformed external data is logged and returned as an empty result so a damaged core cannot break AE2's
     * crafting-provider refresh.</p>
     *
     * @param knowledgeCoreTag root tag owned by a Knowledge Core stack
     * @param recipeSlot       non-negative recipe slot to inspect
     * @return the decoded recipe, or an empty result when the slot is absent or damaged
     * @throws IllegalArgumentException when {@code recipeSlot} is negative
     */
    Optional<KnowledgeCoreUtil.Recipe> getRecipe(NBTTagCompound knowledgeCoreTag, int recipeSlot);

    /**
     * Reads every valid recipe in ascending Knowledge Core slot order.
     *
     * <p>Unknown keys, entries outside the active capacity, and malformed recipes are logged independently and
     * omitted from the returned list.</p>
     *
     * @param knowledgeCoreTag root tag owned by a Knowledge Core stack
     * @param recipeSlotCount  active slot capacity used to reject out-of-range stored entries
     * @return immutable, slot-ordered valid recipe entries
     * @throws IllegalArgumentException when {@code recipeSlotCount} is negative
     */
    List<StoredRecipe> getRecipes(NBTTagCompound knowledgeCoreTag, int recipeSlotCount);

    /**
     * Determines whether a root tag contains no valid recipes within the active capacity.
     *
     * @param knowledgeCoreTag root tag owned by a Knowledge Core stack
     * @param recipeSlotCount  active slot capacity
     * @return {@code true} when no valid recipe can be read
     */
    boolean hasNoRecipes(NBTTagCompound knowledgeCoreTag, int recipeSlotCount);

    /**
     * Encodes one recipe as a standalone strict sparse recipe compound.
     *
     * <p>This entry-level API is the shared boundary used by Knowledge Core storage and virtual pattern
     * projections.</p>
     *
     * @param recipe recipe whose fifteen ingredient positions, result, and Vis cost must be encoded
     * @return a newly allocated recipe compound
     * @throws IllegalArgumentException when the recipe has the wrong ingredient size, an empty result, or an
     *                                  invalid Vis cost
     */
    NBTTagCompound encodeRecipe(KnowledgeCoreUtil.Recipe recipe);

    /**
     * Safely decodes a standalone recipe compound supplied by persisted or projected data.
     *
     * @param recipeTag         encoded recipe compound
     * @param sourceDescription concise location included in corruption logs
     * @return the decoded recipe, or an empty result after malformed data has been logged
     */
    Optional<KnowledgeCoreUtil.Recipe> decodeRecipe(NBTTagCompound recipeTag, String sourceDescription);

    /**
     * Copies every root value except recipe data.
     *
     * <p>This is required when blank and encoded Knowledge Core item types are exchanged: upgrade inventory and
     * third-party extension data survive, while the canonical recipes compound and obsolete root-level numeric
     * recipe slots never leak into a blank core. Legacy slots are discarded without being decoded or migrated.</p>
     *
     * @param sourceTag source root tag, or {@code null} when the source has no data
     * @return a newly allocated compound containing only non-recipe data
     */
    NBTTagCompound copyNonRecipeData(@Nullable NBTTagCompound sourceTag);

    /**
     * Associates a decoded recipe with its physical Knowledge Core slot so consumers can preserve deterministic
     * ordering while compressing empty capacity.
     *
     * @param slot   non-negative Knowledge Core recipe slot
     * @param recipe validated decoded recipe stored at that slot
     */
    record StoredRecipe(int slot, KnowledgeCoreUtil.Recipe recipe) {

        /**
         * Enforces that only valid slot associations can leave the codec.
         */
        public StoredRecipe {
            if (slot < 0) {
                throw new IllegalArgumentException("Knowledge Core stored recipe slot cannot be negative: " + slot);
            }
            Objects.requireNonNull(recipe, "recipe");
        }
    }
}
