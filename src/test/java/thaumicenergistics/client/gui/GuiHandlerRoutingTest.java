package thaumicenergistics.client.gui;

import ae2.api.parts.IFacadeContainer;
import ae2.api.parts.IPart;
import ae2.api.parts.IPartHost;
import ae2.api.parts.SelectedPart;
import ae2.api.storage.ILinkStatus;
import ae2.api.storage.MEStorage;
import ae2.api.util.AEColor;
import ae2.api.util.AECableType;
import ae2.api.util.DimensionalBlockPos;
import ae2.api.util.IConfigManager;
import ae2.container.AEBaseContainer;
import ae2.container.ISubGui;
import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Bootstrap;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.client.gui.block.GuiArcaneAssembler;
import thaumicenergistics.client.gui.item.GuiKnowledgeCore;
import thaumicenergistics.common.gui.ThEGuiOpener;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.item.ContainerKnowledgeCore;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.part.ContainerArcaneTerm;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiHandlerRoutingTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void guiOrdinalRoundTripsAllCurrentEntriesAndSides() {
        for (ModGUIs gui : ModGUIs.values()) {
            for (EnumFacing side : EnumFacing.values()) {
                int ordinal = GuiHandler.calculateOrdinal(gui, side);

                assertAll(gui + " " + side,
                        () -> assertSame(gui, GuiHandler.getGUIFromOrdinal(ordinal)),
                        () -> assertSame(side, GuiHandler.getSideFromOrdinal(ordinal)));
            }
        }
    }

    @Test
    void phaseFiveOrdinalsRemainStable() {
        assertAll(
                () -> assertEquals(4, ModGUIs.ARCANE_TERMINAL.ordinal()),
                () -> assertEquals(5, ModGUIs.WIRELESS_ARCANE_TERMINAL.ordinal()),
                () -> assertEquals(6, ModGUIs.ARCANE_INSCRIBER.ordinal()),
                () -> assertEquals(7, ModGUIs.ARCANE_ASSEMBLER.ordinal()),
                () -> assertEquals(8, ModGUIs.AE2_CRAFT_AMOUNT.ordinal()),
                () -> assertEquals(9, ModGUIs.AE2_CRAFT_CONFIRM.ordinal()),
                () -> assertEquals(10, ModGUIs.AE2_CRAFT_STATUS.ordinal()),
                () -> assertEquals(12, ModGUIs.KNOWLEDGE_CORE_ADD.ordinal()),
                () -> assertEquals(13, ModGUIs.KNOWLEDGE_CORE_DEL.ordinal()),
                () -> assertEquals(14, ModGUIs.KNOWLEDGE_CORE_VIEW.ordinal()));
    }

    @Test
    void nullSideDefaultsToUp() {
        int ordinal = GuiHandler.calculateOrdinal(ModGUIs.ARCANE_TERMINAL, null);

        assertAll(
                () -> assertSame(ModGUIs.ARCANE_TERMINAL, GuiHandler.getGUIFromOrdinal(ordinal)),
                () -> assertSame(EnumFacing.UP, GuiHandler.getSideFromOrdinal(ordinal)));
    }

    @Test
    void legacyCraftSubGuiIdsDoNotCreateLocalBridgeContainers() {
        GuiHandler handler = new GuiHandler();
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        ArcaneTerminalPart part = new ArcaneTerminalPart();
        TestPartHost host = new TestPartHost(part);
        BlockPos pos = new BlockPos(4, 5, 6);
        world.setTileEntity(pos, host);

        assertAll(
                () -> assertNull(handler.getServerGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.AE2_CRAFT_AMOUNT, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ())),
                () -> assertNull(handler.getServerGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.AE2_CRAFT_CONFIRM, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ())),
                () -> assertNull(handler.getServerGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.AE2_CRAFT_STATUS, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ())));
    }

    @Test
    void legacyCraftSubGuiIdsDoNotCreateLocalBridgeGuis() {
        GuiHandler handler = new GuiHandler();
        FakeMinecraft.FakeWorld world = FakeMinecraft.clientWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        ArcaneTerminalPart part = new ArcaneTerminalPart();
        TestPartHost host = new TestPartHost(part);
        BlockPos pos = new BlockPos(4, 5, 6);
        world.setTileEntity(pos, host);

        assertAll(
                () -> assertNull(handler.getClientGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.AE2_CRAFT_AMOUNT, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ())),
                () -> assertNull(handler.getClientGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.AE2_CRAFT_CONFIRM, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ())),
                () -> assertNull(handler.getClientGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.AE2_CRAFT_STATUS, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ())));
    }

    @Test
    void arcaneTerminalPartRouteUsesLocatorForServerContainer() {
        GuiHandler handler = new GuiHandler();
        BlockPos pos = new BlockPos(4, 5, 6);
        ArcaneTerminalPart serverPart = new ArcaneTerminalPart();
        FakeMinecraft.FakeWorld serverWorld = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer serverPlayer = FakeMinecraft.player(serverWorld);
        serverWorld.setTileEntity(pos, new TestPartHost(serverPart));

        Object serverElement = handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.ARCANE_TERMINAL, EnumFacing.NORTH),
                serverPlayer, serverWorld, pos.getX(), pos.getY(), pos.getZ());

        ContainerArcaneTerm serverContainer = assertInstanceOf(ContainerArcaneTerm.class, serverElement);
        assertAll(
                () -> assertSame(serverPart, serverContainer.getHost()),
                () -> assertSame(serverPart, locateArcaneHost(serverContainer, serverPlayer)));
    }

    @Test
    void arcaneInscriberPartRouteUsesLocatorForServerContainer() {
        GuiHandler handler = new GuiHandler();
        BlockPos pos = new BlockPos(7, 8, 9);
        ArcaneTerminalPart serverPart = new ArcaneTerminalPart();
        FakeMinecraft.FakeWorld serverWorld = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer serverPlayer = FakeMinecraft.player(serverWorld);
        serverWorld.setTileEntity(pos, new TestPartHost(serverPart));

        Object serverElement = handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.ARCANE_INSCRIBER, EnumFacing.NORTH),
                serverPlayer, serverWorld, pos.getX(), pos.getY(), pos.getZ());

        ContainerArcaneInscriber serverContainer = assertInstanceOf(ContainerArcaneInscriber.class, serverElement);
        assertAll(
                () -> assertSame(serverPart, serverContainer.getHost()),
                () -> assertSame(serverPart, locateArcaneHost(serverContainer, serverPlayer)));
    }

    @Test
    void knowledgeCoreRouteRequiresArcaneInscriberParentContainer() {
        GuiHandler handler = new GuiHandler();
        BlockPos pos = new BlockPos(8, 9, 10);
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        ArcaneTerminalPart routeHost = new ArcaneTerminalPart();
        world.setTileEntity(pos, new TestPartHost(routeHost));
        player.openContainer = new Container() {
            @Override
            public boolean canInteractWith(EntityPlayer playerIn) {
                return true;
            }
        };

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.KNOWLEDGE_CORE_ADD, EnumFacing.NORTH),
                player, world, pos.getX(), pos.getY(), pos.getZ()));

        assertAll(
                () -> assertTrue(error.getMessage().contains(ModGUIs.KNOWLEDGE_CORE_ADD.name()), error.getMessage()),
                () -> assertTrue(error.getMessage().contains(ContainerArcaneInscriber.class.getName()),
                        error.getMessage()));
    }

    @Test
    void knowledgeCoreRouteReusesParentLocatorAndHostForServerAndClientElements() {
        GuiHandler handler = new GuiHandler();
        BlockPos pos = new BlockPos(11, 12, 13);
        ArcaneTerminalPart serverPart = new ArcaneTerminalPart();
        FakeMinecraft.FakeWorld serverWorld = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer serverPlayer = FakeMinecraft.player(serverWorld);
        serverWorld.setTileEntity(pos, new TestPartHost(serverPart));
        ContainerArcaneInscriber serverParent = openParentInscriber(serverPlayer, serverPart);

        Object serverElement = handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.KNOWLEDGE_CORE_VIEW, EnumFacing.NORTH),
                serverPlayer, serverWorld, pos.getX(), pos.getY(), pos.getZ());

        ContainerKnowledgeCore serverContainer = assertInstanceOf(ContainerKnowledgeCore.class, serverElement);
        ArcaneTerminalPart clientPart = new ArcaneTerminalPart();
        FakeMinecraft.FakeWorld clientWorld = FakeMinecraft.clientWorld();
        FakeMinecraft.FakePlayer clientPlayer = FakeMinecraft.player(clientWorld);
        clientWorld.setTileEntity(pos, new TestPartHost(clientPart));
        ContainerArcaneInscriber clientParent = openParentInscriber(clientPlayer, clientPart);

        Object clientElement = handler.getClientGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.KNOWLEDGE_CORE_VIEW, EnumFacing.NORTH),
                clientPlayer, clientWorld, pos.getX(), pos.getY(), pos.getZ());

        GuiKnowledgeCore clientGui = assertInstanceOf(GuiKnowledgeCore.class, clientElement);
        ContainerKnowledgeCore clientContainer = assertInstanceOf(ContainerKnowledgeCore.class,
                clientGui.inventorySlots);
        assertAll(
                () -> assertSame(serverParent.getLocator(), serverContainer.getLocator()),
                () -> assertSame(serverPart, serverContainer.getHost()),
                () -> assertSame(serverPart, serverContainer.getLocator().locate(serverPlayer,
                        IArcaneTerminalHost.class)),
                () -> assertSame(clientParent.getLocator(), clientContainer.getLocator()),
                () -> assertSame(clientPart, clientContainer.getHost()),
                () -> assertSame(clientPart, clientContainer.getLocator().locate(clientPlayer,
                        IArcaneTerminalHost.class)));
    }

    @Test
    void knowledgeCoreRouteRejectsParentLocatorThatCannotResolveParentHost() {
        GuiHandler handler = new GuiHandler();
        BlockPos pos = new BlockPos(14, 15, 16);
        ArcaneTerminalPart routeHost = new ArcaneTerminalPart();
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        world.setTileEntity(pos, new TestPartHost(routeHost));
        ContainerArcaneInscriber parent = new ContainerArcaneInscriber(player.inventory, routeHost);
        parent.setLocator(new FixedArcaneHostLocator(null));
        player.openContainer = parent;

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.KNOWLEDGE_CORE_DEL, EnumFacing.NORTH),
                player, world, pos.getX(), pos.getY(), pos.getZ()));

        assertAll(
                () -> assertTrue(error.getMessage().contains(ModGUIs.KNOWLEDGE_CORE_DEL.name()), error.getMessage()),
                () -> assertTrue(error.getMessage().contains("locator"), error.getMessage()));
    }

    @Test
    void knowledgeCoreRouteRejectsRouteHostThatDiffersFromParentHost() {
        GuiHandler handler = new GuiHandler();
        BlockPos pos = new BlockPos(17, 18, 19);
        ArcaneTerminalPart parentHost = new ArcaneTerminalPart();
        ArcaneTerminalPart routeHost = new ArcaneTerminalPart();
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        world.setTileEntity(pos, new TestPartHost(routeHost));
        openParentInscriber(player, parentHost);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.KNOWLEDGE_CORE_VIEW, EnumFacing.NORTH),
                player, world, pos.getX(), pos.getY(), pos.getZ()));

        assertAll(
                () -> assertTrue(error.getMessage().contains(ModGUIs.KNOWLEDGE_CORE_VIEW.name()),
                        error.getMessage()),
                () -> assertTrue(error.getMessage().contains("parent"), error.getMessage()),
                () -> assertTrue(error.getMessage().contains("route"), error.getMessage()));
    }

    @Test
    void missingArcaneTerminalPartFailsFastWithRouteContext() {
        GuiHandler handler = new GuiHandler();
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        BlockPos pos = new BlockPos(1, 2, 3);

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.ARCANE_TERMINAL, EnumFacing.NORTH),
                player, world, pos.getX(), pos.getY(), pos.getZ()));

        assertRouteFailureMessage(error, ModGUIs.ARCANE_TERMINAL, pos, EnumFacing.NORTH);
    }

    @Test
    void nonArcaneInscriberPartFailsFastWithRouteContext() {
        GuiHandler handler = new GuiHandler();
        FakeMinecraft.FakeWorld world = FakeMinecraft.clientWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        BlockPos pos = new BlockPos(10, 11, 12);
        world.setTileEntity(pos, new TestPartHost(new NonArcanePart()));

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> handler.getClientGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.ARCANE_INSCRIBER, EnumFacing.NORTH),
                player, world, pos.getX(), pos.getY(), pos.getZ()));

        assertRouteFailureMessage(error, ModGUIs.ARCANE_INSCRIBER, pos, EnumFacing.NORTH);
    }

    @Test
    void arcaneAssemblerTileRouteDoesNotConsultPartPath() {
        GuiHandler handler = new GuiHandler();
        BlockPos pos = new BlockPos(13, 14, 15);
        FakeMinecraft.FakeWorld serverWorld = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer serverPlayer = FakeMinecraft.player(serverWorld);
        PartPathRejectingArcaneAssembler serverTile = new PartPathRejectingArcaneAssembler();
        serverWorld.setTileEntity(pos, serverTile);
        FakeMinecraft.FakeWorld clientWorld = FakeMinecraft.clientWorld();
        FakeMinecraft.FakePlayer clientPlayer = FakeMinecraft.player(clientWorld);
        PartPathRejectingArcaneAssembler clientTile = new PartPathRejectingArcaneAssembler();
        clientWorld.setTileEntity(pos, clientTile);

        Object serverElement = handler.getServerGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.ARCANE_ASSEMBLER, EnumFacing.NORTH),
                serverPlayer, serverWorld, pos.getX(), pos.getY(), pos.getZ());
        Object clientElement = handler.getClientGuiElement(
                GuiHandler.calculateOrdinal(ModGUIs.ARCANE_ASSEMBLER, EnumFacing.NORTH),
                clientPlayer, clientWorld, pos.getX(), pos.getY(), pos.getZ());

        ContainerArcaneAssembler serverContainer = assertInstanceOf(ContainerArcaneAssembler.class, serverElement);
        GuiArcaneAssembler clientGui = assertInstanceOf(GuiArcaneAssembler.class, clientElement);
        ContainerArcaneAssembler clientContainer = assertInstanceOf(ContainerArcaneAssembler.class,
                clientGui.inventorySlots);
        assertAll(
                () -> assertSame(serverTile, serverContainer.getTE()),
                () -> assertSame(serverTile, serverContainer.getLocator().locate(serverPlayer,
                        TileArcaneAssembler.class)),
                () -> assertSame(clientTile, clientContainer.getTE()),
                () -> assertSame(clientTile, clientContainer.getLocator().locate(clientPlayer,
                        TileArcaneAssembler.class)));
    }

    @Test
    void wirelessArcaneContainerFactoryInstallsLocatorReturnFlagAndWindowId() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ArcaneTerminalPart host = new ArcaneTerminalPart();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);

        ContainerArcaneTerm container = ThEGuiOpener.createWirelessArcaneContainer(
                player.inventory, host, locator, true, 77);

        assertAll(
                () -> assertSame(host, container.getHost()),
                () -> assertSame(locator, container.getLocator()),
                () -> assertTrue(container.isReturnedFromSubScreen()),
                () -> assertEquals(77, container.windowId),
                () -> assertSame(host, container.getLocator().locate(player, IArcaneTerminalHost.class)));
    }

    @Test
    void wirelessArcaneTerminalRejectsLegacySlotOnlyOpenPath() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> GuiHandler.openGUI(ModGUIs.WIRELESS_ARCANE_TERMINAL, player, 3));

        assertWirelessLegacyRouteFailure(exception);
    }

    @Test
    void wirelessArcaneTerminalRejectsGenericLegacyOpenPath() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> GuiHandler.openGUI(ModGUIs.WIRELESS_ARCANE_TERMINAL, player, new BlockPos(1, 2, 3),
                        EnumFacing.NORTH));

        assertWirelessLegacyRouteFailure(exception);
    }

    @Test
    void wirelessArcaneTerminalRejectsServerForgeRoute() {
        GuiHandler handler = new GuiHandler();
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        BlockPos pos = new BlockPos(1, 2, 3);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> handler.getServerGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.WIRELESS_ARCANE_TERMINAL, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ()));

        assertWirelessLegacyRouteFailure(exception);
    }

    @Test
    void wirelessArcaneTerminalRejectsClientForgeRoute() {
        GuiHandler handler = new GuiHandler();
        FakeMinecraft.FakeWorld world = FakeMinecraft.clientWorld();
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(world);
        BlockPos pos = new BlockPos(1, 2, 3);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> handler.getClientGuiElement(
                        GuiHandler.calculateOrdinal(ModGUIs.WIRELESS_ARCANE_TERMINAL, EnumFacing.NORTH),
                        player, world, pos.getX(), pos.getY(), pos.getZ()));

        assertWirelessLegacyRouteFailure(exception);
    }

    @Test
    void locatorAwareOpenRejectsUnsupportedGuiBeforePlayerTypeCheck() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        GuiHostLocator locator = new FixedArcaneHostLocator(new ArcaneTerminalPart());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ThEGuiOpener.openLocatorGui(player, ModGUIs.ARCANE_TERMINAL, locator, false));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(ModGUIs.ARCANE_TERMINAL.name())),
                () -> assertTrue(exception.getMessage().contains(locator.getClass().getName())),
                () -> assertTrue(exception.getMessage().contains(player.getClass().getName())));
    }

    @Test
    void locatorAwareOpenRejectsNonServerPlayerWithRouteContext() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        GuiHostLocator locator = new FixedArcaneHostLocator(new ArcaneTerminalPart());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> ThEGuiOpener.openLocatorGui(player, ModGUIs.WIRELESS_ARCANE_TERMINAL, locator, false));

        assertAll(
                () -> assertTrue(exception.getMessage().contains(ModGUIs.WIRELESS_ARCANE_TERMINAL.name())),
                () -> assertTrue(exception.getMessage().contains(locator.getClass().getName())),
                () -> assertTrue(exception.getMessage().contains(player.getClass().getName())),
                () -> assertTrue(exception.getMessage().contains(EntityPlayerMP.class.getName())));
    }

    private static IArcaneTerminalHost locateArcaneHost(AEBaseContainer container, EntityPlayer player) {
        return container.getLocator().locate(player, IArcaneTerminalHost.class);
    }

    private static ContainerArcaneInscriber openParentInscriber(FakeMinecraft.FakePlayer player,
                                                               ArcaneTerminalPart host) {
        ContainerArcaneInscriber parent = new ContainerArcaneInscriber(player.inventory, host);
        parent.setLocator(new FixedArcaneHostLocator(host));
        player.openContainer = parent;
        return parent;
    }

    private static void assertRouteFailureMessage(RuntimeException error, ModGUIs gui, BlockPos pos, EnumFacing side) {
        String message = error.getMessage();

        assertAll(
                () -> assertTrue(message.contains(gui.name()), message),
                () -> assertTrue(message.contains(pos.toString()), message),
                () -> assertTrue(message.contains(side.name()), message),
                () -> assertTrue(message.contains(IArcaneTerminalHost.class.getName()), message));
    }

    private static void assertWirelessLegacyRouteFailure(RuntimeException error) {
        String message = error.getMessage();

        assertAll(
                () -> assertTrue(message.contains(ModGUIs.WIRELESS_ARCANE_TERMINAL.name()), message),
                () -> assertTrue(message.contains("locator-aware"), message),
                () -> assertTrue(message.contains("legacy"), message),
                () -> assertTrue(message.contains("Forge"), message));
    }

    private static final class TestPartHost extends TileEntity implements IPartHost {

        private final IPart part;

        private TestPartHost(IPart part) {
            this.part = part;
        }

        @Override
        public IFacadeContainer getFacadeContainer() {
            return null;
        }

        @Override
        public IPart getPart(EnumFacing side) {
            return side == EnumFacing.NORTH ? this.part : null;
        }

        @Override
        public boolean canAddPart(ItemStack part, EnumFacing side) {
            return false;
        }

        @Override
        public <T extends IPart> T addPart(ae2.api.parts.IPartItem<T> partItem, EnumFacing side,
                                           EntityPlayer owner) {
            return null;
        }

        @Override
        public <T extends IPart> T replacePart(ae2.api.parts.IPartItem<T> partItem, EnumFacing side,
                                               EntityPlayer owner, net.minecraft.util.EnumHand hand) {
            return null;
        }

        @Override
        public void removePartFromSide(EnumFacing side) {
            throw new UnsupportedOperationException("TestPartHost does not mutate parts");
        }

        @Override
        public void markForUpdate() {
        }

        @Override
        public DimensionalBlockPos getLocation() {
            return new DimensionalBlockPos(this.getWorld(), this.getPos());
        }

        @Override
        public TileEntity getTileEntity() {
            return this;
        }

        @Override
        public AEColor getColor() {
            return AEColor.TRANSPARENT;
        }

        @Override
        public void clearContainer() {
        }

        @Override
        public boolean isBlocked(EnumFacing side) {
            return false;
        }

        @Override
        public SelectedPart selectPartLocal(Vec3d pos) {
            return new SelectedPart(this.part, null);
        }

        @Override
        public Iterable<AxisAlignedBB> getCollisionShape(Entity entity) {
            return List.of();
        }

        @Override
        public boolean removePart(IPart part) {
            return false;
        }

        @Override
        public void markForSave() {
        }

        @Override
        public void partChanged() {
        }

        @Override
        public boolean hasRedstone() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void notifyNeighbors() {
        }

        @Override
        public void notifyNeighborNow(EnumFacing side) {
        }

        @Override
        public boolean isInWorld() {
            return true;
        }

        @Override
        public float getCableConnectionLength(AECableType cable) {
            return 0;
        }
    }

    private static final class FixedArcaneHostLocator implements GuiHostLocator {

        private final IArcaneTerminalHost host;

        private FixedArcaneHostLocator(IArcaneTerminalHost host) {
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

    private static final class NonArcanePart implements IPart {

        @Override
        public ae2.api.parts.IPartItem<?> getPartItem() {
            return null;
        }

        @Override
        public ae2.api.networking.IGridNode getGridNode() {
            return null;
        }

        @Override
        public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        }

        @Override
        public void getBoxes(ae2.api.parts.IPartCollisionHelper bch) {
        }

        @Override
        public float getCableConnectionLength(AECableType cable) {
            return 0;
        }
    }

    private static final class PartPathRejectingArcaneAssembler extends TileArcaneAssembler implements IPartHost {

        @Override
        public IFacadeContainer getFacadeContainer() {
            return null;
        }

        @Override
        public IPart getPart(EnumFacing side) {
            throw new AssertionError("Arcane assembler route must use tile locator, not part lookup");
        }

        @Override
        public boolean canAddPart(ItemStack part, EnumFacing side) {
            return false;
        }

        @Override
        public <T extends IPart> T addPart(ae2.api.parts.IPartItem<T> partItem, EnumFacing side,
                                           EntityPlayer owner) {
            return null;
        }

        @Override
        public <T extends IPart> T replacePart(ae2.api.parts.IPartItem<T> partItem, EnumFacing side,
                                               EntityPlayer owner, net.minecraft.util.EnumHand hand) {
            return null;
        }

        @Override
        public void removePartFromSide(EnumFacing side) {
            throw new UnsupportedOperationException("PartPathRejectingArcaneAssembler does not mutate parts");
        }

        @Override
        public void markForUpdate() {
        }

        @Override
        public DimensionalBlockPos getLocation() {
            return new DimensionalBlockPos(this.getWorld(), this.getPos());
        }

        @Override
        public TileEntity getTileEntity() {
            return this;
        }

        @Override
        public AEColor getColor() {
            return AEColor.TRANSPARENT;
        }

        @Override
        public void clearContainer() {
        }

        @Override
        public boolean isBlocked(EnumFacing side) {
            return false;
        }

        @Override
        public SelectedPart selectPartLocal(Vec3d pos) {
            return new SelectedPart((IPart) null, null);
        }

        @Override
        public Iterable<AxisAlignedBB> getCollisionShape(Entity entity) {
            return List.of();
        }

        @Override
        public boolean removePart(IPart part) {
            return false;
        }

        @Override
        public void markForSave() {
        }

        @Override
        public void partChanged() {
        }

        @Override
        public boolean hasRedstone() {
            return false;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public void cleanup() {
        }

        @Override
        public void notifyNeighbors() {
        }

        @Override
        public void notifyNeighborNow(EnumFacing side) {
        }

        @Override
        public boolean isInWorld() {
            return true;
        }

        @Override
        public float getCableConnectionLength(AECableType cable) {
            return 0;
        }
    }

    private static final class ArcaneTerminalPart implements IPart, IArcaneTerminalHost {

        private final ThEInternalInventory craftingInventory = new ThEInternalInventory("crafting", 15, 64);
        private final ThEInternalInventory upgradeInventory = new ThEInternalInventory("upgrades", 1, 1);

        @Override
        public ae2.api.parts.IPartItem<?> getPartItem() {
            return null;
        }

        @Override
        public ae2.api.networking.IGridNode getGridNode() {
            return null;
        }

        @Override
        public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        }

        @Override
        public void getBoxes(ae2.api.parts.IPartCollisionHelper bch) {
        }

        @Override
        public float getCableConnectionLength(AECableType cable) {
            return 0;
        }

        @Override
        public ModGUIs getGui() {
            return ModGUIs.ARCANE_TERMINAL;
        }

        @Override
        public IItemHandler getInventoryByName(String name) {
            return switch (name) {
                case "crafting" -> new InvWrapper(this.craftingInventory);
                case "upgrades" -> new InvWrapper(this.upgradeInventory);
                default -> null;
            };
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
            throw new UnsupportedOperationException("Test host should not open GUIs");
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
                    return new TextComponentString("arcane terminal test storage");
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
