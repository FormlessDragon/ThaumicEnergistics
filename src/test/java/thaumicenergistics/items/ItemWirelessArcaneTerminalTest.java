package thaumicenergistics.items;

import ae2.core.gui.locator.ItemGuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemWirelessArcaneTerminalTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void openFromInventoryPassesFullLocatorWithoutReadingPlayerInventorySlot() {
        ItemWirelessArcaneTerminal terminal =
                assertInstanceOf(ItemWirelessArcaneTerminal.class, ThEItems.WIRELESS_ARCANE_TERMINAL.item());
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        SlotQueryRejectingLocator locator = new SlotQueryRejectingLocator(new ItemStack(terminal));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> terminal.openFromInventory(player, locator, true));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(ModGUIs.WIRELESS_ARCANE_TERMINAL.name())),
                () -> assertTrue(exception.getMessage().contains(player.getClass().getName())));
    }

    private static final class SlotQueryRejectingLocator implements ItemGuiHostLocator {

        private final ItemStack stack;

        private SlotQueryRejectingLocator(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack locateItem(EntityPlayer player) {
            return this.stack;
        }

        @Override
        public Integer getPlayerInventorySlot() {
            throw new AssertionError("Wireless arcane open path must preserve the full locator");
        }
    }
}
