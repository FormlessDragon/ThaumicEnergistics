package thaumicenergistics.container.part;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.networking.IGridNode;
import ae2.api.networking.energy.IEnergySource;
import ae2.api.parts.IPart;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartHost;
import ae2.api.parts.IPartItem;
import ae2.api.storage.ILinkStatus;
import ae2.api.storage.MEStorage;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.container.ISubGui;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.container.slot.SlotArcaneGhostMatrix;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.util.inventory.ThEInternalInventory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneInscriberGhostClientActionTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void clientActionSetsNormalMatrixGhostSlotToDisplayFilterStack() {
        ContainerArcaneInscriber container = newContainer();
        int slotNumber = normalGhostSlot(container).slotNumber;
        ItemStack diamonds = new ItemStack(Items.DIAMOND, 32);

        container.receiveClientAction("moveGhostItem", ghostPayload(slotNumber, diamonds));

        ItemStack ghost = container.getSlot(slotNumber).getStack();
        assertAll(
                () -> assertEquals(Items.DIAMOND, ghost.getItem()),
                () -> assertEquals(1, ghost.getCount()));
    }

    @Test
    void clientActionPreservesNbtWhenSettingGhostSlot() {
        ContainerArcaneInscriber container = newContainer();
        int slotNumber = normalGhostSlot(container).slotNumber;
        ItemStack namedStack = new ItemStack(Items.DIAMOND, 3);
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "{\"text\":\"Infused Diamond\"}");
        namedStack.setTagInfo("display", display);

        container.receiveClientAction("moveGhostItem", ghostPayload(slotNumber, namedStack));

        ItemStack ghost = container.getSlot(slotNumber).getStack();
        assertAll(
                () -> assertEquals(Items.DIAMOND, ghost.getItem()),
                () -> assertEquals(1, ghost.getCount()),
                () -> assertTrue(ghost.hasTagCompound()),
                () -> assertEquals(namedStack.getTagCompound(), ghost.getTagCompound()));
    }

    @Test
    void clientActionRejectsInvalidSlotWithoutMutatingExistingGhost() {
        ContainerArcaneInscriber container = newContainer();
        Slot slot = normalGhostSlot(container);
        slot.putStack(new ItemStack(Items.GOLD_INGOT, 1));

        assertThrows(IllegalArgumentException.class,
                () -> container.receiveClientAction("moveGhostItem",
                        ghostPayload(container.inventorySlots.size(), new ItemStack(Items.DIAMOND, 1))));

        ItemStack ghost = slot.getStack();
        assertAll(
                () -> assertEquals(Items.GOLD_INGOT, ghost.getItem()),
                () -> assertEquals(1, ghost.getCount()));
    }

    @Test
    void clientActionRejectsNonGhostSlotWithoutMutatingGhostSlot() {
        ContainerArcaneInscriber container = newContainer();
        Slot ghostSlot = normalGhostSlot(container);
        ghostSlot.putStack(new ItemStack(Items.GOLD_INGOT, 1));
        int nonGhostSlotNumber = firstNonGhostSlot(container).slotNumber;

        assertThrows(IllegalArgumentException.class,
                () -> container.receiveClientAction("moveGhostItem",
                        ghostPayload(nonGhostSlotNumber, new ItemStack(Items.DIAMOND, 1))));

        assertEquals(Items.GOLD_INGOT, ghostSlot.getStack().getItem());
    }

    @Test
    void clientActionRejectsCrystalGhostSlotWithoutMutatingIt() {
        ContainerArcaneInscriber container = newContainer();
        SlotArcaneGhostMatrix crystalSlot = crystalGhostSlot(container);
        crystalSlot.putStack(new ItemStack(Items.GOLD_INGOT, 1));

        assertThrows(IllegalArgumentException.class,
                () -> container.receiveClientAction("moveGhostItem",
                        ghostPayload(crystalSlot.slotNumber, new ItemStack(Items.DIAMOND, 1))));

        assertEquals(Items.GOLD_INGOT, crystalSlot.getStack().getItem());
    }

    @Test
    void clientActionRejectsInvalidPayloadWithoutMutatingExistingGhost() {
        ContainerArcaneInscriber container = newContainer();
        Slot slot = normalGhostSlot(container);
        slot.putStack(new ItemStack(Items.GOLD_INGOT, 1));

        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> container.receiveClientAction("moveGhostItem",
                                ghostPayload(slot.slotNumber, ItemStack.EMPTY))),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> container.receiveClientAction("moveGhostItem",
                                "{\"slotNumber\":" + slot.slotNumber + "}")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> container.receiveClientAction("moveGhostItem",
                                "{\"slotNumber\":" + slot.slotNumber + ",\"stackTag\":\"\"}")),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> container.receiveClientAction("moveGhostItem",
                                "{\"slotNumber\":" + slot.slotNumber + ",\"stackTag\":\"{invalid\"}")));

        assertEquals(Items.GOLD_INGOT, slot.getStack().getItem());
    }

    private static ContainerArcaneInscriber newContainer() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        return new ContainerArcaneInscriber(player.inventory, new TestArcaneTerminalHost());
    }

    private static SlotArcaneGhostMatrix normalGhostSlot(ContainerArcaneInscriber container) {
        return container.inventorySlots.stream()
                .filter(slot -> slot instanceof SlotArcaneGhostMatrix)
                .map(SlotArcaneGhostMatrix.class::cast)
                .filter(slot -> slot.getSlotIndex() < 9)
                .findFirst()
                .orElseThrow();
    }

    private static SlotArcaneGhostMatrix crystalGhostSlot(ContainerArcaneInscriber container) {
        return container.inventorySlots.stream()
                .filter(slot -> slot instanceof SlotArcaneGhostMatrix)
                .map(SlotArcaneGhostMatrix.class::cast)
                .filter(slot -> slot.getSlotIndex() >= 9)
                .findFirst()
                .orElseThrow();
    }

    private static Slot firstNonGhostSlot(ContainerArcaneInscriber container) {
        Optional<Slot> slot = container.inventorySlots.stream()
                .filter(candidate -> !(candidate instanceof SlotArcaneGhostMatrix))
                .findFirst();
        assertFalse(slot.isEmpty(), "Expected at least one non-ghost slot");
        return slot.get();
    }

    private static String ghostPayload(int slotNumber, ItemStack stack) {
        String escapedStackTag = stack.serializeNBT().toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
        return "{\"slotNumber\":" + slotNumber + ",\"stackTag\":\"" + escapedStackTag + "\"}";
    }

    private static final class TestArcaneTerminalHost implements IArcaneTerminalHost, IPart, IEnergySource {

        private final ItemStackHandler crafting = new ItemStackHandler(15);
        private final ItemStackHandler upgrades = new ItemStackHandler(1);
        private final ThEInternalInventory aeUpgrades = new ThEInternalInventory("Test upgrades", 0, 64);

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
            return ModGUIs.ARCANE_INSCRIBER;
        }

        @Override
        public IItemHandler getInventoryByName(String name) {
            return switch (name.toLowerCase(java.util.Locale.ROOT)) {
                case "crafting" -> this.crafting;
                case "upgrades" -> this.upgrades;
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
        public MEStorage getInventory() {
            return () -> new TextComponentString("arcane inscriber ghost action test storage");
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

        @Override
        public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        }

        @Override
        public ItemStack getMainContainerIcon() {
            return ItemStack.EMPTY;
        }

        @Override
        public ae2.api.upgrades.IUpgradeInventory getUpgrades() {
            return new TestUpgradeInventory(this.aeUpgrades);
        }

        @Override
        public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
            return amt;
        }
    }

    private static final class TestUpgradeInventory implements ae2.api.upgrades.IUpgradeInventory {

        private final ThEInternalInventory inventory;

        private TestUpgradeInventory(ThEInternalInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public net.minecraft.item.Item getUpgradableItem() {
            return Items.AIR;
        }

        @Override
        public int getInstalledUpgrades(net.minecraft.item.Item u) {
            return 0;
        }

        @Override
        public int getMaxInstalled(net.minecraft.item.Item u) {
            return 0;
        }

        @Override
        public void readFromNBT(NBTTagCompound data, String subtag) {
        }

        @Override
        public void writeToNBT(NBTTagCompound data, String subtag) {
        }

        @Override
        public int size() {
            return this.inventory.size();
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            return this.inventory.getStackInSlot(slotIndex);
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            this.inventory.setItemDirect(slotIndex, stack);
        }
    }
}
