package thaumicenergistics.mixin.ae2;

import ae2.api.networking.crafting.ICraftingPlan;
import ae2.crafting.CraftingPlan;
import ae2.me.service.CraftingService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import thaumicenergistics.common.crafting.ArcaneVisAccounting;
import thaumicenergistics.common.crafting.ArcaneVisAccountingImpl;
import thaumicenergistics.core.ThELog;

/**
 * Preserves AE2's missing-item submission check, then removes plan-only Vis immediately before CPU mutation calls.
 */
@Mixin(value = CraftingService.class, remap = false)
public abstract class CraftingServiceMixin {

    @Unique
    private static final ArcaneVisAccounting THEENG$VIS_ACCOUNTING = new ArcaneVisAccountingImpl();

    @ModifyArg(
        method = "submitJob(Lae2/api/networking/crafting/ICraftingPlan;"
            + "Lae2/api/networking/crafting/ICraftingRequester;Lae2/api/networking/crafting/ICraftingCPU;Z"
            + "Lae2/api/networking/security/IActionSource;ZZ)"
            + "Lae2/api/networking/crafting/ICraftingSubmitResult;",
        at = @At(
            value = "INVOKE",
            target = "Lae2/me/cluster/implementations/CraftingCPUCluster;mergeJob("
                + "Lae2/api/networking/IGrid;Lae2/api/networking/crafting/ICraftingPlan;"
                + "Lae2/api/networking/security/IActionSource;)"
                + "Lae2/api/networking/crafting/ICraftingSubmitResult;"),
        index = 1,
        require = 2)
    private ICraftingPlan theeng$sanitizeMergedPlan(ICraftingPlan plan) {
        return theeng$sanitizePlan(plan);
    }

    @ModifyArg(
        method = "submitJob(Lae2/api/networking/crafting/ICraftingPlan;"
            + "Lae2/api/networking/crafting/ICraftingRequester;Lae2/api/networking/crafting/ICraftingCPU;Z"
            + "Lae2/api/networking/security/IActionSource;ZZ)"
            + "Lae2/api/networking/crafting/ICraftingSubmitResult;",
        at = @At(
            value = "INVOKE",
            target = "Lae2/me/cluster/implementations/CraftingCPUCluster;submitJob("
                + "Lae2/api/networking/IGrid;Lae2/api/networking/crafting/ICraftingPlan;"
                + "Lae2/api/networking/security/IActionSource;"
                + "Lae2/api/networking/crafting/ICraftingRequester;)"
                + "Lae2/api/networking/crafting/ICraftingSubmitResult;"),
        index = 1,
        require = 1)
    private ICraftingPlan theeng$sanitizeSubmittedPlan(ICraftingPlan plan) {
        return theeng$sanitizePlan(plan);
    }

    @Unique
    private static ICraftingPlan theeng$sanitizePlan(ICraftingPlan plan) {
        if (plan instanceof CraftingPlan concretePlan) {
            return THEENG$VIS_ACCOUNTING.sanitize(concretePlan);
        }
        ThELog.error("AE2 crafting submission received unsupported plan type {}; Arcane Vis cannot be removed",
            plan == null ? "null" : plan.getClass().getName());
        return plan;
    }
}
