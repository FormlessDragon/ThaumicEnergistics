package thaumicenergistics.tile;

import ae2.api.AECapabilities;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeService;
import ae2.api.networking.IManagedGridNode;
import ae2.api.util.AECableType;
import ae2.block.AEBaseTileBlock;
import ae2.capabilities.Capabilities;
import ae2.core.definitions.BlockDefinition;
import ae2.me.helpers.IGridConnectedTile;
import ae2.tile.AEBaseTile;
import ae2.api.upgrades.Upgrades;
import io.netty.buffer.Unpooled;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import org.objectweb.asm.Type;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumicenergistics.block.BlockBase;
import thaumicenergistics.block.BlockInfusionProvider;
import thaumicenergistics.core.definitions.ThEBlocks;
import thaumicenergistics.test.FakeMinecraft;
import thaumicenergistics.core.definitions.ThEItems;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
class TileNetworkSupergiantMigrationTest {

    @BeforeAll
    static void bootstrapMinecraft() {
        if (!Bootstrap.isRegistered()) {
            Bootstrap.register();
        }
        ASMDataTable asmDataTable = new ASMDataTable();
        asmDataTable.addASMData(
                null,
                CapabilityInject.class.getName(),
                AECapabilities.class.getName(),
                "IN_WORLD_GRID_NODE_HOST",
                Map.of("value", Type.getType("Lae2/api/networking/IInWorldGridNodeHost;")));
        CapabilityManager.INSTANCE.injectCapabilities(asmDataTable);
        Capabilities.register();
    }

    @Test
    void networkTileServerActiveUsesManagedNodeActiveNotOnline() {
        InspectableNetworkTile tile = new InspectableNetworkTile();
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        world.setTileEntity(BlockPos.ORIGIN, tile);
        tile.node.setPowered(true);
        tile.node.setOnline(true);
        tile.node.setActive(false);

        assertAll(
                () -> assertTrue(tile.isPowered()),
                () -> assertFalse(tile.isActive()));
    }

    @Test
    void networkTileClientVisualStateKeepsOnlineAndActiveSeparate() {
        InspectableNetworkTile serverTile = new InspectableNetworkTile();
        serverTile.node.setPowered(true);
        serverTile.node.setOnline(true);
        serverTile.node.setActive(false);
        NBTTagCompound visualState = new NBTTagCompound();

        serverTile.saveVisualStateForTest(visualState);
        InspectableNetworkTile clientTile = new InspectableNetworkTile();
        clientTile.loadVisualStateForTest(visualState);

        assertAll(
                () -> assertTrue(clientTile.isPowered()),
                () -> assertFalse(clientTile.isActive()));
    }

    @Test
    void arcaneAssemblerPartialVisualStateKeepsMissingBooleansAtDefaults() {
        InspectableArcaneAssembler assembler = new InspectableArcaneAssembler();
        NBTTagCompound fullState = new NBTTagCompound();
        fullState.setBoolean("powered", true);
        fullState.setBoolean("online", true);
        fullState.setBoolean("active", true);
        fullState.setBoolean("missingAspect", true);
        fullState.setBoolean("hasEnoughVis", false);
        fullState.setBoolean("hasJob", true);
        fullState.setBoolean("isCrafting", true);
        fullState.setInteger("progress", 73);
        assembler.loadVisualStateForTest(fullState);

        NBTTagCompound partialState = new NBTTagCompound();
        partialState.setBoolean("powered", true);
        partialState.setBoolean("online", true);
        partialState.setBoolean("active", true);
        partialState.setBoolean("hasJob", false);
        assembler.loadVisualStateForTest(partialState);

        assertAll(
                () -> assertFalse(assembler.hasJob()),
                () -> assertTrue(assembler.getHasEnoughVis()),
                () -> assertFalse(assembler.isMissingAspect()),
                () -> assertFalse(assembler.isCrafting()),
                () -> assertEquals(0, assembler.getProgress()));
    }

    @Test
    void arcaneAssemblerVisualStateRoundTripsCraftingFlags() {
        InspectableArcaneAssembler source = new InspectableArcaneAssembler();
        source.node.setPowered(true);
        source.node.setOnline(true);
        source.node.setActive(true);
        NBTTagCompound sourceState = new NBTTagCompound();
        sourceState.setBoolean("missingAspect", true);
        sourceState.setBoolean("hasEnoughVis", false);
        sourceState.setBoolean("hasJob", true);
        sourceState.setBoolean("isCrafting", true);
        sourceState.setInteger("progress", 91);
        source.loadVisualStateForTest(sourceState);
        NBTTagCompound saved = new NBTTagCompound();

        source.saveVisualStateForTest(saved);
        InspectableArcaneAssembler loaded = new InspectableArcaneAssembler();
        loaded.loadVisualStateForTest(saved);

        assertAll(
                () -> assertTrue(loaded.isPowered()),
                () -> assertTrue(loaded.isActive()),
                () -> assertTrue(loaded.isMissingAspect()),
                () -> assertFalse(loaded.getHasEnoughVis()),
                () -> assertTrue(loaded.hasJob()),
                () -> assertTrue(loaded.isCrafting()),
                () -> assertEquals(91, loaded.getProgress()));
    }

