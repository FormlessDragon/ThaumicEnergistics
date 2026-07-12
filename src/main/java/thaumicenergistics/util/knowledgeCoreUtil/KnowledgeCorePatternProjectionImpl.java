package thaumicenergistics.util.knowledgeCoreUtil;

import ae2.api.stacks.AEItemKey;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import thaumicenergistics.core.ThELog;
import thaumicenergistics.core.definitions.ThEItems;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * Strict hidden-item implementation of {@link KnowledgeCorePatternProjection}.
 */
final class KnowledgeCorePatternProjectionImpl implements KnowledgeCorePatternProjection {

    private static final String RECIPE_TAG = "recipe";

    @Override
    public ItemStack encode(KnowledgeCoreUtil.Recipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        ItemStack projection = ThEItems.KNOWLEDGE_CORE_PATTERN.stack();
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(RECIPE_TAG, KnowledgeCoreUtil.getRecipeCodec().encodeRecipe(recipe));
        projection.setTagCompound(tag);
        return projection;
    }

    @Nullable
    @Override
    public KnowledgeCoreUtil.KnowledgeCorePatternDetails decode(@Nullable AEItemKey definition, @Nullable World world) {
        if (definition == null || world == null
            || definition.getItem() != ThEItems.KNOWLEDGE_CORE_PATTERN.item()) {
            return null;
        }

        ItemStack projection = definition.toStack(1);
        NBTTagCompound tag = projection.getTagCompound();
        if (tag == null || !tag.hasKey(RECIPE_TAG)) {
            return null;
        }
        if (!tag.hasKey(RECIPE_TAG, Constants.NBT.TAG_COMPOUND)) {
            ThELog.error("Rejected malformed Knowledge Core pattern projection: recipe payload is not a compound");
            return null;
        }

        return KnowledgeCoreUtil.getRecipeCodec()
            .decodeRecipe(tag.getCompoundTag(RECIPE_TAG), "Knowledge Core pattern projection")
            .map(KnowledgeCoreUtil.KnowledgeCorePatternDetails::new)
            .orElse(null);
    }
}
