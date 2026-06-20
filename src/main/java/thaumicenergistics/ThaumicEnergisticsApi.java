package thaumicenergistics;

import thaumicenergistics.api.IThEApi;
import thaumicenergistics.api.IThEBlocks;
import thaumicenergistics.api.IThEConfig;
import thaumicenergistics.api.IThEItems;
import thaumicenergistics.api.IThELang;
import thaumicenergistics.api.IThESounds;
import thaumicenergistics.api.IThETextures;
import thaumicenergistics.core.ThEFeatureAccess;
import thaumicenergistics.core.ThEFeatures;

/**
 * @author BrockWS
 */
@Deprecated
public class ThaumicEnergisticsApi implements IThEApi {

    private static IThEApi INSTANCE;
    private final ThEFeatureAccess features;

    private ThaumicEnergisticsApi() {
        this.features = ThEFeatures.instance();
    }

    public static IThEApi instance() {
        if (INSTANCE == null)
            INSTANCE = new ThaumicEnergisticsApi();
        return INSTANCE;
    }

    @Override
    public IThEItems items() {
        return this.features.items();
    }

    @Override
    public IThEBlocks blocks() {
        return this.features.blocks();
    }

    @Override
    public IThEConfig config() {
        return this.features.config();
    }

    @Override
    public IThELang lang() {
        return this.features.lang();
    }

    @Override
    public IThETextures textures() {
        return this.features.textures();
    }

    @Override
    public IThESounds sounds() {
        return this.features.sounds();
    }
}
