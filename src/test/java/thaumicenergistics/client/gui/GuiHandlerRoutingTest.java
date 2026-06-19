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
import ae2.container.ISubGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.init.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumicenergistics.api.storage.IArcaneTerminalHost;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.test.FakeMinecraft;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

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

    private static final class ArcaneTerminalPart implements IPart, IArcaneTerminalHost {

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
            return null;
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
