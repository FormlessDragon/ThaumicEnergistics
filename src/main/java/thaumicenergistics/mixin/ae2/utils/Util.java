package thaumicenergistics.mixin.ae2.utils;

import thaumicenergistics.api.storage.ReadOnlyPatternContainer;
import thaumicenergistics.mixin.ae2.accessor.PatternAccessSupportTrackerAccessor;

public class Util {

    public static boolean isReadOnlyTracker(Object tracker) {
        return tracker instanceof PatternAccessSupportTrackerAccessor accessor
            && accessor.theeng$getPatternContainer() instanceof ReadOnlyPatternContainer;
    }

}
