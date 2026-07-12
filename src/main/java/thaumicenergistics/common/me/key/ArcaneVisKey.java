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
import net.minecraft.world.World;
import thaumicenergistics.core.definitions.GuiText;

import java.util.List;

/**
 * The singleton synthetic key used to render used and missing Vis in crafting-plan statistics.
 * This key is intentionally not backed by ME storage and is removed before a plan reaches a crafting CPU.
 */
public final class ArcaneVisKey extends AEKey {

    public static final ArcaneVisKey INSTANCE = new ArcaneVisKey();

    private ArcaneVisKey() {
    }

    @Override
    public AEKeyType getType() {
        return ArcaneVisKeys.INSTANCE;
    }

    @Override
    public AEKey dropSecondary() {
        return this;
    }

    @Override
    public NBTTagCompound toTag() {
        return new NBTTagCompound();
    }

    @Override
    public Object getPrimaryKey() {
        return ArcaneVisKey.class;
    }

    @Override
    public ResourceLocation getId() {
        return ArcaneVisKeys.ID;
    }

    @Override
    public void writeToPacket(PacketBuffer data) {
        // The key has no variable data because only one Arcane Vis key exists.
    }

    @Override
    public Object getReadOnlyStack() {
        return null;
    }

    @Override
    protected ITextComponent computeDisplayName() {
        return GuiText.arcane_vis.text();
    }

    @Override
    public void addDrops(long amount, List<ItemStack> drops, World world, BlockPos pos) {
        // Plan-only Vis statistics are not material resources and must never become drops.
    }

    @Override
    public NBTBase get(String componentId) {
        return null;
    }

    @Override
    public String toString() {
        return "ArcaneVisKey";
    }
}
