package thaumicenergistics.container.item;

import ae2.container.AEBaseContainer;
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
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
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
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.part.ContainerArcaneInscriber;
import thaumicenergistics.container.slot.SlotGhost;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.util.KnowledgeCoreUtil;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerKnowledgeCoreTest {

    private static final String CLIENT_ACTION_KNOWLEDGE_CORE_ADD = "knowledgeCoreAdd";
    private static final String CLIENT_ACTION_KNOWLEDGE_CORE_DELETE = "knowledgeCoreDelete";
    private static final String CLIENT_ACTION_KNOWLEDGE_CORE_VIEW = "knowledgeCoreView";

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
    void usesSupergiantBaseContainerContractDirectly() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);

        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);
        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);

        assertAll(
                () -> assertSame(player.inventory, aeContainer.getPlayerInventory()),
                () -> assertTrue(container.canInteractWith(player)));
    }

    @Test
    void registersKnowledgeCoreGhostSlotsWithSupergiantSlotSemantics() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);

        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);
        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);

        assertAll(
                () -> assertEquals(9, aeContainer.getSlots(ThESlotSemantics.KNOWLEDGE_CORE).size()),
                () -> assertEquals(container.inventorySlots,
                        aeContainer.getSlots(ThESlotSemantics.KNOWLEDGE_CORE)),
                () -> assertTrue(aeContainer.getSlots(ThESlotSemantics.KNOWLEDGE_CORE).stream()
                        .allMatch(slot -> slot instanceof SlotGhost)),
                () -> assertTrue(aeContainer.getSlots(ThESlotSemantics.KNOWLEDGE_CORE).stream()
                        .allMatch(slot -> aeContainer.getSlotSemantic(slot) == ThESlotSemantics.KNOWLEDGE_CORE)));
    }

    @Test
    void bareClickDisplayGhostSlotDoesNotReplaceRecipeResultWithCarriedStack() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ItemStack knowledgeCore = new ItemStack(Items.PAPER);
        ThEInternalInventory ingredients = new ThEInternalInventory("ingredients", 15, 64);
        ItemStack displayedResult = new ItemStack(Items.DIAMOND);
        KnowledgeCoreUtil.setRecipe(knowledgeCore, 0,
                new KnowledgeCoreUtil.Recipe(ingredients, displayedResult, 0));
        host.upgradeInventory.setInventorySlotContents(0, knowledgeCore);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);
        player.inventory.setItemStack(new ItemStack(Items.GOLD_INGOT, 4));

        container.slotClick(0, 0, ClickType.PICKUP, player);

        ItemStack slotStack = container.getSlot(0).getStack();
        assertAll(
                () -> assertEquals(Items.DIAMOND, slotStack.getItem()),
                () -> assertEquals(1, slotStack.getCount()),
                () -> assertEquals(Items.GOLD_INGOT, player.inventory.getItemStack().getItem()),
                () -> assertEquals(4, player.inventory.getItemStack().getCount()));
    }

    @Test
    void supergiantClientActionViewSlotMinusOneReturnsToParentHostMainContainer() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);

        container.receiveClientAction(CLIENT_ACTION_KNOWLEDGE_CORE_VIEW, "-1");

        assertAll(
                () -> assertEquals(1, host.returnCalls),
                () -> assertSame(player, host.returnedPlayer),
                () -> assertSame(container, host.returnedSubGui));
    }

    @Test
    void supergiantClientActionDeleteRejectsRecipeIndexAboveKnowledgeCoreSlotsBeforeReturningToParent() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);
        int invalidIndex = 9;

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> container.receiveClientAction(CLIENT_ACTION_KNOWLEDGE_CORE_DELETE,
                        Integer.toString(invalidIndex)));

        assertAll(
                () -> assertTrue(error.getMessage().contains(Integer.toString(invalidIndex)),
                        error.getMessage()),
                () -> assertTrue(error.getMessage().contains(CLIENT_ACTION_KNOWLEDGE_CORE_DELETE),
                        error.getMessage()),
                () -> assertEquals(0, host.returnCalls));
    }

    @Test
    void supergiantClientActionViewRejectsInvalidNegativeRecipeIndexBeforeReturningToParent() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_VIEW, parent, locator);
        int invalidIndex = -2;

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> container.receiveClientAction(CLIENT_ACTION_KNOWLEDGE_CORE_VIEW,
                        Integer.toString(invalidIndex)));

        assertAll(
                () -> assertTrue(error.getMessage().contains(Integer.toString(invalidIndex)),
                        error.getMessage()),
                () -> assertTrue(error.getMessage().contains(CLIENT_ACTION_KNOWLEDGE_CORE_VIEW),
                        error.getMessage()),
                () -> assertEquals(0, host.returnCalls));
    }

    @Test
    void supergiantClientActionAddStoresCurrentArcaneInscriberRecipeAndReturnsToParent() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        float requiredVis = 37.5f;
        ContainerArcaneInscriber parent = newParent(player, host, locator, requiredVis);
        ItemStack knowledgeCore = new ItemStack(Items.PAPER);
        ItemStack ingredient = new ItemStack(Items.GOLD_INGOT, 3);
        ItemStack result = new ItemStack(Items.DIAMOND, 2);
        host.upgradeInventory.setInventorySlotContents(0, knowledgeCore);
        host.craftingInventory.setInventorySlotContents(0, ingredient);
        parent.getInventory("result").insertItem(0, result.copy(), false);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_ADD, parent, locator);

        container.receiveClientAction(CLIENT_ACTION_KNOWLEDGE_CORE_ADD, "2");
        ItemStack storedKnowledgeCore = host.upgradeInventory.getStackInSlot(0);
        KnowledgeCoreUtil.Recipe recipe = KnowledgeCoreUtil.getRecipe(storedKnowledgeCore, 2);

        assertAll(
                () -> assertEquals(1, host.returnCalls),
                () -> assertSame(player, host.returnedPlayer),
                () -> assertSame(container, host.returnedSubGui),
                () -> assertEquals(result.getItem(), recipe.result().getItem()),
                () -> assertEquals(result.getCount(), recipe.result().getCount()),
                () -> assertEquals(ingredient.getItem(), recipe.ingredients().getStackInSlot(0).getItem()),
                () -> assertEquals(ingredient.getCount(), recipe.ingredients().getStackInSlot(0).getCount()),
                () -> assertEquals(requiredVis, recipe.visCost(), 0.0001f));
    }

    @Test
    void supergiantClientActionDeleteClearsStoredRecipeAndReturnsToParent() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        RecordingArcaneHost host = new RecordingArcaneHost();
        GuiHostLocator locator = new FixedArcaneHostLocator(host);
        ContainerArcaneInscriber parent = newParent(player, host, locator);
        ItemStack knowledgeCore = new ItemStack(Items.PAPER);
        ThEInternalInventory ingredients = new ThEInternalInventory("ingredients", 15, 64);
        KnowledgeCoreUtil.setRecipe(knowledgeCore, 3,
                new KnowledgeCoreUtil.Recipe(ingredients, new ItemStack(Items.DIAMOND), 0));
        host.upgradeInventory.setInventorySlotContents(0, knowledgeCore);
        ContainerKnowledgeCore container = new ContainerKnowledgeCore(
                player, ModGUIs.KNOWLEDGE_CORE_DEL, parent, locator);

        container.receiveClientAction(CLIENT_ACTION_KNOWLEDGE_CORE_DELETE, "3");

        assertAll(
                () -> assertEquals(1, host.returnCalls),
                () -> assertSame(container, host.returnedSubGui),
                () -> assertTrue(KnowledgeCoreUtil.isEmpty(host.upgradeInventory.getStackInSlot(0))));
    }

    private static ContainerArcaneInscriber newParent(FakeMinecraft.FakePlayer player,
                                                      RecordingArcaneHost host,
                                                      GuiHostLocator locator) {
        ContainerArcaneInscriber parent = new ContainerArcaneInscriber(player.inventory, host);
        parent.setLocator(locator);
        return parent;
    }

    private static ContainerArcaneInscriber newParent(FakeMinecraft.FakePlayer player,
                                                      RecordingArcaneHost host,
                                                      GuiHostLocator locator,
                                                      float requiredVis) {
        ContainerArcaneInscriber parent = new FixedVisArcaneInscriber(player, host, requiredVis);
        parent.setLocator(locator);
        return parent;
    }

    private static final class FixedVisArcaneInscriber extends ContainerArcaneInscriber {

        private final float requiredVis;

        private FixedVisArcaneInscriber(FakeMinecraft.FakePlayer player,
                                        IArcaneTerminalHost host,
                                        float requiredVis) {
            super(player.inventory, host);
            this.requiredVis = requiredVis;
        }

        @Override
        protected float getRequiredVis(IRecipe recipe, EntityPlayer player) {
            return this.requiredVis;
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
