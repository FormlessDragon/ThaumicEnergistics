package thaumicenergistics.client.gui;

import ae2.api.config.Settings;
import ae2.api.config.SortOrder;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.Test;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.container.ContainerBase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiBaseTest {

    @Test
    void updateSettingRejectsNullValueBeforeSupergiantManagerCanStoreIt() {
        TestContainer container = new TestContainer(AESettings.SUBJECT.ARCANE_TERMINAL);
        TestGui gui = new TestGui(container);

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> gui.updateSetting(Settings.SORT_BY, null));

        assertTrue(thrown.getMessage().contains(Settings.SORT_BY.getName()));
        assertEquals(SortOrder.NAME, container.getConfigManager().getSetting(Settings.SORT_BY));
    }

    @Test
    void hasConfigSettingUsesConcreteConfigManagerSettings() {
        TestContainer container = new TestContainer(AESettings.SUBJECT.ARCANE_TERMINAL);
        TestGui gui = new TestGui(container);

        assertTrue(gui.hasConfigSetting(Settings.SORT_BY));
        assertFalse(gui.hasConfigSetting(Settings.REDSTONE_CONTROLLED));
    }

    @Test
    void hasConfigSettingRejectsNonConfigurableContainer() {
        TestGui gui = new TestGui(new NonConfigurableContainer());

        assertFalse(gui.hasConfigSetting(Settings.SORT_BY));
    }

    @Test
    void updateSettingWritesConcreteConfigManager() {
        TestContainer container = new TestContainer(AESettings.SUBJECT.ARCANE_TERMINAL);
        TestGui gui = new TestGui(container);

        gui.updateSetting(Settings.SORT_BY, SortOrder.AMOUNT);

        assertEquals(SortOrder.AMOUNT, container.getConfigManager().getSetting(Settings.SORT_BY));
    }

    private static final class TestGui extends GuiBase {
        private TestGui(ContainerBase container) {
            super(container);
        }

        @Override
        protected ResourceLocation getGuiBackground() {
            return new ResourceLocation("thaumicenergistics", "textures/gui/test.png");
        }

        @Override
        protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        }
    }

    private static final class TestContainer extends ContainerBase implements IConfigurableObject {
        private final IConfigManager configManager;

        private TestContainer(AESettings.SUBJECT subject) {
            super(null);
            this.configManager = AESettings.createConfigManager(subject, () -> {
            });
        }

        @Override
        public IConfigManager getConfigManager() {
            return this.configManager;
        }
    }

    private static final class NonConfigurableContainer extends ContainerBase {
        private NonConfigurableContainer() {
            super(null);
        }
    }
}
