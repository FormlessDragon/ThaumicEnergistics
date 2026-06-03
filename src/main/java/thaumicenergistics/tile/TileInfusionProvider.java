package thaumicenergistics.tile;

import ae2.api.config.Actionable;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.nbt.NBTTagCompound;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectSource;
import thaumicenergistics.api.stacks.AEEssentiaKey;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;
import thaumicenergistics.integration.appeng.compat.GridAccessException;
import thaumicenergistics.integration.appeng.grid.GridUtil;
import thaumicenergistics.util.ForgeUtil;

/**
 * @author BrockWS
 */
public class TileInfusionProvider extends TileNetwork implements IAspectSource {

    // Client side only, for rendering aspect icons with goggles
    private AspectList clientAspects = new AspectList();

    public TileInfusionProvider() {
        super();
    }

    public KeyCounter getStoredAspects() {
        try {
            MEStorage storage = GridUtil.getStorageGrid(this).getInventory();
            return SupergiantEssentiaUtil.getAvailableEssentia(storage);
        } catch (GridAccessException e) {
            // Ignore, return an empty list.
            return new KeyCounter();
        }
    }

    @Override
    public AspectList getAspects() {
        if (ForgeUtil.isClient())
            return this.clientAspects;
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
    public boolean takeFromContainer(Aspect aspect, int i) {
        try {
            MEStorage storage = GridUtil.getStorageGrid(this).getInventory();
            long canExtract = SupergiantEssentiaUtil.extract(storage, aspect, i, Actionable.SIMULATE, this.src);
            if (canExtract != i)
                return false;
            SupergiantEssentiaUtil.extract(storage, aspect, i, Actionable.MODULATE, this.src);
            this.markDirty();
        } catch (GridAccessException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(super.getUpdateTag());
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        if (ForgeUtil.isClient())
            return super.writeToNBT(tag);
        super.writeToNBT(tag);
        this.getAspects().writeToNBT(tag, "storedAspects");
        return tag;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("storedAspects")) {
            this.clientAspects = new AspectList();
            this.clientAspects.readFromNBT(tag, "storedAspects");
        }
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
