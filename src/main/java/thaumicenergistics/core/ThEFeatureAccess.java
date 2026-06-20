package thaumicenergistics.core;

import thaumicenergistics.api.IThEBlocks;
import thaumicenergistics.api.IThEConfig;
import thaumicenergistics.api.IThEItems;
import thaumicenergistics.api.IThELang;
import thaumicenergistics.api.IThESounds;
import thaumicenergistics.api.IThETextures;

/**
 * Internal access point for the Thaumic Energistics feature definitions that are required during normal mod startup.
 * <p>
 * This interface exists so runtime code can depend on the local feature bootstrap instead of the deprecated public API
 * facade. The members mirror the feature groups that were historically created through that facade because those
 * objects carry registration data, localization keys, configuration values, and client resource locations needed by
 * the mod itself.
 */
@SuppressWarnings("deprecation")
public interface ThEFeatureAccess {

    /**
     * Supplies item definitions.
     * <p>
     * The item group is exposed here because legacy API item definitions still participate in startup ordering and
     * external adapters must keep returning the same object graph.
     *
     * @return item definition access
     */
    IThEItems items();

    /**
     * Supplies block and tile definitions.
     * <p>
     * The block group is exposed here because block recipes and registries need the local definitions without going
     * through the deprecated public facade.
     *
     * @return block and tile definition access
     */
    IThEBlocks blocks();

    /**
     * Supplies configuration values.
     * <p>
     * The configuration group is exposed here because containers, tiles, renderers, and GUI components read live mod
     * settings during normal runtime.
     *
     * @return configuration access
     */
    IThEConfig config();

    /**
     * Supplies language keys.
     * <p>
     * The language group is exposed here because tooltips, GUI labels, and tile status text need shared translation
     * keys without depending on deprecated API bootstrap.
     *
     * @return language key access
     */
    IThELang lang();

    /**
     * Supplies texture locations.
     * <p>
     * The texture group is exposed here because constructing it registers the sprite locations used by internal GUI
     * slots and client stitching.
     *
     * @return texture location access
     */
    IThETextures textures();

    /**
     * Supplies sound locations.
     * <p>
     * The sound group is exposed here because constructing it registers the sound identifiers used by Knowledge Core
     * interactions and Forge sound registration.
     *
     * @return sound location access
     */
    IThESounds sounds();
}
