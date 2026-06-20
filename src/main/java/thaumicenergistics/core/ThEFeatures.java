package thaumicenergistics.core;

/**
 * Local bootstrap for Thaumic Energistics feature definitions.
 * <p>
 * The bootstrap path intentionally keeps the texture and sound constructors in the local feature graph because those
 * constructors register resource locations used by client stitching and Forge sound registration.
 */
public final class ThEFeatures {

    private ThEFeatures() {
    }

    /**
     * Initializes and returns the shared feature graph during mod startup.
     * <p>
     * Calling this method preserves the startup side effects of constructing texture and sound definitions while keeping
     * the runtime bootstrap independent from the deprecated public API facade.
     *
     * @return shared feature access
     */
    public static ThEFeatureAccess bootstrap() {
        return instance();
    }

    /**
     * Returns the shared feature graph used by both internal code and deprecated external adapters.
     *
     * @return shared feature access
     */
    public static ThEFeatureAccess instance() {
        return Holder.INSTANCE;
    }

    private static final class Holder {
        private static final ThEFeatureAccess INSTANCE = new ThEFeatureAccessImpl();
    }
}
