package thaumicenergistics.mixin.hei;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import mezz.jei.bookmarks.BookmarkItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

@Mixin(value = BookmarkItem.class, remap = false)
public class MixinBookmarkItem {

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lmezz/jei/autocrafting/IngredientUtil;normalizeCopy(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object normalizeAspectList(Object orig, Operation<Object> original) {
        if(orig instanceof AspectList aspects) {
            Aspect[] values = aspects.getAspects();
            if(values.length == 1 && values[0] != null) {
                return new AspectList().add(values[0], 1);
            }
        }
        return original.call(orig);
    }

}
