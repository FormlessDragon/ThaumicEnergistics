package thaumicenergistics.container;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantic;
import ae2.container.SlotSemantics;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.wrapper.PlayerMainInvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.container.block.ContainerArcaneAssembler;
import thaumicenergistics.container.slot.SlotArcaneResult;
import thaumicenergistics.container.slot.SlotGhost;
import thaumicenergistics.container.slot.SlotGhostEssentia;
import thaumicenergistics.container.slot.SlotKnowledgeCore;
import thaumicenergistics.container.slot.SlotUpgrade;
import thaumicenergistics.container.slot.ThEGhostSlot;
import thaumicenergistics.container.slot.ThESlot;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.EssentiaFilter;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerBaseQuickMoveTest {

    private static final long SERVER_THREAD_TIMEOUT_MILLIS = 5000L;

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void constructorRejectsNullPlayerFailFast() {
        NullPointerException thrown = assertThrows(NullPointerException.class, () -> new QuickMoveContainer(null));

        assertEquals("player", thrown.getMessage());
    }

    @Test
    void quickMoveFromPlayerSlotMovesIntoContainerSlotAndReturnsOriginalStack() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        QuickMoveContainer container = QuickMoveContainer.withRealDestination(player);
        ItemStack original = new ItemStack(Items.DIAMOND, 7);
        player.inventory.setInventorySlotContents(9, original.copy());

        ItemStack moved = container.transferStackInSlot(player, 1);

        assertAll(
                () -> assertEquals(Items.DIAMOND, moved.getItem()),
                () -> assertEquals(7, moved.getCount()),
                () -> assertTrue(player.inventory.getStackInSlot(9).isEmpty()),
                () -> assertEquals(Items.DIAMOND, container.storage.getStackInSlot(0).getItem()),
                () -> assertEquals(7, container.storage.getStackInSlot(0).getCount()));
    }

    @Test
    void quickMoveFromContainerSlotMovesIntoPlayerInventoryAndReturnsOriginalStack() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        QuickMoveContainer container = QuickMoveContainer.withRealDestination(player);
        container.storage.setStackInSlot(0, new ItemStack(Items.EMERALD, 5));

        ItemStack moved = container.transferStackInSlot(player, 0);

        assertAll(
                () -> assertEquals(Items.EMERALD, moved.getItem()),
                () -> assertEquals(5, moved.getCount()),
                () -> assertTrue(container.storage.getStackInSlot(0).isEmpty()),
                () -> assertEquals(Items.EMERALD, player.inventory.getStackInSlot(9).getItem()),
                () -> assertEquals(5, player.inventory.getStackInSlot(9).getCount()));
    }

    @Test
    void quickMoveFromPlayerSkipsGhostSlotsAndUsesRealDestination() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        QuickMoveContainer container = QuickMoveContainer.withGhostsAndRealDestination(player);
        player.inventory.setInventorySlotContents(9, new ItemStack(Items.APPLE, 3));

        container.slotClick(5, 0, ClickType.QUICK_MOVE, player);

        assertAll(
                () -> assertTrue(container.ghostInventory.getStackInSlot(0).isEmpty()),
                () -> assertTrue(container.ghostHandler.getStackInSlot(0).isEmpty()),
                () -> assertTrue(container.essentiaGhostInventory.getStackInSlot(0).isEmpty()),
                () -> assertTrue(container.craftingResult.getStackInSlot(0).isEmpty()),
                () -> assertEquals(Items.APPLE, container.storage.getStackInSlot(0).getItem()),
                () -> assertEquals(3, container.storage.getStackInSlot(0).getCount()),
                () -> assertTrue(player.inventory.getStackInSlot(9).isEmpty()));
    }

    @Test
    void playerInventoryBindingsExposeSupergiantSlotSemantics() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        QuickMoveContainer container = QuickMoveContainer.withPlayerBindings(player);

        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);

        assertAll(
                () -> assertEquals(27, aeContainer.getSlots(SlotSemantics.PLAYER_INVENTORY).size()),
                () -> assertEquals(9, aeContainer.getSlots(SlotSemantics.PLAYER_HOTBAR).size()),
                () -> assertEquals(4, aeContainer.getSlots(ThESlotSemantics.PLAYER_ARMOR).size()),
                () -> assertTrue(aeContainer.getSlots(SlotSemantics.PLAYER_INVENTORY).stream()
                        .allMatch(slot -> aeContainer.getSlotSemantic(slot) == SlotSemantics.PLAYER_INVENTORY)),
                () -> assertTrue(aeContainer.getSlots(SlotSemantics.PLAYER_HOTBAR).stream()
                        .allMatch(slot -> aeContainer.getSlotSemantic(slot) == SlotSemantics.PLAYER_HOTBAR)),
                () -> assertTrue(aeContainer.getSlots(ThESlotSemantics.PLAYER_ARMOR).stream()
                        .allMatch(slot -> aeContainer.getSlotSemantic(slot) == ThESlotSemantics.PLAYER_ARMOR)));
    }

    @Test
    void arcaneAssemblerRegistersKnowledgeCoreUpgradeAndPlayerSemantics() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, new TileArcaneAssembler());
        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);
        SlotSemantic knowledgeCore = SlotSemantics.get("THE_KNOWLEDGE_CORE");

        assertNotNull(knowledgeCore);
        assertAll(
                () -> assertSame(knowledgeCore, aeContainer.getSlotSemantic(container.getSlot(0))),
                () -> assertInstanceOf(SlotKnowledgeCore.class, container.getSlot(0)),
                () -> assertEquals(1, aeContainer.getSlots(knowledgeCore).size()),
                () -> assertEquals(5, aeContainer.getSlots(SlotSemantics.UPGRADE).size()),
                () -> assertTrue(aeContainer.getSlots(SlotSemantics.UPGRADE).stream()
                        .allMatch(slot -> slot instanceof SlotUpgrade)),
                () -> assertEquals(27, aeContainer.getSlots(SlotSemantics.PLAYER_INVENTORY).size()),
                () -> assertEquals(9, aeContainer.getSlots(SlotSemantics.PLAYER_HOTBAR).size()));
    }

    @Test
    void normalClickArcaneResultCraftsIntoCarriedStackWithoutLocalPacket() throws Throwable {
        FakeMinecraft.FakePlayer emptyHandedPlayer = FakeMinecraft.player(FakeMinecraft.serverWorld());
        QuickMoveContainer emptyHandedContainer = QuickMoveContainer.withArcaneResult(emptyHandedPlayer);
        emptyHandedContainer.craftingResult.setStackInSlot(0, new ItemStack(Items.DIAMOND));
        emptyHandedContainer.craftableAmount = 1;

        runOnServerThread(() -> emptyHandedContainer.slotClick(0, 0, ClickType.PICKUP, emptyHandedPlayer));

        ItemStack emptyHandedHeld = emptyHandedPlayer.inventory.getItemStack();
        assertAll(
                () -> assertEquals(1, emptyHandedContainer.tryCraftCalls),
                () -> assertEquals(1, emptyHandedContainer.lastTryCraftAmount),
                () -> assertEquals(1, emptyHandedContainer.onCraftCalls),
                () -> assertEquals(Items.DIAMOND, emptyHandedContainer.lastOnCraftInput.getItem()),
                () -> assertEquals(1, emptyHandedContainer.lastOnCraftInput.getCount()),
                () -> assertEquals(Items.DIAMOND, emptyHandedHeld.getItem()),
                () -> assertEquals(1, emptyHandedHeld.getCount()),
                () -> assertEquals(1, emptyHandedContainer.syncCarriedStackCalls),
                () -> assertSame(emptyHandedPlayer, emptyHandedContainer.lastSyncCarriedStackPlayer));

        FakeMinecraft.FakePlayer carryingPlayer = FakeMinecraft.player(FakeMinecraft.serverWorld());
        QuickMoveContainer carryingContainer = QuickMoveContainer.withArcaneResult(carryingPlayer);
        carryingContainer.craftingResult.setStackInSlot(0, new ItemStack(Items.DIAMOND));
        carryingContainer.craftableAmount = 1;
        carryingPlayer.inventory.setItemStack(new ItemStack(Items.DIAMOND, 3));

        runOnServerThread(() -> carryingContainer.slotClick(0, 0, ClickType.PICKUP, carryingPlayer));

        ItemStack carryingHeld = carryingPlayer.inventory.getItemStack();
        assertAll(
                () -> assertEquals(1, carryingContainer.tryCraftCalls),
                () -> assertEquals(1, carryingContainer.lastTryCraftAmount),
                () -> assertEquals(1, carryingContainer.onCraftCalls),
                () -> assertEquals(Items.DIAMOND, carryingContainer.lastOnCraftInput.getItem()),
                () -> assertEquals(1, carryingContainer.lastOnCraftInput.getCount()),
                () -> assertEquals(Items.DIAMOND, carryingHeld.getItem()),
                () -> assertEquals(4, carryingHeld.getCount()),
                () -> assertEquals(1, carryingContainer.syncCarriedStackCalls),
                () -> assertSame(carryingPlayer, carryingContainer.lastSyncCarriedStackPlayer));
    }

    @Test
    void normalClickArcaneResultDefaultSyncHookIgnoresFakePlayerWithoutLocalPacket() throws Throwable {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        DefaultSyncHookContainer container = DefaultSyncHookContainer.withArcaneResult(player);
        container.craftingResult.setStackInSlot(0, new ItemStack(Items.EMERALD));
        container.craftableAmount = 1;

        runOnServerThread(() -> container.slotClick(0, 0, ClickType.PICKUP, player));

        ItemStack held = player.inventory.getItemStack();
        assertAll(
                () -> assertEquals(1, container.tryCraftCalls),
                () -> assertEquals(1, container.onCraftCalls),
                () -> assertEquals(Items.EMERALD, held.getItem()),
                () -> assertEquals(1, held.getCount()));
    }

    private static void runOnServerThread(ThrowingRunnable action) throws Throwable {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread serverThread = SidedThreadGroups.SERVER.newThread(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                thrown.set(throwable);
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
        serverThread.join(SERVER_THREAD_TIMEOUT_MILLIS);
        if (serverThread.isAlive()) {
            serverThread.interrupt();
            throw new AssertionError("Server-side test action did not finish within "
                    + SERVER_THREAD_TIMEOUT_MILLIS + " ms");
        }
        if (thrown.get() != null) {
            throw thrown.get();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Throwable;
    }

    private static final class QuickMoveContainer extends ContainerBase implements ICraftingContainer {
        private final ItemStackHandler storage = new ItemStackHandler(1);
        private final InventoryBasic ghostInventory = new InventoryBasic("ghost", false, 1);
        private final ItemStackHandler ghostHandler = new ItemStackHandler(1);
        private final InventoryBasic essentiaGhostInventory = new InventoryBasic("essentiaGhost", false, 1);
        private final ItemStackHandler craftingResult = new ItemStackHandler(1);
        private int craftableAmount;
        private int tryCraftCalls;
        private int lastTryCraftAmount = -1;
        private int onCraftCalls;
        private ItemStack lastOnCraftInput = ItemStack.EMPTY;
        private int syncCarriedStackCalls;
        private EntityPlayer lastSyncCarriedStackPlayer;

        private QuickMoveContainer(EntityPlayer player) {
            super(player);
        }

        private static QuickMoveContainer withRealDestination(EntityPlayer player) {
            QuickMoveContainer container = new QuickMoveContainer(player);
            container.addSlotToContainer(new ThESlot(container.storage, 0, 0, 0));
            container.bindPlayerInventory(new PlayerMainInvWrapper(player.inventory), 0, 0);
            return container;
        }

        private static QuickMoveContainer withGhostsAndRealDestination(EntityPlayer player) {
            QuickMoveContainer container = new QuickMoveContainer(player);
            container.addSlotToContainer(new SlotGhost(container.ghostInventory, 0, 0, 0));
            container.addSlotToContainer(new ThEGhostSlot(container.ghostHandler, 0, 18, 0));
            container.addSlotToContainer(new SlotGhostEssentia(new EssentiaFilter(1),
                    container.essentiaGhostInventory, 0, 36, 0, 0));
            container.addSlotToContainer(new SlotArcaneResult(container, player, 0, 54, 0));
            container.addSlotToContainer(new ThESlot(container.storage, 0, 72, 0));
            container.bindPlayerInventory(new PlayerMainInvWrapper(player.inventory), 0, 0);
            return container;
        }

        private static QuickMoveContainer withArcaneResult(EntityPlayer player) {
            QuickMoveContainer container = new QuickMoveContainer(player);
            container.addSlotToContainer(new SlotArcaneResult(container, player, 0, 0, 0));
            return container;
        }

        private static QuickMoveContainer withPlayerBindings(EntityPlayer player) {
            QuickMoveContainer container = new QuickMoveContainer(player);
            container.bindPlayerInventory(new PlayerMainInvWrapper(player.inventory), 0, 0);
            container.bindPlayerArmour(player, new PlayerMainInvWrapper(player.inventory), 0, 0);
            return container;
        }

        @Override
        public void onMatrixChanged() {
        }

        @Override
        public int tryCraft(int amount) {
            this.tryCraftCalls++;
            this.lastTryCraftAmount = amount;
            return this.craftableAmount;
        }

        @Override
        public ItemStack onCraft(ItemStack crafted) {
            this.onCraftCalls++;
            this.lastOnCraftInput = crafted.copy();
            return crafted.copy();
        }

        @Override
        protected void syncCarriedStack(EntityPlayer player) {
            this.syncCarriedStackCalls++;
            this.lastSyncCarriedStackPlayer = player;
        }

        @Override
        public IItemHandler getInventory(String name) {
            if ("result".equals(name)) {
                return this.craftingResult;
            }
            throw new IllegalArgumentException("Unknown test inventory: " + name);
        }
    }

    private static final class DefaultSyncHookContainer extends CraftingResultContainer {

        private DefaultSyncHookContainer(EntityPlayer player) {
            super(player);
        }

        private static DefaultSyncHookContainer withArcaneResult(EntityPlayer player) {
            DefaultSyncHookContainer container = new DefaultSyncHookContainer(player);
            container.addSlotToContainer(new SlotArcaneResult(container, player, 0, 0, 0));
            return container;
        }
    }

    private abstract static class CraftingResultContainer extends ContainerBase implements ICraftingContainer {
        protected final ItemStackHandler craftingResult = new ItemStackHandler(1);
        protected int craftableAmount;
        protected int tryCraftCalls;
        protected int onCraftCalls;

        private CraftingResultContainer(EntityPlayer player) {
            super(player);
        }

        @Override
        public void onMatrixChanged() {
        }

        @Override
        public int tryCraft(int amount) {
            this.tryCraftCalls++;
            return this.craftableAmount;
        }

        @Override
        public ItemStack onCraft(ItemStack crafted) {
            this.onCraftCalls++;
            return crafted.copy();
        }

        @Override
        public IItemHandler getInventory(String name) {
            if ("result".equals(name)) {
                return this.craftingResult;
            }
            throw new IllegalArgumentException("Unknown test inventory: " + name);
        }
    }
}
