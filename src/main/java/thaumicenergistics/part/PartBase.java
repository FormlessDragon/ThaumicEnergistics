package thaumicenergistics.part;

import ae2.api.config.Setting;
import ae2.api.implementations.IPowerChannelState;
import ae2.api.networking.GridFlags;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IManagedGridNode;
import ae2.api.networking.events.GridBootingStatusChange;
import ae2.api.networking.events.GridPowerStatusChange;
import ae2.api.networking.security.IActionHost;
import ae2.api.networking.security.IActionSource;
import ae2.api.parts.*;
import ae2.api.util.AECableType;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.items.IItemHandler;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.integration.appeng.compat.DimensionalCoord;
import thaumicenergistics.integration.appeng.compat.GridAccessException;
import thaumicenergistics.integration.appeng.compat.ThEPartItemStack;
import thaumicenergistics.integration.appeng.compat.Upgrades;
import thaumicenergistics.integration.appeng.grid.GridUtil;
import thaumicenergistics.integration.appeng.grid.IThEGridHost;
import thaumicenergistics.integration.appeng.grid.ThEGridBlock;
import thaumicenergistics.integration.appeng.util.ThEActionSource;
import thaumicenergistics.integration.appeng.util.ThEConfigManager;
import thaumicenergistics.item.ItemPartBase;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.IThEGridNodeBlock;
import thaumicenergistics.util.IThEOwnable;
import thaumicenergistics.util.ItemHandlerUtil;
import thaumicenergistics.util.inventory.IThEInvTile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * @author BrockWS
 * @author Alex811
 */
public abstract class PartBase implements IPart, IThEGridHost, IActionHost, IPowerChannelState, IThEInvTile, IThEOwnable, IThEGridNodeBlock {

    protected ThEConfigManager configManager = new ThEConfigManager();

    protected ThEGridBlock gridBlock;
    protected IManagedGridNode managedGridNode;
    protected IGridNode gridNode;
    protected IPartHost host;
    protected TileEntity hostTile;
    protected EntityPlayer owner;
    protected ItemPartBase item;
    protected int lightOpacity = -1;
    public EnumFacing side;
    public IActionSource source;

    protected boolean isPowered;
    protected boolean isActive;

    public PartBase(ItemPartBase item) {
        this.item = item;
        this.source = new ThEActionSource(this);
        this.getConfigManager().registerSettings(this.getAESettingSubject());
    }

    protected abstract AESettings.SUBJECT getAESettingSubject();

    protected IManagedGridNode configureGridNode(IManagedGridNode node) {
        return node;
    }

    public void settingChanged(Setting<?> setting) {
    }

    public ItemStack getRepr() {
        return new ItemStack(this.item);
    }

    public ItemStack getItemStack(ThEPartItemStack type) {
        return getRepr();
    }

    @Override
    public IPartItem<?> getPartItem() {
        return this.item;
    }

    public boolean canWork() {
        return false;
    }

    public double getIdlePowerUsage() {
        return 0;
    }

    @Override
    public DimensionalCoord getLocation() {
        if (this.hostTile != null && this.hostTile.hasWorld() && this.hostTile.getWorld().provider != null)
            return new DimensionalCoord(this.hostTile.getWorld(), this.hostTile.getPos());
        return null;
    }

    @Override
    public boolean requireDynamicRender() {
        return false;
    }

    @Override
    public boolean isSolid() {
        return false;
    }

    @Override
    public boolean canConnectRedstone() {
        return false;
    }

    @Override
    public void writeToNBT(NBTTagCompound nbt) {
        if (managedGridNode != null)
            this.managedGridNode.saveToNBT(nbt);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        if (managedGridNode != null)
            this.managedGridNode.loadFromNBT(nbt);
    }

    protected int blockLight(int emit) {
        if (this.lightOpacity >= 0)
            return (int) (emit * (this.lightOpacity / 255.0F));
        TileEntity te = this.getTile();
        return this.lightOpacity = 255 - te.getWorld().getBlockLightOpacity(te.getPos().offset(this.side));
    }

    public ThEConfigManager getConfigManager() {
        return this.configManager;
    }

    @Override
    public int getLightLevel() {
        return 0;
    }

    @Override
    public boolean isLadder(EntityLivingBase entityLivingBase) {
        return false;
    }

    @Override
    public void onNeighborChanged(IBlockAccess iBlockAccess, BlockPos blockPos, BlockPos blockPos1) {
        this.host.markForUpdate();
    }

    @Override
    public int isProvidingStrongPower() {
        return 0;
    }

    @Override
    public int isProvidingWeakPower() {
        return 0;
    }

    @Override
    public void writeToStream(PacketBuffer buf) {
        buf.writeBoolean(this.isActive());
        buf.writeBoolean(this.isPowered());
    }

