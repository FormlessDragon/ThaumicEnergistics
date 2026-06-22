package thaumicenergistics.tile;

import ae2.api.implementations.IPowerChannelState;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.security.IActionSource;
import ae2.api.storage.MEStorage;
import ae2.api.util.DimensionalBlockPos;
import ae2.tile.grid.AENetworkedTile;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Base tile for Thaumic Energistics machines that connect directly through Supergiant's managed grid node lifecycle.
 */
public abstract class ThENetworkTile extends AENetworkedTile implements IPowerChannelState {
    private static final String TAG_POWERED = "powered";
    private static final String TAG_ONLINE = "online";
    private static final String TAG_ACTIVE = "active";

    protected final IActionSource src = IActionSource.ofMachine(this);

    private boolean powered;
    private boolean online;
    private boolean active;

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.getCurrentPoweredState());
        data.writeBoolean(this.getCurrentOnlineState());
        data.writeBoolean(this.getCurrentActiveState());
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean nextPowered = data.readBoolean();
        boolean nextOnline = data.readBoolean();
        boolean nextActive = data.readBoolean();
        changed = changed
                || nextPowered != this.powered
                || nextOnline != this.online
                || nextActive != this.active;
        this.powered = nextPowered;
        this.online = nextOnline;
        this.active = nextActive;
        return changed;
    }

    @Override
    protected void saveVisualState(NBTTagCompound data) {
        super.saveVisualState(data);
        data.setBoolean(TAG_POWERED, this.getCurrentPoweredState());
        data.setBoolean(TAG_ONLINE, this.getCurrentOnlineState());
        data.setBoolean(TAG_ACTIVE, this.getCurrentActiveState());
    }

    @Override
    protected void loadVisualState(NBTTagCompound data) {
        super.loadVisualState(data);
        this.powered = data.hasKey(TAG_POWERED) && data.getBoolean(TAG_POWERED);
        this.online = data.hasKey(TAG_ONLINE) && data.getBoolean(TAG_ONLINE);
        this.active = data.hasKey(TAG_ACTIVE) && data.getBoolean(TAG_ACTIVE);
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        boolean previousPowered = this.powered;
        boolean previousOnline = this.online;
        boolean previousActive = this.active;
        this.powered = this.getCurrentPoweredState();
        this.online = this.getCurrentOnlineState();
        this.active = this.getCurrentActiveState();
        if (previousPowered != this.powered || previousOnline != this.online || previousActive != this.active) {
            this.saveChanges();
            this.markForUpdate();
        }
    }

    public DimensionalBlockPos getLocation() {
        return new DimensionalBlockPos(this);
    }

    @Override
    public boolean isPowered() {
        if (this.world != null && !this.world.isRemote) {
            return this.getCurrentPoweredState();
        }
        return this.powered;
    }

    @Override
    public boolean isActive() {
        if (this.world != null && !this.world.isRemote) {
            return this.getCurrentActiveState();
        }
        return this.active;
    }

    protected MEStorage getNetworkStorage() {
        IGrid grid = this.getMainNode().getGrid();
        if (grid == null || grid.getStorageService() == null) {
            return null;
        }
        return grid.getStorageService().getInventory();
    }

    private boolean getCurrentPoweredState() {
        return this.getMainNode().isPowered();
    }

    private boolean getCurrentOnlineState() {
        return this.getMainNode().isOnline();
    }

    private boolean getCurrentActiveState() {
        return this.getMainNode().isActive();
    }
}
