package thaumicenergistics.me.cell;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AEKeyTypes;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.core.AEConfig;
import ae2.items.storage.StorageCellTooltipComponent;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Blocks;
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
import thaumcraft.api.aspects.Aspect;
import thaumicenergistics.items.CreativeEssentiaCell;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.me.key.AEEssentiaKeys;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreativeEssentiaCellHandlerTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
        bootstrapForgeSide();
        AEConfig.init();
        registerEssentiaKeyType();
    }

    @Test
    void tooltipDataIsEmptyForNonCellStack() {
        Optional<StorageCellTooltipComponent> tooltipData = CreativeEssentiaCellHandler.INSTANCE.getTooltipData(
                new ItemStack(Blocks.STONE));

        assertFalse(tooltipData.isPresent());
    }

    @Test
    void singletonRecognizesCreativeEssentiaCellInventory() {
        ItemStack stack = new ItemStack(new CreativeEssentiaCell());

        assertTrue(CreativeEssentiaCellHandler.INSTANCE.isCell(stack));
        assertInstanceOf(
                CreativeEssentiaCellInventory.class,
                CreativeEssentiaCellHandler.INSTANCE.getCellInventory(stack, null));
    }

    @Test
    void itemTooltipDataUsesLocalHandlerForUnconfiguredCell() {
        CreativeEssentiaCell item = new CreativeEssentiaCell();
        Optional<StorageCellTooltipComponent> tooltipData = item.getStackTooltipData(new ItemStack(item));

        assertTrue(tooltipData.isPresent());
        StorageCellTooltipComponent component = tooltipData.orElseThrow();
        assertTrue(component.content().isEmpty());
        assertTrue(component.upgrades().isEmpty());
        assertFalse(component.hasMoreContent());
        assertFalse(component.showAmounts());
    }

    @Test
    void creativeEssentiaCellAddInformationUsesConfiguredTooltipHook() {
        CreativeEssentiaCell item = new TooltipHookProbeCreativeEssentiaCell();
        ItemStack stack = new ItemStack(item);
        List<String> lines = new ArrayList<>();

        item.addInformation(stack, null, lines, ITooltipFlag.TooltipFlags.NORMAL);

        assertTrue(lines.contains(TooltipHookProbeCreativeEssentiaCell.SENTINEL));
    }

    @Test
    void creativeEssentiaCellInventoryInsertGuardsInvalidRequestsAndHonorsConfiguredKeys() {
        AEEssentiaKey configuredKey = essentiaKey(Aspect.AIR);
        AEEssentiaKey unconfiguredKey = essentiaKey(Aspect.FIRE);
        CreativeEssentiaCellInventory inventory = configuredInventory(configuredKey);

        assertEquals(0, inventory.insert(null, 5, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, inventory.insert(configuredKey, 0, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, inventory.insert(configuredKey, -5, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(5, inventory.insert(configuredKey, 5, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, inventory.insert(unconfiguredKey, 5, Actionable.MODULATE, IActionSource.empty()));
    }

    @Test
    void creativeEssentiaCellInventoryExtractGuardsInvalidRequestsAndHonorsConfiguredKeys() {
        AEEssentiaKey configuredKey = essentiaKey(Aspect.AIR);
        AEEssentiaKey unconfiguredKey = essentiaKey(Aspect.FIRE);
        CreativeEssentiaCellInventory inventory = configuredInventory(configuredKey);

        assertEquals(0, inventory.extract(null, 5, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, inventory.extract(configuredKey, 0, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, inventory.extract(configuredKey, -5, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(5, inventory.extract(configuredKey, 5, Actionable.MODULATE, IActionSource.empty()));
        assertEquals(0, inventory.extract(unconfiguredKey, 5, Actionable.MODULATE, IActionSource.empty()));
    }

    @Test
    void creativeEssentiaCellInventoryAvailableStacksAreEmptyWhenUnconfigured() {
        CreativeEssentiaCellInventory inventory = new CreativeEssentiaCellInventory(
                new ItemStack(new CreativeEssentiaCell()));
        KeyCounter available = new KeyCounter();

        inventory.getAvailableStacks(available);

        assertTrue(available.isEmpty());
    }

    @Test
    void creativeEssentiaCellInventoryAvailableStacksExposeOnlyConfiguredKeys() {
        AEEssentiaKey configuredKey = essentiaKey(Aspect.AIR);
        AEEssentiaKey unconfiguredKey = essentiaKey(Aspect.FIRE);
        CreativeEssentiaCellInventory inventory = configuredInventory(configuredKey);
        KeyCounter available = new KeyCounter();

        inventory.getAvailableStacks(available);

        assertEquals(Long.MAX_VALUE, available.get(configuredKey));
        assertEquals(0, available.get(unconfiguredKey));
        assertEquals(1, available.size());
    }

    @Test
    void creativeEssentiaCellInventoryCanFitInsideCellMatchesConfiguration() {
        CreativeEssentiaCellInventory unconfiguredInventory = new CreativeEssentiaCellInventory(
                new ItemStack(new CreativeEssentiaCell()));
        CreativeEssentiaCellInventory configuredInventory = configuredInventory(essentiaKey(Aspect.AIR));

        assertTrue(unconfiguredInventory.canFitInsideCell());
        assertFalse(configuredInventory.canFitInsideCell());
    }

    private static CreativeEssentiaCellInventory configuredInventory(AEKey configuredKey) {
        CreativeEssentiaCell item = new CreativeEssentiaCell();
        ItemStack stack = new ItemStack(item);
        item.getConfigInventory(stack).setStack(0, new GenericStack(configuredKey, 0));
        return new CreativeEssentiaCellInventory(stack);
    }

    private static AEEssentiaKey essentiaKey(Aspect aspect) {
        AEEssentiaKey key = AEEssentiaKey.of(aspect);
        assertNotNull(key);
        return key;
    }

    private static void registerEssentiaKeyType() {
        boolean registered = AEKeyTypes.getAll().stream()
                .anyMatch(keyType -> AEEssentiaKeys.ID.equals(keyType.getId()));
        if (!registered) {
            AEKeyTypes.register(AEEssentiaKeys.INSTANCE);
        }
        assertSame(AEEssentiaKeys.INSTANCE, AEKeyTypes.get(AEEssentiaKeys.ID));
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

    private static final class TooltipHookProbeCreativeEssentiaCell extends CreativeEssentiaCell {
        private static final String SENTINEL = "tooltip-hook-called";

        @Override
        public void addToTooltip(ItemStack stack, List<String> lines) {
            lines.add(SENTINEL);
        }
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
            return new UnsupportedOperationException("Unexpected Forge sided handler call during creative cell test");
        }
    }
}
