package thaumicenergistics.core;

import thaumicenergistics.api.IThEConfig;
import thaumicenergistics.api.IThESounds;
import thaumicenergistics.api.IThETextures;
import thaumicenergistics.config.ThEConfig;
import thaumicenergistics.init.ThESounds;
import thaumicenergistics.init.ThETextures;

final class ThEFeatureAccessImpl implements ThEFeatureAccess {

    private final IThESounds sounds;
    private final IThEConfig config;
    private final IThETextures textures;

    ThEFeatureAccessImpl() {
        this.sounds = new ThESounds();
        this.config = new ThEConfig();
        this.textures = new ThETextures();
    }

    @Override
    public IThEConfig config() {
        return this.config;
    }

    @Override
    public IThETextures textures() {
        return this.textures;
    }

    @Override
    public IThESounds sounds() {
        return this.sounds;
    }
}
