package thaumicenergistics.common.me.key;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.api.stacks.EssentiaStack;

import java.util.List;
import java.util.Objects;

public final class AEEssentiaKey extends AEKey {
    private static final String TAG_ASPECT = "Aspect";

    private final String aspectTag;
    private final Aspect aspect;

    private AEEssentiaKey(Aspect aspect) {
        this.aspect = Objects.requireNonNull(aspect, "aspect");
        this.aspectTag = aspect.getTag();
    }

    @Nullable
    public static AEEssentiaKey of(Aspect aspect) {
        return aspect == null ? null : new AEEssentiaKey(aspect);
    }

    @Nullable
    public static AEEssentiaKey of(EssentiaStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return of(stack.getAspect());
    }

    public static AEEssentiaKey fromTag(NBTTagCompound tag) {
        try {
            String aspectId = tag.getString(TAG_ASPECT);
            Aspect aspect = Aspect.getAspect(aspectId);
            if (aspect == null) {
                throw new IllegalArgumentException("Unknown aspect '" + aspectId + "'");
            }
            return new AEEssentiaKey(aspect);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Could not read essentia key from tag " + tag, e);
        }
    }

    public static AEEssentiaKey fromPacket(PacketBuffer data) {
        try {
            String aspectId = data.readString(Short.MAX_VALUE);
            Aspect aspect = Aspect.getAspect(aspectId);
            if (aspect == null) {
                throw new IllegalArgumentException("Unknown aspect '" + aspectId + "'");
            }
            return new AEEssentiaKey(aspect);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Could not read essentia key from packet", e);
        }
    }

    public Aspect getAspect() {
        return this.aspect;
    }

    public String getAspectTag() {
        return this.aspectTag;
    }

    public EssentiaStack toStack(int amount) {
        return new EssentiaStack(this.aspect, amount);
    }

    @Override
    public AEKeyType getType() {
        return AEEssentiaKeys.INSTANCE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public NBTTagCompound toTag() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(TAG_ASPECT, this.aspectTag);
        return tag;
    }

    @Override
    public Object getPrimaryKey() {
        return this.aspectTag;
    }

    @Override
    public ResourceLocation getId() {
        return new ResourceLocation("thaumcraft", this.aspectTag);
    }

    @Override
    public void writeToPacket(PacketBuffer packetBuffer) {
        packetBuffer.writeString(this.aspectTag);
    }

    @Override
    public Object getReadOnlyStack() {
        return this.aspect;
    }

    @Override
    protected ITextComponent computeDisplayName() {
        return new TextComponentTranslation(this.aspect.getName());
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, World world, BlockPos pos) {
        // Aspects are voided as same as fluid
    }

    @Override
    public NBTBase get(String tag) {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof AEEssentiaKey
                && this.aspectTag.equals(((AEEssentiaKey) other).aspectTag);
    }

    @Override
    public int hashCode() {
        return this.aspectTag.hashCode();
    }

    @Override
    public String toString() {
        return "AEEssentiaKey[" + this.aspectTag + "]";
    }

}
