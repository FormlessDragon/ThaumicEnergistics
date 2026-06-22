package thaumicenergistics.part;

import ae2.api.implementations.IPowerChannelState;
import ae2.api.networking.IInWorldGridNodeHost;
import ae2.api.networking.security.IActionHost;
import ae2.api.networking.security.IActionSource;
import ae2.api.parts.IPartModel;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.Upgrades;
import ae2.block.IOwnerAwareTile;
import ae2.items.parts.PartItem;
import ae2.me.helpers.IGridConnectedTile;
import ae2.parts.PartModel;
import ae2.parts.reporting.AbstractTerminalPart;
import ae2.tile.grid.AENetworkedTile;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEParts;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.core.definitions.ThEBlocks;
import thaumicenergistics.tile.TileInfusionProvider;
import thaumicenergistics.util.inventory.ThEKnowledgeCoreInventory;
import thaumicenergistics.util.inventory.ThEInternalInventory;
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class SupergiantPartApiTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void thaumicPartsUseSupergiantPartApiDirectly() {
        PartArcaneTerminal terminal = ThEParts.ARCANE_TERMINAL.item().createPart();
        PartArcaneInscriber inscriber = ThEParts.ARCANE_INSCRIBER.item().createPart();
        AbstractTerminalPart terminalBase = terminal;
        IArcaneTerminalHost terminalHost = terminal;
        PartArcaneTerminal inscriberTerminal = inscriber;

        assertAll(
                () -> assertSame(terminal, terminalBase),
                () -> assertSame(terminal, terminalHost),
                () -> assertSame(inscriber, inscriberTerminal),
                () -> assertPartItem(ThEParts.ARCANE_TERMINAL.item(), PartArcaneTerminal.class),
                () -> assertPartItem(ThEParts.ARCANE_INSCRIBER.item(), PartArcaneInscriber.class),
                () -> assertSupergiantPartModel(terminal.getStaticModels(), PartArcaneTerminal.MODEL_BASE,
                        PartArcaneTerminal.MODEL_OFF, new ResourceLocation(ModGlobals.MOD_ID_AE2,
                                "part/display_status_off")),
                () -> assertSupergiantPartModel(inscriber.getStaticModels(), PartArcaneInscriber.MODEL_BASE,
                        PartArcaneInscriber.MODEL_OFF, new ResourceLocation(ModGlobals.MOD_ID_AE2,
                                "part/display_status_off")));
    }

    @Test
    void thaumicPartsBindUpgradeInventoriesToSupergiantPartItemStacks() {
        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEParts.ARCANE_TERMINAL.item(), 1);

        PartItem<PartArcaneTerminal> terminalItem = ThEParts.ARCANE_TERMINAL.item();
        PartArcaneTerminal terminal = terminalItem.createPart();
        ItemStack terminalIcon = terminal.getMainContainerIcon();

        assertAll(
                () -> assertSame(terminalItem, terminalIcon.getItem()),
                () -> assertEquals(1, terminalIcon.getCount()),
                () -> assertUpgradeInventoryUsesPartItemStack(terminal));
    }

    @Test
    void arcaneTerminalHostReturnsArcaneUpgradeInventory() {
        PartArcaneTerminal terminal = ThEParts.ARCANE_TERMINAL.item().createPart();
        IArcaneTerminalHost host = terminal;
        IUpgradeInventory upgrades = host.getArcaneUpgradeInventory();
        IItemHandler upgradeHandler = upgrades.toItemHandler();

        assertAll(
                () -> assertEquals(1, upgradeHandler.getSlots()),
                () -> assertSame(upgrades, host.getArcaneUpgradeInventory()),
                () -> assertInstanceOf(ThEUpgradeInventory.class, upgrades),
                () -> assertSame(upgradeHandler, terminal.getInventoryByName("upgrades")),
                () -> assertFalse(((ThEUpgradeInventory) upgrades).isKnowledgeCoreSlot()));
    }

    @Test
    void arcaneTerminalHostReturnsTypedCraftingInventory() {
        PartArcaneTerminal terminal = ThEParts.ARCANE_TERMINAL.item().createPart();
        IArcaneTerminalHost host = terminal;
        ThEInternalInventory crafting = assertInstanceOf(ThEInternalInventory.class,
                host.getArcaneCraftingInventory());
        IItemHandler craftingBridge = terminal.getInventoryByName("crafting");
        ItemStack expectedStack = new ItemStack(Items.DIAMOND, 2);

        crafting.setInventorySlotContents(0, expectedStack.copy());

        assertAll(
                () -> assertEquals(15, crafting.size()),
                () -> assertSame(crafting, host.getArcaneCraftingInventory()),
                () -> assertTrue(ItemStack.areItemStacksEqual(expectedStack, craftingBridge.getStackInSlot(0))));
    }

    @Test
    void arcaneInscriberHostReturnsKnowledgeCoreInventory() {
        PartArcaneInscriber inscriber = ThEParts.ARCANE_INSCRIBER.item().createPart();
        IArcaneTerminalHost host = inscriber;
        IUpgradeInventory upgrades = host.getArcaneUpgradeInventory();
        IItemHandler upgradeHandler = upgrades.toItemHandler();

        assertAll(
                () -> assertEquals(1, upgradeHandler.getSlots()),
                () -> assertSame(upgrades, host.getArcaneUpgradeInventory()),
                () -> assertInstanceOf(ThEKnowledgeCoreInventory.class, upgrades),
                () -> assertSame(upgradeHandler, inscriber.getInventoryByName("upgrades")),
                () -> assertTrue(((ThEKnowledgeCoreInventory) upgrades).isKnowledgeCoreSlot()),
                () -> assertTrue(upgradeHandler.isItemValid(0, ThEItems.KNOWLEDGE_CORE.stack(1))),
                () -> assertFalse(upgradeHandler.isItemValid(0, ThEItems.UPGRADE_ARCANE.stack(1))));
    }

    @Test
    void upgradeInventoriesUseSupergiantUpgradeApiDirectly() {
        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 1);

        ThEUpgradeInventory inventory = new ThEUpgradeInventory(
                "upgrades",
                1,
                1,
                ThEBlocks.ARCANE_ASSEMBLER.stack(1));
        ItemStack arcaneUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);
        ItemStack unsupportedUpgrade = ThEItems.DIFFUSION_CORE.stack(1);

        assertAll(
                () -> assertEquals(1, Upgrades.getMaxInstallable(
                        ThEItems.UPGRADE_ARCANE.item(),
                        ThEBlocks.ARCANE_ASSEMBLER.item())),
                () -> assertEquals(0, Upgrades.getMaxInstallable(
                        ThEItems.DIFFUSION_CORE.item(),
                        ThEBlocks.ARCANE_ASSEMBLER.item())),
                () -> assertTrue(inventory.isItemValidForSlot(0, arcaneUpgrade)),
                () -> assertFalse(inventory.isItemValidForSlot(0, unsupportedUpgrade)));

        inventory.setInventorySlotContents(0, arcaneUpgrade);

        assertAll(
                () -> assertInstanceOf(IUpgradeInventory.class, inventory),
                () -> assertEquals(ThEBlocks.ARCANE_ASSEMBLER.item(), inventory.getUpgradableItem()),
                () -> assertEquals(1, inventory.getMaxInstalled(ThEItems.UPGRADE_ARCANE.item())),
                () -> assertEquals(1, inventory.getInstalledUpgrades(ThEItems.UPGRADE_ARCANE.item())),
                () -> assertFalse(inventory.isItemValidForSlot(0, arcaneUpgrade)));
    }

    @Test
    void networkTilesUseSupergiantGridApiDirectly() {
        InspectableInfusionProvider infusionProvider = new InspectableInfusionProvider();
        AENetworkedTile networkedTile = infusionProvider;
        IGridConnectedTile gridConnectedTile = infusionProvider;
        IInWorldGridNodeHost nodeHost = infusionProvider;
        IActionHost actionHost = infusionProvider;
        IPowerChannelState powerState = infusionProvider;
        IOwnerAwareTile ownerAware = infusionProvider;
        IActionSource actionSource = infusionProvider.actionSource();

        assertAll(
                () -> assertSame(infusionProvider, networkedTile),
                () -> assertSame(infusionProvider, gridConnectedTile),
                () -> assertSame(infusionProvider, nodeHost),
                () -> assertSame(infusionProvider, actionHost),
                () -> assertSame(infusionProvider, powerState),
                () -> assertSame(infusionProvider, ownerAware),
                () -> assertInstanceOf(IInWorldGridNodeHost.class, infusionProvider),
                () -> assertInstanceOf(IActionHost.class, infusionProvider),
                () -> assertInstanceOf(IPowerChannelState.class, infusionProvider),
                () -> assertInstanceOf(IOwnerAwareTile.class, infusionProvider),
                () -> assertSame(infusionProvider, actionSource.machine().orElseThrow()),
                () -> assertFalse(actionSource.player().isPresent()),
                () -> assertFalse(actionSource.context(Object.class).isPresent()));
    }

    private static void assertSupergiantPartModel(IPartModel model, ResourceLocation... expectedModels) {
        assertAll(
                () -> assertInstanceOf(PartModel.class, model),
                () -> assertTrue(model.requireCableConnection()),
                () -> assertEquals(List.of(expectedModels), model.getModels()));
    }

    private static <T extends ae2.api.parts.IPart> void assertPartItem(PartItem<T> item, Class<T> partClass) {
        assertAll(
                () -> assertEquals(PartItem.class, item.getClass()),
                () -> assertEquals(partClass, item.getPartClass()),
                () -> assertInstanceOf(partClass, item.createPart()));
    }

    private static void assertUpgradeInventoryUsesPartItemStack(PartArcaneTerminal terminal) {
        IUpgradeInventory arcaneUpgrades = terminal.getArcaneUpgradeInventory();
        IItemHandler upgrades = terminal.getInventoryByName("upgrades");
        ItemStack arcaneUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);

        assertAll(
                () -> assertEquals(1, upgrades.getSlots()),
                () -> assertSame(arcaneUpgrades.toItemHandler(), upgrades),
                () -> assertInstanceOf(ThEUpgradeInventory.class, arcaneUpgrades),
                () -> assertInstanceOf(ThEInternalInventory.class, arcaneUpgrades),
                () -> assertTrue(upgrades.isItemValid(0, arcaneUpgrade)),
                () -> assertTrue(upgrades.insertItem(0, arcaneUpgrade.copy(), true).isEmpty()),
                () -> assertEquals(0, upgrades.getStackInSlot(0).getCount()));
    }

    private static final class InspectableInfusionProvider extends TileInfusionProvider {
        IActionSource actionSource() {
            return this.src;
        }
    }

}
