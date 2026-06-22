package thaumicenergistics.tile;

import ae2.api.config.Actionable;
import ae2.api.networking.GridFlags;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import java.util.Objects;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumicenergistics.core.definitions.ThEBlocks;
import thaumicenergistics.me.key.AEEssentiaKey;
import thaumicenergistics.util.ForgeUtil;

/**
 * @author BrockWS
 */
public class TileInfusionProvider extends ThENetworkTile implements IAspectSource {
    private static final String TAG_STORED_ASPECTS = "storedAspects";

    // Client side only, for rendering aspect icons with goggles
    private AspectList clientAspects = new AspectList();

    public TileInfusionProvider() {
        super();
        this.getMainNode()
                .setIdlePowerUsage(1.0)
                .setFlags(GridFlags.REQUIRE_CHANNEL);
    }

    @Override
    public ItemStack getItemFromTile() {
        return ThEBlocks.INFUSION_PROVIDER.stack();
    }

    public KeyCounter getStoredAspects() {
        KeyCounter essentia = new KeyCounter();
        MEStorage storage = this.getNetworkStorage();
        if (storage == null) {
            return essentia;
        }
        for (Object2LongMap.Entry<AEKey> entry : storage.getAvailableStacks()) {
            if (entry.getKey() instanceof AEEssentiaKey && entry.getLongValue() > 0) {
                essentia.add(entry.getKey(), entry.getLongValue());
            }
        }
        return essentia;
    }

    @Override
    public AspectList getAspects() {
        if (this.isClientTile())
            return this.clientAspects;
        return this.getStoredAspectSnapshot();
    }

    protected AspectList getStoredAspectSnapshot() {
        AspectList list = new AspectList();
        KeyCounter stored = this.getStoredAspects();
        for (Object2LongMap.Entry<AEKey> entry : stored) {
            if (entry.getKey() instanceof AEEssentiaKey) {
                AEEssentiaKey key = (AEEssentiaKey) entry.getKey();
                long amount = entry.getLongValue();
                list.add(key.getAspect(), amount >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount);
            }
        }
        return list;
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        NBTTagCompound tag = new NBTTagCompound();
        this.getStoredAspectSnapshot().writeToNBT(tag, TAG_STORED_ASPECTS);
        ByteBufUtils.writeTag(data, tag);
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        NBTTagCompound tag = Objects.requireNonNull(ByteBufUtils.readTag(data), TAG_STORED_ASPECTS);
        AspectList nextClientAspects = new AspectList();
        nextClientAspects.readFromNBT(tag, TAG_STORED_ASPECTS);
        changed = changed || !aspectListsEqual(this.clientAspects, nextClientAspects);
        this.clientAspects = nextClientAspects;
        return changed;
    }

    public void refreshVisualState() {
        this.saveChanges();
        this.markForUpdate();
    }

    @Override
    public boolean takeFromContainer(Aspect aspect, int i) {
        MEStorage storage = this.getNetworkStorage();
        AEEssentiaKey key = AEEssentiaKey.of(aspect);
        if (storage == null || key == null || i <= 0) {
            return false;
        }
        IActionSource source = Objects.requireNonNull(this.src, "source");
        long canExtract = storage.extract(key, i, Actionable.SIMULATE, source);
        if (canExtract != i)
            return false;
        storage.extract(key, i, Actionable.MODULATE, source);
        this.refreshVisualState();
        return true;
    }

    @Override
    protected void saveVisualState(NBTTagCompound tag) {
        super.saveVisualState(tag);
        if (this.isClientTile())
            return;
        this.getStoredAspectSnapshot().writeToNBT(tag, TAG_STORED_ASPECTS);
    }

    @Override
    protected void loadVisualState(NBTTagCompound tag) {
        super.loadVisualState(tag);
        if (tag.hasKey(TAG_STORED_ASPECTS)) {
            this.clientAspects = new AspectList();
            this.clientAspects.readFromNBT(tag, TAG_STORED_ASPECTS);
        }
    }

    private static boolean aspectListsEqual(AspectList first, AspectList second) {
        if (first.size() != second.size()) {
            return false;
        }
        for (Aspect aspect : first.getAspects()) {
            if (first.getAmount(aspect) != second.getAmount(aspect)) {
                return false;
            }
        }
        return true;
    }

    private boolean isClientTile() {
        return this.world == null ? ForgeUtil.isClient() : this.world.isRemote;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    @Override
    public boolean doesContainerContainAmount(Aspect aspect, int i) {
        return this.containerContains(aspect) >= i;
    }

    @Override
    public int containerContains(Aspect aspect) {
        return this.getAspects().getAmount(aspect);
    }

    @Override
    public boolean doesContainerAccept(Aspect aspect) {
        return false;
    }

    @Override
    public void setAspects(AspectList aspectList) {
        // Ignored
    }

    @Override
    public int addToContainer(Aspect aspect, int i) {
        // Ignored
        return i;
    }

    @Override
    @Deprecated
    public boolean takeFromContainer(AspectList aspectList) {
        return false;
    }

    @Override
    @Deprecated
    public boolean doesContainerContain(AspectList aspectList) {
        return false;
    }
}
