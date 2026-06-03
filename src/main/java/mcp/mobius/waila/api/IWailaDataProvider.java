package mcp.mobius.waila.api;

import net.minecraft.item.ItemStack;

import java.util.List;

public interface IWailaDataProvider {
    List<String> getWailaBody(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor, IWailaConfigHandler config);
}
