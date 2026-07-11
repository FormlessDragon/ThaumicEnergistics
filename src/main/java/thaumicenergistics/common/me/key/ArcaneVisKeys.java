package thaumicenergistics.common.me.key;

import ae2.api.stacks.AEKeyType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.core.definitions.GuiText;

import java.util.Objects;

/**
 * Registers the single diagnostic key used by unavailable arcane patterns.
 */
public final class ArcaneVisKeys extends AEKeyType {

    public static final ResourceLocation ID = ThaumicEnergistics.id("arcane_vis");
    public static final ArcaneVisKeys INSTANCE = new ArcaneVisKeys();

    private ArcaneVisKeys() {
        super(ID, ArcaneVisKey.class, GuiText.arcane_vis.text());
    }

    @Override
    public ArcaneVisKey readFromPacket(PacketBuffer input) {
        Objects.requireNonNull(input, "input");
        return ArcaneVisKey.INSTANCE;
    }

    @Override
    public ArcaneVisKey loadKeyFromTag(NBTTagCompound tag) {
        Objects.requireNonNull(tag, "tag");
        return ArcaneVisKey.INSTANCE;
    }
}
