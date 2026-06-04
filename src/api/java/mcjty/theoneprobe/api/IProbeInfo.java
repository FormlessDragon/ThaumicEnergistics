package mcjty.theoneprobe.api;

import net.minecraft.item.ItemStack;

public interface IProbeInfo {
    String STARTLOC = "";
    String ENDLOC = "";

    IProbeInfo horizontal();

    IProbeInfo vertical();

    IProbeInfo item(ItemStack stack);

    IProbeInfo itemLabel(ItemStack stack);

    IProbeInfo text(String text);
}
