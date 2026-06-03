package thaumicenergistics.api.stacks;

import ae2.api.stacks.AEKeyType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.lang.GuiText;

import java.util.Objects;

public final class AEEssentiaKeys extends AEKeyType {
    public static final AEEssentiaKeys INSTANCE = new AEEssentiaKeys();
    public static final ResourceLocation ID = new ResourceLocation("thaumicenergistics", "essentia");

    private AEEssentiaKeys() {
        super(ID, AEEssentiaKey.class, GuiText.Essentias.text());
    }

    @Override
    public AEEssentiaKey readFromPacket(PacketBuffer input) {
        Objects.requireNonNull(input, "input");
        return AEEssentiaKey.fromPacket(input);
    }

    @Override
    public AEEssentiaKey loadKeyFromTag(NBTTagCompound tag) {
        Objects.requireNonNull(tag, "tag");
        return AEEssentiaKey.fromTag(tag);
    }
}
