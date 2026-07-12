package thaumicenergistics.util.knowledgeCoreUtil;

import ae2.api.stacks.AEItemKey;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Defines the virtual encoded-pattern representation used to show Knowledge Core recipes in AE2 terminals.
 *
 * <p>The projection deliberately delegates recipe payload serialization to {@link KnowledgeCoreRecipeCodec}; this
 * keeps terminal display data identical to persisted Knowledge Core data while using an AE2 encoded-pattern item for
 * native output icons and tooltips.</p>
 */
public interface KnowledgeCorePatternProjection {

    /**
     * Shared projection implementation used by item registration, assembler inventories, and pattern details.
     */
    KnowledgeCorePatternProjection INSTANCE = new KnowledgeCorePatternProjectionImpl();

    /**
     * Encodes one validated recipe into a hidden AE2 encoded-pattern item.
     *
     * @param recipe recipe to project
     * @return a newly allocated encoded-pattern stack
     * @throws IllegalArgumentException when the recipe violates the shared codec contract
     */
    ItemStack encode(KnowledgeCoreUtil.Recipe recipe);

    /**
     * Decodes one hidden projection item for AE2's pattern APIs.
     *
     * @param definition encoded item key supplied by AE2
     * @param world      world used by AE2 while resolving pattern details
     * @return decoded Knowledge Core details, or {@code null} when the item is not a valid projection
     */
    @Nullable
    KnowledgeCoreUtil.KnowledgeCorePatternDetails decode(@Nullable AEItemKey definition, @Nullable World world);
}
