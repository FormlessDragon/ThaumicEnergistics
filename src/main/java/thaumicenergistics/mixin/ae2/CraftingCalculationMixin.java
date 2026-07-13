package thaumicenergistics.mixin.ae2;

import ae2.api.networking.IGrid;
import ae2.api.networking.crafting.CalculationStrategy;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingSimulationRequester;
import ae2.api.stacks.GenericStack;
import ae2.crafting.CraftingCalculation;
import ae2.crafting.CraftingPlan;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicenergistics.common.crafting.ArcaneVisAccounting;
import thaumicenergistics.common.crafting.ArcaneVisAccountingImpl;
import thaumicenergistics.common.crafting.ArcaneVisSnapshot;
import thaumicenergistics.common.crafting.ArcaneVisSnapshotFactory;
import thaumicenergistics.common.crafting.ArcaneVisSnapshotFactoryImpl;
import thaumicenergistics.common.crafting.ArcaneVisSnapshotImpl;
import thaumicenergistics.core.ThELog;

/**
 * Captures mutable aura state on the server thread and adds Vis statistics after AE2 finishes its pure calculation.
 */
@Mixin(value = CraftingCalculation.class, remap = false)
public abstract class CraftingCalculationMixin {

    @Unique
    private static final ArcaneVisAccounting THEENG$VIS_ACCOUNTING = new ArcaneVisAccountingImpl();

    @Unique
    private static final ArcaneVisSnapshotFactory THEENG$VIS_SNAPSHOT_FACTORY =
        ArcaneVisSnapshotFactoryImpl.INSTANCE;

    @Unique
    private ArcaneVisSnapshot theeng$visSnapshot = ArcaneVisSnapshotImpl.empty();

    @Inject(
        method = "<init>(Lnet/minecraft/world/World;Lae2/api/networking/IGrid;"
            + "Lae2/api/networking/crafting/ICraftingSimulationRequester;Lae2/api/stacks/GenericStack;"
            + "Lae2/api/networking/crafting/CalculationStrategy;)V",
        at = @At("TAIL"),
        require = 1)
    private void theeng$captureVisSnapshot(
        World level,
        IGrid grid,
        ICraftingSimulationRequester simRequester,
        GenericStack output,
        CalculationStrategy strategy,
        CallbackInfo callback) {
        this.theeng$visSnapshot = THEENG$VIS_SNAPSHOT_FACTORY.capture(
            grid, simRequester.getAdditionalProviders());
    }

    @Inject(
        method = "run()Lae2/api/networking/crafting/ICraftingPlan;",
        at = @At("RETURN"),
        cancellable = true,
        require = 1)
    private void theeng$decoratePlanWithVis(CallbackInfoReturnable<ICraftingPlan> callback) {
        ICraftingPlan result = callback.getReturnValue();
        if (result == null) {
            return;
        }
        if (!(result instanceof CraftingPlan plan)) {
            ThELog.error("AE2 crafting calculation returned unsupported plan type {}; Vis statistics were not added",
                result.getClass().getName());
            return;
        }
        callback.setReturnValue(THEENG$VIS_ACCOUNTING.decorate(plan, this.theeng$visSnapshot));
    }
}
