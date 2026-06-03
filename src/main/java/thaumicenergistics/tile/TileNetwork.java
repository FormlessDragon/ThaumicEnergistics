package thaumicenergistics.tile;

import ae2.api.implementations.IPowerChannelState;
import ae2.api.networking.GridFlags;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.events.GridBootingStatusChange;
import ae2.api.networking.events.GridPowerStatusChange;
import ae2.api.networking.security.IActionHost;
import ae2.api.stacks.AEItemKey;
import ae2.api.util.AECableType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.util.EnumFacing;
import thaumicenergistics.api.IThELangKey;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.integration.appeng.compat.DimensionalCoord;
import thaumicenergistics.integration.appeng.compat.GridAccessException;
import thaumicenergistics.integration.appeng.grid.GridUtil;
import thaumicenergistics.integration.appeng.grid.IThEGridHost;
import thaumicenergistics.integration.appeng.grid.ThEGridBlock;
import thaumicenergistics.integration.appeng.util.ThEActionSource;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.IThEGridNodeBlock;
import thaumicenergistics.util.IThEOwnable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author BrockWS
 * @author Alex811
 */
public abstract class TileNetwork extends TileBase implements IThEGridHost, IActionHost, IPowerChannelState, IThEOwnable, IThEGridNodeBlock {

    protected ThEGridBlock gridBlock;
    protected IManagedGridNode managedGridNode;
    protected IGridNode gridNode;
    protected ThEActionSource src;
    protected EntityPlayer owner;
    protected boolean isPowered = false;
    protected boolean isActive = false;

    public TileNetwork() {
        this.gridBlock = new ThEGridBlock(this, this, true);
        this.src = new ThEActionSource(this);
    }

    public ThEGridBlock getGridBlock() {
        return this.gridBlock;
    }

    @Override
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
                    .setIdlePowerUsage(this.gridBlock.getIdlePowerUsage())
                    .setInWorldNode(this.gridBlock.isWorldAccessible())
                    .setVisualRepresentation(AEItemKey.of(this.gridBlock.getMachineRepresentation())));
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

    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Override
    public void gridChanged() {

    }

    @Override
    public void setOwner(EntityPlayer player) {
        this.owner = player;
    }

    @Override
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
        try {
            this.isPowered = GridUtil.getEnergyGrid(this).isNetworkPowered();
            this.markDirty();
        } catch (GridAccessException e) {
            // should ignore?
            this.isPowered = false;
        }
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