    @Test
    void arcaneAssemblerStreamRoundTripsVisualState() {
        InspectableArcaneAssembler source = new InspectableArcaneAssembler();
        source.node.setPowered(true);
        source.node.setOnline(true);
        source.node.setActive(false);
        NBTTagCompound sourceState = new NBTTagCompound();
        sourceState.setBoolean("missingAspect", true);
        sourceState.setBoolean("hasEnoughVis", false);
        sourceState.setBoolean("hasJob", true);
        sourceState.setBoolean("isCrafting", true);
        sourceState.setInteger("progress", 64);
        source.loadVisualStateForTest(sourceState);
        io.netty.buffer.ByteBuf stream = Unpooled.buffer();

        source.writeToStreamForTest(stream);
        InspectableArcaneAssembler loaded = new InspectableArcaneAssembler();
        boolean changed = loaded.readFromStreamForTest(stream);

        assertAll(
                () -> assertTrue(changed),
                () -> assertTrue(loaded.isPowered()),
                () -> assertFalse(loaded.isActive()),
                () -> assertTrue(loaded.isMissingAspect()),
                () -> assertFalse(loaded.getHasEnoughVis()),
                () -> assertTrue(loaded.hasJob()),
                () -> assertTrue(loaded.isCrafting()),
                () -> assertEquals(64, loaded.getProgress()));
    }

    @Test
    void networkTilesExposeLegacyMachineRepresentation() {
        assertAll(
                () -> assertItemStackMatchesDefinition(
                        ThEBlocks.ARCANE_ASSEMBLER,
                        new TileArcaneAssembler().getItemFromTile()),
                () -> assertItemStackMatchesDefinition(
                        ThEBlocks.INFUSION_PROVIDER,
                        new TileInfusionProvider().getItemFromTile()));
    }

