package thaumicenergistics.container.block;

import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.container.slot.AppEngSlot;
import ae2.container.slot.RestrictedInputSlot;
import ae2.container.slot.SlotBackgroundIcon;
import ae2.api.upgrades.Upgrades;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.container.ThESlotSemantics;
import thaumicenergistics.container.slot.SlotKnowledgeCore;
import thaumicenergistics.container.slot.SlotUpgrade;
import thaumicenergistics.container.slot.ThESlot;
import thaumicenergistics.init.ThEBlocks;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.tile.TileArcaneAssembler;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerArcaneAssemblerSupergiantMigrationTest {

    private static final Item TEST_AE_UPGRADE_CARD = Upgrades.createUpgradeCardItem();

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }

        Upgrades.add(TEST_AE_UPGRADE_CARD, ThEBlocks.ARCANE_ASSEMBLER.item(), 5);
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
                () -> assertFalse(upgradeSlots.stream().anyMatch(SlotUpgrade.class::isInstance)),
                () -> assertFalse(upgradeSlots.stream().anyMatch(ThESlot.class::isInstance)),
                () -> assertTrue(upgradeSlots.stream().allMatch(RestrictedInputSlot.class::isInstance)),
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
    void upgradeSlotsUseSupergiantBackgroundIconsAndPositions() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, new TestArcaneAssemblerTile());
        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);
        List<Slot> upgradeSlots = aeContainer.getSlots(SlotSemantics.UPGRADE);

        assertEquals(5, upgradeSlots.size());
        for (int i = 0; i < upgradeSlots.size(); i++) {
            Slot slot = upgradeSlots.get(i);
            AppEngSlot appEngSlot = assertInstanceOf(AppEngSlot.class, slot, "upgrade slot " + i);
            int expectedY = 8 + i * 18;

            assertAll("upgrade slot " + i,
                    () -> assertInstanceOf(RestrictedInputSlot.class, slot),
                    () -> assertEquals(SlotBackgroundIcon.UPGRADE, appEngSlot.getBackgroundIcon()),
                    () -> assertEquals(186, slot.xPos),
                    () -> assertEquals(expectedY, slot.yPos),
                    () -> assertSame(SlotSemantics.UPGRADE, aeContainer.getSlotSemantic(slot)));
        }
    }

    @Test
    void upgradeSlotsValidateAeUpgradeCardsAndRejectOrdinaryItems() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        ContainerArcaneAssembler container = new ContainerArcaneAssembler(player, new TestArcaneAssemblerTile());
        AEBaseContainer aeContainer = assertInstanceOf(AEBaseContainer.class, container);
        Slot upgradeSlot = aeContainer.getSlots(SlotSemantics.UPGRADE).get(0);

        assertInstanceOf(RestrictedInputSlot.class, upgradeSlot);
        assertAll(
                () -> assertTrue(upgradeSlot.isItemValid(new ItemStack(TEST_AE_UPGRADE_CARD))),
                () -> assertFalse(upgradeSlot.isItemValid(new ItemStack(Items.DIAMOND))));
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
        tile.coreInventory.setInventorySlotContents(0, new ItemStack(Items.EMERALD, 5));

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

        private final ThEInternalInventory coreInventory = new ThEInternalInventory("cores", 1, 64);
        private final ThEUpgradeInventory upgradeInventory = new ThEUpgradeInventory(
                "upgrades", 5, 1, ThEBlocks.ARCANE_ASSEMBLER.stack(1));

        @Override
        public IItemHandler getInventoryByName(String name) {
            return switch (name) {
                case "cores" -> new InvWrapper(this.coreInventory);
                case "upgrades" -> new InvWrapper(this.upgradeInventory);
                default -> throw new IllegalArgumentException("Unknown test inventory: " + name);
            };
        }

        public ThEUpgradeInventory getUpgradeInventory() {
            return this.upgradeInventory;
        }
    }
}
