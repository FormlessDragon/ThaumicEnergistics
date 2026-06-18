package thaumicenergistics.tile;

import ae2.api.AECapabilities;
import ae2.api.implementations.IPowerChannelState;
import ae2.api.networking.IGrid;
import ae2.api.networking.GridFlags;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IInWorldGridNodeHost;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.events.GridBootingStatusChange;
import ae2.api.networking.events.GridPowerStatusChange;
import ae2.api.networking.security.IActionHost;
import ae2.api.networking.security.IActionSource;
import ae2.api.util.AECableType;
import ae2.api.util.DimensionalBlockPos;
import ae2.block.IOwnerAwareTile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import thaumicenergistics.api.IThELangKey;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.util.ForgeUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author BrockWS
 * @author Alex811
 */
@Deprecated
public abstract class TileNetwork extends TileBase implements IInWorldGridNodeHost, IActionHost, IPowerChannelState, IOwnerAwareTile {

    protected IManagedGridNode managedGridNode;
    protected IGridNode gridNode;
    protected final IActionSource src;
    protected EntityPlayer owner;
    protected boolean isPowered = false;
    protected boolean isActive = false;

    public TileNetwork() {
        this.src = IActionSource.ofMachine(this);
    }

    public IGridNode getGridNode() {
        return this.gridNode;
    }

    @Override
    public void invalidate() {
        if (this.managedGridNode != null) {
            this.managedGridNode.destroy();
            this.managedGridNode = null;
            this.gridNode = null;
        }
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.invalidate();
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull EnumFacing side) {
        return this.getActionableNode();
    }

    @Nonnull
    public AECableType getCableConnectionType(@Nonnull EnumFacing side) {
        return AECableType.SMART;
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        if (this.managedGridNode == null && ForgeUtil.isServer()) {
            this.managedGridNode = this.configureGridNode(GridHelper.createManagedNode(this, (owner, node) -> owner.gridChanged())
                    .setFlags(GridFlags.REQUIRE_CHANNEL)
                    .setIdlePowerUsage(this.getIdlePowerUsage())
                    .setInWorldNode(true)
                    .setVisualRepresentation(this.getMachineRepresentation()));
            if (this.owner != null) {
                this.managedGridNode.setOwningPlayer(this.owner);
            }
            this.managedGridNode.create(this.getWorld(), this.getPos());
            this.gridNode = this.managedGridNode.getNode();
        }
        return this.gridNode;
    }

    protected IManagedGridNode configureGridNode(IManagedGridNode node) {
        return node;
    }

    protected double getIdlePowerUsage() {
        return 1;
    }

    @Nonnull
    protected ItemStack getMachineRepresentation() {
        World world = this.getWorld();
        BlockPos pos = this.getPos();
        if (world == null || pos == null) {
            return ItemStack.EMPTY;
        }
        IBlockState state = world.getBlockState(pos);
        return new ItemStack(state.getBlock());
    }

    public DimensionalBlockPos getLocation() {
        return new DimensionalBlockPos(this);
    }

    public void gridChanged() {

    }

    @Override
    public void setOwner(EntityPlayer player) {
        this.owner = player;
    }

    public EntityPlayer getOwner() {
        return this.owner;
    }

    public void securityBreak() {
        this.getWorld().destroyBlock(this.getPos(), true);
    }

    @Override
    public boolean isPowered() {
        return this.isPowered;
    }

    @Override
    public boolean isActive() {
        if (ForgeUtil.isServer())
            return this.gridNode != null && this.gridNode.isActive();
        else
            return this.isActive;
    }

    @Override
    public void markDirty() {   // server-side, initiate client sync
        super.markDirty();
        if (world == null) return;
        IBlockState state = world.getBlockState(this.getPos());
        world.notifyBlockUpdate(this.getPos(), state, state, 2);
    }

    public final void updateBootStatus(GridBootingStatusChange event) {   // sync client
        this.markDirty();
    }

    public void updatePowerStatus(GridPowerStatusChange event) {   // sync client
        IGridNode node = this.getGridNode();
        IGrid grid = node != null ? node.grid() : null;
        this.isPowered = grid != null && grid.getEnergyService() != null && grid.getEnergyService().isNetworkPowered();
        this.markDirty();
    }

    @Override
    public NBTTagCompound getUpdateTag() {  // sync, server-side, returns what to send to the client when the TileEntity's chunk gets loaded by it
        NBTTagCompound nbtTagCompound = super.getUpdateTag();
        nbtTagCompound.setBoolean("powered", this.isPowered());
        nbtTagCompound.setBoolean("active", this.isActive());
        return nbtTagCompound;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tag) {   // sync, client-side, receives from getUpdateTag()
        super.handleUpdateTag(tag);
        isPowered = tag.getBoolean("powered");
        isActive = tag.getBoolean("active");
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() { // sync, server-side, returns what to send to the client on block update, triggered by markDirty()
        return new SPacketUpdateTileEntity(this.getPos(), 1, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) { // sync, client-side, receives from getUpdatePacket()
        handleUpdateTag(packet.getNbtCompound());
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == AECapabilities.IN_WORLD_GRID_NODE_HOST) {
            return AECapabilities.IN_WORLD_GRID_NODE_HOST.cast(this);
        }
        return super.getCapability(capability, facing);
    }

    public void withPowerStateText(Consumer<String> consumer, Function<IThELangKey, String> localizationMapper) {
        if (this.isPowered()) {
            if (this.isActive())
                consumer.accept(localizationMapper.apply(ThEApi.instance().lang().deviceOnline()));
            else
                consumer.accept(localizationMapper.apply(ThEApi.instance().lang().deviceMissingChannel()));
        } else
            consumer.accept(localizationMapper.apply(ThEApi.instance().lang().deviceOffline()));
    }
}
