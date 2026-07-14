package thaumicenergistics.part;

import ae2.api.networking.IGridNode;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.parts.IPartItem;
import ae2.api.parts.IPartModel;
import ae2.items.parts.PartModels;
import ae2.parts.p2p.P2PModels;
import ae2.parts.p2p.P2PTunnelPart;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import thaumcraft.api.aura.AuraHelper;
import ae2.api.stacks.AEKeyType;
import thaumicenergistics.thaumicenergistics.Reference;
import thaumicenergistics.util.ArcaneP2PTransfer;

import java.util.List;
import java.util.stream.Collectors;

public class ArcaneP2PTunnelPart extends P2PTunnelPart<ArcaneP2PTunnelPart> implements IGridTickable {

    private static final float DEFAULT_VIS_PER_TICK = 1.0f;
    public static final ResourceLocation MODEL = new ResourceLocation(Reference.MOD_ID, "part/p2p/p2p_tunnel_arcane");
    private static final P2PModels MODELS = new P2PModels(MODEL);

    public ArcaneP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IGridTickable.class, this);
    }

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    @Override
    protected float getPowerDrainPerTick() {
        return 0.5f;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 20, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return this.transferVis() ? TickRateModulation.URGENT : TickRateModulation.SLOWER;
    }

    private boolean transferVis() {
        if (this.isOutput() || !this.getMainNode().isActive()) {
            return false;
        }

        List<ArcaneP2PTunnelPart> outputs = this.getActiveOutputs();
        if (outputs.isEmpty()) {
            return false;
        }

        TileEntity inputTile = this.getTileEntity();
        if (inputTile == null || inputTile.getWorld() == null || this.getSide() == null) {
            return false;
        }
        World inputWorld = inputTile.getWorld();
        BlockPos inputPos = inputTile.getPos().offset(this.getSide());
        float available = AuraHelper.getVis(inputWorld, inputPos);
        if (available <= 0) {
            return false;
        }

        float requested = Math.min(DEFAULT_VIS_PER_TICK, available);
        float drained = AuraHelper.drainVis(inputWorld, inputPos, requested, false);
        if (drained <= 0) {
            return false;
        }

        List<Float> shares = ArcaneP2PTransfer.distribute(drained, outputs.size());
        boolean transferred = false;
        for (int i = 0; i < shares.size(); i++) {
            ArcaneP2PTunnelPart output = outputs.get(i);
            TileEntity outputTile = output.getTileEntity();
            AuraHelper.addVis(outputTile.getWorld(), outputTile.getPos().offset(output.getSide()), shares.get(i));
            transferred = true;
        }
        if (transferred) {
            this.deductTransportCost((long) Math.ceil(drained * 100.0f), AEKeyType.items());
        }
        return transferred;
    }

    private List<ArcaneP2PTunnelPart> getActiveOutputs() {
        return this.getOutputs().stream()
                .filter(output -> output.getMainNode().isActive())
                .filter(output -> output.getTileEntity() != null)
                .filter(output -> output.getTileEntity().getWorld() != null)
                .filter(output -> output.getSide() != null)
                .collect(Collectors.toList());
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

}
