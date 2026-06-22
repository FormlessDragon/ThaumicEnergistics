package thaumicenergistics.init.internal;

import ae2.core.AEConfig;
import ae2.core.definitions.AEItems;
import ae2.recipes.game.StorageCellDisassemblyRecipe;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.common.util.CompoundDataFixer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.IFMLSidedHandler;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.StartupQuery;
import net.minecraftforge.fml.common.eventhandler.EventBus;
import net.minecraftforge.fml.relauncher.Side;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.core.definitions.ThEItems;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class EssentiaStorageCellDisassemblyTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
        bootstrapForgeSide();
        AEConfig.init();
    }

    @Test
    void essentiaStorageCellsDisassembleIntoAeHousingAndMatchingComponent() {
        InitStorageCells.registerEssentiaCellDisassembly();

        assertDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_1K.item()),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_1K.item()));
        assertDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_4K.item()),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_4K.item()));
        assertDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_16K.item()),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_16K.item()));
        assertDisassembly(
                Objects.requireNonNull(ThEItems.ESSENTIA_CELL_64K.item()),
                Objects.requireNonNull(ThEItems.ESSENTIA_COMPONENT_64K.item()));
    }

    private static void assertDisassembly(Item cell, Item component) {
        List<ItemStack> result = StorageCellDisassemblyRecipe.getDisassemblyResult(null, cell);

        assertEquals(2, result.size());
        assertSame(AEItems.ITEM_CELL_HOUSING.item(), result.get(0).getItem());
        assertEquals(1, result.get(0).getCount());
        assertSame(component, result.get(1).getItem());
        assertEquals(1, result.get(1).getCount());
    }

    private static void bootstrapForgeSide() {
        try {
            FMLCommonHandler.instance().beginLoading(new TestSidedHandler());
        } catch (RuntimeException e) {
            if (!isExpectedMappedJUnitBootstrapFailure(e)) {
                throw e;
            }
        }
        assertSame(Side.SERVER, FMLCommonHandler.instance().getSide());
    }

    private static boolean isExpectedMappedJUnitBootstrapFailure(RuntimeException e) {
        Throwable current = e;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if ("net.minecraftforge.fml.relauncher.ReflectionHelper$UnableToAccessFieldException".equals(className)
                    || "net.minecraftforge.fml.relauncher.ReflectionHelper$UnableToFindFieldException".equals(className)
                    || (message != null && message.contains("field_150915_c"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static final class TestSidedHandler implements IFMLSidedHandler {
        @Override
        public List<String> getAdditionalBrandingInformation() {
            return Collections.emptyList();
        }

        @Override
        public Side getSide() {
            return Side.SERVER;
        }

        @Override
        public void haltGame(String message, Throwable exception) {
            throw new RuntimeException(message, exception);
        }

        @Override
        public void showGuiScreen(Object clientGuiElement) {
            throw unsupported();
        }

        @Override
        public void queryUser(StartupQuery query) {
            throw unsupported();
        }

        @Override
        public void beginServerLoading(MinecraftServer server) {
            throw unsupported();
        }

        @Override
        public void finishServerLoading() {
            throw unsupported();
        }

        @Override
        public File getSavesDirectory() {
            throw unsupported();
        }

        @Override
        public MinecraftServer getServer() {
            throw unsupported();
        }

        @Override
        public boolean isDisplayCloseRequested() {
            throw unsupported();
        }

        @Override
        public boolean shouldServerShouldBeKilledQuietly() {
            throw unsupported();
        }

        @Override
        public void addModAsResource(ModContainer container) {
            throw unsupported();
        }

        @Override
        public String getCurrentLanguage() {
            throw unsupported();
        }

        @Override
        public void serverStopped() {
            throw unsupported();
        }

        @Override
        public NetworkManager getClientToServerNetworkManager() {
            throw unsupported();
        }

        @Override
        public INetHandler getClientPlayHandler() {
            throw unsupported();
        }

        @Override
        public void fireNetRegistrationEvent(
                EventBus bus,
                NetworkManager manager,
                Set<String> channelSet,
                String channel,
                Side side) {
            throw unsupported();
        }

        @Override
        public boolean shouldAllowPlayerLogins() {
            throw unsupported();
        }

        @Override
        public void allowLogins() {
            throw unsupported();
        }

        @Override
        public IThreadListener getWorldThread(INetHandler net) {
            throw unsupported();
        }

        @Override
        public void processWindowMessages() {
            throw unsupported();
        }

        @Override
        public String stripSpecialChars(String message) {
            return message;
        }

        @Override
        public void reloadRenderers() {
            throw unsupported();
        }

        @Override
        public void fireSidedRegistryEvents() {
            throw unsupported();
        }

        @Override
        public CompoundDataFixer getDataFixer() {
            throw unsupported();
        }

        @Override
        public boolean isDisplayVSyncForced() {
            throw unsupported();
        }

        private static UnsupportedOperationException unsupported() {
            return new UnsupportedOperationException("Unexpected Forge sided handler call during storage cell disassembly test");
        }
    }
}
