package thaumicenergistics.client.gui.part;

import ae2.client.gui.implementations.GuiPriority;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.TextComponentString;
import thaumicenergistics.container.part.ContainerPriorityBridge;
import thaumicenergistics.part.PartEssentiaStorageBus;

/**
 * @author Alex811
 */
public class GuiPriorityBridge extends GuiPriority {

    public GuiPriorityBridge(EntityPlayer player, PartEssentiaStorageBus part) {
        super(new ContainerPriorityBridge(player.inventory, part), player.inventory, new TextComponentString(part.getRepr().getDisplayName()));
    }
}
