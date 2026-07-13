package thaumicenergistics.items.tools.powerd;

import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.items.tools.powered.WirelessTerminalItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.client.gui.ModGUIs;

public class WirelessArcaneTermItem extends WirelessTerminalItem {

    public WirelessArcaneTermItem(double powerCapacity) {
        super(powerCapacity,
            "wireless_arcane_terminal",
            ItemStack::new,
            WirelessArcaneTerminalGuiHost::new,
            WirelessTermDefinitionFactories.arcaneTermContainer(),
            WirelessTermDefinitionFactories.arcaneTermScreen(),
            "wireless_arcane_terminal",
            3);
    }

    @Override
    protected boolean openFromInventory(EntityPlayer player, ItemGuiHostLocator locator, boolean returningFromSubmenu) {
        ThEGuiOpener.openLocatorGui(player, ModGUIs.WIRELESS_ARCANE_TERMINAL, locator, returningFromSubmenu);
        return true;
    }

}
