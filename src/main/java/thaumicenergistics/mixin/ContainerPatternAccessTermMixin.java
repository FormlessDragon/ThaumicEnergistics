package thaumicenergistics.mixin;

import ae2.api.stacks.AEItemKey;
import ae2.container.implementations.ContainerPatternAccessTerm;
import ae2.helpers.InventoryAction;
import ae2.helpers.patternprovider.PatternContainer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicenergistics.api.storage.ReadOnlyPatternContainer;
import thaumicenergistics.mixin.accessor.ContainerPatternAccessTermTrackerAccessor;

/**
 * Enforces read-only Knowledge Core projections at AE2's authoritative Pattern Access Terminal mutation boundary.
 */
@Mixin(value = ContainerPatternAccessTerm.class, remap = false)
public abstract class ContainerPatternAccessTermMixin {

    @Shadow
    @Final
    private Long2ObjectOpenHashMap<?> byId;

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true, require = 1)
    private void theeng$hideEmptyReadOnlyContainer(PatternContainer container,
                                                  CallbackInfoReturnable<Boolean> callback) {
        if (container instanceof ReadOnlyPatternContainer
            && container.getTerminalPatternInventory().isEmpty()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "doAction", at = @At("HEAD"), cancellable = true, require = 1)
    private void theeng$rejectReadOnlyAction(EntityPlayerMP player, InventoryAction action, int slot, long id,
                                             CallbackInfo callback) {
        if (theeng$isReadOnlyTracker(this.byId.get(id))) {
            callback.cancel();
        }
    }

    @Inject(method = "movePatternToTarget", at = @At("HEAD"), cancellable = true, require = 1)
    private void theeng$rejectReadOnlyQuickMove(EntityPlayerMP player, Slot sourceSlot, AEItemKey sourcePattern,
                                                ReferenceSet<?> usedContainers, @Coerce Object container, int slot,
                                                CallbackInfoReturnable<Boolean> callback) {
        if (theeng$isReadOnlyTracker(container)) {
            callback.setReturnValue(false);
        }
    }

    @Unique
    private static boolean theeng$isReadOnlyTracker(Object tracker) {
        return tracker instanceof ContainerPatternAccessTermTrackerAccessor accessor
            && accessor.theeng$getPatternContainer() instanceof ReadOnlyPatternContainer;
    }
}
