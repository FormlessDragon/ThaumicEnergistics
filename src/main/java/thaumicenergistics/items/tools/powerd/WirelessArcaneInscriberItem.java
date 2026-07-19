package thaumicenergistics.items.tools.powerd;

import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.items.tools.powered.WirelessTerminalItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumicenergistics.client.gui.ModGUIs;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.container.item.WirelessArcaneInscriberGuiHost;

public class WirelessArcaneInscriberItem extends WirelessTerminalItem {

    public WirelessArcaneInscriberItem(double powerCapacity) {
        super(powerCapacity,
            "wireless_arcane_inscriber",
            ItemStack::new,
            WirelessArcaneInscriberGuiHost::new,
            WirelessTermDefinitionFactories.arcaneInscriberContainer(),
            WirelessTermDefinitionFactories.arcaneInscriberScreen(),
            "wireless_arcane_inscriber",
            3);
    }

    @Override
    protected boolean openFromInventory(EntityPlayer player, ItemGuiHostLocator locator, boolean returningFromSubmenu) {
        ThEGuiOpener.openItemGui(player, ModGUIs.WIRELESS_ARCANE_INSCRIBER, locator, returningFromSubmenu);
        return true;
    }

}
