package thaumicenergistics.mixin.ae2;

import ae2.api.stacks.AEItemKey;
import ae2.container.implementations.PatternAccessSupport;
import ae2.helpers.InventoryAction;
import ae2.helpers.patternprovider.PatternContainer;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumicenergistics.api.storage.ReadOnlyPatternContainer;
import thaumicenergistics.mixin.ae2.utils.Util;

@Mixin(value = PatternAccessSupport.class, remap = false)
public class PatternAccessSupportMixin {

    @Shadow
    @Final
    private Long2ObjectOpenHashMap<?> byId;

    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true, require = 1)
    private void theeng$hideEmptyReadOnlyContainer(PatternContainer container,
                                                   CallbackInfoReturnable<Boolean> callback) {
        if (container instanceof ReadOnlyPatternContainer && container.getTerminalPatternInventory().isEmpty()) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "doAction", at = @At("HEAD"), cancellable = true, require = 1)
    private void theeng$rejectReadOnlyAction(EntityPlayerMP player, InventoryAction action, int slot, long id, CallbackInfoReturnable<Boolean> cir) {
        if (Util.isReadOnlyTracker(this.byId.get(id))) {
            // ContainerPEATerm delegates to its parent when this action is not handled. A read-only
            // provider must consume the request here so that the parent cannot mutate its slot.
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "movePatternToTarget", at = @At("HEAD"), cancellable = true, require = 1)
    private void theeng$rejectReadOnlyQuickMove(EntityPlayerMP player, Slot sourceSlot, AEItemKey sourcePattern,
                                                ReferenceSet<?> usedContainers, @Coerce Object container, int slot,
                                                CallbackInfoReturnable<Boolean> callback) {
        if (Util.isReadOnlyTracker(container)) {
            callback.setReturnValue(false);
        }
    }

}
