package thaumicenergistics.container;

import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.config.SortOrder;
import ae2.api.util.IConfigManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import org.junit.jupiter.api.Test;
import thaumicenergistics.config.AESettings;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerBaseConfigurableTest {

    @Test
    void detectAndSendChangesRejectsNullServerValueBeforeClientManagerCanStoreIt() throws Throwable {
        runOnServerThread(() -> {
            TestContainer container = new TestContainer(new NullSortByConfigManager());
            container.attachListener(new FakeListener());

            IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                    container::detectAndSendChanges);

            assertTrue(thrown.getMessage().contains(Settings.SORT_BY.getName()));
            assertEquals(SortOrder.NAME, container.clientManager().getSetting(Settings.SORT_BY));
        });
    }

    @Test
    void detectAndSendChangesSkipsConfigSyncUntilListenerIsAttached() throws Throwable {
        runOnServerThread(() -> {
            IConfigManager serverManager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
            });
            TestContainer container = new TestContainer(serverManager);
            serverManager.putSetting(Settings.SORT_BY, SortOrder.AMOUNT);

            container.detectAndSendChanges();

            assertEquals(SortOrder.NAME, container.clientManager().getSetting(Settings.SORT_BY));
        });
    }

    @Test
    void detectAndSendChangesSyncsServerManagerChangesToClientManager() throws Throwable {
        runOnServerThread(() -> {
            IConfigManager serverManager = AESettings.createConfigManager(AESettings.SUBJECT.ARCANE_TERMINAL, () -> {
            });
            TestContainer container = new TestContainer(serverManager);
            container.attachListener(new FakeListener());
            serverManager.putSetting(Settings.SORT_BY, SortOrder.AMOUNT);

            container.detectAndSendChanges();

            assertEquals(SortOrder.AMOUNT, container.clientManager().getSetting(Settings.SORT_BY));
        });
    }

    private static void runOnServerThread(ThrowingRunnable runnable) throws Throwable {
        Throwable[] failure = new Throwable[1];
        Thread thread = SidedThreadGroups.SERVER.newThread(() -> {
            try {
                runnable.run();
            } catch (Throwable e) {
                failure[0] = e;
            }
        });
        thread.start();
        thread.join();
        if (failure[0] != null) {
            throw failure[0];
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }

    private static final class TestContainer extends ContainerBaseConfigurable {
        private TestContainer(IConfigManager serverConfigManager) {
            super(null, serverConfigManager);
        }

        private void attachListener(IContainerListener listener) {
            this.listeners.add(listener);
        }

        private IConfigManager clientManager() {
            return this.clientConfigManager;
        }

        @Override
        protected AESettings.SUBJECT getAESettingSubject() {
            return AESettings.SUBJECT.ARCANE_TERMINAL;
        }
    }

    private static final class FakeListener implements IContainerListener {
        @Override
        public void sendAllContents(Container containerToSend, NonNullList<ItemStack> itemsList) {
        }

        @Override
        public void sendSlotContents(Container containerToSend, int slotInd, ItemStack stack) {
        }

        @Override
        public void sendWindowProperty(Container containerIn, int varToUpdate, int newValue) {
        }

        @Override
        public void sendAllWindowProperties(Container containerIn, IInventory inventory) {
        }
    }

    private static final class NullSortByConfigManager implements IConfigManager {
        @Override
        public Set<Setting<?>> getSettings() {
            return Set.of(Settings.SORT_BY);
        }

        @Override
        public <T extends Enum<T>> T getSetting(Setting<T> setting) {
            return null;
        }

        @Override
        public <T extends Enum<T>> void putSetting(Setting<T> setting, T newValue) {
            throw new UnsupportedOperationException("Test server manager is read-only");
        }

        @Override
        public void writeToNBT(NBTTagCompound destination) {
            throw new UnsupportedOperationException("Test server manager does not write NBT");
        }

        @Override
        public void readFromNBT(NBTTagCompound src) {
            throw new UnsupportedOperationException("Test server manager does not read NBT");
        }

        @Override
        public boolean importSettings(Map<String, String> settings) {
            throw new UnsupportedOperationException("Test server manager does not import settings");
        }

        @Override
        public Map<String, String> exportSettings() {
            throw new UnsupportedOperationException("Test server manager does not export settings");
        }
    }
}
