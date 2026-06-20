package thaumicenergistics.container.item;

import ae2.api.storage.ILinkStatus;
import ae2.api.storage.MEStorage;
import ae2.api.util.IConfigManager;
import ae2.api.networking.IGridNode;
import ae2.api.parts.IPart;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartHost;
import ae2.api.parts.IPartItem;
import ae2.api.util.AECableType;
import ae2.container.ISubGui;
import ae2.core.gui.locator.GuiHostLocator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.ActionType;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.network.packets.PacketUIAction;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerKnowledgeCoreTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void constructorRejectsNonKnowledgeCoreGui() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        ContainerArcaneInscriber parent = newParent(player, host, new FixedArcaneHostLocator(host));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ContainerKnowledgeCore(player, ModGUIs.ARCANE_INSCRIBER, parent, parent.getLocator()));

        assertTrue(error.getMessage().contains(ModGUIs.ARCANE_INSCRIBER.name()), error.getMessage());
    }

    @Test
    void constructorRejectsNullParentLocator() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        ContainerArcaneInscriber parent = new ContainerArcaneInscriber(player.inventory, host);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ContainerKnowledgeCore(player, ModGUIs.KNOWLEDGE_CORE_ADD, parent, null));

        assertTrue(error.getMessage().contains("locator"), error.getMessage());
    }

    @Test
    void constructorRejectsNullParentContainer() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        GuiHostLocator locator = new FixedArcaneHostLocator(new RecordingArcaneHost());

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new ContainerKnowledgeCore(player, ModGUIs.KNOWLEDGE_CORE_ADD, null, locator));

        assertTrue(error.getMessage().contains("parent"), error.getMessage());
    }

    @Test
    void constructorRejectsLocatorHostMismatch() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost parentHost = new RecordingArcaneHost();
        RecordingArcaneHost locatorHost = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(locatorHost);
        ContainerArcaneInscriber parent = newParent(player, parentHost, locator);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new ContainerKnowledgeCore(player, ModGUIs.KNOWLEDGE_CORE_DEL, parent, locator));

        assertAll(
                () -> assertTrue(error.getMessage().contains(ModGUIs.KNOWLEDGE_CORE_DEL.name()),
                        error.getMessage()),
                () -> assertTrue(error.getMessage().contains("locator"), error.getMessage()),
                () -> assertTrue(error.getMessage().contains("parent"), error.getMessage()));
    }

    @Test
    void exposesParentLocatorAndHostAsSubGuiContext() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);

        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);

        assertAll(
                () -> assertInstanceOf(ISubGui.class, container),
                () -> assertSame(locator, container.getLocator()),
                () -> assertSame(host, container.getHost()),
                () -> assertSame(host, container.getLocator().locate(player, IArcaneTerminalHost.class)));
    }

    @Test
    void onActionReturnsToParentHostMainContainer() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);

        container.handleServerAction(player, new PacketUIAction(ActionType.KNOWLEDGE_CORE_VIEW, -1));

        assertAll(
                () -> assertEquals(1, host.returnCalls),
                () -> assertSame(player, host.returnedPlayer),
                () -> assertSame(container, host.returnedSubGui));
    }

    @Test
    void handleServerActionRejectsNonKnowledgeCorePacketAction() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> container.handleServerAction(player, new PacketUIAction(ActionType.CLEAR_GRID, 0)));

        assertTrue(error.getMessage().contains(ActionType.CLEAR_GRID.name()), error.getMessage());
    }

    @Test
    void handleServerActionRejectsRecipeIndexAboveKnowledgeCoreSlotsBeforeReturningToParent() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);
        int invalidIndex = 9;

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> container.handleServerAction(player,
                        new PacketUIAction(ActionType.KNOWLEDGE_CORE_DEL, invalidIndex)));

        assertAll(
                () -> assertTrue(error.getMessage().contains(Integer.toString(invalidIndex)),
                        error.getMessage()),
                () -> assertTrue(error.getMessage().contains(ActionType.KNOWLEDGE_CORE_DEL.name()),
                        error.getMessage()),
                () -> assertEquals(0, host.returnCalls));
    }

    @Test
    void handleServerActionRejectsInvalidNegativeRecipeIndexBeforeReturningToParent() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);
        int invalidIndex = -2;

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> container.handleServerAction(player,
                        new PacketUIAction(ActionType.KNOWLEDGE_CORE_VIEW, invalidIndex)));

        assertAll(
                () -> assertTrue(error.getMessage().contains(Integer.toString(invalidIndex)),
                        error.getMessage()),
                () -> assertTrue(error.getMessage().contains(ActionType.KNOWLEDGE_CORE_VIEW.name()),
                        error.getMessage()),
                () -> assertEquals(0, host.returnCalls));
    }

    private static ContainerArcaneInscriber newParent(FakeMinecraft.FakePlayer player,
                                                      RecordingArcaneHost host,
                                                      GuiHostLocator locator) {
        ContainerArcaneInscriber parent = new ContainerArcaneInscriber(player.inventory, host);
        parent.setLocator(locator);
        return parent;
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

    private static final class RecordingArcaneHost implements IArcaneTerminalHost, IPart {

        private final ThEInternalInventory craftingInventory = new ThEInternalInventory("crafting", 15, 64);
        private final ThEInternalInventory upgradeInventory = new ThEInternalInventory("upgrades", 1, 1);
        private int returnCalls;
        private EntityPlayer returnedPlayer;
        private ISubGui returnedSubGui;

        @Override
        public IPartItem<?> getPartItem() {
            return null;
        }

        @Override
        public IGridNode getGridNode() {
            return null;
        }

        @Override
        public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity blockEntity) {
        }

        @Override
        public void getBoxes(IPartCollisionHelper bch) {
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
        public World getVisWorld() {
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
            this.returnCalls++;
            this.returnedPlayer = player;
            this.returnedSubGui = subGui;
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
                    return new TextComponentString("knowledge core test storage");
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
