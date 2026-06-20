package thaumicenergistics.common.gui;

import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEGuiOpenerTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void wirelessLocatorUsesExactWirelessArcaneHostTypeBeforeFailingFast() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingWirelessLocator locator = new RecordingWirelessLocator();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ThEGuiOpener.locateWirelessArcaneHost(player, ModGUIs.WIRELESS_ARCANE_TERMINAL, locator));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(WirelessArcaneTerminalGuiHost.class.getName())),
                () -> assertSame(WirelessArcaneTerminalGuiHost.class, locator.requestedHostType),
                () -> assertEquals(1, locator.locateCalls));
    }

    @Test
    void wirelessLocatorRejectsUnsupportedGuiBeforeHostLookup() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingWirelessLocator locator = new RecordingWirelessLocator();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ThEGuiOpener.locateWirelessArcaneHost(player, ModGUIs.ARCANE_TERMINAL, locator));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(ModGUIs.ARCANE_TERMINAL.name())),
                () -> assertTrue(exception.getMessage().contains("locator-aware")),
                () -> assertEquals(0, locator.locateCalls));
    }

    private static final class RecordingWirelessLocator implements GuiHostLocator {

        private Class<?> requestedHostType;
        private int locateCalls;

        @Override
        public <T> T locate(EntityPlayer player, Class<T> hostInterface) {
            this.requestedHostType = hostInterface;
            this.locateCalls++;
            return null;
        }
    }

}
