package thaumicenergistics.part;

import ae2.api.config.AccessRestriction;
import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.config.StorageFilter;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.events.GridBootingStatusChange;
import ae2.api.networking.events.GridPowerStatusChange;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartModel;
import ae2.api.storage.IStorageMounts;
import ae2.api.storage.IStorageProvider;
import ae2.api.util.AECableType;
import ae2.container.ISubGui;
import ae2.helpers.IPriorityHost;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.integration.appeng.ThEPartModel;
import thaumicenergistics.integration.appeng.grid.EssentiaContainerMEStorage;
import thaumicenergistics.item.part.ItemEssentiaStorageBus;
import thaumicenergistics.util.AEUtil;
import thaumicenergistics.util.ForgeUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author BrockWS
 * @author Alex811
 */
public class PartEssentiaStorageBus extends PartSharedEssentiaBus implements IStorageProvider, IPriorityHost {

    public static ResourceLocation[] MODELS = new ResourceLocation[]{
            new ResourceLocation(Reference.MOD_ID, "part/essentia_storage_bus/base"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_storage_bus/on"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_storage_bus/off"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_storage_bus/has_channel")
    };

    private static final IPartModel MODEL_ON = new ThEPartModel(MODELS[0], MODELS[1]);
    private static final IPartModel MODEL_OFF = new ThEPartModel(MODELS[0], MODELS[2]);
    private static final IPartModel MODEL_HAS_CHANNEL = new ThEPartModel(MODELS[0], MODELS[3]);

    private EssentiaContainerMEStorage storage;
    private boolean wasActive = false;
    private IAspectContainer lastConnectedContainer = null;
    private int priority = 0;
    private final Runnable upgradesChanged = this::upgradesChanged;

    public PartEssentiaStorageBus(ItemEssentiaStorageBus item) {
        super(item, 63, 5);
    }

    @Override
    protected AESettings.SUBJECT getAESettingSubject() {
        return AESettings.SUBJECT.ESSENTIA_STORAGE_BUS;
    }

    @Override
    protected IManagedGridNode configureGridNode(IManagedGridNode node) {
        return node.addService(IStorageProvider.class, this);
    }

    @Override
    public void settingChanged(Setting<?> setting) {
        super.settingChanged(setting);
        EssentiaContainerMEStorage storage = this.storage;
        if (storage != null) {
            if (setting == Settings.ACCESS)
                storage.setBaseAccess(this.getConfigManager().getSetting(Settings.ACCESS));
            else if (setting == Settings.STORAGE_FILTER)
                storage.setReportInaccessible(this.getConfigManager().getSetting(Settings.STORAGE_FILTER));
            else
                return;
            this.triggerUpdate();
        }
    }

    protected void upgradesChanged() {
        EssentiaContainerMEStorage storage = this.getStorage();
        if (storage != null)
            storage.setWhitelist(!this.hasInverterCard());
        this.triggerUpdate();
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.lastConnectedContainer = this.getConnectedContainer();
        this.upgradeChangeListeners.add(this.upgradesChanged);
        this.upgradesChanged();
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.upgradeChangeListeners.remove(this.upgradesChanged);
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(ThEApi.instance().config().tickTimeEssentiaStorageBusMin(), ThEApi.instance().config().tickTimeEssentiaStorageBusMax(), false);
    }

    @Override
    public boolean canWork() {
        return false;
    }

    @Override
    protected TickRateModulation doWork() {
        return TickRateModulation.SLOWER;
    }

    @Override
    public void mountInventories(IStorageMounts mounts) {
        EssentiaContainerMEStorage storage = this.getStorage();
        if (storage != null) {
            mounts.mount(storage, this.priority);
        }
    }

    @Override
    public void onNeighborChanged(IBlockAccess access, BlockPos pos, BlockPos neighbor) {
        if (pos == null || neighbor == null)
            return;
        if (pos.offset(this.side).equals(neighbor)) {
            IAspectContainer connectedContainer = this.getConnectedContainer();
            if (this.lastConnectedContainer != connectedContainer) {
                this.lastConnectedContainer = connectedContainer;
                this.storage = null;
            }
            this.triggerUpdate();
        }
        super.onNeighborChanged(access, pos, neighbor);
    }

    @Override
    public boolean onUseItemOn(ItemStack itemStack, EntityPlayer player, EnumHand hand, Vec3d vec3d) {
        if ((player.isSneaking() && AEUtil.isWrench(player.getHeldItem(hand), player, this.getTile().getPos())))
            return false;

        if (ForgeUtil.isServer())
            GuiHandler.openGUI(ModGUIs.ESSENTIA_STORAGE_BUS, player, this.hostTile.getPos(), this.side);

        return true;
    }

    @Override
    public int getPriority() {
        return this.priority;
    }

    @Override
    public void setPriority(int i) {
        this.priority = i;
        this.triggerUpdate();
        this.host.markForSave();
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return this.getRepr();
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        GuiHandler.openGUI(ModGUIs.ESSENTIA_STORAGE_BUS, player, this.hostTile.getPos(), this.side);
    }

    @Nonnull
    @Override
    public IPartModel getStaticModels() {
        if (this.isPowered())
            if (this.isActive())
                return MODEL_HAS_CHANNEL;
            else
                return MODEL_ON;
        return MODEL_OFF;
    }

    @Nullable
    private EssentiaContainerMEStorage getStorage() {
        if (this.storage == null) {
            IAspectContainer connectedContainer = this.getConnectedContainer();
            if (connectedContainer != null)
                return this.storage = new EssentiaContainerMEStorage(connectedContainer, this.config,
                        !this.hasInverterCard(),
                        this.getConfigManager().getSetting(Settings.ACCESS),
                        this.getConfigManager().getSetting(Settings.STORAGE_FILTER)
                );
            return null;
        }
        return this.storage;
    }

    private IAspectContainer getConnectedContainer() {
        return this.getConnectedTE() instanceof IAspectContainer ? (IAspectContainer) this.getConnectedTE() : null;
    }

    @Override
    public void getBoxes(IPartCollisionHelper box) {
        box.addBox(3, 3, 15, 13, 13, 16);
        box.addBox(2, 2, 14, 14, 14, 15);
        box.addBox(5, 5, 12, 11, 11, 14);
    }

    @Override
    public float getCableConnectionLength(AECableType aeCableType) {
        return 4;
    }

    @Override
    public void updateBootStatus(GridBootingStatusChange event) {
        super.updateBootStatus(event);
        this.triggerBootUpdate();
    }

    @Override
    public void updatePowerStatus(GridPowerStatusChange event) {
        super.updatePowerStatus(event);
        this.triggerBootUpdate();
    }

    public void triggerBootUpdate() {
        IGridNode node = this.getGridNode();
        final boolean currentActive = node != null && node.isActive();
        if (this.wasActive != currentActive) {
            this.wasActive = currentActive;
            this.triggerUpdate();
        }
    }

    public void triggerUpdate() {
        if (this.managedGridNode != null && this.managedGridNode.isReady()) {
            IStorageProvider.requestUpdate(this.managedGridNode);
        }
        if (this.host != null) {
            this.host.markForUpdate();
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        this.priority = tag.getInteger("priority");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("priority", this.priority);
    }
}