    @Test
    void infusionProviderExposesInWorldGridNodeHostCapability() {
        TileInfusionProvider infusionProvider = new TileInfusionProvider();

        assertAll(
                () -> assertTrue(infusionProvider.hasCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, null)),
                () -> assertSame(infusionProvider,
                        infusionProvider.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, null)));
    }

    @Test
    void infusionProviderStreamRoundTripsClientAspects() {
        InspectableInfusionProvider source = new InspectableInfusionProvider();
        source.node.setPowered(true);
        source.node.setOnline(true);
        source.node.setActive(true);
        source.setStoredAspectsForTest(new AspectList()
                .add(Aspect.AIR, 12)
                .add(Aspect.FIRE, 7));
        io.netty.buffer.ByteBuf stream = Unpooled.buffer();

        source.writeToStreamForTest(stream);
        InspectableInfusionProvider loaded = new InspectableInfusionProvider();
        FakeMinecraft.clientWorld().setTileEntity(BlockPos.ORIGIN, loaded);
        boolean changed = loaded.readFromStreamForTest(stream);

        assertAll(
                () -> assertTrue(changed),
                () -> assertEquals(12, loaded.containerContains(Aspect.AIR)),
                () -> assertEquals(7, loaded.containerContains(Aspect.FIRE)),
                () -> assertEquals(0, loaded.containerContains(Aspect.WATER)));
    }

    @Test
    void infusionProviderBlockActivationRequestsVisualUpdate() {
        FakeMinecraft.FakeWorld world = FakeMinecraft.serverWorld();
        BlockPos pos = new BlockPos(10, 11, 12);
        RecordingInfusionProvider tile = new RecordingInfusionProvider();
        world.setTileEntity(pos, tile);

        boolean handled = new BlockInfusionProvider().onBlockActivated(
                world,
                pos,
                ThEBlocks.INFUSION_PROVIDER.block().getDefaultState(),
                FakeMinecraft.player(world),
                EnumHand.MAIN_HAND,
                EnumFacing.UP,
                0.5f,
                0.5f,
                0.5f);

        assertAll(
                () -> assertTrue(handled),
                () -> assertEquals(1, tile.saveChangesCalls),
                () -> assertEquals(1, tile.markForUpdateCalls),
                () -> assertEquals(0, tile.markDirtyCalls));
    }

    @Test
    void arcaneAssemblerAddsCoreAndUpgradeInventoriesToAdditionalDrops() {
        Upgrades.add(ThEItems.UPGRADE_ARCANE.item(), ThEBlocks.ARCANE_ASSEMBLER.item(), 1);
        TileArcaneAssembler assembler = new TileArcaneAssembler();
        IItemHandler cores = assembler.getInventoryByName("cores");
        IItemHandler upgrades = assembler.getInventoryByName("upgrades");
        ItemStack core = ThEItems.KNOWLEDGE_CORE.stack(1);
        ItemStack upgrade = ThEItems.UPGRADE_ARCANE.stack(1);

        assertTrue(cores.insertItem(0, core.copy(), false).isEmpty());
        assertTrue(upgrades.insertItem(0, upgrade.copy(), false).isEmpty());

        List<ItemStack> drops = new ArrayList<>();
        assembler.addAdditionalDrops(drops);

        assertAll(
                () -> assertFalse(drops.isEmpty()),
                () -> assertTrue(drops.stream().anyMatch(drop -> ItemStack.areItemStacksEqual(core, drop))),
                () -> assertTrue(drops.stream().anyMatch(drop -> ItemStack.areItemStacksEqual(upgrade, drop))));
    }

    private static void assertItemStackMatchesDefinition(BlockDefinition<?> definition, ItemStack stack) {
        assertAll(
                () -> assertFalse(stack.isEmpty()),
                () -> assertSame(definition.item(), stack.getItem()),
                () -> assertEquals(1, stack.getCount()));
    }

    private static void assertContainsItem(List<ItemStack> drops, ItemStack expected) {
        assertTrue(drops.stream().anyMatch(drop -> ItemStack.areItemStacksEqual(expected, drop)));
    }

    private static List<String> powerStateKeys() {
        return List.of(
                "tooltip.thaumicenergistics.device_offline",
                "tooltip.thaumicenergistics.device_missing_channel",
                "tooltip.thaumicenergistics.device_online");
    }

    private static final class TestNetworkBlock extends BlockBase<ThENetworkTile> {
        private TestNetworkBlock() {
            super(ThENetworkTile.class);
        }
    }

    private static final class RecordingGridConnectedTile extends TileEntity implements IGridConnectedTile {
        private final IManagedGridNode mainNode = new RecordingManagedGridNode();
        private int mainNodeCalls;
        private int actionableNodeCalls;
        private boolean ownerSet;
        private boolean ownerWasSetBeforeActionableNode;

        @Override
        public IManagedGridNode getMainNode() {
            this.mainNodeCalls++;
            return this.mainNode;
        }

        @Override
        public IGridNode getActionableNode() {
            this.actionableNodeCalls++;
            this.ownerWasSetBeforeActionableNode = this.ownerSet;
            return null;
        }

        @Override
        public void setOwner(EntityPlayer owner) {
            this.ownerSet = true;
        }

        @Override
        public void saveChanges() {
        }

        @Override
        public AECableType getCableConnectionType(EnumFacing dir) {
            return AECableType.SMART;
        }
    }

    private static final class RecordingDropTile extends AEBaseTile {
        private int additionalDropCalls;
        private final List<ItemStack> additionalDropItems = new ArrayList<>();

        @Override
        public void addAdditionalDrops(List<ItemStack> drops) {
            this.additionalDropCalls++;
            ItemStack drop = ThEItems.UPGRADE_ARCANE.stack(1);
            this.additionalDropItems.add(drop.copy());
            drops.add(drop);
        }
    }

    private static class InspectableNetworkTile extends ThENetworkTile {
        private RecordingManagedGridNode node;

        @Override
        protected IManagedGridNode createMainNode() {
            this.node = new RecordingManagedGridNode();
            return this.node;
        }

        @Override
        public ItemStack getItemFromTile() {
            return ThEBlocks.INFUSION_PROVIDER.stack();
        }

        void saveVisualStateForTest(NBTTagCompound data) {
            this.saveVisualState(data);
        }

        void loadVisualStateForTest(NBTTagCompound data) {
            this.loadVisualState(data);
        }

        void setClientVisualPowerState(boolean powered, boolean online, boolean active) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("powered", powered);
            tag.setBoolean("online", online);
            tag.setBoolean("active", active);
            this.loadVisualState(tag);
        }

    }

    private static final class InspectableArcaneAssembler extends TileArcaneAssembler {
        private RecordingManagedGridNode node;

        @Override
        protected IManagedGridNode createMainNode() {
            this.node = new RecordingManagedGridNode();
            return this.node;
        }

        void saveVisualStateForTest(NBTTagCompound data) {
            this.saveVisualState(data);
        }

        void loadVisualStateForTest(NBTTagCompound data) {
            this.loadVisualState(data);
        }

        void writeToStreamForTest(io.netty.buffer.ByteBuf data) {
            this.writeToStream(data);
        }

        boolean readFromStreamForTest(io.netty.buffer.ByteBuf data) {
            return this.readFromStream(data);
        }

    }

    private static class InspectableInfusionProvider extends TileInfusionProvider {
        private RecordingManagedGridNode node;
        private AspectList storedAspects = new AspectList();

        @Override
        protected IManagedGridNode createMainNode() {
            this.node = new RecordingManagedGridNode();
            return this.node;
        }

        @Override
        protected AspectList getStoredAspectSnapshot() {
            return this.storedAspects.copy();
        }

        void setStoredAspectsForTest(AspectList storedAspects) {
            this.storedAspects = storedAspects.copy();
        }

        void writeToStreamForTest(io.netty.buffer.ByteBuf data) {
            this.writeToStream(data);
        }

        boolean readFromStreamForTest(io.netty.buffer.ByteBuf data) {
            return this.readFromStream(data);
        }

    }

    private static final class RecordingInfusionProvider extends InspectableInfusionProvider {
        private int saveChangesCalls;
        private int markForUpdateCalls;
        private int markDirtyCalls;

        @Override
        public void saveChanges() {
            this.saveChangesCalls++;
        }

        @Override
        public void markForUpdate() {
            this.markForUpdateCalls++;
        }

        @Override
        public void markDirty() {
            this.markDirtyCalls++;
        }
    }

    private static final class RecordingManagedGridNode implements IManagedGridNode {
        private boolean active;
        private boolean online;
        private boolean powered;
        private Set<EnumFacing> exposedSides = EnumSet.allOf(EnumFacing.class);
        private ItemStack visualRepresentation = ItemStack.EMPTY;

        void setActive(boolean active) {
            this.active = active;
        }

        void setOnline(boolean online) {
            this.online = online;
        }

        void setPowered(boolean powered) {
            this.powered = powered;
        }

        @Override
        public void destroy() {
        }

        @Override
        public void create(World level, BlockPos blockPos) {
        }

        @Override
        public void loadFromNBT(NBTTagCompound nodeData) {
        }

        @Override
        public void saveToNBT(NBTTagCompound nodeData) {
        }

        @Override
        public IManagedGridNode setFlags(GridFlags... flags) {
            return this;
        }

        @Override
        public IManagedGridNode setExposedOnSides(Set<EnumFacing> directions) {
            this.exposedSides = EnumSet.copyOf(directions);
            return this;
        }

        @Override
        public IManagedGridNode setIdlePowerUsage(double usagePerTick) {
            return this;
        }

        @Override
        public IManagedGridNode setVisualRepresentation(ItemStack visualRepresentation) {
            this.visualRepresentation = visualRepresentation.copy();
            return this;
        }

        @Override
        public IManagedGridNode setVisualRepresentation(ae2.api.stacks.AEItemKey visualRepresentation) {
            return this;
        }

        @Override
        public IManagedGridNode setInWorldNode(boolean accessible) {
            return this;
        }

        @Override
        public IManagedGridNode setTagName(String tagName) {
            return this;
        }

        @Override
        public IManagedGridNode setGridColor(ae2.api.util.AEColor gridColor) {
            return this;
        }

        @Override
        public <T extends IGridNodeService> IManagedGridNode addService(Class<T> serviceClass, T service) {
            return this;
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public boolean isActive() {
            return this.active;
        }

        @Override
        public boolean isOnline() {
            return this.online;
        }

        @Override
        public boolean isPowered() {
            return this.powered;
        }

        @Override
        public boolean hasGridBooted() {
            return this.online || this.active;
        }

        @Override
        public void setOwningPlayerId(int ownerPlayerId) {
        }

        @Override
        public void setOwningPlayer(net.minecraft.entity.player.EntityPlayer ownerPlayer) {
        }

        @Override
        public IGridNode getNode() {
            return null;
        }
    }

    private static final class RecordingWorld extends FakeMinecraft.FakeWorld {
        private final List<ItemStack> spawnedItems = new ArrayList<>();

        private RecordingWorld() {
            super(false);
        }

        @Override
        public boolean spawnEntity(Entity entityIn) {
            if (entityIn instanceof EntityItem) {
                this.spawnedItems.add(((EntityItem) entityIn).getItem().copy());
                return true;
            }
            throw new UnsupportedOperationException("RecordingWorld only records item drops");
        }

        @Override
        public void removeTileEntity(BlockPos pos) {
        }
    }

}
