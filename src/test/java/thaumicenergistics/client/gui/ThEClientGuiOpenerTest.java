package thaumicenergistics.client.gui;

import ae2.api.storage.ILinkStatus;
import ae2.api.storage.MEStorage;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.util.IConfigManager;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.style.GuiStyleManager;
import ae2.container.ISubGui;
import ae2.core.gui.locator.GuiHostLocator;
import ae2.core.gui.locator.ItemGuiHostLocator;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.LanguageManager;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.client.gui.block.GuiArcaneAssembler;
import thaumicenergistics.client.gui.item.GuiKnowledgeCore;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.item.WirelessArcaneTerminalGuiHost;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.items.ItemWirelessArcaneTerminal;
import thaumicenergistics.network.packets.PacketOpenLocatorGUI;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ThEClientGuiOpenerTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
        initializeGuiStyles();
    }

    static void initializeGuiStyles() {
        TestResourceManager resourceManager = new TestResourceManager();
        new LanguageManager(new MetadataSerializer(), "en_us").onResourceManagerReload(resourceManager);
        GuiStyleManager.initialize(resourceManager);
    }

    @Test
    void arcaneTerminalClientFactoryCreatesContainerAndScreenWithPacketState() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        TestArcaneHost host = new TestArcaneHost(ModGUIs.ARCANE_TERMINAL);
        GuiHostLocator locator = new FixedHostLocator(host);
        GuiScreen screen = new GuiScreen() {
        };
        RealFactoryClientOpenContext client = new RealFactoryClientOpenContext(player, (container, message) -> screen);
        PacketOpenLocatorGUI packet = packet(ModGUIs.ARCANE_TERMINAL, locator, true, 91);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        ContainerArcaneTerm arcaneTerm = assertInstanceOf(ContainerArcaneTerm.class, client.openContainer);
        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.SUCCESS, result.status()),
                () -> assertSame(screen, client.displayedScreen),
                () -> assertSame(host, arcaneTerm.getHost()),
                () -> assertSame(locator, arcaneTerm.getLocator()),
                () -> assertTrue(arcaneTerm.isReturnedFromSubScreen()),
                () -> assertEquals(91, arcaneTerm.windowId),
                () -> assertSame(client.openContainer, client.screenFactoryContainer),
                () -> assertSame(packet, client.screenFactoryMessage));
    }

    @Test
    void arcaneInscriberClientFactoryCreatesContainerAndScreenWithPacketState() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        TestArcaneHost host = new TestArcaneHost(ModGUIs.ARCANE_INSCRIBER);
        GuiHostLocator locator = new FixedHostLocator(host);
        GuiScreen screen = new GuiScreen() {
        };
        RealFactoryClientOpenContext client = new RealFactoryClientOpenContext(player, (container, message) -> screen);
        PacketOpenLocatorGUI packet = packet(ModGUIs.ARCANE_INSCRIBER, locator, true, 92);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        ContainerArcaneInscriber inscriber = assertInstanceOf(ContainerArcaneInscriber.class, client.openContainer);
        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.SUCCESS, result.status()),
                () -> assertSame(screen, client.displayedScreen),
                () -> assertSame(host, inscriber.getHost()),
                () -> assertSame(locator, inscriber.getLocator()),
                () -> assertTrue(inscriber.isReturnedFromSubScreen()),
                () -> assertEquals(92, inscriber.windowId),
                () -> assertSame(client.openContainer, client.screenFactoryContainer),
                () -> assertSame(packet, client.screenFactoryMessage));
    }

    @Test
    void wirelessArcaneTerminalClientFactoryCreatesContainerAndScreenWithPacketState() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        ItemWirelessArcaneTerminal terminal = new TestWirelessArcaneTerminalItem(
                "wireless_arcane_terminal_client_opener_test");
        StackItemLocator itemLocator = new StackItemLocator(new ItemStack(terminal));
        WirelessArcaneTerminalGuiHost host = new TestWirelessArcaneTerminalGuiHost(
                terminal, terminal, player, itemLocator, (callbackPlayer, callbackSubGui) -> {
        });
        GuiHostLocator locator = new FixedHostLocator(host);
        GuiScreen screen = new GuiScreen() {
        };
        RealFactoryClientOpenContext client = new RealFactoryClientOpenContext(player, (container, message) -> screen);
        PacketOpenLocatorGUI packet = packet(ModGUIs.WIRELESS_ARCANE_TERMINAL, locator, true, 96);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        ContainerArcaneTerm arcaneTerm = assertInstanceOf(ContainerArcaneTerm.class, client.openContainer);
        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.SUCCESS, result.status()),
                () -> assertSame(screen, client.displayedScreen),
                () -> assertSame(host, arcaneTerm.getHost()),
                () -> assertSame(locator, arcaneTerm.getLocator()),
                () -> assertTrue(arcaneTerm.isReturnedFromSubScreen()),
                () -> assertEquals(96, arcaneTerm.windowId),
                () -> assertSame(client.openContainer, client.screenFactoryContainer),
                () -> assertSame(packet, client.screenFactoryMessage));
    }

    @Test
    void arcaneAssemblerClientFactoryCreatesContainerAndScreen() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        TileArcaneAssembler host = new TileArcaneAssembler();
        GuiHostLocator locator = new FixedHostLocator(host);
        RealFactoryClientOpenContext client = new RealFactoryClientOpenContext(player);
        PacketOpenLocatorGUI packet = packet(ModGUIs.ARCANE_ASSEMBLER, locator, false, 93);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        ContainerArcaneAssembler container = assertInstanceOf(ContainerArcaneAssembler.class, client.openContainer);
        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.SUCCESS, result.status()),
                () -> assertInstanceOf(GuiArcaneAssembler.class, client.displayedScreen),
                () -> assertInstanceOf(AEBaseGui.class, client.displayedScreen),
                () -> assertSame(host, container.getTE()),
                () -> assertSame(locator, container.getLocator()),
                () -> assertEquals(93, container.windowId));
    }

    @Test
    void knowledgeCoreClientFactoryUsesOpenInscriberParentBeforeReplacementForEachAction() {
        for (ModGUIs action : List.of(
                ModGUIs.KNOWLEDGE_CORE_ADD,
                ModGUIs.KNOWLEDGE_CORE_DEL,
                ModGUIs.KNOWLEDGE_CORE_VIEW)) {
            FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
            TestArcaneHost host = new TestArcaneHost(ModGUIs.ARCANE_INSCRIBER);
            GuiHostLocator locator = new FixedHostLocator(host);
            ContainerArcaneInscriber parent = new ContainerArcaneInscriber(player.inventory, host);
            parent.setLocator(locator);
            player.openContainer = parent;
            RealFactoryClientOpenContext client = new RealFactoryClientOpenContext(player);
            client.openContainer = parent;
            int windowId = 94 + action.ordinal();
            PacketOpenLocatorGUI packet = packet(action, locator, false, windowId);

            ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

            ContainerKnowledgeCore container = assertInstanceOf(ContainerKnowledgeCore.class, client.openContainer);
            assertAll(
                    () -> assertSame(ThEClientGuiOpener.OpenStatus.SUCCESS, result.status()),
                    () -> assertInstanceOf(GuiKnowledgeCore.class, client.displayedScreen),
                    () -> assertSame(host, container.getHost()),
                    () -> assertSame(locator, container.getLocator()),
                    () -> assertSame(action, container.getGUIAction()),
                    () -> assertEquals(windowId, container.windowId));
        }
    }

    @Test
    void containerCreationFailureResetsContainerClosesScreenAndSendsClosePacket() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.createContainerFailure = new IllegalStateException("container failed");
        PacketOpenLocatorGUI packet = wirelessPacket(37);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.CONTAINER, result.stage()),
                () -> assertEquals(List.of(37), client.closeWindowIds),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertTrue(client.screenClosed),
                () -> assertTrue(result.closePacketSent()),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    @Test
    void screenCreationFailureDoesNotInstallNewContainerAndRestoresInventoryContainer() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.createdContainer = new TestContainer();
        client.createScreenFailure = new IllegalStateException("screen failed");
        PacketOpenLocatorGUI packet = wirelessPacket(41);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.SCREEN, result.stage()),
                () -> assertEquals(41, client.createdContainer.windowId),
                () -> assertFalse(client.installedContainers.contains(client.createdContainer)),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertEquals(List.of(41), client.closeWindowIds),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    @Test
    void displayFailureRestoresInventoryContainerAfterInstallingNewContainer() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.createdContainer = new TestContainer();
        client.displayFailure = new IllegalStateException("display failed");
        PacketOpenLocatorGUI packet = wirelessPacket(53);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.DISPLAY, result.stage()),
                () -> assertEquals(53, client.createdContainer.windowId),
                () -> assertSame(client.createdContainer, client.installedContainers.get(0)),
                () -> assertSame(client.inventoryContainer, client.installedContainers.get(1)),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertEquals(List.of(53), client.closeWindowIds),
                () -> assertTrue(client.screenClosed),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    @Test
    void missingConnectionReportsDiagnosticAndStillResetsLocalState() {
        TestClientOpenContext client = new TestClientOpenContext();
        client.connectionPresent = false;
        client.createContainerFailure = new IllegalStateException("container failed");
        PacketOpenLocatorGUI packet = wirelessPacket(79);

        ThEClientGuiOpener.OpenResult result = ThEClientGuiOpener.openLocatorGui(client, packet);

        assertAll(
                () -> assertSame(ThEClientGuiOpener.OpenStatus.FAILED, result.status()),
                () -> assertSame(ThEClientGuiOpener.OpenStage.CONTAINER, result.stage()),
                () -> assertEquals(1, client.closeWindowAttempts),
                () -> assertTrue(client.closeWindowIds.isEmpty()),
                () -> assertFalse(result.closePacketSent()),
                () -> assertTrue(result.connectionMissing()),
                () -> assertTrue(result.diagnostic().contains("connection")),
                () -> assertSame(client.inventoryContainer, client.openContainer),
                () -> assertTrue(result.openContainerReset()),
                () -> assertTrue(result.screenClosed()));
    }

    private static PacketOpenLocatorGUI wirelessPacket(int windowId) {
        return new PacketOpenLocatorGUI(
                ModGUIs.WIRELESS_ARCANE_TERMINAL,
                new FixedHostLocator(new TestArcaneHost(ModGUIs.WIRELESS_ARCANE_TERMINAL)),
                false,
                windowId);
    }

    private static PacketOpenLocatorGUI packet(ModGUIs gui, GuiHostLocator locator, boolean returnedFromSubScreen,
                                               int windowId) {
        return new PacketOpenLocatorGUI(gui, locator, returnedFromSubScreen, windowId);
    }

    private static final class RealFactoryClientOpenContext implements ThEClientGuiOpener.ClientOpenContext {

        private final FakeMinecraft.FakePlayer player;
        private final Container inventoryContainer;
        private final BiFunction<Container, PacketOpenLocatorGUI, GuiScreen> screenFactory;
        private final List<Integer> closeWindowIds = new ArrayList<>();
        private final List<Container> installedContainers = new ArrayList<>();
        private Container openContainer;
        private GuiScreen displayedScreen;
        private Container screenFactoryContainer;
        private PacketOpenLocatorGUI screenFactoryMessage;
        private boolean screenClosed;

        private RealFactoryClientOpenContext(FakeMinecraft.FakePlayer player) {
            this(player, null);
        }

        private RealFactoryClientOpenContext(FakeMinecraft.FakePlayer player,
                                             BiFunction<Container, PacketOpenLocatorGUI, GuiScreen> screenFactory) {
            this.player = player;
            this.inventoryContainer = player.inventoryContainer;
            this.openContainer = player.openContainer;
            this.screenFactory = screenFactory;
        }

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String describePlayer() {
            return this.player.getClass().getName();
        }

        @Override
        public Container inventoryContainer() {
            return this.inventoryContainer;
        }

        @Override
        public void setOpenContainer(Container container) {
            this.openContainer = container;
            this.player.openContainer = container;
            this.installedContainers.add(container);
        }

        @Override
        public Container createContainer(PacketOpenLocatorGUI message) {
            return ThEClientGuiOpener.createClientContainer(this.player, this.openContainer, message);
        }

        @Override
        public GuiScreen createScreen(Container container, PacketOpenLocatorGUI message) {
            this.screenFactoryContainer = container;
            this.screenFactoryMessage = message;
            if (this.screenFactory != null) {
                return this.screenFactory.apply(container, message);
            }
            return ThEClientGuiOpener.createClientScreen(this.player, container, message);
        }

        @Override
        public void displayScreen(GuiScreen screen) {
            this.displayedScreen = screen;
        }

        @Override
        public boolean sendCloseWindow(int windowId) {
            this.closeWindowIds.add(windowId);
            return true;
        }

        @Override
        public void closeScreen() {
            this.screenClosed = true;
        }
    }

    private static final class TestClientOpenContext implements ThEClientGuiOpener.ClientOpenContext {

        private final TestContainer inventoryContainer = new TestContainer();
        private final List<Integer> closeWindowIds = new ArrayList<>();
        private final List<Container> installedContainers = new ArrayList<>();
        private Container openContainer = new TestContainer();
        private Container createdContainer = new TestContainer();
        private RuntimeException createContainerFailure;
        private RuntimeException createScreenFailure;
        private RuntimeException displayFailure;
        private boolean connectionPresent = true;
        private boolean screenClosed;
        private int closeWindowAttempts;

        @Override
        public boolean hasPlayer() {
            return true;
        }

        @Override
        public String describePlayer() {
            return "test-client-player";
        }

        @Override
        public Container inventoryContainer() {
            return this.inventoryContainer;
        }

        @Override
        public void setOpenContainer(Container container) {
            this.openContainer = container;
            this.installedContainers.add(container);
        }

        @Override
        public Container createContainer(PacketOpenLocatorGUI message) {
            if (this.createContainerFailure != null) {
                throw this.createContainerFailure;
            }
            return this.createdContainer;
        }

        @Override
        public GuiScreen createScreen(Container container, PacketOpenLocatorGUI message) {
            if (this.createScreenFailure != null) {
                throw this.createScreenFailure;
            }
            return new GuiScreen() {
            };
        }

        @Override
        public void displayScreen(GuiScreen screen) {
            if (this.displayFailure != null) {
                throw this.displayFailure;
            }
        }

        @Override
        public boolean sendCloseWindow(int windowId) {
            this.closeWindowAttempts++;
            if (!this.connectionPresent) {
                return false;
            }
            this.closeWindowIds.add(windowId);
            return true;
        }

        @Override
        public void closeScreen() {
            this.screenClosed = true;
        }
    }

    private static final class TestContainer extends Container {

        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return true;
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

    private static final class StackItemLocator implements ItemGuiHostLocator {

        private final ItemStack stack;

        private StackItemLocator(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack locateItem(EntityPlayer player) {
            return this.stack;
        }
    }

    private static final class TestWirelessArcaneTerminalItem extends ItemWirelessArcaneTerminal {

        private TestWirelessArcaneTerminalItem(String id) {
            super(id);
        }

        @Override
        public IUpgradeInventory getUpgrades(ItemStack stack) {
            return UpgradeInventories.empty();
        }
    }

    private static final class TestWirelessArcaneTerminalGuiHost extends WirelessArcaneTerminalGuiHost {

        private TestWirelessArcaneTerminalGuiHost(ItemWirelessArcaneTerminal stackItem,
                                                  ItemWirelessArcaneTerminal terminalItem,
                                                  EntityPlayer player,
                                                  ItemGuiHostLocator locator,
                                                  BiConsumer<EntityPlayer, ISubGui> returnToMainContainer) {
            super(stackItem, terminalItem, player, locator, returnToMainContainer);
        }

        @Override
        protected void updateConnectedAccessPoint() {
        }

        @Override
        protected void updateLinkStatus() {
        }
    }

    private static final class TestArcaneHost extends TileEntity implements IArcaneTerminalHost {

        private final ModGUIs gui;
        private final ThEInternalInventory craftingInventory = new ThEInternalInventory("crafting", 15, 64);
        private final ThEUpgradeInventory upgradeInventory =
                new ThEUpgradeInventory("upgrades", 1, 1, new ItemStack(net.minecraft.init.Items.STICK));

        private TestArcaneHost(ModGUIs gui) {
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
        public IUpgradeInventory getArcaneUpgradeInventory() {
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
        public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
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
                    return new TextComponentString("client opener test storage");
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

    private static final class TestResourceManager implements IResourceManager {

        private static final Path RESOURCE_ROOT = Path.of("src/main/resources").toAbsolutePath().normalize();
        private static final ResourceLocation TERMINAL_STYLE =
                new ResourceLocation("ae2", "screens/terminals/terminal.json");
        private static final String TERMINAL_STYLE_WITH_AUTO_CRAFTING_DISABLED = """
                {
                  "$schema": "../schema.json",
                  "includes": ["base_terminal.json"],
                  "text": {
                    "dialog_title": {
                      "text": {
                        "translate": "gui.ae2.Terminal"
                      },
                      "position": {
                        "left": 8,
                        "top": 6
                      }
                    }
                  },
                  "terminalStyle": {
                    "supportsAutoCrafting": false
                  }
                }
                """;

        @Override
        public Set<String> getResourceDomains() {
            return Set.of("ae2", "thaumicenergistics");
        }

        @Override
        public IResource getResource(ResourceLocation location) throws IOException {
            if (TERMINAL_STYLE.equals(location)) {
                return new StringResource(location, TERMINAL_STYLE_WITH_AUTO_CRAFTING_DISABLED);
            }

            Path resourcePath = RESOURCE_ROOT
                    .resolve("assets")
                    .resolve(location.getNamespace())
                    .resolve(location.getPath())
                    .normalize();
            if (resourcePath.startsWith(RESOURCE_ROOT) && Files.isRegularFile(resourcePath)) {
                return new FileResource(location, resourcePath);
            }

            URL classpathResource = TestResourceManager.class.getClassLoader().getResource(classpathName(location));
            if (classpathResource != null) {
                return new ClasspathResource(location, classpathResource);
            }

            throw new FileNotFoundException(location.toString());
        }

        @Override
        public List<IResource> getAllResources(ResourceLocation location) throws IOException {
            return List.of(this.getResource(location));
        }

        private static String classpathName(ResourceLocation location) {
            return "assets/" + location.getNamespace() + "/" + location.getPath();
        }
    }

    private static final class StringResource implements IResource {

        private final ResourceLocation location;
        private final String content;

        private StringResource(ResourceLocation location, String content) {
            this.location = location;
            this.content = content;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return this.location;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(this.content.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Nullable
        @Override
        public <T extends IMetadataSection> T getMetadata(String sectionName) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return RESOURCE_PACK_NAME;
        }

        @Override
        public void close() {
        }
    }

    private static final class FileResource implements IResource {

        private final ResourceLocation location;
        private final Path path;

        private FileResource(ResourceLocation location, Path path) {
            this.location = location;
            this.path = path;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return this.location;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return Files.newInputStream(this.path);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to open test resource: " + this.path, e);
            }
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Nullable
        @Override
        public <T extends IMetadataSection> T getMetadata(String sectionName) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return RESOURCE_PACK_NAME;
        }

        @Override
        public void close() {
        }
    }

    private static final class ClasspathResource implements IResource {

        private final ResourceLocation location;
        private final URL resource;

        private ClasspathResource(ResourceLocation location, URL resource) {
            this.location = location;
            this.resource = resource;
        }

        @Override
        public ResourceLocation getResourceLocation() {
            return this.location;
        }

        @Override
        public InputStream getInputStream() {
            try {
                return this.resource.openStream();
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to open classpath test resource: " + this.resource, e);
            }
        }

        @Override
        public boolean hasMetadata() {
            return false;
        }

        @Nullable
        @Override
        public <T extends IMetadataSection> T getMetadata(String sectionName) {
            return null;
        }

        @Override
        public String getResourcePackName() {
            return "classpath";
        }

        @Override
        public void close() {
        }
    }

    private static final String RESOURCE_PACK_NAME = "client-opener-test";
}
