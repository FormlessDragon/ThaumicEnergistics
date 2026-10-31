package thaumicenergistics.mixin.ae2;

import ae2.api.util.KeyTypeSelection;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumicenergistics.api.IAEKeyViewable;

@Mixin(value = KeyTypeSelection.class, remap = false)
public class KeyTypeSelectionMixin {

    @WrapOperation(method = "<init>(Lae2/api/util/KeyTypeSelection$Listener;Ljava/util/function/Predicate;)V", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/Object2BooleanLinkedOpenHashMap;put(Ljava/lang/Object;Z)Z"))
    private boolean theeng$wrapWithCondition(Object2BooleanLinkedOpenHashMap<?> instance, Object k, boolean v, Operation<Boolean> original) {
        if (k instanceof IAEKeyViewable viewable && !viewable.showInKeyTypeSelection()) {
            return false;
        }

        return original.call(instance, k, v);
    }
}