    @Override
    public boolean readFromStream(PacketBuffer buf) {
        this.isActive = buf.readBoolean();
        this.isPowered = buf.readBoolean();
        return true;
    }

    @Override
    public IGridNode getGridNode() {
        return this.gridNode;
    }

    @Override
    public void onEntityCollision(Entity entity) {

    }

    @Override
    public void removeFromWorld() {
        if (this.managedGridNode != null)
            this.managedGridNode.destroy();
    }

    @Override
    public void addToWorld() {
        if (ForgeUtil.isClient())
            return;
        this.gridBlock = new ThEGridBlock(this);
        this.managedGridNode = this.configureGridNode(GridHelper.createManagedNode(this, (owner, node) -> owner.gridChanged())
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(this.getIdlePowerUsage())
                .setInWorldNode(false)
                .setVisualRepresentation(this.getRepr()));
        if (this.owner != null) {
            this.managedGridNode.setOwningPlayer(this.owner);
        }
        if (this.hostTile != null) {
            this.managedGridNode.create(this.hostTile.getWorld(), this.hostTile.getPos());
        }
        this.gridNode = this.managedGridNode.getNode();
        //this.setPower(null); TODO
        BlockPos pos = this.gridBlock.getLocation().getPos();
        this.onNeighborChanged(null, pos, pos.offset(this.side));
    }

    @Override
    public IGridNode getExternalFacingNode() {
        return null;
    }

    @Override
    public void setPartHostInfo(EnumFacing side, IPartHost host, TileEntity te) {
        this.side = side;
        this.host = host;
        this.hostTile = te;
        // TODO this.setPower(null);
    }

    @Override
    public boolean onUseItemOn(ItemStack stack, EntityPlayer entityPlayer, EnumHand enumHand, Vec3d vec3d) {
        return false;
    }

    @Override
    public boolean onShiftClicked(EntityPlayer entityPlayer, Vec3d vec3d) {
        return false;
    }

    @Override
    public void addPartDrop(List<ItemStack> list, boolean b) {
        list.addAll(ItemHandlerUtil.getInventoryAsList(this.getInventoryByName("upgrades")));
    }

    @Override
    public float getCableConnectionLength(AECableType aeCableType) {
        return 3;
    }

    @Override
    public void animateTick(World world, BlockPos blockPos, Random random) {

    }

    @Override
    public void onPlacement(EntityPlayer player) {
        this.setOwner(player);
    }

    @Override
    public void setOwner(EntityPlayer player) {
        this.owner = player;
    }

    @Override
    public EntityPlayer getOwner() {
        return this.owner;
    }

    @Override
    public boolean canBePlacedOn(BusSupport busSupport) {
        return busSupport == BusSupport.CABLE;
    }

    @Override
    public void getBoxes(IPartCollisionHelper box) {
        // Should be overridden and not used
        box.addBox(4, 4, 12, 12, 12, 14);
        box.addBox(6, 6, 11, 10, 10, 12);
    }

    @Nullable
    public IGridNode getGridNode(@Nonnull EnumFacing dir) {
        return this.gridNode;
    }

    @Override
    public ThEGridBlock getGridBlock() {
        return this.gridBlock;
    }

    @Nonnull
    public AECableType getCableConnectionType(@Nonnull EnumFacing dir) {
        return AECableType.GLASS;
    }

    public void securityBreak() {
        if (this.getRepr().isEmpty() || this.getGridNode() == null) return;
        this.host.removePart(this);
        EnumFacing facing = this.side;
        Vec3d offset = new Vec3d(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
        offset = offset.scale(.5);
        BlockPos pos = this.getTile().getPos();
        Vec3d posVec = new Vec3d(pos).add(.5, .5, .5).add(offset);
        World world = this.getTile().getWorld();
        world.playEvent(2001, pos, Block.getStateId(world.getBlockState(pos)));
        world.spawnEntity(new EntityItem(world, posVec.x, posVec.y, posVec.z, this.getRepr().copy()));
    }

    @Override
    public boolean isPowered() {
        return this.isPowered;
    }

    @Override
    public boolean isActive() {
        return this.gridNode != null ? this.gridNode.isActive() : this.isActive;
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return this.gridNode;
    }

    public void updateBootStatus(GridBootingStatusChange event) {
        this.host.markForUpdate();
    }

    public void updatePowerStatus(GridPowerStatusChange event) {
        try {
            this.isPowered = GridUtil.getEnergyGrid(this).isNetworkPowered();
            this.host.markForUpdate();
        } catch (GridAccessException e) {
            // should ignore?
            this.isPowered = false;
        }
    }

    @Override
    public void gridChanged() {

    }

    public int getInstalledUpgrades(Upgrades u) {
        return 0;
    }

    public TileEntity getTile() {
        return this.hostTile;
    }

    public IItemHandler getInventoryByName(String name) {
        return null;
    }
}
