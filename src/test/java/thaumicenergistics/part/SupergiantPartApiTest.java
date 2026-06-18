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
import ae2.parts.PartModel;
import ae2.parts.reporting.AbstractTerminalPart;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.core.definitions.ThEItems;
import thaumicenergistics.core.definitions.ThEParts;
import thaumicenergistics.init.ModGlobals;
import thaumicenergistics.init.ThEBlocks;
import thaumicenergistics.tile.TileInfusionProvider;
import thaumicenergistics.tile.TileNetwork;
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

        assertAll(
                () -> assertTrue(AbstractTerminalPart.class.isAssignableFrom(PartArcaneTerminal.class)),
                () -> assertTrue(IArcaneTerminalHost.class.isAssignableFrom(PartArcaneTerminal.class)),
                () -> assertTrue(PartArcaneTerminal.class.isAssignableFrom(PartArcaneInscriber.class)),
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
        IActionSource actionSource = infusionProvider.actionSource();

        assertAll(
                () -> assertTrue(IInWorldGridNodeHost.class.isAssignableFrom(TileNetwork.class)),
                () -> assertTrue(IActionHost.class.isAssignableFrom(TileNetwork.class)),
                () -> assertTrue(IPowerChannelState.class.isAssignableFrom(TileNetwork.class)),
                () -> assertTrue(IOwnerAwareTile.class.isAssignableFrom(TileNetwork.class)),
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
        IItemHandler upgrades = terminal.getInventoryByName("upgrades");
        ItemStack arcaneUpgrade = ThEItems.UPGRADE_ARCANE.stack(1);

        assertAll(
                () -> assertEquals(1, upgrades.getSlots()),
                () -> assertInstanceOf(InvWrapper.class, upgrades),
                () -> assertInstanceOf(ThEUpgradeInventory.class, ((InvWrapper) upgrades).getInv()),
                () -> assertInstanceOf(ThEInternalInventory.class, ((InvWrapper) upgrades).getInv()),
                () -> assertInstanceOf(IUpgradeInventory.class, ((InvWrapper) upgrades).getInv()),
                () -> assertTrue(upgrades.isItemValid(0, arcaneUpgrade)),
                () -> assertTrue(upgrades.insertItem(0, arcaneUpgrade.copy(), true).isEmpty()),
                () -> assertEquals(0, upgrades.getStackInSlot(0).getCount()));
    }

    private static final class InspectableInfusionProvider extends TileInfusionProvider {
        private IActionSource actionSource() {
            return this.src;
        }
    }

}
