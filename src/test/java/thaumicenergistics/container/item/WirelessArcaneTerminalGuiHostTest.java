package thaumicenergistics.container.item;

import ae2.container.ISubGui;
import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.ItemGuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.items.ItemWirelessArcaneTerminal;
import thaumicenergistics.test.FakeMinecraft;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WirelessArcaneTerminalGuiHostTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void returnToMainContainerDelegatesToSuperclassCallbackWithoutSlotLookup() {
        ItemWirelessArcaneTerminal terminal = new ItemWirelessArcaneTerminal("wireless_arcane_terminal_test");
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        SlotQueryRejectingLocator locator = new SlotQueryRejectingLocator(new ItemStack(terminal));
        AtomicInteger callbackCalls = new AtomicInteger();
        TestSubGui subGui = new TestSubGui();
        WirelessArcaneTerminalGuiHost host = new TestWirelessArcaneTerminalGuiHost(
                terminal, terminal, player, locator, (callbackPlayer, callbackSubGui) -> {
            assertSame(player, callbackPlayer);
            assertSame(subGui, callbackSubGui);
            callbackCalls.incrementAndGet();
        });

        host.returnToMainContainer(player, subGui);

        assertAll(
                () -> assertEquals(1, callbackCalls.get()),
                () -> assertEquals(0, locator.slotQueries));
    }

    private static final class SlotQueryRejectingLocator implements ItemGuiHostLocator {

        private final ItemStack stack;
        private int slotQueries;

        private SlotQueryRejectingLocator(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack locateItem(EntityPlayer player) {
            return this.stack;
        }

        @Override
        public Integer getPlayerInventorySlot() {
            this.slotQueries++;
            throw new AssertionError("Wireless arcane host return path must use the callback locator route");
        }
    }

    private static final class TestWirelessArcaneTerminalGuiHost extends WirelessArcaneTerminalGuiHost {

        private TestWirelessArcaneTerminalGuiHost(ItemWirelessArcaneTerminal stackItem,
                                                  ItemWirelessArcaneTerminal terminalItem,
                                                  EntityPlayer player,
                                                  ItemGuiHostLocator locator,
                                                  java.util.function.BiConsumer<EntityPlayer, ISubGui> returnToMainContainer) {
            super(stackItem, terminalItem, player, locator, returnToMainContainer);
        }

        @Override
        protected void updateConnectedAccessPoint() {
        }

        @Override
        protected void updateLinkStatus() {
        }
    }

    private static final class TestSubGui implements ISubGui {

        @Override
        public GuiHostLocator getLocator() {
            return null;
        }

        @Override
        public ae2.api.storage.ISubGuiHost getHost() {
            return null;
        }
    }
}
