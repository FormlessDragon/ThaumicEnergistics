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
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumicenergistics.api.stacks.EssentiaStack;
import thaumicenergistics.api.ThEApi;
import thaumicenergistics.client.gui.GuiHandler;
import thaumicenergistics.config.AESettings;
import thaumicenergistics.init.ModGUIs;
import thaumicenergistics.integration.appeng.SupergiantEssentiaUtil;
import thaumicenergistics.integration.appeng.ThEPartModel;
import thaumicenergistics.item.part.ItemEssentiaImportBus;
import thaumicenergistics.util.AEUtil;
import thaumicenergistics.util.ForgeUtil;
import thaumicenergistics.util.ThELog;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

/**
 * @author BrockWS
 */
public class PartEssentiaImportBus extends PartSharedEssentiaBus {

    public static ResourceLocation[] MODELS = new ResourceLocation[]{
            new ResourceLocation(Reference.MOD_ID, "part/essentia_import_bus/base"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_import_bus/on"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_import_bus/off"),
            new ResourceLocation(Reference.MOD_ID, "part/essentia_import_bus/has_channel")
    };

    private static final IPartModel MODEL_ON = new ThEPartModel(MODELS[0], MODELS[1]);
    private static final IPartModel MODEL_OFF = new ThEPartModel(MODELS[0], MODELS[2]);
    private static final IPartModel MODEL_HAS_CHANNEL = new ThEPartModel(MODELS[0], MODELS[3]);

    public PartEssentiaImportBus(ItemEssentiaImportBus item) {
        super(item);
    }

    @Override
    protected AESettings.SUBJECT getAESettingSubject() {
        return AESettings.SUBJECT.ESSENTIA_IMPORT_BUS;
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull IGridNode node) {
        return new TickingRequest(ThEApi.instance().config().tickTimeEssentiaImportBusMin(), ThEApi.instance().config().tickTimeEssentiaImportBusMax(), false);
    }

    @Override
    public boolean canWork() {
        return this.getConnectedTE() instanceof IAspectContainer;
    }

    @Override
    protected TickRateModulation doWork() {

        @Nullable
        IAspectContainer container = toAspectContainer(getConnectedTE());

        if (container == null) {
            return TickRateModulation.IDLE;
        }

        @Nullable
        Aspect[] aspects = Optional.of(container)
                .map(IAspectContainer::getAspects)
                .map(AspectList::getAspects)
                .orElse(null);

        if (aspects == null) {
            return TickRateModulation.IDLE;
        }

        MEStorage storage = this.getNetworkStorage();
        if (storage == null) {
            return TickRateModulation.IDLE;
        }

        for (Aspect aspect : aspects) {
            if (this.config.hasAspects() && !this.config.isInFilter(aspect)) // Check filter
                continue;
            EssentiaStack inContainer = new EssentiaStack(aspect, Math.min(container.containerContains(aspect), this.calculateAmountToSend()));

            long simulated = SupergiantEssentiaUtil.insert(storage, aspect, inContainer.getAmount(), Actionable.SIMULATE, this.source);
            if (simulated > 0) {
                int amountToInsert = (int) Math.min(simulated, inContainer.getAmount());
                if (!container.takeFromContainer(inContainer.getAspect(), amountToInsert)) {
                    continue;
                }

                long inserted = SupergiantEssentiaUtil.insert(storage, aspect, amountToInsert, Actionable.MODULATE, this.source);
                if (inserted >= amountToInsert) {
                    return TickRateModulation.FASTER; // Only do one every tick
                }

                int remaining = amountToInsert - (int) inserted;
                if (remaining > 0 && container.addToContainer(aspect, remaining) > 0) {
                    ThELog.warn("EssentiaImportBus could not roll back all {} essentia after a partial ME insert at {}", aspect.getTag(), this.hostTile != null ? this.hostTile.getPos() : "unknown");
                }
                if (inserted > 0) {
                    return TickRateModulation.FASTER; // Only do one every tick
                }
            }
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
            GuiHandler.openGUI(ModGUIs.ESSENTIA_IMPORT_BUS, player, this.hostTile.getPos(), this.side);

        this.host.markForUpdate();
        return true;
    }

    @Override
    public void getBoxes(IPartCollisionHelper box) {
        box.addBox(6, 6, 11, 10, 10, 13);
        box.addBox(5, 5, 13, 11, 11, 14);
        box.addBox(4, 4, 14, 12, 12, 16);
    }
}
