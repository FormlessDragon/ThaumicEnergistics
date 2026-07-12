package thaumicenergistics.mixin.accessor;

import ae2.helpers.patternprovider.PatternContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the owning pattern container from AE2's private Pattern Access Terminal tracker without reflection.
 *
 * <p>The server interaction mixin needs this association to enforce the read-only marker before AE2 copies or moves a
 * projected pattern.</p>
 */
@Mixin(targets = "ae2.container.implementations.ContainerPatternAccessTerm$ContainerTracker", remap = false)
public interface ContainerPatternAccessTermTrackerAccessor {

    /**
     * Returns the pattern container represented by this private AE2 tracker.
     *
     * @return tracked pattern container
     */
    @Accessor("container")
    PatternContainer theeng$getPatternContainer();
}
