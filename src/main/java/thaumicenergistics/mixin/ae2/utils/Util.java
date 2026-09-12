package thaumicenergistics.mixin.ae2.utils;

import thaumicenergistics.api.storage.ReadOnlyPatternContainer;
import thaumicenergistics.mixin.ae2.accessor.PatternAccessSessionTrackerAccessor;

public class Util {

    public static boolean isReadOnlyTracker(Object tracker) {
        return tracker instanceof PatternAccessSessionTrackerAccessor accessor
            && accessor.theeng$getPatternContainer() instanceof ReadOnlyPatternContainer;
    }

}
