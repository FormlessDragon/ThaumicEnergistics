package thaumicenergistics.common.me.key;

import ae2.api.stacks.AEKeyType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import thaumicenergistics.ThaumicEnergistics;
import thaumicenergistics.core.definitions.GuiText;

import java.util.Objects;

/**
 * Registers the single synthetic key used to display Vis in an AE2 crafting plan.
 */
public final class ArcaneVisKeys extends AEKeyType {

    public static final int AMOUNT_PER_VIS = 1_000;
    public static final ResourceLocation ID = ThaumicEnergistics.id("arcane_vis");
    public static final ArcaneVisKeys INSTANCE = new ArcaneVisKeys();

    private ArcaneVisKeys() {
        super(ID, ArcaneVisKey.class, GuiText.arcane_vis.text());
    }

    @Override
    public int getAmountPerOperation() {
        return AMOUNT_PER_VIS;
    }

    @Override
    public int getAmountPerByte() {
        return 8 * AMOUNT_PER_VIS;
    }

    @Override
    public int getAmountPerUnit() {
        return AMOUNT_PER_VIS;
    }

    @Override
    public String getUnitSymbol() {
        return "Vis";
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
