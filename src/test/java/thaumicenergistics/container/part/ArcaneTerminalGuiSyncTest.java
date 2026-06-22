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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
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
import thaumicenergistics.util.inventory.ThEUpgradeInventory;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArcaneTerminalGuiSyncTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
    }

    @Test
    void visStatePacketRoundTripPreservesFloatsAndConsumesPayload() {
        ArcaneTerminalVisState original = new ArcaneTerminalVisState(12.5f, 3.25f, 0.5f);
        ByteBuf buffer = Unpooled.buffer();

        original.writeToPacket(buffer);
        ArcaneTerminalVisState decoded = new ArcaneTerminalVisState(buffer);

        assertAll(
                () -> assertEquals(original, decoded),
                () -> assertEquals(12.5f, decoded.getVisAvailable()),
                () -> assertEquals(3.25f, decoded.getVisRequired()),
                () -> assertEquals(0.5f, decoded.getDiscount()),
                () -> assertEquals(0, buffer.readableBytes(), "ArcaneTerminalVisState should consume its payload"));
    }

    @Test
    void defaultStateKeepsLegacyUnsetSemanticsAndEqualityIsValueBased() {
        ArcaneTerminalVisState sameAsDefault = new ArcaneTerminalVisState(-1f, -1f, 0f);
        ArcaneTerminalVisState changed = new ArcaneTerminalVisState(0f, -1f, 0f);

        assertAll(
                () -> assertEquals(sameAsDefault, ArcaneTerminalVisState.EMPTY),
                () -> assertEquals(sameAsDefault.hashCode(), ArcaneTerminalVisState.EMPTY.hashCode()),
                () -> assertEquals(-1f, ArcaneTerminalVisState.EMPTY.getVisAvailable()),
                () -> assertEquals(-1f, ArcaneTerminalVisState.EMPTY.getVisRequired()),
                () -> assertEquals(0f, ArcaneTerminalVisState.EMPTY.getDiscount()),
                () -> assertNotEquals(changed, ArcaneTerminalVisState.EMPTY));
    }

    @Test
    void terminalDoesNotSampleVisDuringConstructionOrDetectWithoutFullSyncListener() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost(ModGUIs.ARCANE_TERMINAL);
        host.setVisSample(32f, 8f, 0.25f);
        RecordingArcaneTerm container = new RecordingArcaneTerm(player.inventory, host);

        assertEquals(ArcaneTerminalVisState.EMPTY, container.getVisState());

        host.setVisSample(64f, 16f, 0.5f);
        container.detectAndSendChanges();

        assertEquals(ArcaneTerminalVisState.EMPTY, container.getVisState());
    }

    @Test
    void inscriberRefreshesArcaneFlagThroughContainerState() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost(ModGUIs.ARCANE_INSCRIBER);
        RecordingArcaneInscriber container = new RecordingArcaneInscriber(player.inventory, host);

        assertTrue(container.hasArcaneGhostSlots(), "test container should exercise the real inscriber slot layout");
        assertEquals(false, container.isRecipeArcane());

        container.allowSampling();
        container.setArcaneRecipePresent(true);
        container.refreshIsArcane();

        assertEquals(true, container.isRecipeArcane());

        container.setArcaneRecipePresent(false);
        container.refreshIsArcane();

        assertEquals(false, container.isRecipeArcane());
    }

    @Test
    void inscriberDoesNotSampleArcaneFlagDuringConstructionOrDetectWithoutFullSyncListener() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost(ModGUIs.ARCANE_INSCRIBER);
        host.setArcaneRecipePresent(true);

        RecordingArcaneInscriber container = new RecordingArcaneInscriber(player.inventory, host);

        assertEquals(false, container.isRecipeArcane());

        container.detectAndSendChanges();

        assertEquals(false, container.isRecipeArcane());
    }

    @Test
    void fullGuiSyncPreparationSamplesVisAndArcaneStateAfterConstruction() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.serverWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost(ModGUIs.ARCANE_INSCRIBER);
        host.setVisSample(40f, 9f, 0.2f);
        host.setArcaneRecipePresent(true);
        RecordingArcaneInscriber container = new RecordingArcaneInscriber(player.inventory, host);

        assertAll(
                () -> assertEquals(ArcaneTerminalVisState.EMPTY, container.getVisState()),
                () -> assertEquals(false, container.isRecipeArcane()));

        container.allowSampling();
        container.prepareFullGuiSyncState();

        assertAll(
                () -> assertEquals(new ArcaneTerminalVisState(Float.MAX_VALUE, 9f, 0f), container.getVisState()),
                () -> assertEquals(true, container.isRecipeArcane()));
    }

    @Test
    void clientContainerAppliesSupergiantGuiSyncPayloadForTerminalAndInscriberState() {
        FakeMinecraft.FakePlayer player = FakeMinecraft.player(FakeMinecraft.clientWorld());
        TestArcaneTerminalHost host = new TestArcaneTerminalHost(ModGUIs.ARCANE_INSCRIBER);
        RecordingArcaneInscriber container = new RecordingArcaneInscriber(player.inventory, host);
        ArcaneTerminalVisState syncedVisState = new ArcaneTerminalVisState(40f, 9f, 0.2f);
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeShort(102);
        syncedVisState.writeToPacket(buffer);
        buffer.writeShort(103);
        buffer.writeBoolean(true);
        buffer.writeShort(-1);

        container.receiveServerSyncData(buffer);

        assertAll(
                () -> assertEquals(syncedVisState, container.getVisState()),
                () -> assertEquals(true, container.isRecipeArcane()),
                () -> assertEquals(0, buffer.readableBytes(), "Gui sync payload should be fully consumed"));
    }

    private static final class RecordingArcaneTerm extends ContainerArcaneTerm {

        private boolean samplingAllowed;

        private RecordingArcaneTerm(net.minecraft.entity.player.InventoryPlayer ip, IArcaneTerminalHost host) {
            super(ip, host);
        }

        private void allowSampling() {
            this.samplingAllowed = true;
        }

        @Override
        protected float getWorldVis() {
            this.requireSamplingAllowed("world vis");
            return ((TestArcaneTerminalHost) this.getArcaneHost()).visAvailable;
        }

        @Override
        public float getCurrentRequiredVis() {
            this.requireSamplingAllowed("required vis");
            return ((TestArcaneTerminalHost) this.getArcaneHost()).visRequired;
        }

        @Override
        protected float getDiscount(EntityPlayer player) {
            this.requireSamplingAllowed("vis discount");
            return ((TestArcaneTerminalHost) this.getArcaneHost()).discount;
        }

        private void requireSamplingAllowed(String sampleName) {
            if (!this.samplingAllowed) {
                throw new AssertionError("Container construction must not sample " + sampleName);
            }
        }
    }

    private static final class RecordingArcaneInscriber extends ContainerArcaneInscriber {

        private boolean samplingAllowed;

        private RecordingArcaneInscriber(net.minecraft.entity.player.InventoryPlayer ip, IArcaneTerminalHost host) {
            super(ip, host);
        }

        private void allowSampling() {
            this.samplingAllowed = true;
        }

        private void setArcaneRecipePresent(boolean arcaneRecipePresent) {
            ((TestArcaneTerminalHost) this.getArcaneHost()).setArcaneRecipePresent(arcaneRecipePresent);
        }

        private boolean hasArcaneGhostSlots() {
            return this.inventorySlots.stream().anyMatch(SlotArcaneGhostMatrix.class::isInstance);
        }

        @Override
        protected boolean hasArcaneRecipeInMatrix() {
            if (!this.samplingAllowed) {
                throw new AssertionError("Container construction must not sample arcane recipe state");
            }
            return ((TestArcaneTerminalHost) this.getArcaneHost()).arcaneRecipePresent;
        }

        @Override
        public float getCurrentRequiredVis() {
            if (!this.samplingAllowed) {
                throw new AssertionError("Container construction must not sample required vis");
            }
            return ((TestArcaneTerminalHost) this.getArcaneHost()).visRequired;
        }
    }

    private static final class TestArcaneTerminalHost implements IArcaneTerminalHost, IPart, IEnergySource {

        private final ModGUIs gui;
        private final ItemStackHandler crafting = new ItemStackHandler(15);
        private final ThEUpgradeInventory upgrades =
                new ThEUpgradeInventory("upgrades", 1, 1, new ItemStack(Items.STICK));
        private final ThEInternalInventory aeUpgrades = new ThEInternalInventory("Test upgrades", 0, 64);
        private float visAvailable;
        private float visRequired;
        private float discount;
        private boolean arcaneRecipePresent;

        private TestArcaneTerminalHost(ModGUIs gui) {
            this.gui = gui;
        }

        private void setVisSample(float visAvailable, float visRequired, float discount) {
            this.visAvailable = visAvailable;
            this.visRequired = visRequired;
            this.discount = discount;
        }

        private void setArcaneRecipePresent(boolean arcaneRecipePresent) {
            this.arcaneRecipePresent = arcaneRecipePresent;
        }

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
            return this.gui;
        }

        @Override
        public IItemHandler getInventoryByName(String name) {
            return switch (name.toLowerCase(java.util.Locale.ROOT)) {
                case "crafting" -> this.crafting;
                case "upgrades" -> this.upgrades.toItemHandler();
                default -> null;
            };
        }

        @Override
        public ae2.api.upgrades.IUpgradeInventory getArcaneUpgradeInventory() {
            return this.upgrades;
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
            return new MEStorage() {
                @Override
                public ITextComponent getDescription() {
                    return new TextComponentString("arcane terminal gui sync test storage");
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

        @Override
        public void returnToMainContainer(EntityPlayer player, ae2.container.ISubGui subGui) {
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
