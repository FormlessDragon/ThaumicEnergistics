package thaumicenergistics.util;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.storage.MEStorage;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;

/**
 * @author BrockWS
 */
public class AEUtil {

    private static KeyBinding focusKeyBinding;

    public static String getDisplayName(Object o) {
        if (o instanceof ItemStack)
            return ((ItemStack) o).getDisplayName();
        if (o instanceof EntityItem)
            return AEUtil.getDisplayName(((EntityItem) o).getItem());
        if (o instanceof Item)
            return new ItemStack((Item) o).getDisplayName();
        if (o instanceof Block)
            return ((Block) o).getLocalizedName();
        return "NAMENOTFOUND";
    }

    public static String getModID(Object o) {
        ResourceLocation rl = null;
        if (o instanceof ItemStack)
            return AEUtil.getModID(((ItemStack) o).getItem());
        else if (o instanceof EntityItem)
            return AEUtil.getModID(((EntityItem) o).getItem());
        else if (o instanceof Item)
            rl = ((Item) o).getRegistryName();
        else if (o instanceof Block)
            rl = ((Block) o).getRegistryName();

        return rl != null ? rl.getNamespace() : "MODIDNOTFOUND";
    }

    public static long getStackSize(Object o) {
        if (o instanceof ItemStack)
            return ((ItemStack) o).getCount();
        if (o instanceof EntityItem)
            return AEUtil.getStackSize(((EntityItem) o).getItem());
        return 0;
    }

    public static long insertEssentia(MEStorage storage, Aspect aspect, long amount, Actionable mode, IActionSource source) {
        return SupergiantEssentiaUtil.insert(storage, aspect, amount, mode, source);
    }

    public static long extractEssentia(MEStorage storage, Aspect aspect, long amount, Actionable mode, IActionSource source) {
        return SupergiantEssentiaUtil.extract(storage, aspect, amount, mode, source);
    }

    public static long getStoredEssentiaAmount(MEStorage storage, Aspect aspect) {
        return SupergiantEssentiaUtil.getStoredAmount(storage, aspect);
    }

    public static KeyBinding getFocusKeyBinding() {
        if (AEUtil.focusKeyBinding == null) {
            for (KeyBinding key : Minecraft.getMinecraft().gameSettings.keyBindings)
                if (key.getKeyCategory().equalsIgnoreCase("key.appliedenergistics2.category") &&
                        key.getKeyDescription().equalsIgnoreCase("key.toggle_focus.desc")) {
                    AEUtil.focusKeyBinding = key;
                    break;
                }
        }
        return AEUtil.focusKeyBinding;
    }

    public static boolean isWrench(ItemStack stack, EntityPlayer player, BlockPos pos) {
        if (stack.isEmpty())
            return false;
        return false;
    }
}
