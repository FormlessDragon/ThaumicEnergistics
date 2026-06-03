package thaumicenergistics.part;

import ae2.api.config.Actionable;
import ae2.api.networking.IGridNode;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.parts.IPartCollisionHelper;
import ae2.api.parts.IPartModel;
import ae2.api.storage.MEStorage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;
import thaumicenergistics.integration.appeng.ThEPartModel;
import thaumicenergistics.item.part.ItemEssentiaExportBus;
import thaumicenergistics.util.AEUtil;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ThELog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author BrockWS
 */
public class PartEssentiaExportBus extends PartSharedEssentiaBus {

    public static ResourceLocation[] MODELS = new ResourceLocation[]{
            new ResourceLocation(Reference.MOD_ID, "part/essentia_export_bus/base"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_export_bus/on"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_export_bus/off"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_export_bus/has_channel")
    };

    private static final IPartModel MODEL_ON = new ThEPartModel(MODELS[0], MODELS[1]);
    private static final IPartModel MODEL_OFF = new ThEPartModel(MODELS[0], MODELS[2]);
    private static final IPartModel MODEL_HAS_CHANNEL = new ThEPartModel(MODELS[0], MODELS[3]);

    // FIXME: Remove after issue fixed in TC.
    // https://github.com/Nividica/ThaumicEnergistics/issues/361
    // https://github.com/Azanor/thaumcraft-beta/issues/1604
    private boolean reportedWarning = false;

    public PartEssentiaExportBus(ItemEssentiaExportBus item) {
        super(item);
    }

    @Override
    protected AESettings.SUBJECT getAESettingSubject() {
        return AESettings.SUBJECT.ESSENTIA_EXPORT_BUS;
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(ThEApi.instance().config().tickTimeEssentiaExportBusMin(), ThEApi.instance().config().tickTimeEssentiaExportBusMax(), false);
    }

    @Override
    public boolean canWork() {
        // We only want to run if there is something in the filter
        return toAspectContainer(getConnectedTE()) != null
                && this.config.hasAspects();
    }

    @Override
    protected TickRateModulation doWork() {

        @Nullable
        TileEntity connectedTE = getConnectedTE();

        IAspectContainer container = toAspectContainer(connectedTE);

        if (container == null) {
            return TickRateModulation.IDLE;
        }

        MEStorage storage = this.getNetworkStorage();
        if (storage == null) {
            return TickRateModulation.IDLE;
        }

        for (Aspect aspect : this.config) { // Gather a list of aspects that can be put into the container
            if (aspect == null)
                continue;
            // Can container hold the aspect + does ae2 hold the aspect
            if (!container.doesContainerAccept(aspect))
                continue;

            int amountToSend = this.calculateAmountToSend();
            long available = SupergiantEssentiaUtil.extract(storage, aspect, amountToSend, Actionable.SIMULATE, this.source);
            if (available <= 0)
                continue;

            int requested = (int) Math.min(available, amountToSend);
            long extracted = SupergiantEssentiaUtil.extract(storage, aspect, requested, Actionable.MODULATE, this.source);
            if (extracted <= 0) {
                continue;
            }

            // Try to add to container, since we can't simulate it
            int notAdded;
            // FIXME: Remove after issue fixed in TC.
            // https://github.com/Nividica/ThaumicEnergistics/issues/361
            // https://github.com/Azanor/thaumcraft-beta/issues/1604
            try {
                notAdded = container.addToContainer(aspect, (int) extracted);
            } catch (NullPointerException ignored) {
                SupergiantEssentiaUtil.insert(storage, aspect, extracted, Actionable.MODULATE, this.source);
                if (!reportedWarning)
                    ThELog.warn("container.addToContainer threw a NullPointerException. Thaumcraft Bug. Nividica/ThaumicEnergistics#361. Remove EssentiaExportBus from {}", this.hostTile != null ? this.hostTile.getPos() : connectedTE.getPos());
                reportedWarning = true;
                return TickRateModulation.IDLE;
            }
            reportedWarning = false;
            // Couldn't contain it all
            int accepted = (int) extracted - notAdded;

            if (accepted <= 0) {
                SupergiantEssentiaUtil.insert(storage, aspect, extracted, Actionable.MODULATE, this.source);
                continue;
            }
            if (notAdded > 0) {
                SupergiantEssentiaUtil.insert(storage, aspect, notAdded, Actionable.MODULATE, this.source);
            }
            return TickRateModulation.FASTER; // Only do one every tick
        }

        return TickRateModulation.SLOWER;
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

    @Override
    public boolean onUseItemOn(ItemStack itemStack, EntityPlayer player, EnumHand hand, Vec3d vec3d) {
        if ((player.isSneaking() && AEUtil.isWrench(player.getHeldItem(hand), player, this.getTile().getPos())))
            return false;

        if (ForgeUtil.isServer())
            GuiHandler.openGUI(ModGUIs.ESSENTIA_EXPORT_BUS, player, this.hostTile.getPos(), this.side);

        this.host.markForUpdate();
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper box) {
        box.addBox(4, 4, 12, 12, 12, 14);
        box.addBox(5, 5, 14, 11, 11, 15);
        box.addBox(6, 6, 15, 10, 10, 16);
        box.addBox(6, 6, 11, 10, 10, 12);
    }
}
