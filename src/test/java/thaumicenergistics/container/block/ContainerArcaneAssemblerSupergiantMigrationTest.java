package thaumicenergistics.container.block;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.slot.SlotKnowledgeCore;
import thaumicenergistics.container.slot.SlotUpgrade;
import thaumicenergistics.container.slot.ThESlot;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerArcaneAssemblerSupergiantMigrationTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void assemblerUsesSupergiantContainerContractWithNullHost() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        Object container = new ContainerArcaneAssembler(player, new TestArcaneAssemblerTile());

        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);

        assertAll(
                () -> assertSame(player.inventory, aeContainer.getPlayerInventory()),
                () -> assertNull(aeContainer.getTileEntity()));
    }

    @Test
    void canInteractWithPreservesLocalContainerUserVisibleBehavior() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, new TestArcaneAssemblerTile());

        assertTrue(container.canInteractWith(player));
    }

    @Test
    void playerInventorySlotsUseSupergiantSlotBinding() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, new TestArcaneAssemblerTile());
        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);

        List<Slot> mainSlots = aeContainer.getSlots(SlotSemantics.PLAYER_INVENTORY);
        List<Slot> hotbarSlots = aeContainer.getSlots(SlotSemantics.PLAYER_HOTBAR);

        assertAll(
                () -> assertEquals(27, mainSlots.size()),
                () -> assertEquals(9, hotbarSlots.size()),
                () -> assertEquals(8, mainSlots.get(0).xPos),
                () -> assertEquals(149, mainSlots.get(0).yPos),
                () -> assertEquals(8, hotbarSlots.get(0).xPos),
                () -> assertEquals(207, hotbarSlots.get(0).yPos),
                () -> assertTrue(mainSlots.stream().noneMatch(ThESlot.class::isInstance)),
                () -> assertTrue(hotbarSlots.stream().noneMatch(ThESlot.class::isInstance)));
    }

    @Test
    void registersKnowledgeCoreUpgradeAndPlayerSlotSemantics() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, new TestArcaneAssemblerTile());
        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);
        List<Slot> upgradeSlots = aeContainer.getSlots(SlotSemantics.UPGRADE);
        List<Slot> mainSlots = aeContainer.getSlots(SlotSemantics.PLAYER_INVENTORY);
        List<Slot> hotbarSlots = aeContainer.getSlots(SlotSemantics.PLAYER_HOTBAR);

        assertAll(
                () -> assertSame(ThESlotSemantics.KNOWLEDGE_CORE, aeContainer.getSlotSemantic(container.getSlot(0))),
                () -> assertInstanceOf(SlotKnowledgeCore.class, container.getSlot(0)),
                () -> assertEquals(List.of(container.getSlot(0)), aeContainer.getSlots(ThESlotSemantics.KNOWLEDGE_CORE)),
                () -> assertEquals(5, upgradeSlots.size()),
                () -> assertTrue(upgradeSlots.stream().allMatch(slot -> slot instanceof SlotUpgrade)),
                () -> assertTrue(upgradeSlots.stream()
                        .allMatch(slot -> aeContainer.getSlotSemantic(slot) == SlotSemantics.UPGRADE)),
                () -> assertEquals(27, mainSlots.size()),
                () -> assertTrue(mainSlots.stream()
                        .allMatch(slot -> aeContainer.getSlotSemantic(slot) == SlotSemantics.PLAYER_INVENTORY)),
                () -> assertEquals(9, hotbarSlots.size()),
                () -> assertTrue(hotbarSlots.stream()
                        .allMatch(slot -> aeContainer.getSlotSemantic(slot) == SlotSemantics.PLAYER_HOTBAR)));
    }

    @Test
    void constructorRejectsNullTileWithExplicitDiagnostic() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());

        NullPointerException thrown = assertThrows(NullPointerException.class,
                () -> new ContainerArcaneAssembler(player, null));

        assertTrue(thrown.getMessage().contains("TE") || thrown.getMessage().contains("tile"));
    }

    @Test
    void quickMoveFromPlayerInventoryReturnsOriginalMovedStack() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneAssemblerTile tile = new TestArcaneAssemblerTile();
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, tile);
        ItemStack original = new ItemStack(Items.DIAMOND, 7);
        player.inventory.setInventorySlotContents(9, original.copy());

        ItemStack moved = container.transferStackInSlot(player, 6);

        assertAll(
                () -> assertEquals(Items.DIAMOND, moved.getItem()),
                () -> assertEquals(7, moved.getCount()),
                () -> assertTrue(player.inventory.getStackInSlot(9).isEmpty()),
                () -> assertEquals(Items.DIAMOND, tile.coreInventory.getStackInSlot(0).getItem()),
                () -> assertEquals(7, tile.coreInventory.getStackInSlot(0).getCount()));
    }

    @Test
    void quickMoveFromCoreSlotReturnsOriginalMovedStackAndMovesIntoPlayerInventory() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneAssemblerTile tile = new TestArcaneAssemblerTile();
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, tile);
        tile.coreInventory.setStackInSlot(0, new ItemStack(Items.EMERALD, 5));

        ItemStack moved = container.transferStackInSlot(player, 0);

        assertAll(
                () -> assertEquals(Items.EMERALD, moved.getItem()),
                () -> assertEquals(5, moved.getCount()),
                () -> assertTrue(tile.coreInventory.getStackInSlot(0).isEmpty()),
                () -> assertEquals(Items.EMERALD, player.inventory.getStackInSlot(9).getItem()),
                () -> assertEquals(5, player.inventory.getStackInSlot(9).getCount()));
    }

    @Test
    void quickMoveInvalidIndexesReturnEmptyStack() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, new TestArcaneAssemblerTile());

        assertAll(
                () -> assertTrue(container.transferStackInSlot(player, -1).isEmpty()),
                () -> assertTrue(container.transferStackInSlot(player, container.inventorySlots.size()).isEmpty()));
    }

    private static final class TestArcaneAssemblerTile extends TileArcaneAssembler {

        private final ItemStackHandler coreInventory = new ItemStackHandler(1);
        private final ItemStackHandler upgradeInventory = new ItemStackHandler(5);

        @Override
        public IItemHandler getInventoryByName(String name) {
            return switch (name) {
                case "cores" -> this.coreInventory;
                case "upgrades" -> this.upgradeInventory;
                default -> throw new IllegalArgumentException("Unknown test inventory: " + name);
            };
        }
    }
}
