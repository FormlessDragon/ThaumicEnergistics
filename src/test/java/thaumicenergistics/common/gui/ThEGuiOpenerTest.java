package thaumicenergistics.common.gui;

import ae2.api.storage.ILinkStatus;
import ae2.api.storage.MEStorage;
import ae2.api.util.IConfigManager;
import ae2.container.AEBaseContainer;
import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
    void ordinaryArcaneTerminalInstallsServerContainerAndLocatorPacket() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ArcaneTerminalHost host = new ArcaneTerminalHost(ModGUIs.ARCANE_TERMINAL);
        FixedHostLocator locator = new FixedHostLocator(host);
        RecordingServerOpenContext context = new RecordingServerOpenContext(player, 42);

        ThEGuiOpener.openLocatorGui(context, ModGUIs.ARCANE_TERMINAL, locator, true);

        ContainerArcaneTerm container = assertInstanceOf(ContainerArcaneTerm.class, context.installedContainer);
        assertAll(
                () -> assertEquals(1, context.closeCalls),
                () -> assertEquals(1, context.nextWindowIdCalls),
                () -> assertEquals(1, context.sendCalls),
                () -> assertSame(host, container.getHost()),
                () -> assertSame(locator, container.getLocator()),
                () -> assertTrue(container.isReturnedFromSubScreen()),
                () -> assertEquals(42, container.windowId),
                () -> assertSame(container, player.openContainer),
                () -> assertSame(ModGUIs.ARCANE_TERMINAL, context.sentPacket.gui()),
                () -> assertSame(locator, context.sentPacket.locator()),
                () -> assertTrue(context.sentPacket.returnedFromSubScreen()),
                () -> assertEquals(42, context.sentPacket.windowId()));
    }

    @Test
    void ordinaryArcaneInscriberInstallsServerContainerWithHostAndLocator() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ArcaneTerminalHost host = new ArcaneTerminalHost(ModGUIs.ARCANE_INSCRIBER);
        FixedHostLocator locator = new FixedHostLocator(host);
        RecordingServerOpenContext context = new RecordingServerOpenContext(player, 43);

        ThEGuiOpener.openLocatorGui(context, ModGUIs.ARCANE_INSCRIBER, locator, false);

        ContainerArcaneInscriber container = assertInstanceOf(ContainerArcaneInscriber.class,
                context.installedContainer);
        assertAll(
                () -> assertSame(host, container.getHost()),
                () -> assertSame(locator, container.getLocator()),
                () -> assertEquals(43, container.windowId),
                () -> assertSame(host, container.getLocator().locate(player, IArcaneTerminalHost.class)));
    }

    @Test
    void ordinaryArcaneAssemblerInstallsServerContainerWithTileLocator() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TileArcaneAssembler host = new TileArcaneAssembler();
        FixedHostLocator locator = new FixedHostLocator(host);
        RecordingServerOpenContext context = new RecordingServerOpenContext(player, 44);

        ThEGuiOpener.openLocatorGui(context, ModGUIs.ARCANE_ASSEMBLER, locator, false);

        ContainerArcaneAssembler container = assertInstanceOf(ContainerArcaneAssembler.class,
                context.installedContainer);
        assertAll(
                () -> assertSame(host, container.getTE()),
                () -> assertSame(locator, container.getLocator()),
                () -> assertEquals(44, container.windowId),
                () -> assertSame(host, container.getLocator().locate(player, TileArcaneAssembler.class)));
    }

    @Test
    void knowledgeCoreViewReadsParentContextBeforeClosingCurrentContainer() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ArcaneTerminalHost host = new ArcaneTerminalHost(ModGUIs.ARCANE_INSCRIBER);
        FixedHostLocator locator = new FixedHostLocator(host);
        ContainerArcaneInscriber parent = new ContainerArcaneInscriber(player.inventory, host);
        parent.setLocator(locator);
        player.openContainer = parent;
        RecordingServerOpenContext context = new RecordingServerOpenContext(player, 45);
        context.closeContainerReplacement = new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        };

        ThEGuiOpener.openLocatorGui(context, ModGUIs.KNOWLEDGE_CORE_VIEW, locator, false);

        ContainerKnowledgeCore container = assertInstanceOf(ContainerKnowledgeCore.class, context.installedContainer);
        assertAll(
                () -> assertSame(parent, context.openContainerObservedBeforeClose),
                () -> assertEquals(1, context.closeCalls),
                () -> assertSame(host, container.getHost()),
                () -> assertSame(locator, container.getLocator()),
                () -> assertEquals(45, container.windowId),
                () -> assertSame(ModGUIs.KNOWLEDGE_CORE_VIEW, container.getGUIAction()));
    }

    @Test
    void unsupportedLocatorGuiFailsFastWithRouteContext() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        FixedHostLocator locator = new FixedHostLocator(new ArcaneTerminalHost(ModGUIs.ARCANE_TERMINAL));
        RecordingServerOpenContext context = new RecordingServerOpenContext(player, 46);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ThEGuiOpener.openLocatorGui(context, ModGUIs.AE2_PRIORITY, locator, false));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(ModGUIs.AE2_PRIORITY.name()),
                        exception.getMessage()),
                () -> assertTrue(exception.getMessage().contains(locator.getClass().getName()),
                        exception.getMessage()),
                () -> assertTrue(exception.getMessage().contains(player.getClass().getName()),
                        exception.getMessage()),
                () -> assertEquals(0, context.closeCalls));
    }

    @Test
    void locatorAwareOpenRejectsNonServerPlayerWithRouteContext() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        GuiHostLocator locator = new FixedHostLocator(new ArcaneTerminalHost(ModGUIs.ARCANE_TERMINAL));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ThEGuiOpener.openLocatorGui(player, ModGUIs.ARCANE_TERMINAL, locator, false));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(ModGUIs.ARCANE_TERMINAL.name()),
                        exception.getMessage()),
                () -> assertTrue(exception.getMessage().contains(locator.getClass().getName()),
                        exception.getMessage()),
                () -> assertTrue(exception.getMessage().contains(player.getClass().getName()),
                        exception.getMessage()));
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

    private static final class RecordingServerOpenContext implements ThEGuiOpener.ServerOpenContext {

        private final FakeMinecraft.FakePlayer player;
        private final int nextWindowId;
        private int closeCalls;
        private int nextWindowIdCalls;
        private int sendCalls;
        private Container installedContainer;
        private PacketOpenLocatorGUI sentPacket;
        private Container closeContainerReplacement;
        private Container openContainerObservedBeforeClose;

        private RecordingServerOpenContext(FakeMinecraft.FakePlayer player, int nextWindowId) {
            this.player = player;
            this.nextWindowId = nextWindowId;
        }

        @Override
        public EntityPlayer player() {
            return this.player;
        }

        @Override
        public Container openContainer() {
            if (this.closeCalls == 0) {
                this.openContainerObservedBeforeClose = this.player.openContainer;
            }
            return this.player.openContainer;
        }

        @Override
        public void closeContainer() {
            this.closeCalls++;
            if (this.closeContainerReplacement != null) {
                this.player.openContainer = this.closeContainerReplacement;
            }
        }

        @Override
        public int nextWindowId() {
            this.nextWindowIdCalls++;
            return this.nextWindowId;
        }

        @Override
        public void send(PacketOpenLocatorGUI packet) {
            this.sendCalls++;
            this.sentPacket = packet;
        }

        @Override
        public void install(Container container, int windowId) {
            this.installedContainer = container;
            container.windowId = windowId;
            this.player.openContainer = container;
        }
    }

    private static final class FixedHostLocator implements GuiHostLocator {

        private final Object host;

        private FixedHostLocator(Object host) {
            this.host = host;
        }

        @Override
        public <T> T locate(EntityPlayer player, Class<T> hostInterface) {
            if (hostInterface.isInstance(this.host)) {
                return hostInterface.cast(this.host);
            }
            return null;
        }
    }

    private static final class ArcaneTerminalHost extends TileEntity implements IArcaneTerminalHost {

        private final ModGUIs gui;
        private final ThEInternalInventory craftingInventory = new ThEInternalInventory("crafting", 15, 64);
        private final ThEUpgradeInventory upgradeInventory =
                new ThEUpgradeInventory("upgrades", 1, 1, new ItemStack(net.minecraft.init.Items.STICK));

        private ArcaneTerminalHost(ModGUIs gui) {
            this.gui = gui;
        }

        @Override
        public ModGUIs getGui() {
            return this.gui;
        }

        @Override
        public IItemHandler getInventoryByName(String name) {
            return switch (name) {
                case "crafting" -> new InvWrapper(this.craftingInventory);
                case "upgrades" -> this.upgradeInventory.toItemHandler();
                default -> null;
            };
        }

        @Override
        public ae2.api.upgrades.IUpgradeInventory getArcaneUpgradeInventory() {
            return this.upgradeInventory;
        }

        @Override
        public boolean hasVisSource() {
            return false;
        }

        @Override
        public net.minecraft.world.World getVisWorld() {
            return null;
        }

        @Override
        public BlockPos getVisPos() {
            return BlockPos.ORIGIN;
        }

        @Override
        public BlockPos getReturnPos() {
            return BlockPos.ORIGIN;
        }

        @Override
        public EnumFacing getReturnSide() {
            return EnumFacing.UP;
        }

        @Override
        public void returnToMainContainer(EntityPlayer player, ae2.container.ISubGui subGui) {
            throw new UnsupportedOperationException("Test host should not open parent containers");
        }

        @Override
        public ItemStack getMainContainerIcon() {
            return ItemStack.EMPTY;
        }

        @Override
        public MEStorage getInventory() {
            return new MEStorage() {
                @Override
                public ITextComponent getDescription() {
                    return new TextComponentString("arcane terminal opener test storage");
                }
            };
        }

        @Override
        public ILinkStatus getLinkStatus() {
            return ILinkStatus.ofConnected();
        }

        @Override
        public IConfigManager getConfigManager() {
            return IConfigManager.builder(() -> {
            }).build();
        }
    }

}
