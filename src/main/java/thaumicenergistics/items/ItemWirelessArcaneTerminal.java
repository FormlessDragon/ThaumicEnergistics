package thaumicenergistics.items;

import ae2.api.features.HotkeyAction;
import ae2.api.implementations.items.AddWirelessTerminalEvent;
import ae2.container.GuiIds;
import ae2.items.tools.powered.WirelessTerminalItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.init.ModGUIs;

public class ItemWirelessArcaneTerminal extends WirelessTerminalItem {

    private static final double POWER_CAPACITY = 1600000;

    public ItemWirelessArcaneTerminal(String id) {
        super(POWER_CAPACITY, id, GuiIds.GuiKey.WIRELESS_CRAFTING_TERMINAL, ItemStack::new,
                WirelessArcaneTerminalGuiHost::new, HotkeyAction.WIRELESS_TERMINAL, 3, false);
        AddWirelessTerminalEvent.register(event -> event.builder(id, this,
                (definition, player, locator, terminalStack, returningFromSubmenu) ->
                        !terminalStack.isEmpty() && openArcaneGui(player, locator.getPlayerInventorySlot()),
                WirelessArcaneTerminalGuiHost::new,
                ItemStack::new)
                .hotkeyName("wireless_arcane_terminal")
                .upgradeSlots(3)
                .addTerminal());
    }

    private static boolean openArcaneGui(EntityPlayer player, Integer slot) {
        if (slot == null) {
            return false;
        }
        GuiHandler.openGUI(ModGUIs.WIRELESS_ARCANE_TERMINAL, player, slot);
        return true;
    }

    @Override
    protected boolean openFromInventory(EntityPlayer player, ae2.core.gui.locator.ItemGuiHostLocator locator, boolean returningFromSubmenu) {
        return openArcaneGui(player, locator.getPlayerInventorySlot());
    }

    @Override
    public GuiIds.GuiKey getGuiKey(ItemStack stack) {
        return GuiIds.GuiKey.WIRELESS_CRAFTING_TERMINAL;
    }

}
